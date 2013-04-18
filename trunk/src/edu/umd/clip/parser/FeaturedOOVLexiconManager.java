/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.jobs.Job;
import edu.umd.clip.jobs.JobGroup;
import edu.umd.clip.jobs.JobManager;
import edu.umd.clip.math.LBFGS;
import static edu.umd.clip.parser.Grammar.Language.arabic;
import static edu.umd.clip.parser.Grammar.Language.chinese;
import static edu.umd.clip.parser.Grammar.Language.english;
import edu.umd.clip.util.BiCounter;
import edu.umd.clip.util.BiSet;
import edu.umd.clip.util.GlobalLogger;
import edu.umd.clip.util.NBestList;
import edu.umd.clip.util.UniCounter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

/**
 *
 * @author zqhuang
 */
public class FeaturedOOVLexiconManager implements Serializable {

    private static final long serialVersionUID = 1L;
    protected HashMap<String, double[]> tagGivenOOVProbs;
    private static final Object oovLock = new Object();
    private Map<String, double[]> tagGivenWordProbsMap;
    private Map<String, double[]> wordGivenTagProbsMap;
    private Map<String, double[]> wordTagCountsMap;
    private Map<String, double[]> wordTagDiffCountsMap;
    private Map<String, double[]> predTagWeightMap;
    private Map<String, DoubleArray> predTagObservationMap;
    private Map<String, DoubleArray> predTagExpectationMap;
    private List<PredicateExtractor> predicateExtractorList;
    private List<PredicateExtractor> freqPredicateExtractorList;
    private List<PredicateExtractor> rarePredicateExtractorList;
    private Set<String> observedPredSet;
    private UniCounter<String> wordCountsMap;
    private UniCounter<String> wordDiffCountsMap;
    private BiSet<String, String> wordPredSet;
    private BiSet<String, Integer> observedPredTagSet;
    private BiSet<Integer, String> observedTagWordSet;
    private BiSet<String, Integer> observedWordTagSet;
    private BiSet<String, Integer> possibleWordTagSet;
    private BiSet<Integer, String> possibleTagWordSet;
    private BiSet<Integer, String> observedTagPredSet;
    private DoubleArray expectedTagCounts;
    private DoubleArray observedTagCounts;
    private Grammar.Language lang;
    private boolean wordPredPredefined;
    private double objectiveScore = 0;
    private int numFeats = 0;
    private int iterNum = 100;
    private FeaturedLexiconManager featuredLexicon;
    private int numNodes;
    private double lexRegWeight = 0.005;
    private double rareWordThreshold = 5;
    private double[] globalWeights;
    private double[] globalGradients;
    private int topTagSize = 10;
    private double topTagProbPercentage = 0.9;

    public FeaturedOOVLexiconManager(FeaturedLexiconManager featuredLexicon) {
        this.featuredLexicon = featuredLexicon;
        this.lang = featuredLexicon.getLanguage();
    }

    public void setupGrammar(Grammar grammar) {
        numNodes = grammar.getNumNodes();
    }

    public void setupArray() {
        tagGivenOOVProbs = new HashMap<String, double[]>();
        clear();
    }

    public void setIterNum(int iterNum) {
        this.iterNum = iterNum;
    }

    public void setRareWordThreshold(double rareWordThreshold) {
        this.rareWordThreshold = rareWordThreshold - 0.01;
    }

    public void setLexRegWeight(double lexRegWeight) {
        this.lexRegWeight = lexRegWeight;
    }

    public List<String> extractPredicates(String word) {
        List<String> predicates = new ArrayList<String>();
        if (wordPredPredefined) {
            for (String predicate : wordPredSet.get(word)) {
                if (observedPredSet.contains(predicate)) {
                    predicates.add(predicate);
                }
            }
        } else {
            List<PredicateExtractor> extractorList = freqPredicateExtractorList;
            if (wordCountsMap.getCount(word) < rareWordThreshold) {
                extractorList = rarePredicateExtractorList;
            }
            for (PredicateExtractor predicateExtractor : extractorList) {
                for (String predicate : predicateExtractor.extract(word)) {
                    if (observedPredSet.contains(predicate)) {
                        predicates.add(predicate);
                    }
                }
            }
        }
        return predicates;
    }

//    public synchronized double[] getScores(final String word, int tag, boolean viterbi) {
//        double[][] unseenTagCounter = featuredLexicon.getUnseenLatentTagCounts();
//        double totalUnseenTokens = featuredLexicon.getTotalUnseenTokens();
//        double c_W = wordCounter.getCount(word);
//        boolean seen = c_W != 0;
//        double[] scores = new double[numStates[tag]];
//        double[] probs = null;
//        boolean seenOOV = false;
//        if (seen & observedTagWordSet.contains(tag, word)) {
//            probs = tagGivenWordProbsMap.get(word)[tag];
//        } else {
//            if (tagGivenNewWordProbsMap == null) {
//                tagGivenNewWordProbsMap = new HashMap<String, double[][]>();
//            }
//            seenOOV = tagGivenNewWordProbsMap.containsKey(word);
//            if (seenOOV) {
//                probs = tagGivenNewWordProbsMap.get(word)[tag];
//            }
//        }
//
//        if (probs == null) {
//            probs = new double[numStates[tag]];
//            if (!seen && !seenOOV) {
//                double[][] nodeScores = new double[numNodes][];
//                List<String> predicates = extractPredicates(word);
//                double totalScore = 0;
//                for (String predicate : predicates) {
//                    double[][] predNodeWeights = predTagWeightMap.get(predicate);
//                    for (int ni = 0; ni < numNodes; ni++) {
//                        double[] weights = predNodeWeights[ni];
//                        if (weights == null) {
//                            continue;
//                        }
//                        if (nodeScores[ni] == null) {
//                            nodeScores[ni] = new double[numStates[ni]];
//                        }
//                        for (int si = 0; si < numStates[ni]; si++) {
//                            nodeScores[ni][si] += weights[si];
//                        }
//                    }
//                }
//
//                NBestList<NodeProbPair> nodeProbNBestList = new NBestList<NodeProbPair>(nbestNodesForOOVWords);
//                for (int ni = 0; ni < numNodes; ni++) {
//                    if (nodeScores[ni] != null) {
//                        double nodeTotalScore = 0;
//                        for (int si = 0; si < numStates[ni]; si++) {
//                            nodeScores[ni][si] = Math.exp(nodeScores[ni][si]);
//                            nodeTotalScore += nodeScores[ni][si];
//                        }
//                        totalScore += nodeTotalScore;
//                        nodeProbNBestList.add(new NodeProbPair(ni, nodeTotalScore));
//                    }
//                }
//
//                Set<Integer> nbestNodeSet = new HashSet<Integer>();
//                double topProbs = 0;
//                for (NodeProbPair pair : nodeProbNBestList) {
//                    topProbs += pair.getProb();
//                    nbestNodeSet.add(pair.getNode());
//                    if (topProbs > totalScore * 0.9) {
//                        break;
//                    }
//                }
//
//                for (int ni = 0; ni < numNodes; ni++) {
//                    if (nodeScores[ni] == null) {
//                        continue;
//                    }
//                    if (nbestNodeSet.contains(ni)) {
//                        for (int si = 0; si < numStates[ni]; si++) {
//                            nodeScores[ni][si] /= totalScore;
//                        }
//                    } else {
//                        nodeScores[ni] = null;
//                    }
//                }
//                tagGivenNewWordProbsMap.put(word, nodeScores);
//                if (nodeScores[tag] != null) {
//                    System.arraycopy(nodeScores[tag], 0, probs, 0, numStates[tag]);
//                }
//            }
//        }
//
//        for (int si = 0; si < numStates[tag]; si++) {
//            double p_T_W = probs[si];
//            double c_Tunseen = unseenTagCounter[tag][si];
//            double p_T_U = (totalUnseenTokens == 0) ? 1 : c_Tunseen / totalUnseenTokens;
////            if (c_W > unknownSmoothingThreshold) {
//            p_T_W = (1 - 0.0001) * p_T_W + 0.0001 * p_T_U;
////             pb_T_W = (c_TW + 0.0001 * p_T_U) / (c_W + 0.0001);
////            } else {
////                p_T_W = (1 - wordSmoothingParam) * p_T_W + wordSmoothingParam * p_T_U;
////            }
//            if (seen) {
//                scores[si] = p_T_W * c_W / expectedTagCounts[tag][si];
//            } else {
////                scores[si] = p_T_W * p_T_U / expectedNodeCounts[tag][si];
//                scores[si] = p_T_W / expectedTagCounts[tag][si];
//            }
//        }
//
////        scores = featuredLexicon.smooth(scores, tag);
//        if (viterbi) {
//            for (int si = 0; si < numStates[tag]; si++) {
//                scores[si] = Math.log(scores[si]);
//            }
//        }
//        return scores;
//    }
    /**
     * Load word predicates from a file
     *
     * @param wordPredFile
     */
    public void loadWordPredicates(String wordPredFile) {
        if (wordPredFile == null) {
            wordPredPredefined = false;
        } else {
            try {
                wordPredPredefined = true;
                InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(wordPredFile), Charset.forName("UTF-8"));
                BufferedReader bReader = new BufferedReader(inputStreamReader);
                wordPredSet = new BiSet<String, String>();
                String line;
                while ((line = bReader.readLine()) != null) {
                    line = line.trim();
                    List<String> items = Arrays.asList(line.trim().split(" +"));
                    String word = items.get(0).intern();
                    for (int i = 1; i < items.size(); i++) {
                        wordPredSet.add(word, items.get(i).intern());
                    }
                }
                bReader.close();
                inputStreamReader.close();
            } catch (Exception ex) {
                System.exit(1);
            }
        }
    }

    public void initPredStats() {
        wordCountsMap = featuredLexicon.getWordCountsMap();
        if (!wordPredPredefined) {
            predicateExtractorList = new ArrayList<PredicateExtractor>();
            freqPredicateExtractorList = new ArrayList<PredicateExtractor>();
            rarePredicateExtractorList = new ArrayList<PredicateExtractor>();
            switch (lang) {
                case english: {
                    // use english
                    PredicateExtractor wordIdentityExtractor = new WordIdentityExtractor();
                    PredicateExtractor prefixSuffixExtractor = new PrefixSuffixExtractor();
                    PredicateExtractor digitInitcaseHyphen = new DigitCaseHyphenExtractor();

                    predicateExtractorList.add(wordIdentityExtractor);
                    predicateExtractorList.add(prefixSuffixExtractor);
                    predicateExtractorList.add(digitInitcaseHyphen);

                    freqPredicateExtractorList.addAll(predicateExtractorList);
                    rarePredicateExtractorList.addAll(predicateExtractorList);
                    break;
                }
                case chinese: {
                    // chinese
                    PredicateExtractor wordIdentityExtractor = new WordIdentityExtractor();
                    PredicateExtractor prefixSuffixExtractor = new PrefixSuffixExtractor();
                    PredicateExtractor digitInitcaseHyphen = new DigitCaseHyphenExtractor();

                    predicateExtractorList.add(wordIdentityExtractor);
                    predicateExtractorList.add(prefixSuffixExtractor);
                    predicateExtractorList.add(digitInitcaseHyphen);

                    freqPredicateExtractorList.addAll(predicateExtractorList);
                    rarePredicateExtractorList.addAll(predicateExtractorList);
                    break;
                }

                case arabic: {
                    // arabic
                    PredicateExtractor wordIdentityExtractor = new WordIdentityExtractor();
                    PredicateExtractor prefixSuffixExtractor = new PrefixSuffixExtractor();
                    PredicateExtractor digitInitcaseHyphen = new DigitCaseHyphenExtractor();

                    predicateExtractorList.add(wordIdentityExtractor);
                    predicateExtractorList.add(prefixSuffixExtractor);
                    predicateExtractorList.add(digitInitcaseHyphen);

                    freqPredicateExtractorList.addAll(predicateExtractorList);
                    rarePredicateExtractorList.addAll(predicateExtractorList);
                    break;
                }

                case others: {
                    PredicateExtractor wordIdentityExtractor = new WordIdentityExtractor();
                    PredicateExtractor prefixSuffixExtractor = new PrefixSuffixExtractor();
                    PredicateExtractor digitInitcaseHyphen = new DigitCaseHyphenExtractor();

                    predicateExtractorList.add(wordIdentityExtractor);
                    predicateExtractorList.add(prefixSuffixExtractor);
                    predicateExtractorList.add(digitInitcaseHyphen);

                    freqPredicateExtractorList.addAll(predicateExtractorList);
                    rarePredicateExtractorList.addAll(predicateExtractorList);
                    break;
                }

                default: {
                    throw new Error(String.format("lang=%d is not supported", lang));
                }
            }
            wordPredSet = new BiSet<String, String>();
            for (Entry<String, Double> wordCountEntry : wordCountsMap.entrySet()) {
                String word = wordCountEntry.getKey();
                double count = wordCountEntry.getValue();
                if (count < rareWordThreshold) {
                    for (PredicateExtractor predicateExtractor : rarePredicateExtractorList) {
                        for (String predicate : predicateExtractor.extract(word)) {
                            wordPredSet.add(word, predicate);
                        }
                    }
                } else {
                    for (PredicateExtractor predicateExtractor : freqPredicateExtractorList) {
                        for (String predicate : predicateExtractor.extract(word)) {
                            wordPredSet.add(word, predicate);
                        }
                    }
                }
            }
        }

        for (String word : wordCountsMap.keySet()) {
            if (!wordPredSet.containsKey(word) || wordPredSet.get(word).isEmpty()) {
                GlobalLogger.log(Level.SEVERE, "No predicate set for word: " + word);
                System.exit(1);
            }
        }

        observedTagPredSet = new BiSet<Integer, String>();
        observedPredTagSet = new BiSet<String, Integer>();
        observedTagWordSet = new BiSet<Integer, String>();
        observedWordTagSet = new BiSet<String, Integer>();
        possibleWordTagSet = new BiSet<String, Integer>();
        possibleTagWordSet = new BiSet<Integer, String>();
        observedPredSet = new HashSet<String>();

        observedTagWordSet = featuredLexicon.getTagWordSet();
        observedWordTagSet = featuredLexicon.getWordTagSet();

        for (Entry<String, HashSet<Integer>> entry : observedWordTagSet.entrySet()) {
            String word = entry.getKey();
            for (String pred : wordPredSet.get(word)) {
                for (Integer tag : entry.getValue()) {
                    observedPredTagSet.add(pred, tag);
                    observedTagPredSet.add(tag, pred);
                }
            }
        }

        for (String word : wordCountsMap.keySet()) {
            for (String pred : wordPredSet.get(word)) {
                for (int tag : observedPredTagSet.get(pred)) {
                    possibleWordTagSet.add(word, tag);
                    possibleTagWordSet.add(tag, word);
                }
            }
        }

        for (String pred : observedPredTagSet.keySet()) {
            observedPredSet.add(pred);
        }
    }

    public void setupLexicon() {
        initPredStats();
        initParams();
        indirectOptimization();
    }

    public double[] getProbs(String word) {
        // compute surface tag probabilities.
        synchronized (oovLock) {
            if (tagGivenOOVProbs.containsKey(word)) {
                return tagGivenOOVProbs.get(word);
            }
        }
        double[] tagProbs = new double[numNodes];
        boolean[] activeTags = new boolean[numNodes];
        List<String> oovPredicates = extractPredicates(word);
        for (String predicate : oovPredicates) {
            double[] weights = predTagWeightMap.get(predicate);
            for (int tag : observedPredTagSet.get(predicate)) {
                tagProbs[tag] += weights[tag];
                activeTags[tag] = true;
            }
        }

        double totalScore = 0;
        NBestList<NodeProbPair> topTagProbList = new NBestList<NodeProbPair>(topTagSize);
        for (int tag = 0; tag < numNodes; tag++) {
            if (activeTags[tag]) {
                tagProbs[tag] = Math.exp(tagProbs[tag]);
                totalScore += tagProbs[tag];
                topTagProbList.add(new NodeProbPair(tag, tagProbs[tag]));
            }
        }

        boolean[] topTags = new boolean[numNodes];
        double topTagScoreSum = 0;
        for (NodeProbPair pair : topTagProbList) {
            topTagScoreSum += pair.getProb();
            topTags[pair.getNode()] = true;
            if (topTagScoreSum > totalScore * topTagProbPercentage) {
                break;
            }
        }

        for (int ni = 0; ni < numNodes; ni++) {
            if (topTags[ni]) {
                tagProbs[ni] /= totalScore;
            } else {
                tagProbs[ni] = 0;  // prune unlikely tags 
            }
        }

        synchronized (oovLock) {
            tagGivenOOVProbs.put(word, tagProbs);
        }

        return tagProbs;
    }

    public void parallelCompTagGivenWordProb() {
        for (double[] probs : tagGivenWordProbsMap.values()) {
            Arrays.fill(probs, 0);
        }
        Arrays.fill(expectedTagCounts.getArray(), 0);

        JobManager jobManager = JobManager.getInstance();
        try {
            JobGroup grp = jobManager.createJobGroup("compRandS");
            int ti = 0;
            int maxWordNum = 1000;
            List<String> wordList = new ArrayList<String>();
            for (final String word : wordTagDiffCountsMap.keySet()) {
                ti++;
                wordList.add(word);
                if (wordList.size() == maxWordNum) {
                    final List<String> finalWordList = new ArrayList<String>(wordList);
                    wordList.clear();
                    Job job = new Job(new Runnable() {
                        @Override
                        public void run() {
                            compTagGivenWordProbAndExpectedTagCount(finalWordList);
                        }
                    }, String.valueOf(ti) + "-th word");
                    job.setPriority(ti);
                    jobManager.addJob(grp, job);
                }
            }
            if (!wordList.isEmpty()) {
                final List<String> finalWordList = new ArrayList<String>(wordList);
                wordList.clear();
                Job job = new Job(new Runnable() {
                    @Override
                    public void run() {
                        compTagGivenWordProbAndExpectedTagCount(finalWordList);
                    }
                }, String.valueOf(ti) + "-th word");
                job.setPriority(ti);
                jobManager.addJob(grp, job);
            }
            grp.join();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * For each word in the list, compute p(t|w) following the following steps:
     * 1. compute lambda x f = sum_i lambda_i f_i 2. compute exp(lambda x f) 3.
     * compute p(t|w) and p(t)
     *
     * @param wordList A list of words
     */
    public void compTagGivenWordProbAndExpectedTagCount(List<String> wordList) {
        for (String word : wordList) {
            double[] twProbs = tagGivenWordProbsMap.get(word);
            double wordCount = wordCountsMap.getCount(word);

            Set<String> wpSet = wordPredSet.get(word);
            Set<Integer> pwtSet = possibleWordTagSet.get(word);

            for (String pred : wpSet) {
                double[] ptWeights = predTagWeightMap.get(pred);
                for (int tag : pwtSet) {
                    twProbs[tag] += ptWeights[tag];
                }
            }

            double totalProb = 0;
            for (int tag : pwtSet) {
                twProbs[tag] = Math.exp(twProbs[tag]);
                totalProb += twProbs[tag];
            }

            for (int tag : pwtSet) {
                twProbs[tag] /= totalProb;
            }

            expectedTagCounts.add(twProbs, wordCount);
        }
    }

    /////todo: continue from here
    /**
     * Compute p(t|w)
     */
    public void parallelCompWordGivenTagProb() {
        for (Entry<String, double[]> entry : wordGivenTagProbsMap.entrySet()) {
            double[] probs = entry.getValue();
            Arrays.fill(probs, 0);
        }

        JobManager jobManager = JobManager.getInstance();
        try {
            JobGroup grp = jobManager.createJobGroup("compRandS");
            int ti = 0;
            int maxWordNum = 1000;
            List<String> wordList = new ArrayList<String>();
            for (final String word : wordTagDiffCountsMap.keySet()) {
                ti++;
                wordList.add(word);
                if (wordList.size() == maxWordNum) {
                    final List<String> finalWordList = new ArrayList<String>(wordList);
                    wordList.clear();
                    Job job = new Job(new Runnable() {
                        @Override
                        public void run() {
                            compWordGivenNodeProb(finalWordList);
                        }
                    }, String.valueOf(ti) + "-th word");
                    job.setPriority(ti);
                    jobManager.addJob(grp, job);
                }
            }
            if (!wordList.isEmpty()) {
                final List<String> finalWordList = new ArrayList<String>(wordList);
                wordList.clear();
                Job job = new Job(new Runnable() {
                    @Override
                    public void run() {
                        compWordGivenNodeProb(finalWordList);
                    }
                }, String.valueOf(ti) + "-th word");
                job.setPriority(ti);
                jobManager.addJob(grp, job);
            }
            grp.join();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Compute p(w|t) from p(t|w)p(w)/p(t)
     *
     * @param wordList A list of words
     */
    public void compWordGivenNodeProb(List<String> wordList) {
        for (String word : wordList) {
            double[] twProbs = tagGivenWordProbsMap.get(word);
            double[] wtProbs = wordGivenTagProbsMap.get(word);

            double wCount = wordCountsMap.getCount(word);
            double[] etCounts = expectedTagCounts.getArray();
            for (int ni : possibleWordTagSet.get(word)) {
                wtProbs[ni] = twProbs[ni] * wCount / etCounts[ni];
            }
        }
    }

    /**
     * Compute e*(w,t) and e*(w)
     */
    public void parallelCompDiffCounts() {
        JobManager jobManager = JobManager.getInstance();
        try {
            JobGroup grp = jobManager.createJobGroup("compRandS");
            int ti = 0;
            int maxWordNum = 1000;
            List<String> wordList = new ArrayList<String>();
            for (final String word : wordTagDiffCountsMap.keySet()) {
                ti++;
                wordList.add(word);
                if (wordList.size() == maxWordNum) {
                    final List<String> finalWordList = new ArrayList<String>(wordList);
                    wordList.clear();
                    Job job = new Job(new Runnable() {
                        @Override
                        public void run() {
                            compWordNodeCountDiff(finalWordList);
                        }
                    }, String.valueOf(ti) + "-th word");
                    job.setPriority(ti);
                    jobManager.addJob(grp, job);
                }
            }
            if (!wordList.isEmpty()) {
                final List<String> finalWordList = new ArrayList<String>(wordList);
                wordList.clear();
                Job job = new Job(new Runnable() {
                    @Override
                    public void run() {
                        compWordNodeCountDiff(finalWordList);
                    }
                }, String.valueOf(ti) + "-th word");
                job.setPriority(ti);
                jobManager.addJob(grp, job);
            }
            grp.join();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Compute e'(w,t) = e(t) * p(w|t) and e'(w) = sum_t e'(w,t) e*(w,t) =
     * e(w,t) - e'(w,t), e*(w) = e(w) - e'(w)
     *
     * @param wordList
     */
    public void compWordNodeCountDiff(List<String> wordList) {
        for (String word : wordList) {
            double[] wtCounts = wordTagCountsMap.get(word);
            double[] wtDiffCounts = wordTagDiffCountsMap.get(word);
            double[] wtProbs = wordGivenTagProbsMap.get(word);

            double total = 0;
            double[] otCounts = observedTagCounts.getArray();
            for (int tag : possibleWordTagSet.get(word)) {
                wtDiffCounts[tag] = wtCounts[tag] - otCounts[tag] * wtProbs[tag];
                total += wtDiffCounts[tag];
            }
            wordDiffCountsMap.setCount(word, total);
        }
    }

    /**
     * Compute the derivatives of features based on the fractional counts
     */
    public void parallelCompRandS() {
        for (Entry<String, DoubleArray> entry : predTagObservationMap.entrySet()) {
            String pred = entry.getKey();
            double[] observations = entry.getValue().getArray();
            double[] expectations = predTagExpectationMap.get(pred).getArray();
            Arrays.fill(observations, 0);
            Arrays.fill(expectations, 0);
        }

        JobManager jobManager = JobManager.getInstance();
        try {
            JobGroup grp = jobManager.createJobGroup("compRandS");
            int ti = 0;
            int maxWordNum = 1000;
            List<String> wordList = new ArrayList<String>();
            for (final String word : wordTagDiffCountsMap.keySet()) {
                ti++;
                wordList.add(word);
                if (wordList.size() == maxWordNum) {
                    final List<String> finalWordList = new ArrayList<String>(wordList);
                    wordList.clear();
                    Job job = new Job(new Runnable() {
                        @Override
                        public void run() {
                            compRandS(finalWordList);
                        }
                    }, String.valueOf(ti) + "-th word");
                    job.setPriority(ti);
                    jobManager.addJob(grp, job);
                }
            }
            if (!wordList.isEmpty()) {
                final List<String> finalWordList = new ArrayList<String>(wordList);
                wordList.clear();
                Job job = new Job(new Runnable() {
                    @Override
                    public void run() {
                        compRandS(finalWordList);
                    }
                }, String.valueOf(ti) + "-th word");
                job.setPriority(ti);
                jobManager.addJob(grp, job);
            }
            grp.join();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Compute the R and S derivatives of the objective
     *
     * @param wordList
     */
    public void compRandS(List<String> wordList) {
        for (String word : wordList) {
            double[] wtDiffCounts = wordTagDiffCountsMap.get(word);
            double[] twProbs = tagGivenWordProbsMap.get(word);
            double wDiffCount = wordDiffCountsMap.getCount(word);

            Set<Integer> wtSet = possibleWordTagSet.get(word);

            for (String pred : wordPredSet.get(word)) {
                DoubleArray observations = predTagObservationMap.get(pred);
                DoubleArray expectations = predTagExpectationMap.get(pred);
                observations.add(wtDiffCounts);
                expectations.add(twProbs, wDiffCount);
            }
        }
    }

    /**
     * Compute the objective function for indirect EM optimization
     *
     * @param weights feature weights
     */
    public void compObjectiveScore() {
        objectiveScore = 0;
        for (Entry<String, double[]> entry : wordTagCountsMap.entrySet()) {
            String word = entry.getKey();
            double[] counts = entry.getValue();
            double[] probs = wordGivenTagProbsMap.get(word);
            for (int ni : observedWordTagSet.get(word)) {
                objectiveScore += counts[ni] * Math.log(probs[ni]);
            }
        }
        double probLoss = -objectiveScore;
        //        objectiveScore += ruleManager.compObjective();
        objectiveScore -= compRegTerm();
        double regLoss = -probLoss - objectiveScore;
        objectiveScore *= -1;

//        GlobalLogger.log(Level.FINE, String.format(" probLoss = %.2f, regLoss = %.2f, totalLoss = %.2f\n", probLoss, regLoss, objectiveScore));
    }

    /**
     * Compute the regularization term
     *
     * @param weights
     * @return
     */
    public double compRegTerm() {
        double score = 0;
        for (int i = 0; i < numFeats; i++) {
            score += lexRegWeight * globalWeights[i] * globalWeights[i];
        }
        return score;
    }

    public void initParams() {
        predTagWeightMap = new HashMap<String, double[]>();
        predTagObservationMap = new HashMap<String, DoubleArray>();
        predTagExpectationMap = new HashMap<String, DoubleArray>();

        for (String pred : observedPredSet) {
            // prodNodeWeigthMap
            double[] weights = new double[numNodes];
            predTagWeightMap.put(pred, weights);

            // predNodeObservationMap
            DoubleArray observations = new DoubleArray(new double[numNodes]);
            predTagObservationMap.put(pred, observations);

            // predNodeExpectationMap
            DoubleArray expectations = new DoubleArray(new double[numNodes]);
            predTagExpectationMap.put(pred, expectations);
        }


        wordTagCountsMap = new HashMap<String, double[]>();
        wordTagDiffCountsMap = new HashMap<String, double[]>();
        tagGivenWordProbsMap = new HashMap<String, double[]>();
        wordGivenTagProbsMap = new HashMap<String, double[]>();


        expectedTagCounts = new DoubleArray(new double[numNodes]);
        observedTagCounts = new DoubleArray(new double[numNodes]);

        for (String word : wordCountsMap.keySet()) {
            double[] wtCounts = new double[numNodes];
            wordTagCountsMap.put(word, wtCounts);

            // wordNodeCountDiffMap
            double[] wtDiffCounts = new double[numNodes];
            wordTagDiffCountsMap.put(word, wtDiffCounts);

            // nodeGivenWordProbMap
            double[] nwProbs = new double[numNodes];
            tagGivenWordProbsMap.put(word, nwProbs);

            // wordGivenNodeProbMap
            double[] wnProbs = new double[numNodes];
            wordGivenTagProbsMap.put(word, wnProbs);
        }

        BiCounter<Integer, String> tagWordCountsMap = featuredLexicon.getTagWordCountsMap();
        for (Entry<Integer, UniCounter<String>> biEntry : tagWordCountsMap.entrySet()) {
            int tag = biEntry.getKey();
            for (Entry<String, Double> uniEntry : biEntry.getValue().entrySet()) {
                String word = uniEntry.getKey();
                double count = uniEntry.getValue();
                double[] wtCounts = wordTagCountsMap.get(word);
                wtCounts[tag] = count;
            }
        }

        calcNumFeatures();
        wordDiffCountsMap = new UniCounter<String>();
        globalWeights = new double[numFeats];
        globalGradients = new double[numFeats];
    }

    public void calcNumFeatures() {
        numFeats = 0;
        for (Entry<Integer, HashSet<String>> otpSet : observedTagPredSet.entrySet()) {
            numFeats += otpSet.getValue().size();
        }
    }

    /**
     * Compute the gradient of the objective.
     *
     * @param weights
     * @return
     */
    public void compGradients() {

        int featIndex = 0;
        for (Entry<String, DoubleArray> entry : predTagObservationMap.entrySet()) {
            String pred = entry.getKey();
            double[] observations = entry.getValue().getArray();
            double[] expectations = predTagExpectationMap.get(pred).getArray();
            for (int ni : observedPredTagSet.get(pred)) {
                globalGradients[featIndex] = expectations[ni] - observations[ni];
                featIndex++;
            }

        }

        for (int si = 0; si < numFeats; si++) {
            globalGradients[si] += 2 * lexRegWeight * globalWeights[si];
        }
    }

    public void updateWeights() {
        int featIndex = 0;

        for (Entry<String, double[]> entry : predTagWeightMap.entrySet()) {
            String pred = entry.getKey();
            double[] weights = entry.getValue();
            for (int tag : observedPredTagSet.get(pred)) {
                weights[tag] = globalWeights[featIndex];
                featIndex++;
            }
        }
    }

    public void parallelCompProbs() {
        parallelCompTagGivenWordProb();
        parallelCompWordGivenTagProb();
    }

    public void indirectOptimization() {
        parallelCompProbs();

        int n = numFeats;
        int m = 5;
        int iprint[] = new int[2];
        iprint[0] = -1;
        iprint[1] = 0;
        boolean diagco = false;
        double eps = 1.0e-5;
        double xtol = 1.0e-16;
        int icall = 0;

        int[] iflag = new int[1];
        iflag[0] = 0;
        double[] diag = new double[numFeats];

//        double ave_abs_diff = 0;
//        double total_abs_diff = 0;
//        double max_abs_diff = 0;

        GlobalLogger.log(Level.FINE, String.format("Start LBFGS training of the OOV model"));

        double prevObjectiveScore = Double.NEGATIVE_INFINITY;
        int numNoChanges = 0;

        do {
//            System.out.printf("%2d:", icall);
            parallelCompDiffCounts();
            parallelCompRandS();
            compObjectiveScore();
            compGradients();
//            System.out.printf(", maxDelta = %.2f, totalDelta = %.2f\n", max_abs_diff, total_abs_diff);
//            System.out.printf("    %d-th objective: %.20f    gradient total: %.20f\tave: %.20f\ttmax: %.20f\tmax_i: %d\t\n", icall, objectiveScore, total_abs_diff, ave_abs_diff, max_abs_diff, max_i);
//            System.out.flush();

            double change = Math.abs((objectiveScore - prevObjectiveScore) / objectiveScore);
            if (change < 1e-10) {
                numNoChanges++;
            } else {
                numNoChanges = 0;
            }
            prevObjectiveScore = objectiveScore;

            try {
                LBFGS.lbfgs(n, m, globalWeights, objectiveScore, globalGradients, diagco, diag, iprint, eps, xtol, iflag);
            } catch (LBFGS.ExceptionWithIflag e) {
                GlobalLogger.log(Level.SEVERE, e.toString());
            }
            updateWeights();
            parallelCompProbs();
            icall++;
            GlobalLogger.log(Level.FINE, String.format("%d-th LBFGS: %.2f", icall, objectiveScore));
        } while (iflag[0] != 0 && icall <= iterNum && numNoChanges < 5);
        clear();
    }

    public void clear() {
        globalGradients = null;
        globalWeights = null;
        tagGivenWordProbsMap = null;
        wordGivenTagProbsMap = null;
        wordTagCountsMap = null;
        wordTagDiffCountsMap = null;
        predTagObservationMap = null;
        predTagExpectationMap = null;
    }
}
