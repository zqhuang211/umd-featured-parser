 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.util.BiCounter;
import edu.umd.clip.util.BiSet;
import edu.umd.clip.util.GlobalLogger;
import edu.umd.clip.util.UniCounter;
import edu.umd.clip.jobs.Job;
import edu.umd.clip.jobs.JobGroup;
import edu.umd.clip.jobs.JobManager;
import edu.umd.clip.math.LBFGS;
import static edu.umd.clip.parser.Grammar.Language.others;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author zqhuang
 */
public class FeaturedLexiconManager extends LexiconManager implements Serializable {

    private static final long serialVersionUID = 1L;
    protected double[][] latentTagExpectedCounts;
    protected HashMap<String, double[]>[] latentTagGivenWordProbs;
    protected HashMap<String, double[]>[] latentTagGivenOOVProbs;
    protected Set<String> seenOOVSet;
    private static final Object oovLock = new Object();
    protected HashMap<String, HashMap<Integer, DoubleArray>> wordLatentTagCountsMap;
    protected HashMap<String, HashMap<Integer, DoubleArray>> wordLatentTagDiffCountsMap;
    protected HashMap<String, HashMap<Integer, DoubleArray>> latentTagGivenWordTagProbsMap;
    protected HashMap<String, HashMap<Integer, DoubleArray>> wordGivenLatentTagProbsMap;
    protected HashMap<String, HashMap<Integer, DoubleArray>> predLatentTagWeightsMap;
    protected HashMap<String, HashMap<Integer, DoubleArray>> predLatentTagCountsMap;
    protected HashMap<String, HashMap<Integer, DoubleArray>> predLatentTagExpectedCountsMap;
    protected HashMap<Integer, DoubleArray> latentTagExpectedCountsMap;
    protected BiCounter<String, Integer> wordTagCountsMap;
    protected BiCounter<String, Integer> wordTagDiffCountsMap;
    protected BiSet<String, String> wordPredSet;
    protected BiSet<String, Integer> predTagSet;
    protected BiSet<Integer, String> tagPredSet;
    protected BiSet<String, Integer> wordTagSet;
    protected BiSet<Integer, String> tagWordSet;
    protected Set<String> predSet;
    protected List<PredicateExtractor> predicateExtractorList;
    protected List<PredicateExtractor> freqPredicateExtractorList;
    protected List<PredicateExtractor> rarePredicateExtractorList;
    protected OptimizationMode optimizationMode;
    protected RuleManager ruleManager;
    protected FeaturedOOVLexiconManager oovLexicon;
    protected double objectiveScore = 0;
    protected int numFeats = 0;
    protected int numLexFeats = 0;
    protected int iterNum = 20;
    protected int numJobs = 1;
    protected double lexRegWeight = 1;
    protected double synRegWeight = 0;
    protected Grammar.Language lang; // use 0 for english, 1 for arabic
    protected int iflag[] = new int[1];
    protected int iprint[] = new int[2];
    protected double[] diag = null;
    protected boolean wordPredPredefined = false;
    public static final double minimumFeatureWeight = -20;
    protected double[] globalWeights = null;
    protected double[] globalGradients = null;
    protected FeaturedLexiconManager finerLexicon;

    public FeaturedLexiconManager() {
    }

    public FeaturedLexiconManager(Grammar.Language lang) {
        this.lang = lang;
        oovLexicon = new FeaturedOOVLexiconManager(this);
    }

    protected enum OptimizationMode {

        indirect, direct
    }

    @Override
    public void setupGrammar(Grammar grammar) {
        super.setupGrammar(grammar);
        ruleManager = grammar.getRuleManager();
        if (oovLexicon != null) {
            oovLexicon.setupGrammar(grammar);
        }
    }

    @Override
    public void setupArray() {
        super.setupArray();
        if (oovLexicon != null) {
            oovLexicon.setupArray();
        }
        latentTagGivenWordProbs = new HashMap[numNodes];
        for (Entry<String, HashMap<Integer, DoubleArray>> biEntry : latentTagGivenWordTagProbsMap.entrySet()) {
            String word = biEntry.getKey();
            double wordCount = wordCountsMap.getCount(word);
            for (Entry<Integer, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
                int tag = uniEntry.getKey();
                double wordTagCount = wordTagCountsMap.getCount(word, tag);
                double[] ltgwtProbs = uniEntry.getValue().getArray();
                double[] ltgwProbs = new double[numStates[tag]];
                for (int si = 0; si < numStates[tag]; si++) {
                    ltgwProbs[si] = ltgwtProbs[si] * wordTagCount / wordCount;
//                    ltgwProbs[si] = ltgwtProbs[si];
                }

                if (latentTagGivenWordProbs[tag] == null) {
                    latentTagGivenWordProbs[tag] = new HashMap<String, double[]>();
                }
                latentTagGivenWordProbs[tag].put(word, ltgwProbs);
            }
        }

        latentTagExpectedCounts = new double[numNodes][];
        for (Entry<Integer, DoubleArray> entry : latentTagExpectedCountsMap.entrySet()) {
            latentTagExpectedCounts[entry.getKey()] = entry.getValue().getArray();
        }

        latentTagGivenOOVProbs = new HashMap[numNodes];
        seenOOVSet = new HashSet<String>();

        wordLatentTagCountsMap = null;
        wordLatentTagDiffCountsMap = null;
        latentTagGivenWordTagProbsMap = null;
        wordGivenLatentTagProbsMap = null;
        predLatentTagCountsMap = null;
        predLatentTagExpectedCountsMap = null;
        globalGradients = null;
        globalWeights = null;
    }

    public void clear() {
//        wordLatentTagCountsMap = null;
//        wordLatentTagDiffCountsMap = null;
//        predLatentTagCountsMap = null;
//        predLatentTagExpectedCountsMap = null;
//        wordGivenLatentTagProbsMap = null;

        for (Entry<String, HashSet<Integer>> ptEntry : predTagSet.entrySet()) {
            String pred = ptEntry.getKey();

            HashMap<Integer, DoubleArray> pltCountsMap = predLatentTagCountsMap.get(pred);
            HashMap<Integer, DoubleArray> pltExpectedCountsMap = predLatentTagExpectedCountsMap.get(pred);

            for (int ni : ptEntry.getValue()) {
                pltCountsMap.get(ni).setArray(null);
                pltExpectedCountsMap.get(ni).setArray(null);
            }
        }

        for (Entry<String, HashSet<Integer>> wtEntry : wordTagSet.entrySet()) {
            String word = wtEntry.getKey();

            HashMap<Integer, DoubleArray> wltDiffCountsMap = wordLatentTagDiffCountsMap.get(word);
            HashMap<Integer, DoubleArray> wltProbsMap = wordGivenLatentTagProbsMap.get(word);
            for (int ni : wtEntry.getValue()) {
                wltDiffCountsMap.get(ni).setArray(null);
                wltProbsMap.get(ni).setArray(null);
            }
        }

        globalGradients = null;
        globalWeights = null;
        System.gc();
    }

    public void setIterNum(int iterNum) {
        this.iterNum = iterNum;
    }

    public OptimizationMode getOptimizationMode() {
        return optimizationMode;
    }

    public void setOptimizationMode(OptimizationMode optMode) {
        this.optimizationMode = optMode;
    }

    public double getLexRegWeight() {
        return lexRegWeight;
    }

    @Override
    public boolean isRuleManagerUpdated() {
        if (optimizationMode == OptimizationMode.direct) {
            return true;
        } else {
            return false;
        }
    }

    public BiSet<String, Integer> getPredTagSet() {
        return predTagSet;
    }

    public BiSet<Integer, String> getTagPredSet() {
        return tagPredSet;
    }

    public BiSet<Integer, String> getTagWordSet() {
        return tagWordSet;
    }

    public BiSet<String, Integer> getWordTagSet() {
        return wordTagSet;
    }

    public boolean isUsePredefinedWordPredicates() {
        return wordPredPredefined;
    }

    public FeaturedOOVLexiconManager getOOVLexicon() {
        return oovLexicon;
    }

    public void setupOOVLexicon() {
        oovLexicon.setupLexicon();
    }

    public List<String> extractPredicates(String word) {
        List<String> predicates = new ArrayList<String>();
        if (wordPredPredefined) {
            for (String predicate : wordPredSet.get(word)) {
                if (predLatentTagWeightsMap.containsKey(predicate)) {
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
                    if (predSet.contains(predicate)) {
                        predicates.add(predicate);
                    }
                }
            }
        }
        return predicates;
    }

    public double[][] getOOVLatentTagProbs(String word) {
        double[][] latentTagProbs = null;
        if (finerLexicon != null) {
            double[][] fineLatentTagProbs = finerLexicon.getOOVLatentTagProbs(word);
            latentTagProbs = new double[numNodes][];

            int[] fineNumStates = finerLexicon.numStates;
            int[][] ffine2coarseMapping = finerLexicon.fine2coarseMapping;
            for (int ni = 0; ni < numNodes; ni++) {
                if (fineLatentTagProbs[ni] == null) {
                    continue;
                }
                int fineStateNum = fineNumStates[ni];
                int coarseStateNum = numStates[ni];

                latentTagProbs[ni] = new double[coarseStateNum];
                for (int fsi = 0; fsi < fineStateNum; fsi++) {
                    int csi = ffine2coarseMapping[ni][fsi];
                    latentTagProbs[ni][csi] += fineLatentTagProbs[ni][fsi];
                }
            }
        } else {
            double[] tagProbs = oovLexicon.getProbs(word);
            latentTagProbs = new double[numNodes][];
            List<String> predicates = extractPredicates(word);
            for (String predicate : predicates) {
                HashMap<Integer, DoubleArray> latentTagWeightsMap = predLatentTagWeightsMap.get(predicate);
                for (Entry<Integer, DoubleArray> entry : latentTagWeightsMap.entrySet()) {
                    int ni = entry.getKey();
                    double[] weights = entry.getValue().getArray();
                    if (tagProbs[ni] == 0) {
                        continue;
                    }
                    if (latentTagProbs[ni] == null) {
                        latentTagProbs[ni] = new double[numStates[ni]];
                    }
                    for (int si = 0; si < numStates[ni]; si++) {
                        latentTagProbs[ni][si] += weights[si];
                    }
                }
            }

            for (int ni = 0; ni < numNodes; ni++) {
                if (tagProbs[ni] == 0) {
                    continue;
                }
                if (latentTagProbs[ni] == null) {
                    latentTagProbs[ni] = new double[numStates[ni]];
                    Arrays.fill(latentTagProbs[ni], tagProbs[ni] / numStates[ni]);
                } else {
                    double totalLatentScore = 0;
                    for (int si = 0; si < numStates[ni]; si++) {
                        latentTagProbs[ni][si] = Math.exp(latentTagProbs[ni][si]);
                        totalLatentScore += latentTagProbs[ni][si];
                    }
                    for (int si = 0; si < numStates[ni]; si++) {
                        latentTagProbs[ni][si] = tagProbs[ni] * latentTagProbs[ni][si] / totalLatentScore;
                    }
                }
            }
        }

        synchronized (oovLock) {
            for (int ni = 0; ni < numNodes; ni++) {
                if (latentTagProbs[ni] == null) {
                    continue;
                }
                if (latentTagGivenOOVProbs[ni] == null) {
                    latentTagGivenOOVProbs[ni] = new HashMap<String, double[]>();
                }
                latentTagGivenOOVProbs[ni].put(word, latentTagProbs[ni]);
                seenOOVSet.add(word);
            }
        }

        return latentTagProbs;
    }

    public HashMap<String, HashMap<Integer, DoubleArray>> getLatentTagGivenWordTagProbsMap() {
        return latentTagGivenWordTagProbsMap;
    }

    public HashMap<Integer, DoubleArray> getLatentTagExpectedCountsMap() {
        return latentTagExpectedCountsMap;
    }

    @Override
    public void setupCoarseLexiconManager(LexiconManager fineLexiconManager) {
        super.setupCoarseLexiconManager(fineLexiconManager);
        finerLexicon = (FeaturedLexiconManager) fineLexiconManager;

        latentTagGivenWordTagProbsMap = new HashMap<String, HashMap<Integer, DoubleArray>>();
        latentTagExpectedCountsMap = new HashMap<Integer, DoubleArray>();

        int[] fineNumStates = fineLexiconManager.numStates;
        int[][] ffine2coarseMapping = fineLexiconManager.fine2coarseMapping;

        for (Entry<String, HashMap<Integer, DoubleArray>> biEntry : finerLexicon.getLatentTagGivenWordTagProbsMap().entrySet()) {
            String word = biEntry.getKey();
            HashMap<Integer, DoubleArray> coarseLtwProbsMap = new HashMap<Integer, DoubleArray>();
            latentTagGivenWordTagProbsMap.put(word, coarseLtwProbsMap);
            for (Entry<Integer, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
                int tag = uniEntry.getKey();
                int fineStateNum = fineNumStates[tag];
                int coarseStateNum = numStates[tag];
                double[] fineProbs = uniEntry.getValue().getArray();
                double[] coarseProbs = new double[coarseStateNum];
                for (int si = 0; si < fineStateNum; si++) {
                    int csi = ffine2coarseMapping[tag][si];
                    coarseProbs[csi] += fineProbs[si];
                }
                coarseLtwProbsMap.put(tag, new DoubleArray(coarseProbs));
            }
        }

        for (Entry<Integer, DoubleArray> uniEntry : finerLexicon.getLatentTagExpectedCountsMap().entrySet()) {
            int tag = uniEntry.getKey();
            int fineStateNum = fineNumStates[tag];
            int coarseStateNum = numStates[tag];
            double[] fineCounts = uniEntry.getValue().getArray();
            double[] coarseCounts = new double[coarseStateNum];
            for (int si = 0; si < fineStateNum; si++) {
                int csi = ffine2coarseMapping[tag][si];
                coarseCounts[csi] += fineCounts[si];
            }
            latentTagExpectedCountsMap.put(tag, new DoubleArray(coarseCounts));
        }

        wordTagCountsMap = finerLexicon.wordTagCountsMap;
        wordTagSet = finerLexicon.wordTagSet;
    }

    @Override
    public double[] getProbs(int tag, String word, boolean viterbi) {
        double c_W = wordCountsMap.getCount(word);
        boolean seen = c_W != 0;
        double[] scores = new double[numStates[tag]];
        double[] probs = null;
        boolean seenOOV = false;
        if (seen && wordTagSet.contains(word, tag)) {
            probs = latentTagGivenWordProbs[tag].get(word);
        } else if (!seen) {
            synchronized (oovLock) {
                seenOOV = seenOOVSet.contains(word);
                if (seenOOV && latentTagGivenOOVProbs[tag] != null && latentTagGivenOOVProbs[tag].containsKey(word)) {
                    probs = latentTagGivenOOVProbs[tag].get(word);
                }
            }
        }

        if (probs == null) {
            probs = new double[numStates[tag]];
            if (!seen && !seenOOV) {
                double[][] latentTagProbs = getOOVLatentTagProbs(word);
                if (latentTagProbs[tag] != null) {
                    System.arraycopy(latentTagProbs[tag], 0, probs, 0, numStates[tag]);
                }
            }
        }

        for (int si = 0; si < numStates[tag]; si++) {
            double p_T_W = probs[si];
            double c_Tunseen = unseenLatentTagCounts[tag][si];
            double p_T_U = (totalUnseenTokens == 0) ? 1 : c_Tunseen / totalUnseenTokens;
            p_T_W = (1 - 0.0001) * p_T_W + 0.0001 * p_T_U;

            double[] expectedCounts = latentTagExpectedCounts[tag];
            if (seen) {
                scores[si] = p_T_W * c_W / expectedCounts[si];
            } else {
//                scores[si] = p_T_W * p_T_U / expectedNodeCounts[tag][si];
                scores[si] = p_T_W / expectedCounts[si];
            }
        }

//        scores = lexicon.smooth(scores, tag);
        if (viterbi) {
            for (int si = 0; si < numStates[tag]; si++) {
                scores[si] = Math.log(scores[si]);
            }
        }
        return scores;
    }

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
                BufferedReader inputReader = new BufferedReader(inputStreamReader);
                wordPredSet = new BiSet<String, String>();
                String line;
                while ((line = inputReader.readLine()) != null) {
                    line = line.trim();
                    List<String> items = Arrays.asList(line.trim().split(" +"));
                    String word = items.get(0).intern();
                    for (int i = 1; i < items.size(); i++) {
                        wordPredSet.add(word, items.get(i).intern());
                    }
                }
                inputReader.close();
                inputStreamReader.close();
            } catch (Exception ex) {
                Logger.getLogger(FeaturedLexiconManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // TODO: make sure initialization works properly
    @Override
    public void initParams() {
        super.initParams(); //To change body of generated methods, choose Tools | Templates.

        initPredStats();

        predLatentTagWeightsMap = new HashMap<String, HashMap<Integer, DoubleArray>>(); // initlaize to zero
        for (Entry<String, HashSet<Integer>> entry : predTagSet.entrySet()) {
            String pred = entry.getKey();
            HashMap<Integer, DoubleArray> nodeWeightMap = new HashMap<Integer, DoubleArray>();
            for (int ni : entry.getValue()) {
                nodeWeightMap.put(ni, new DoubleArray(new double[numStates[ni]])); // initialize to zero
            }
            predLatentTagWeightsMap.put(pred, nodeWeightMap);
        }

        wordTagDiffCountsMap = new BiCounter<String, Integer>();
        wordTagCountsMap = new BiCounter<String, Integer>();
        latentTagExpectedCountsMap = new HashMap<Integer, DoubleArray>(); // initlaize to zero

        for (Entry<Integer, UniCounter<String>> biEntry : tagWordCountsMap.entrySet()) {
            int tag = biEntry.getKey();
            for (Entry<String, Double> uniEntry : biEntry.getValue().entrySet()) {
                String word = uniEntry.getKey();
                double count = uniEntry.getValue();
                wordTagCountsMap.incrementCount(word, tag, count);
                wordTagDiffCountsMap.incrementCount(word, tag, 0);
            }
            latentTagExpectedCountsMap.put(tag, new DoubleArray(new double[numStates[tag]]));
        }

        wordLatentTagCountsMap = new HashMap<String, HashMap<Integer, DoubleArray>>(); // copy counts
        wordLatentTagDiffCountsMap = new HashMap<String, HashMap<Integer, DoubleArray>>(); // initialize to zero
        latentTagGivenWordTagProbsMap = new HashMap<String, HashMap<Integer, DoubleArray>>(); // initlaize to zero
        wordGivenLatentTagProbsMap = new HashMap<String, HashMap<Integer, DoubleArray>>(); // initlaize to zero
        for (Entry<String, HashSet<Integer>> wtEntry : wordTagSet.entrySet()) {
            String word = wtEntry.getKey();

            HashMap<Integer, DoubleArray> wltCountsMap = new HashMap<Integer, DoubleArray>();
            wordLatentTagCountsMap.put(word, wltCountsMap);

            HashMap<Integer, DoubleArray> wltDiffCountsMap = new HashMap<Integer, DoubleArray>();
            wordLatentTagDiffCountsMap.put(word, wltDiffCountsMap);

            HashMap<Integer, DoubleArray> ltwProbsMap = new HashMap<Integer, DoubleArray>();
            latentTagGivenWordTagProbsMap.put(word, ltwProbsMap);

            HashMap<Integer, DoubleArray> wltProbsMap = new HashMap<Integer, DoubleArray>();
            wordGivenLatentTagProbsMap.put(word, wltProbsMap);

            for (int ni : wtEntry.getValue()) {
                wltCountsMap.put(ni, latentTagWordCountsMap.get(ni, word));
                wltProbsMap.put(ni, latentTagWordProbsMap.get(ni, word));
                wltDiffCountsMap.put(ni, new DoubleArray(new double[numStates[ni]]));
                ltwProbsMap.put(ni, new DoubleArray(new double[numStates[ni]]));
            }
        }

        predLatentTagCountsMap = new HashMap<String, HashMap<Integer, DoubleArray>>(); // initlaize to zero
        predLatentTagExpectedCountsMap = new HashMap<String, HashMap<Integer, DoubleArray>>(); // initlaize to zero
        for (Entry<String, HashSet<Integer>> entry : predTagSet.entrySet()) {
            String pred = entry.getKey();

            HashMap<Integer, DoubleArray> tagObservationMap = new HashMap<Integer, DoubleArray>();
            predLatentTagCountsMap.put(pred, tagObservationMap);

            HashMap<Integer, DoubleArray> tagExpectationMap = new HashMap<Integer, DoubleArray>();
            predLatentTagExpectedCountsMap.put(pred, tagExpectationMap);

            for (int tag : entry.getValue()) {
                tagObservationMap.put(tag, new DoubleArray(new double[numStates[tag]])); // initialize to zero
                tagExpectationMap.put(tag, new DoubleArray(new double[numStates[tag]])); // initialize to zero
            }
        }

        initOptimizationParams();
    }

    public void initOptimizationParams() {
        calcNumFeatures();
        globalWeights = new double[numFeats];
        int[] featIndex = new int[1];
        initLexiconWeights();
        featIndex[0] = numLexFeats;
        if (optimizationMode == OptimizationMode.direct) {
            ruleManager.initFeatureRichWeights(globalWeights, featIndex);
        }
        globalGradients = new double[numFeats];
        diag = new double[numFeats];
        iflag[0] = 0;
        iprint[0] = -1;
        iprint[1] = 0;
    }

    public void setLanguage(Grammar.Language lang) {
        this.lang = lang;
    }

    public Grammar.Language getLanguage() {
        return lang;
    }

    public void setRegWeights(double lexRegWeight, double synRegWeight) {
        this.lexRegWeight = lexRegWeight;
        this.synRegWeight = synRegWeight;
    }

    /**
     * Tally the predicates in the training data
     */
    public void initPredStats() {
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

        predTagSet = new BiSet<String, Integer>();
        tagPredSet = new BiSet<Integer, String>();
        tagWordSet = new BiSet<Integer, String>();
        wordTagSet = new BiSet<String, Integer>();
        for (Entry<Integer, UniCounter<String>> biEntry : tagWordCountsMap.entrySet()) {
            int tag = biEntry.getKey();
            for (String word : biEntry.getValue().keySet()) {
                tagWordSet.add(tag, word);
                wordTagSet.add(word, tag);
                for (String pred : wordPredSet.get(word)) {
                    predTagSet.add(pred, tag);
                    tagPredSet.add(tag, pred);
                }
            }
        }

        predSet = new HashSet<String>();
        for (Entry<String, HashSet<String>> entry : wordPredSet.entrySet()) {
            String word = entry.getKey();
            if (wordCountsMap.containsKey(word)) {
                predSet.addAll(entry.getValue());
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
            int wordsPerJob = (int) Math.ceil(wordCountsMap.size() / numJobs);

            List<String> wordList = new ArrayList<String>();
            for (final String word : wordLatentTagDiffCountsMap.keySet()) {
                ti++;
                wordList.add(word);
                if (wordList.size() == wordsPerJob) {
                    final List<String> finalWordList = new ArrayList<String>(wordList);
                    wordList.clear();
                    Job job = new Job(new Runnable() {
                        @Override
                        public void run() {
                            compWordTagDiffCounts(finalWordList);
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
                        compWordTagDiffCounts(finalWordList);
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
     * Compute e'(w,t) = e(t) * p(w|t) e'(w) = sum_t e'(w,t) e*(w,t) = e(w,t) -
     * e'(w,t) e*(w) = e(w) - e'(w)
     *
     * @param wordList
     */
    public void compWordTagDiffCounts(List<String> wordList) {
        for (String word : wordList) {
            HashMap<Integer, DoubleArray> ltCountsMap = wordLatentTagCountsMap.get(word);
            HashMap<Integer, DoubleArray> ltDiffCountsMap = wordLatentTagDiffCountsMap.get(word);
            UniCounter<Integer> wsnCountDiff = wordTagDiffCountsMap.getCounter(word);
            HashMap<Integer, DoubleArray> wtProbsMap = wordGivenLatentTagProbsMap.get(word);
            for (Entry<Integer, DoubleArray> uniEntry : ltDiffCountsMap.entrySet()) {
                int ni = uniEntry.getKey();
                double[] ltCounts = nodeCounts[ni];
                double[] diffCounts = uniEntry.getValue().getArray();
                double[] probs = wtProbsMap.get(ni).getArray();
                double total = 0;
                if (!ltCountsMap.containsKey(ni)) {
                    throw new Error("unobserved word/tag pair: " + word + "/" + nodeList.get(ni));
                } else {
                    double[] observedCounts = ltCountsMap.get(ni).getArray();
                    for (int si = 0; si < numStates[ni]; si++) {
                        diffCounts[si] = observedCounts[si] - ltCounts[si] * probs[si];
                        total += diffCounts[si];
                    }
                }
                wsnCountDiff.setCount(ni, total);
            }
        }
    }

//    public void setObservedCounts() {
//        if (wordLatentTagCountsMap == null) {
//            wordLatentTagCountsMap = new HashMap<String, HashMap<Integer, DoubleArray>>();
//        }
//        for (Entry<Integer, HashMap<String, DoubleArray>> biEntry : latentTagWordCountsMap.entrySet()) {
//            int ni = biEntry.getKey();
//            double[] nCounts = latentTagCountsMap.get(ni).getArray();
//            Arrays.fill(nCounts, 0);
//            for (Entry<String, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
//                String word = uniEntry.getKey();
//                double[] counts = uniEntry.getValue().getArray();
//                for (int si = 0; si < numStates[ni]; si++) {
//                    nCounts[si] += counts[si];
//                }
//
//                // copy counts
//                HashMap<Integer, DoubleArray> nCountMap = wordLatentTagCountsMap.get(word);
//                if (nCountMap == null) {
//                    nCountMap = new HashMap<Integer, DoubleArray>();
//                    wordLatentTagCountsMap.put(word, nCountMap);
//                }
//                nCountMap.put(ni, uniEntry.getValue());
//            }
//        }
//    }
    public void initLexiconWeights() {
        int featIndex = 0;
        for (Entry<String, HashMap<Integer, DoubleArray>> biEntry : predLatentTagWeightsMap.entrySet()) {
            for (Entry<Integer, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
                double[] weights = uniEntry.getValue().getArray();
                for (int si = 0; si < weights.length; si++) {
                    globalWeights[featIndex] = weights[si];
                    featIndex++;
                }
            }
        }
    }

    /**
     * update feature weights
     *
     * @param newWeights
     */
    public void updateWeights() {
        int featIndex = 0;
//        double max_feat = Double.NEGATIVE_INFINITY;
//        double min_feat = Double.POSITIVE_INFINITY;
        for (Entry<String, HashMap<Integer, DoubleArray>> biEntry : predLatentTagWeightsMap.entrySet()) {
            for (Entry<Integer, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
                int ni = uniEntry.getKey();
                double[] weights = uniEntry.getValue().getArray();
                for (int si = 0; si < numStates[ni]; si++) {
                    weights[si] = globalWeights[featIndex];
//                    if (globalWeights[featIndex] > max_feat) {
//                        max_feat = globalWeights[featIndex];
//                    }
//                    if (globalWeights[featIndex] < min_feat) {
//                        min_feat = globalWeights[featIndex];
//                    }
                    featIndex++;
                }
            }
        }
//        System.out.printf("\n*********** max_weight = %.4f\n*********** min_weight = %.4f\n\n", max_feat, min_feat);
    }

    /**
     * Compute p(t|w)
     */
    public void parallelCompWordGivenLatentTagProb() {
        for (HashMap<Integer, DoubleArray> wtProbMap : wordGivenLatentTagProbsMap.values()) {
            for (DoubleArray probArray : wtProbMap.values()) {
                probArray.clearArray();
            }
        }
        JobManager jobManager = JobManager.getInstance();
        try {
            JobGroup grp = jobManager.createJobGroup("compRandS");
            int ti = 0;
            int maxWordNum = 1000;
//            int maxWordNum = wordNodeCountDiffMap.size();

            List<String> wordList = new ArrayList<String>();
            for (final String word : wordLatentTagDiffCountsMap.keySet()) {
                ti++;
                wordList.add(word);
                if (wordList.size() == maxWordNum) {
                    final List<String> finalWordList = new ArrayList<String>(wordList);
                    wordList.clear();
                    Job job = new Job(new Runnable() {
                        @Override
                        public void run() {
                            compWordGivenLatentTagProb(finalWordList);
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
                        compWordGivenLatentTagProb(finalWordList);
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
    public void compWordGivenLatentTagProb(List<String> wordList) {
        for (String word : wordList) {
            HashMap<Integer, DoubleArray> ltwProbMap = latentTagGivenWordTagProbsMap.get(word);
            HashMap<Integer, DoubleArray> wltPorbMap = wordGivenLatentTagProbsMap.get(word);
            UniCounter<Integer> wtCounter = wordTagCountsMap.getCounter(word);

            for (Entry<Integer, DoubleArray> ltwProbEntry : ltwProbMap.entrySet()) {
                int tag = ltwProbEntry.getKey();
                double[] ltwProbs = ltwProbEntry.getValue().getArray();
                double[] wltProbs = wltPorbMap.get(tag).getArray();
                double wtCount = wtCounter.getCount(tag);
                double[] ltProbs = latentTagExpectedCountsMap.get(tag).getArray();
                for (int si = 0; si < ltProbs.length; si++) {
                    wltProbs[si] = ltwProbs[si] * wtCount / ltProbs[si];
                }
            }
        }
    }

    /**
     * Compute p(t|w)
     */
    public void parallelCompLatentTagGivenWordProb() {
        for (HashMap<Integer, DoubleArray> nwProbMap : latentTagGivenWordTagProbsMap.values()) {
            for (DoubleArray probArray : nwProbMap.values()) {
                probArray.clearArray();
            }
        }
        for (DoubleArray nProbArray : latentTagExpectedCountsMap.values()) {
            nProbArray.clearArray();
        }

        JobManager jobManager = JobManager.getInstance();
        try {
            JobGroup grp = jobManager.createJobGroup("compRandS");
            int ti = 0;
            int maxWordNum = 1000;
            List<String> wordList = new ArrayList<String>();
            for (final String word : wordLatentTagDiffCountsMap.keySet()) {
                ti++;
                wordList.add(word);
                if (wordList.size() == maxWordNum) {
                    final List<String> finalWordList = new ArrayList<String>(wordList);
                    wordList.clear();
                    Job job = new Job(new Runnable() {
                        @Override
                        public void run() {
                            compNodeGivenWordProbAndNodeProb(finalWordList);
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
                        compNodeGivenWordProbAndNodeProb(finalWordList);
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
    public void compNodeGivenWordProbAndNodeProb(List<String> wordList) {
        for (String word : wordList) {
            HashMap<Integer, DoubleArray> nwProbMap = latentTagGivenWordTagProbsMap.get(word);
            UniCounter<Integer> wsnCounter = wordTagCountsMap.getCounter(word);
            for (String pred : wordPredSet.get(word)) {
                HashMap<Integer, DoubleArray> pnWeightMap = predLatentTagWeightsMap.get(pred);
                for (Entry<Integer, DoubleArray> entry : pnWeightMap.entrySet()) {
                    int node = entry.getKey();
                    if (!nwProbMap.containsKey(node)) {
                        continue;
                    }
                    DoubleArray probArray = nwProbMap.get(node);
                    double[] weights = entry.getValue().getArray();
                    probArray.add(weights);
                }
            }

            for (Entry<Integer, DoubleArray> nodeProbEntry : nwProbMap.entrySet()) {
                int ni = nodeProbEntry.getKey();
                double[] probs = nodeProbEntry.getValue().getArray();
                double totalProb = 0;
                for (int si = 0; si < probs.length; si++) {
                    probs[si] = Math.exp(probs[si]);
                    totalProb += probs[si];
                }
                for (int si = 0; si < probs.length; si++) {
                    probs[si] /= totalProb;
                }
                DoubleArray nodeProbArray = latentTagExpectedCountsMap.get(ni);
                nodeProbArray.add(probs, wsnCounter.getCount(ni));
            }
        }
    }

    /**
     * Compute the derivatives of features based on the fractional counts
     */
    public void parallelCompRandS() {
        for (Entry<String, HashMap<Integer, DoubleArray>> biEntry : predLatentTagWeightsMap.entrySet()) {
            String pred = biEntry.getKey();
            for (DoubleArray array : predLatentTagCountsMap.get(pred).values()) {
                Arrays.fill(array.getArray(), 0);
            }
            for (DoubleArray array : predLatentTagExpectedCountsMap.get(pred).values()) {
                Arrays.fill(array.getArray(), 0);
            }
        }

        JobManager jobManager = JobManager.getInstance();
        try {
            JobGroup grp = jobManager.createJobGroup("compRandS");
            int ti = 0;
            int maxWordNum = 1000;
            List<String> wordList = new ArrayList<String>();
            for (final String word : wordLatentTagDiffCountsMap.keySet()) {
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
            HashMap<Integer, DoubleArray> wnCountDiffMap = wordLatentTagDiffCountsMap.get(word);
            HashMap<Integer, DoubleArray> nwProbMap = latentTagGivenWordTagProbsMap.get(word);
            UniCounter<Integer> wsnCountDiffCounter = wordTagDiffCountsMap.getCounter(word);

            for (String pred : wordPredSet.get(word)) {
                HashMap<Integer, DoubleArray> nodeObservationMap = predLatentTagCountsMap.get(pred);
                HashMap<Integer, DoubleArray> nodeExpectationMap = predLatentTagExpectedCountsMap.get(pred);
                for (Entry<Integer, DoubleArray> entry : wnCountDiffMap.entrySet()) {
                    int node = entry.getKey();
                    DoubleArray countArray = entry.getValue();
                    DoubleArray probArray = nwProbMap.get(node);
                    double wsnCountDiff = wsnCountDiffCounter.getCount(node);

                    DoubleArray observationArray = nodeObservationMap.get(node);
                    DoubleArray expectationArray = nodeExpectationMap.get(node);

                    if (observationArray != null) {
                        observationArray.add(countArray.getArray());
                    }
                    if (expectationArray != null) {
                        expectationArray.add(probArray.getArray(), wsnCountDiff);
                    }
                }
            }
        }
    }

    /**
     * Compute the objective function for indirect EM optimization
     *
     * @param weights feature weights
     */
    public void compIndirectObjectiveScore() {
        objectiveScore = 0;
        for (Entry<String, HashMap<Integer, DoubleArray>> biEntry : wordLatentTagCountsMap.entrySet()) {
            String word = biEntry.getKey();
            HashMap<Integer, DoubleArray> wnProbMap = wordGivenLatentTagProbsMap.get(word);
            for (Entry<Integer, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
                int ni = uniEntry.getKey();
                double[] counts = uniEntry.getValue().getArray();
                double[] probs = wnProbMap.get(ni).getArray();
                for (int si = 0; si < numStates[ni]; si++) {
                    objectiveScore += counts[si] * Math.log(probs[si]);
                }
            }
        }
        if (optimizationMode == OptimizationMode.direct) {
            objectiveScore += ruleManager.compObjective();
        }
        double probLoss = -objectiveScore;
        double regLoss = compRegTerm();
        objectiveScore -= regLoss;
        objectiveScore *= -1;
//        System.out.printf(" probLoss = %.2f, regLoss = %.2f, totalLoss = %.2f", probLoss, regLoss, objectiveScore);
    }

    /**
     * Compute the regularization term
     *
     * @param weights
     * @return
     */
    public double compRegTerm() {
        double score = 0;
        for (int i = 0; i < numLexFeats; i++) {
            score += lexRegWeight * globalWeights[i] * globalWeights[i];
        }
        for (int i = numLexFeats; i < numFeats; i++) {
            score += synRegWeight * globalWeights[i] * globalWeights[i];
        }
        return score;
    }

    /**
     * Compute the number of features
     */
    public void calcNumFeatures() {
        numLexFeats = 0;
        for (HashMap<Integer, DoubleArray> ltWeightsMap : predLatentTagWeightsMap.values()) {
            for (DoubleArray weightsArray : ltWeightsMap.values()) {
                numLexFeats += weightsArray.getArray().length;
            }
        }
        int numFeatArray[] = new int[1];
        numFeatArray[0] = numLexFeats;
        if (optimizationMode == OptimizationMode.direct) {
            ruleManager.getNumFeatures(numFeatArray);
        }
        numFeats = numFeatArray[0];
    }

    /**
     * Compute the gradient of the objective.
     *
     * @param weights
     * @return
     */
    public void compGradients() {
        int featIndex = 0;
        for (Entry<String, HashMap<Integer, DoubleArray>> biEntry : predLatentTagWeightsMap.entrySet()) {
            String pred = biEntry.getKey();
            HashMap<Integer, DoubleArray> pltWeightsMap = biEntry.getValue();
            HashMap<Integer, DoubleArray> pltCountsMap = predLatentTagCountsMap.get(pred);
            HashMap<Integer, DoubleArray> pltExpectedCountsMap = predLatentTagExpectedCountsMap.get(pred);
            for (Entry<Integer, DoubleArray> uniEntry : pltWeightsMap.entrySet()) {
                int tag = uniEntry.getKey();
                double[] counts = pltCountsMap.get(tag).getArray();
                double[] expectedCounts = pltExpectedCountsMap.get(tag).getArray();
                for (int si = 0; si < numStates[tag]; si++) {
                    globalGradients[featIndex] = expectedCounts[si] - counts[si];
                    featIndex++;
                }
            }
        }

        int[] featIndexArray = new int[1];
        assert (featIndex == numLexFeats);
        featIndexArray[0] = featIndex;
        if (optimizationMode == OptimizationMode.direct) {
            ruleManager.compGraident(globalGradients, featIndexArray);
        }
        for (int si = 0; si < numLexFeats; si++) {
            globalGradients[si] += 2 * lexRegWeight * globalWeights[si];
        }
        for (int si = numLexFeats; si < numFeats; si++) {
            globalGradients[si] += 2 * synRegWeight * globalWeights[si];
        }
    }

    @Override
    public void mergeStates(int[] newNumStates) {
        for (HashMap<Integer, DoubleArray> pnWeightMaps : predLatentTagWeightsMap.values()) {
            for (Entry<Integer, DoubleArray> pnWeightEntry : pnWeightMaps.entrySet()) {
                int tag = pnWeightEntry.getKey();
                DoubleArray weightArray = pnWeightEntry.getValue();
                int oldStateNum = numStates[tag];
                int newStateNum = newNumStates[tag];

                assert newStateNum == oldStateNum * 2;
                double[] oldWeights = weightArray.getArray();
                double[] newWeights = new double[newStateNum];

                for (int osi = 0; osi < oldStateNum; osi++) {
                    int nsi = fine2coarseMapping[tag][osi];
                    newWeights[nsi] += oldWeights[osi];
                }
                weightArray.setArray(newWeights);
            }
        }

        resetParams(newNumStates);
        initOptimizationParams();
        parallelCompProbs();
    }

    @Override
    public void splitStates(int[] newNumStates) {
//        super.splitStates(newNumStates);
//        for (HashMap<Integer, DoubleArray> wnWeightMaps : wordGivenLatentTagProbsMap.values()) {
//            for (Entry<Integer, DoubleArray> wnWeightEntry : wnWeightMaps.entrySet()) {
//                int tag = wnWeightEntry.getKey();
//                int oldStateNum = numStates[tag];
//                int newStateNum = newNumStates[tag];
//                int splitFactor = newStateNum / oldStateNum;
//
//                DoubleArray probArray = wnWeightEntry.getValue();
//                double[] oldProbs = probArray.getArray();
//                double[] newProbs = new double[newStateNum];
//                for (int osi = 0; osi < oldStateNum; osi++) {
//                    for (int si = 0; si < splitFactor; si++) {
//                        int nsi = osi * splitFactor + si;
//                        newProbs[nsi] = oldProbs[osi];
//                    }
//                }
//                probArray.setArray(newProbs);
//            }
//        }

        for (HashMap<Integer, DoubleArray> biEntry : predLatentTagWeightsMap.values()) {
            for (Entry<Integer, DoubleArray> uniEntry : biEntry.entrySet()) {
                int tag = uniEntry.getKey();
                DoubleArray weightArray = uniEntry.getValue();
                int oldStateNum = numStates[tag];
                int newStateNum = newNumStates[tag];
                int splitFactor = newStateNum / oldStateNum;

                double[] oldWeights = weightArray.getArray();
                double[] newWeights = new double[newStateNum];

                for (int osi = 0; osi < oldStateNum; osi++) {
                    for (int si = 0; si < splitFactor; si++) {
                        int nsi = osi * splitFactor + si;
                        newWeights[nsi] = oldWeights[osi];
                    }
                }
                weightArray.setArray(newWeights);
            }
        }

        resetParams(newNumStates);
        initOptimizationParams();
        parallelCompProbs();
    }

    public void resetParams(int[] newNumStates) {
        for (Entry<String, HashSet<Integer>> ptEntry : predTagSet.entrySet()) {
            String pred = ptEntry.getKey();

            HashMap<Integer, DoubleArray> pltCountsMap = predLatentTagCountsMap.get(pred);
            HashMap<Integer, DoubleArray> pltExpectedCountsMap = predLatentTagExpectedCountsMap.get(pred);

            for (int ni : ptEntry.getValue()) {
                int newStateNum = newNumStates[ni];

                pltCountsMap.get(ni).setArray(new double[newStateNum]);
                pltExpectedCountsMap.get(ni).setArray(new double[newStateNum]);
            }
        }

        for (Entry<String, HashSet<Integer>> wtEntry : wordTagSet.entrySet()) {
            String word = wtEntry.getKey();

            HashMap<Integer, DoubleArray> wltCountsMap = wordLatentTagCountsMap.get(word);
            HashMap<Integer, DoubleArray> wltDiffCountsMap = wordLatentTagDiffCountsMap.get(word);
            HashMap<Integer, DoubleArray> ltwProbsMap = latentTagGivenWordTagProbsMap.get(word);
            HashMap<Integer, DoubleArray> wltProbsMap = wordGivenLatentTagProbsMap.get(word);
            for (int ni : wtEntry.getValue()) {
                int newStateNum = newNumStates[ni];

                wltCountsMap.get(ni).setArray(new double[newStateNum]);
                wltDiffCountsMap.get(ni).setArray(new double[newStateNum]);
                ltwProbsMap.get(ni).setArray(new double[newStateNum]);
                wltProbsMap.get(ni).setArray(new double[newStateNum]);
            }
        }


        for (Entry<Integer, DoubleArray> entry : latentTagExpectedCountsMap.entrySet()) {
            int tag = entry.getKey();
            DoubleArray array = entry.getValue();
            array.setArray(new double[newNumStates[tag]]);
        }
    }

    public void printWeights() {
        for (Entry<String, HashMap<Integer, DoubleArray>> biEntry : predLatentTagWeightsMap.entrySet()) {
            String pred = biEntry.getKey();
            System.out.println("=====================");
            for (Entry<Integer, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
                int tag = uniEntry.getKey();
                double[] weights = uniEntry.getValue().getArray();
                for (int si = 0; si < numStates[tag]; si++) {
                    System.out.printf("%s_%d->%s     %.2f\n", nodeList.get(tag), si, pred, weights[si]);
                }
            }
            System.out.println();
        }
    }

    /**
     * Return num_feat * reg_weight
     *
     * @return
     */
    public void parallelCompProbs() {
        parallelCompLatentTagGivenWordProb();
        parallelCompWordGivenLatentTagProb();

        if (optimizationMode == OptimizationMode.direct) {
            int[] featIndexArray = new int[1];
            featIndexArray[0] = numLexFeats;
            ruleManager.compFeatureRichProbs(globalWeights, featIndexArray);
        }
    }

    public void doIndirectOptimization() {

//        if (resetVariables) {
//            initVariables();
//            parallelCompProbs();
//            resetVariables = false;
//            iterNum = largetIterNum;
//        } else if (smoothingMode) {
//            int[] featIndexArray = new int[1];
//            featIndexArray[0] = numLexFeats;
//            if (direct) {
//                ruleManager.compFeatureRichProbs(globalWeights, featIndexArray);
//            }
//        } else {
////            filterRules();
//        }
        int n = numFeats;
        int m = 5;
        boolean diagco = false;
        double eps = 1.0e-5;
        double xtol = 1.0e-16;
        int icall = 0;
        iflag[0] = 0;

//        double ave_abs_diff = 0;
//        double total_abs_diff = 0;
        double max_abs_diff = 0;

        double startingObjective = 0;
        double endingObjective = 0;

        GlobalLogger.log(Level.FINE, String.format("Start indirect LBFGS training"));
        double prevObjectiveScore = Double.NEGATIVE_INFINITY;
        int numNoChanges = 0;
        do {
//            System.out.printf("%2d:", icall);
            parallelCompDiffCounts();
            parallelCompRandS();
            compIndirectObjectiveScore();
            if (icall == 0) {
                startingObjective = objectiveScore;
            }
            endingObjective = objectiveScore;

//            parallelCompApproxRandS();
//            compApproxObjectiveScore();

            compGradients();

//            total_abs_diff = 0;
//            max_abs_diff = 0;
//            int max_i = 0;
//            for (int i = 0; i < numFeats; i++) {
//                total_abs_diff += Math.abs(globalGradients[i]);
//                if (Math.abs(globalGradients[i]) > max_abs_diff) {
//                    max_abs_diff = Math.abs(globalGradients[i]);
//                    max_i = i;
//                }
//            }
//            ave_abs_diff = total_abs_diff / numFeats;
//            System.out.flush();
//            System.err.flush();
//            System.out.printf(", maxDelta = %.2f, totalDelta = %.2f", max_abs_diff, total_abs_diff);

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
                System.err.println("runLBFGS: lbfgs failed.\n" + e);
                return;
            }
//
//            double max_weight = Double.NEGATIVE_INFINITY;
//            double min_weight = Double.POSITIVE_INFINITY;
//            for (int i = 0; i < numFeats; i++) {
//                if (globalWeights[i] > max_weight) {
//                    max_weight = globalWeights[i];
//                }
//                if (globalWeights[i] < min_weight) {
//                    min_weight = globalWeights[i];
//                }
//            }
//            System.out.printf(", minWeight = %.2f, maxWeight = %.2f\n", min_weight, max_weight);

            updateWeights();
            parallelCompProbs();
//            ruleManager.filterRareRules();

            icall++;
            GlobalLogger.log(Level.FINE, String.format("%d-th LBFGS: %.2f", icall, objectiveScore));
        } while (iflag[0] != 0 && icall <= iterNum && numNoChanges < 5);
//        ruleManager.filterScores();
//        System.out.printf("starting objective: %.2f, ending objective: %.2f\n", startingObjective, endingObjective);
//        System.out.printf("LBFGS: %.2f -> %.2f\n", startingObjective, endingObjective);
    }

//    public void initDirectOptimization() {
//        direct = true;
//        iflag[0] = 0;
//        calcNumFeatures();
//        globalWeights = new double[numFeats];
//        int[] featIndex = new int[1];
//        initLexiconWeights();
//        featIndex[0] = numLexFeats;
//        ruleManager.initFeatureRichWeights(globalWeights, featIndex);
//        globalGradients = new double[numFeats];
//        diag = new double[numFeats];
//        //        diag = new double[numFeats];
//    }
//    public void initIndirectOptimization() {
//        direct = false;
//        iflag[0] = 0;
//        calcNumFeatures();
//        globalWeights = new double[numFeats];
//        int[] featIndex = new int[1];
//        initLexiconWeights();
//        featIndex[0] = numLexFeats;
////        ruleManager.initFeatureRichWeights(globalWeights, featIndex);
//        globalGradients = new double[numFeats];
//        diag = new double[numFeats];
//    }
    public void doDirectOptimization(double llr) {
        int n = numFeats;
        int m = 5;
        boolean diagco = false;
        double eps = 1.0e-5;
        double xtol = 1.0e-16;

        parallelCompDiffCounts();
        parallelCompRandS();
        double regLoss = compRegTerm();
        objectiveScore = llr - compRegTerm();
        objectiveScore *= -1;
//        System.out.printf(" probLoss = %.2f, regLoss = %.2f, totalLoss = %.2f", -llr, regLoss, objectiveScore);

        compGradients();

        try {
            LBFGS.lbfgs(n, m, globalWeights, objectiveScore, globalGradients, diagco, diag, iprint, eps, xtol, iflag);
        } catch (LBFGS.ExceptionWithIflag e) {
            System.err.println("runLBFGS: lbfgs failed.\n" + e);
            return;
        }
        double max_weight = Double.NEGATIVE_INFINITY;
        double min_weight = Double.POSITIVE_INFINITY;
        for (int i = 0; i < numFeats; i++) {
            if (globalWeights[i] > max_weight) {
                max_weight = globalWeights[i];
            }
            if (globalWeights[i] < min_weight) {
                min_weight = globalWeights[i];
            }
        }
//        System.out.printf(", minWeight = %.2f, maxWeight = %.2f\n", min_weight, max_weight);
        updateWeights();
        parallelCompProbs();
    }

    @Override
    public void doMStep() {
//        super.doMStep();
        parallelCompProbs();
    }

    @Override
    public void doMStep(double logLikelihood) {
        switch (optimizationMode) {
            case direct:
                doDirectOptimization(logLikelihood);
                break;
            case indirect:
                doIndirectOptimization();
                break;
        }
    }
}
