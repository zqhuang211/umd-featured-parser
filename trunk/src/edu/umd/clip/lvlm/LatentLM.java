/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.lvlm;

import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.util.ScalingTools;
import edu.umd.clip.util.UniCounter;
import edu.umd.clip.jobs.Job;
import edu.umd.clip.jobs.JobGroup;
import edu.umd.clip.jobs.JobManager;
import edu.umd.clip.lvlm.State.StateType;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author zqhuang
 */
public class LatentLM implements Serializable {

    private static final long serialVersionUID = 1L;
    private WordStateManager wordStateManager;
    private double totalLogScore;
    private double totalWordCount;
    public static double MINIMUM_PROB = 10e-40;

    public LatentLM() {
        wordStateManager = new WordStateManager();
    }

    public void calcPerplexity(String testFile) {
        try {
            InputStreamReader streamReader = testFile != null ? new InputStreamReader(new FileInputStream(testFile), Charset.forName("UTF-8")) : new InputStreamReader(System.in, Charset.forName("UTF-8"));
            BufferedReader bufferedReader = new BufferedReader(streamReader);
            String line = "";
            totalLogScore = 0;
            totalWordCount = 0;

            Word sosWord = wordStateManager.getWord("##SOS_WORD##");
            Word eosWord = wordStateManager.getWord("##EOS_WORD##");
            Word sos2Word = wordStateManager.getWord("##SOS2_WORD##");
            Word eos2Word = wordStateManager.getWord("##EOS2_WORD##");

            JobManager jobManager = JobManager.getInstance();
            JobGroup grp = jobManager.createJobGroup("doEMStep");
            int i = 0;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.equals("")) {
                    continue;
                }
                final String finalLine = line;
                String[] stringWords = line.trim().split("\\s+");
                final List<Token> sentence = new ArrayList<Token>();
                sentence.add(new Token(sos2Word));
                sentence.add(new Token(sosWord));
                for (String stringWord : stringWords) {
                    Word word = wordStateManager.getWord(stringWord);
                    sentence.add(new Token(word));
                }
                sentence.add(new Token(eosWord));
                sentence.add(new Token(eos2Word));

                i++;
                Job job = new Job(
                        new Runnable() {

                            public void run() {
                                doBackward(sentence);
                                double logLikelihood = getSentenceLogScore(sentence);
                                if (Double.isInfinite(logLikelihood) || Double.isNaN(logLikelihood)) {
                                    printErrString(String.format("The following sentence is ignored because it has problematic log likelihood score: %f\n%s\n", logLikelihood, finalLine));
                                } else {
                                    addLogScore(logLikelihood);
                                    addWordCount(sentence.size() - 4);
                                }
                            }
                        },
                        String.valueOf(i) + "-th sentence");
                job.setPriority(i);
                jobManager.addJob(grp, job);
            }
            grp.join();
            bufferedReader.close();
            double perplexity = Math.pow(2, -totalLogScore / totalWordCount / ArrayMath.LOG2);
            System.out.format("Total word count: %.0f\nOveral likelihood: %.6f\nAverage likelihood: %.6f\nPerplexity: %.6f\n",
                    totalWordCount,
                    totalLogScore,
                    totalLogScore / totalWordCount,
                    perplexity);
        } catch (IOException ex) {
            Logger.getLogger(LatentLM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Load training data from the specified file or stdin, and convert each
     * sentence to a list of {@link Token}s structured for the EM algorithm. SOS
     * and EOS tokens are appended to the begining and ending of each sentence;
     * rare words are replaced with UNK.
     *
     * @param trainingFile input plain text file with one sentence per line
     * @return list of list of tokens
     */
    public List<List<Token>> loadTrainingData(String trainingFile, int unkThreshold) {
        try {
            InputStreamReader streamReader = trainingFile != null ? new InputStreamReader(new FileInputStream(trainingFile), Charset.forName("UTF-8")) : new InputStreamReader(System.in, Charset.forName("UTF-8"));
            BufferedReader bufferedReader = new BufferedReader(streamReader);
            String line = "";
            UniCounter<String> wordCount = new UniCounter<String>();
            List<List<String>> stringTrainingData = new ArrayList<List<String>>();
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.equals("")) {
                    continue;
                }

                String[] words = line.trim().split("\\s+");
                List<String> stringSentence = new ArrayList<String>();
                for (int i = 0; i < words.length; i++) {
                    stringSentence.add(words[i]);
                    wordCount.incrementCount(words[i], 1);
                }

                stringTrainingData.add(stringSentence);
            }

            bufferedReader.close();

            List<List<Token>> trainingData = new ArrayList<List<Token>>();
            Word sosWord = wordStateManager.getOrCreateWord("##SOS_WORD##", false, StateType.aux);
            Word eosWord = wordStateManager.getOrCreateWord("##EOS_WORD##", false, StateType.aux);
            Word unkWord = wordStateManager.getOrCreateWord("##UNK_WORD##", true, StateType.unk);
            Word sos2Word = wordStateManager.getOrCreateWord("##SOS2_WORD##", false, StateType.aux);
            Word eos2Word = wordStateManager.getOrCreateWord("##EOS2_WORD##", false, StateType.aux);
//            wordStateManager.getOrCreateWord("##UNI_WORD##", false, StateType.uni);
            for (List<String> stringSentence : stringTrainingData) {
                List<Token> sentence = new ArrayList<Token>();
                sos2Word.addWordCount(1);
                sentence.add(new Token(sos2Word));
                sosWord.addWordCount(1);
                sentence.add(new Token(sosWord));
                for (String stringWord : stringSentence) {
                    Word word = null;
                    if (wordCount.getCount(stringWord) <= unkThreshold + 0.01) {
                        word = unkWord;
                    } else {
                        word = wordStateManager.getOrCreateWord(stringWord, true, StateType.kwn);
                    }

                    word.addWordCount(1);
                    sentence.add(new Token(word));
                }

                eosWord.addWordCount(1);
                sentence.add(new Token(eosWord));
                eos2Word.addWordCount(1);
                sentence.add(new Token(eos2Word));

                trainingData.add(sentence);
            }
//            wordStateManager.addUniversalStateToWords();
            return trainingData;
        } catch (IOException ex) {
            Logger.getLogger(LatentLM.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Add log likelihood score of the parellel EM training algorithm
     * @param score
     */
    private synchronized void addLogScore(double score) {
        totalLogScore += score;
    }

    private synchronized void addWordCount(int count) {
        totalWordCount += count;
    }

    private synchronized void printErrString(String str) {
        System.err.println(str);
    }

    public void doInitializationStep(List<List<Token>> trainingData) {
        wordStateManager.resetCounts();
        JobManager jobManager = JobManager.getInstance();
        JobGroup grp = jobManager.createJobGroup("doEMStep");
        int i = 0;
        for (final List<Token> sentence : trainingData) {
            i++;
            Job job = new Job(
                    new Runnable() {

                        public void run() {
                            tallyInitialCounts(sentence);
                        }
                    },
                    String.valueOf(i) + "-th sentence");
            job.setPriority(i);
            jobManager.addJob(grp, job);
        }

        grp.join();
        wordStateManager.calcKneserNeySmoothingParams();
        wordStateManager.updateCounts(false);
    }

    /**
     * Run one EM iteration. If calcTrigramCounts is true, the trigram emission
     * and transition statistics are also calculated for computing conditional
     * entropy in the splitting step. The E-step is explicitly carried out but
     * the M-step is not and is implicitly carried when the probabilities are
     * needed in computation
     *
     * @param trainingData
     * @param calcTrigramCounts
     * @return
     */
    public double doEMStep(List<List<Token>> trainingData, final boolean calcTrigramCounts) {
        wordStateManager.resetCounts();
        totalLogScore = 0;

        JobManager jobManager = JobManager.getInstance();
        JobGroup grp = jobManager.createJobGroup("doEMStep");
        int i = 0;
        for (final List<Token> sentence : trainingData) {
            i++;
            Job job = new Job(
                    new Runnable() {

                        public void run() {
                            doForward(sentence);
                            doBackward(sentence);
                            double logScore = getSentenceLogScore(sentence);
                            addLogScore(logScore);
                            tallyBigramCounts(sentence);
                            if (calcTrigramCounts) {
                                tallyTrigramCounts(sentence);
                            }

                            resetScores(sentence);
                        }
                    },
                    String.valueOf(i) + "-th sentence");
            job.setPriority(i);
            jobManager.addJob(grp, job);
        }

        grp.join();

        wordStateManager.updateCounts(true);
        return totalLogScore;
    }

    /**
     * Split some of the states into two.
     */
    public void doSplitting() {
        wordStateManager.doSplitting();
    }

    public void setSplittingRate(double splittingRate) {
        wordStateManager.setSplittingRate(splittingRate);
    }

    private static double getSentenceLogScore(List<Token> sentence) {
        Token sosToken = sentence.get(1);
        return Math.log(sosToken.getBackwardScores()[0]) + ScalingTools.getLogScale(sosToken.getBackwardScale());
    }

    public void resetScores(List<Token> sentence) {
        for (Token token : sentence) {
            token.reset();
        }

    }

    /**
     * Run forward part of the E-step
     *
     * @param sentence
     */
    public void doForward(List<Token> sentence) {
        int tokenNum = sentence.size();

        Token sosToken = sentence.get(1);
        Word sosWord = sosToken.getWord();
        int sosStateNum = sosWord.getStateNum();
        if (sosStateNum != 1) {
            throw new Error("sosStateNum != 1");
        }
        double[] sosForwardScores = new double[1];
        sosForwardScores[0] = 1;

        sosToken.setForwardScores(sosForwardScores);
        sosToken.setForwardScale(0);

        for (int i = 2; i < tokenNum - 1; i++) {
            Token prev2Token = sentence.get(i - 2);
            Word prev2Word = prev2Token.getWord();

            Token prevToken = sentence.get(i - 1);
            Word prevWord = prevToken.getWord();
            int prevStateNum = prevToken.getStateNum();

            Token currToken = sentence.get(i);
            Word currWord = currToken.getWord();
            int currStateNum = currToken.getStateNum();

            double[] prevForwardScores = prevToken.getForwardScores();
            double[] currForwardScores = new double[currStateNum];

            double trigramTransitionProb = prevWord.getTrigramWordTransitionProb(prev2Word, currWord);
            double kneserNeyBigramLambda = prevWord.getKneserNeyBigramLambda(prev2Word);
            double[][] transitionProbs = prevWord.getTransitionProbs(currWord);
            double kneserNeyDiscountedFraction = prevWord.getKneserNeyBigramDiscountedFraction(currWord);
            double[] stateProbs = currWord.getStateProbs();
            double[] latentKneserNeyLambda = prevWord.getLatentKneserNeyLambda();
            double prevKneserNeyUnigramLambda = prevWord.getKneserNeyUnigramLambda();
            double kneserNeyUnigramProb = currWord.getKneserNeyUnigramProb();


            for (int csi = 0; csi < currStateNum; csi++) {
                for (int psi = 0; psi < prevStateNum; psi++) {
                    currForwardScores[csi] +=
                            prevForwardScores[psi] *
                            (trigramTransitionProb / currStateNum + kneserNeyBigramLambda *
                            (kneserNeyDiscountedFraction * latentKneserNeyLambda[psi] * transitionProbs[psi][csi] +
                            prevKneserNeyUnigramLambda * kneserNeyUnigramProb * stateProbs[csi]));
                }
            }

            int currForwardScale = ScalingTools.scaleArray(currForwardScores, prevToken.getForwardScale());
            currToken.setForwardScores(currForwardScores);
            currToken.setForwardScale(currForwardScale);
        }

    }

    /**
     * Run backward part of the E-step
     * 
     * @param sentence
     */
    public void doBackward(List<Token> sentence) {
        int tokenNum = sentence.size();
        Token endToken = sentence.get(tokenNum - 2);
        double[] endBackwardScores = new double[1];
        endBackwardScores[0] = 1;

        endToken.setBackwordScores(endBackwardScores);
        endToken.setBackwardScale(0);

        for (int i = tokenNum - 3; i >= 1; i--) {
            Token nextToken = sentence.get(i + 1);
            Word nextWord = nextToken.getWord();
            int nextStateNum = nextToken.getStateNum();

            Token currToken = sentence.get(i);
            Word currWord = currToken.getWord();
            int currStateNum = currToken.getStateNum();

            Token prevToken = sentence.get(i - 1);
            Word prevWord = prevToken.getWord();

            double[] nextBackwardScores = nextToken.getBackwardScores();
            double[] currBackwardScores = new double[currStateNum];

            double trigramTransitionProb = currWord.getTrigramWordTransitionProb(prevWord, nextWord);
            double kneserNeyBigramLambda = currWord.getKneserNeyBigramLambda(prevWord);
            double[][] transitionScores = currWord.getTransitionProbs(nextWord);
            double[] stateProbs = nextWord.getStateProbs();
            double kneserNeyDiscountedFraction = currWord.getKneserNeyBigramDiscountedFraction(nextWord);
            double[] latentKneserNeyLambda = currWord.getLatentKneserNeyLambda();
            double kneserNeyUnigramProb = nextWord.getKneserNeyUnigramProb();
            double kneserNeyUnigramLambda = currWord.getKneserNeyUnigramLambda();

            for (int csi = 0; csi < currStateNum; csi++) {
                for (int nsi = 0; nsi < nextStateNum; nsi++) {
                    if (transitionScores == null) {
                        currBackwardScores[csi] += nextBackwardScores[nsi] *
                                (trigramTransitionProb / nextStateNum +
                                kneserNeyBigramLambda *
                                (kneserNeyUnigramLambda * kneserNeyUnigramProb * stateProbs[nsi]));
                    } else {
                        currBackwardScores[csi] += nextBackwardScores[nsi] *
                                (trigramTransitionProb / nextStateNum +
                                kneserNeyBigramLambda *
                                (latentKneserNeyLambda[csi] * kneserNeyDiscountedFraction * transitionScores[csi][nsi] +
                                kneserNeyUnigramLambda * kneserNeyUnigramProb * stateProbs[nsi]));
                    }
                }
            }

            int currBackwardScale = ScalingTools.scaleArray(currBackwardScores, nextToken.getBackwardScale());
            currToken.setBackwordScores(currBackwardScores);
            currToken.setBackwardScale(currBackwardScale);
        }

    }

    /**
     * Tally initial bigram and trigram statistics. Each instance of bigram or
     * trigram contribute one count.
     *
     * @param sentence
     */
    public void tallyInitialCounts(List<Token> sentence) {
        int tokenNum = sentence.size();

//        State sos2State = wordStateManager.getState(3);
//        State eos2State = wordStateManager.getState(4);
//        sos2State.addUnigramStateCount(1);
//        sosState.addEmissionCount(sentence.get(0).getWord(), 1);

        for (int i = 1; i < tokenNum - 1; i++) {
            Token prevToken = sentence.get(i - 1);
            Token currToken = sentence.get(i);
            Token nextToken = sentence.get(i + 1);

            Word prevWord = prevToken.getWord();
            Word currWord = currToken.getWord();
            Word nextWord = nextToken.getWord();

            List<State> prevStateList = prevToken.getStateList();
            List<State> currStateList = currToken.getStateList();
            List<State> nextStateList = nextToken.getStateList();

//            int prevStateNum = prevStateList.size();
            int currStateNum = currStateList.size();
            int nextStateNum = nextStateList.size();
            for (State currState : currStateList) {
                currState.addUnigramStateCount(1.0 / currStateNum);
//                currState.addEmissionCount(currWord, 1.0 / currStateNum);
                for (State nextState : nextStateList) {
                    currState.addTransitionCount(nextState, 1.0 / (currStateNum * nextStateNum));
                }

//                for (State prevState : prevStateList) {
////                    currState.addEmissionCount(prevState, currWord, 1.0 / (prevStateNum * currStateNum));
//                    for (State nextState : nextStateList) {
//                        currState.addTransitionCount(prevState, nextState, 1.0 / (prevStateNum * currStateNum * nextStateNum));
//                    }
//                }
            }

            currWord.addTransition(nextWord);
            currWord.addTrigramWordTransition(prevWord, nextWord);
        }
//        eosState.addEmissionCount(sentence.get(tokenNum - 1).getWord(), 1);
    }

    /**
     * Tally bigram statistics
     *
     * @param sentence
     */
    public void tallyBigramCounts(List<Token> sentence) {
        int tokenNum = sentence.size();
        Token sosToken = sentence.get(1);
        double sentenceScore = sosToken.getBackwardScores()[0];
        int sentenceScale = sosToken.getBackwardScale();

        for (int i = 1; i < tokenNum - 2; i++) {
            Token prevToken = sentence.get(i - 1);
            Token currToken = sentence.get(i);
            Token nextToken = sentence.get(i + 1);
            currToken.tallyTransitionCount(prevToken, nextToken, sentenceScore, sentenceScale);
        }
    }

    /**
     * Tally trigram statistics
     *
     * @param sentence
     */
    public void tallyTrigramCounts(List<Token> sentence) {
        int tokenNum = sentence.size();
        Token startToken = sentence.get(1);
        double sentenceScore = startToken.getBackwardScores()[0];
        int sentenceScale = startToken.getBackwardScale();

        for (int i = 2; i < tokenNum - 2; i++) { // start with i = 1 because we don't need trigram probabilities for SOS
            Token currToken = sentence.get(i);
            Token prevToken = sentence.get(i - 1);
            Token prev2Token = sentence.get(i - 2);
            Token nextToken = sentence.get(i + 1);
            currToken.tallyTransitionCount(prev2Token, prevToken, nextToken, sentenceScore, sentenceScale);
        }
// we also don't need any trigram probabilities for i=totalNum-1
    }

    /**
     * Save the language model into a file
     * 
     * @param modelFile
     * @return
     */
    public boolean save(String modelFile) {
        try {
            FileOutputStream fos = new FileOutputStream(modelFile); // save to file
            GZIPOutputStream gzos = new GZIPOutputStream(fos); // compressed
            ObjectOutputStream out = new ObjectOutputStream(gzos);
            out.writeObject(this);
            out.flush();
            out.close();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            System.out.println("StatckTrace: ");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static LatentLM load(String modelFile) {
        LatentLM latentLM = null;
        try {
            FileInputStream fis = new FileInputStream(modelFile);
            GZIPInputStream gzis = new GZIPInputStream(fis);
            ObjectInputStream in = new ObjectInputStream(gzis);

            latentLM = (LatentLM) in.readObject();
            in.close();
        } catch (IOException e) {
            System.out.println("IOException\n" + e);
            System.out.println("StackTrace");
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found!");
            return null;
        }
        return latentLM;
    }
}
