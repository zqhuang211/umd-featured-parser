/*
 * LatentTrainer.java
 *
 * Created on May 21, 2007, 11:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.util.Numberer;
import edu.umd.clip.util.UniCounter;
import edu.umd.clip.jobs.Job;
import edu.umd.clip.jobs.JobGroup;
import edu.umd.clip.jobs.JobManager;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author
 */
public class LatentTrainer {

    private static final long serialVersionUID = 1L;
    public static final double SCALE = Math.exp(30);
    public static final double SMOOTHING_ALPHA = 0.01;
    private LatentEmission latentEmission;
    private LatentTransition latentTransition;
    private Collection<AlphaBetaSequence> trainingCorpus;
    private double mergeRate;
    private double logLikelihood = 0.0;

    public Collection<AlphaBetaSequence> getTrainingCorpus() {
        return trainingCorpus;
    }

    public void loadTrainingData(String trainingFile, int unk) {
        try {
            WordTagItem sosWordTagItem = new WordTagItem("***SOS_WORD***", Numberer.number("tags", "SOS"));
            WordTagItem eosWordTagItem = new WordTagItem("***EOS_WORD***", Numberer.number("tags", "EOS"));

            TagBankReader tagBankReader = new TagBankReader(new BufferedReader(new InputStreamReader(new FileInputStream(trainingFile), Charset.forName("UTF8"))));
            List<WordTagSequence> wordTagSequences = new ArrayList<WordTagSequence>();

            UniCounter<String> wordCounts = new UniCounter<String>();
            while (tagBankReader.hasNext()) {
                WordTagSequence sequence = tagBankReader.next();
                wordTagSequences.add(sequence);
                for (WordTagItem item : sequence) {
                    wordCounts.incrementCount(item.getWord(), 1);
                }
            }
            LatentTagStates.initLatentTagStates();

            trainingCorpus = new ArrayList<AlphaBetaSequence>();
            for (WordTagSequence wordTagSequence : wordTagSequences) {
                AlphaBetaSequence alphaBetaSequence = new AlphaBetaSequence();
                alphaBetaSequence.add(new AlphaBetaItem(sosWordTagItem));
                for (WordTagItem wordTagItem : wordTagSequence) {
                    if (wordCounts.getCount(wordTagItem.getWord()) <= unk + 0.1) {
                        wordTagItem.setWord("***UNK_WORD***");
                    }
                    alphaBetaSequence.add(new AlphaBetaItem(wordTagItem));
                }
                alphaBetaSequence.add(new AlphaBetaItem(eosWordTagItem));
                trainingCorpus.add(alphaBetaSequence);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LatentTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public LatentTrainer(LatentEmission latentEmission, LatentTransition latentTransition, double mergeRate) {
        this.latentEmission = latentEmission;
        this.latentTransition = latentTransition;
        this.mergeRate = mergeRate;
    }

    public void splitStates() {
        LatentTagStates.splitStates();
        latentEmission.splitStates();
        latentTransition.splitStates();
    }

    public void setSmoothingFlag(boolean smoothingFlag) {
        latentEmission.setLatentSmoothingFlag(smoothingFlag);
        latentTransition.setLatentSmoothingFlag(smoothingFlag);
    }

    private void resetAlphaBetaSequenceScores(AlphaBetaSequence sequence) {
        for (AlphaBetaItem item : sequence) {
            item.resetAlphaBetaScores();
        }
    }

    private void clearAlphaBetaSequenceScores(AlphaBetaSequence sequence) {
        for (AlphaBetaItem item : sequence) {
            item.clearAlphaBetaScores();
        }
    }

    protected synchronized void addLL(double ll) {
        logLikelihood += ll;
    }

    public double doExpectationStep() {
        logLikelihood = 0.0;

        latentEmission.resetLatentCount();
        latentTransition.resetLatentCount();

        JobManager jobManager = JobManager.getInstance();
        try {
            JobGroup grp = jobManager.createJobGroup("doExpectatoinStep");
            int i = 0;
            for (final AlphaBetaSequence alphaBetaSequence : trainingCorpus) {
                Job job = new Job(
                        new Runnable() {

                            public void run() {
                                resetAlphaBetaSequenceScores(alphaBetaSequence);
                                double ll = doExpectationStep(alphaBetaSequence);
                                addLL(ll);
                                clearAlphaBetaSequenceScores(alphaBetaSequence);
                            }
                        },
                        "a sentence");
                job.setPriority(i++);
                jobManager.addJob(grp, job);
            }
            grp.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return logLikelihood;
    }

    public double doExpectationStep(AlphaBetaSequence sequence) {
        doForawardBackward(sequence);
        AlphaBetaItem lastAlphaBetaItem = sequence.get(sequence.size() - 1);
        double sentenceScore = computeSentenceScore(lastAlphaBetaItem);
        double weight = sequence.getWeight();


        double sentenceScale = lastAlphaBetaItem.getAlphaScale();
        double ll = (Math.log(sentenceScore) + Math.log(SCALE) * sentenceScale) * weight;
        sentenceScore /= weight;

        for (int i = 1; i < sequence.size(); i++) {
            AlphaBetaItem prevAlphaBetaItem = sequence.get(i - 1);
            AlphaBetaItem currAlphaBetaItem = sequence.get(i);
            increaseAllLatentCount(prevAlphaBetaItem, currAlphaBetaItem, sentenceScore, sentenceScale);
        }
        return ll;
    }

    private void increaseAllLatentCount(AlphaBetaItem prevAlphaBetaItem, AlphaBetaItem currAlphaBetaItem,
            double sentenceScore, double sentenceScale) {
        int prevTag = prevAlphaBetaItem.getTag();
        int currTag = currAlphaBetaItem.getTag();
        String currWord = currAlphaBetaItem.getWord();

        double[] emissionProb = null;
        double[][] transitionProb = null;

        emissionProb = latentEmission.getLatentProb(currTag, currWord);
        transitionProb = latentTransition.getLatentProb(prevTag, currTag);

        SentenceLatentBigram sentenceLatentBigram = new SentenceLatentBigram(prevAlphaBetaItem, currAlphaBetaItem, emissionProb, transitionProb, sentenceScore, sentenceScale);
        SentenceLatentUnigram sentenceLatentUnigram = new SentenceLatentUnigram(currAlphaBetaItem, sentenceScore, sentenceScale);


        latentEmission.addLatentCount(currTag, currWord, sentenceLatentUnigram.getUnigramScore());
        latentTransition.addLatentCount(prevTag, currTag,
                sentenceLatentBigram.getBigramScore(), sentenceLatentUnigram.getUnigramScore());
    }

    public void doMaximizationStep() {
        latentEmission.updateLatentProbability();
        latentTransition.updateLatentProbability();
    }

    public void doForawardBackward(AlphaBetaSequence sentence) {
        doForward(sentence);
        doBackward(sentence);
    }

    public void doForward(AlphaBetaSequence sentence) {
        AlphaBetaItem prevAlphaBetaItem = null;
        for (int i = 0; i < sentence.size(); i++) {
            AlphaBetaItem currAlphaBetaItem = sentence.get(i);
            if (i == 0) {
                double[] alphaScores = new double[1];
                alphaScores[0] = 1;
                currAlphaBetaItem.setAlphaScores(alphaScores);
                currAlphaBetaItem.scaleAlphaScores(0);
                prevAlphaBetaItem = currAlphaBetaItem;
            } else {
                int prevTag = prevAlphaBetaItem.getTag();
                int currTag = currAlphaBetaItem.getTag();
                String currWord = currAlphaBetaItem.getWord();

                int prevTagStateNum = prevAlphaBetaItem.getTagStateNum();
                int currTagStateNum = currAlphaBetaItem.getTagStateNum();
                double[] emissionProb = null;
                double[][] transitionProb = null;

                emissionProb = latentEmission.getLatentProb(currTag, currWord);
                transitionProb = latentTransition.getLatentProb(prevTag, currTag);

                double[] prevAlphaScores = prevAlphaBetaItem.getAlphaScores();
                double[] currAlphaScores = new double[currTagStateNum];
                for (int cts = 0; cts < currTagStateNum; cts++) {
                    double score = 0;
                    for (int pts = 0; pts < prevTagStateNum; pts++) {
                        score += prevAlphaScores[pts] * transitionProb[pts][cts];
                    }
                    score *= emissionProb[cts];
                    currAlphaScores[cts] = score;
                }
                currAlphaBetaItem.setAlphaScores(currAlphaScores);
                currAlphaBetaItem.scaleAlphaScores(prevAlphaBetaItem.getAlphaScale());
                prevAlphaBetaItem = currAlphaBetaItem;
            }
        }
    }

    public void doBackward(AlphaBetaSequence sentence) {
        AlphaBetaItem nextAlphaBetaItem = null;
        for (int i = sentence.size() - 1; i >= 0; i--) {
            AlphaBetaItem currAlphaBetaItem = sentence.get(i);
            if (i == sentence.size() - 1) {
                double[] betaScores = new double[1];
                betaScores[0] = 1;
                currAlphaBetaItem.setBetaScores(betaScores);
                currAlphaBetaItem.scaleBetaScores(0);
                nextAlphaBetaItem = currAlphaBetaItem;
            } else {
                int currTag = currAlphaBetaItem.getTag();
                int nextTag = nextAlphaBetaItem.getTag();
                String nextWord = nextAlphaBetaItem.getWord();

                int currTagStateNum = currAlphaBetaItem.getTagStateNum();
                int nextTagStateNum = nextAlphaBetaItem.getTagStateNum();

                double[] emissionProb = null;
                double[][] transitionProb = null;

                emissionProb = latentEmission.getLatentProb(nextTag, nextWord);
                transitionProb = latentTransition.getLatentProb(currTag, nextTag);

                double[] nextBetaScores = nextAlphaBetaItem.getBetaScores();
                double[] currBetaScores = new double[currTagStateNum];
                for (int cts = 0; cts < currTagStateNum; cts++) {
                    double score = 0;
                    for (int nts = 0; nts < nextTagStateNum; nts++) {
                        score += transitionProb[cts][nts] *
                                emissionProb[nts] * nextBetaScores[nts];
                    }
                    currBetaScores[cts] = score;
                }
                currAlphaBetaItem.setBetaScores(currBetaScores);
                currAlphaBetaItem.scaleBetaScores(nextAlphaBetaItem.getBetaScale());
                nextAlphaBetaItem = currAlphaBetaItem;
            }
        }
    }

    public static double computeSentenceScore(AlphaBetaItem lastAlphaBetaItem) {
        double score = 0;
        double[] scores = lastAlphaBetaItem.getAlphaScores();
        for (int csi = 0; csi < scores.length; csi++) {
            score += scores[csi];
        }
        return score;
    }

    public void mergeStates() {
        boolean[][] latentStateMergeSignal = LatentTagStates.getLatentStateMergeSignal();
        int tagSetSize = latentStateMergeSignal.length;
        double[][] mergeLoss = new double[tagSetSize][];
        for (int i = 0; i < tagSetSize; i++) {
            if (latentStateMergeSignal[i] != null) {
                mergeLoss[i] = new double[latentStateMergeSignal[i].length];
            }
        }

        for (AlphaBetaSequence alphaBetaSequence : trainingCorpus) {
            /* step 1: do expectation step, accumulate statistics */
            resetAlphaBetaSequenceScores(alphaBetaSequence);
            doExpectationStep(alphaBetaSequence);

            /* step 2: compute the loss of merging each pair of new states */
            for (int i = 1; i < alphaBetaSequence.size() - 1; i++) {
                AlphaBetaItem alphaBetaItem = alphaBetaSequence.get(i);
                accumulateMergeLoss(alphaBetaItem, mergeLoss);
            }
            clearAlphaBetaSequenceScores(alphaBetaSequence);
        }

        /* step 3: compute the merging threshold, and mark states for merging */
        List<Double> mergeLossList = new ArrayList<Double>();
        for (int i = 0; i < tagSetSize; i++) {
            if (mergeLoss[i] == null) {
                continue;
            }
            for (int j = 0; j < mergeLoss[i].length; j++) {
                mergeLossList.add(mergeLoss[i][j]);
            }
        }
        Collections.sort(mergeLossList);
        double mergeThreshold = mergeLossList.get((int) (mergeLossList.size() * mergeRate));


        for (int i = 0; i < tagSetSize; i++) {
            if (mergeLoss[i] == null) {
                continue;
            }
            for (int j = 0; j < mergeLoss[i].length; j++) {
                if (mergeLoss[i][j] < mergeThreshold) {
                    LatentTagStates.setLatentStateMergeSignal(i, j, true);
                }
            }
        }

        /* step 4: merge and update state statistics*/
        latentEmission.mergeStates();
        latentTransition.mergeStates();
        LatentTagStates.mergeStates();

        /* step 5: do maximiziaiton step */
        latentEmission.updateLatentProbability();
        latentTransition.updateLatentProbability();
    }

    private void accumulateMergeLoss(AlphaBetaItem alphaBetaItem, double[][] mergeLoss) {
        int tag = alphaBetaItem.getTag();
        if (mergeLoss[tag] == null) {
            return;
        }
        int tagStateNum = alphaBetaItem.getTagStateNum();
        if (tagStateNum % 2 != 0) {
            throw new RuntimeException("Error: I cannot handle this: the tagStateNum is not 0 module 2.");
        }
        double oriLikelihood = 0;
        double[] alphaScores = alphaBetaItem.getAlphaScores();
        double[] betaScores = alphaBetaItem.getBetaScores();
        for (int ts = 0; ts < tagStateNum; ts++) {
            oriLikelihood += alphaScores[ts] * betaScores[ts];
        }

        double[] unigramCount = latentTransition.getUnigramCount(tag);
        for (int ots = 0; ots < tagStateNum / 2; ots++) {
            int ts1 = 2 * ots;
            int ts2 = ts1 + 1;
            double p1 = unigramCount[ts1] / (unigramCount[ts1] + unigramCount[ts2]);
            double p2 = unigramCount[ts2] / (unigramCount[ts1] + unigramCount[ts2]);

            double score1 = alphaScores[ts1] * betaScores[ts1] +
                    alphaScores[ts2] * betaScores[ts2];
            double score2 = (alphaScores[ts1] + alphaScores[ts2]) *
                    (p1 * betaScores[ts1] + p2 * betaScores[ts2]);

            double diff = oriLikelihood / (oriLikelihood - score1 + score2);
            mergeLoss[tag][ots] += Math.log(diff);
        }
    }
}
