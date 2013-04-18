/*
 * LexiconManager.java
 *
 * Created on Sep 25, 2007, 2:37:26 PM
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.util.BiCounter;
import edu.umd.clip.util.BiMap;
import edu.umd.clip.util.Pair;
import edu.umd.clip.util.UniCounter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class LexiconManager implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    protected UniCounter<String> wordCountsMap = new UniCounter<String>();
    protected UniCounter<Integer> tagCountsMap = new UniCounter<Integer>();
    protected BiCounter<Integer, String> tagWordCountsMap = new BiCounter<Integer, String>();
    protected double[] tagCounts;
    protected UniCounter<String>[] tagWordCounts;
    protected BiMap<Integer, String, DoubleArray> latentTagWordProbsMap = new BiMap<Integer, String, DoubleArray>();
    protected BiMap<Integer, String, DoubleArray> latentTagWordCountsMap = new BiMap<Integer, String, DoubleArray>();
    protected Map<String, DoubleArray>[] latentTagWordCounts;
    protected double[] unseenTagCounts;
    protected double[][] unseenLatentTagCounts;
    protected double totalUnseenTokens;
    protected double totalTokens;
    protected double rareWordThreshold = 11.99;
    protected double unknownSmoothingThreshold = 100;
    protected double wordSmoothingParam = 0.1;
    protected double affixSmoothingParam = 0.1;
    protected boolean smoothingMode = false;
    protected boolean trainedWithPunc = true;
    protected boolean trainedWithPuncChecked = false;
    //
    protected Map<String, Integer> nodeMap;
    protected List<String> nodeList;
    protected int numNodes;
    protected int[] numStates;
    protected boolean[] isPhrasalNode; // True for phrasal nodes, False for POS nodes
    protected double[][][] smoothingMatrix;
    protected int[][] fine2coarseMapping; // map the current state to the coarser state, initialized before coarse-to-fine parsing
    protected double[][] nodeCounts;
    protected OOVHandler oovHandler = OOVHandler.heuristic;

    public void setupGrammar(Grammar grammar) {
        nodeMap = grammar.getNodeMap();
        nodeList = grammar.getNodeList();
        numNodes = grammar.getNumNodes();
        numStates = grammar.getNumStates();
        isPhrasalNode = grammar.getIsPhrasalNode();
        smoothingMatrix = grammar.getSmoothingMatrix();
        fine2coarseMapping = grammar.getFine2coarseMapping();
        nodeCounts = grammar.getNodeCounts();
    }

    double[][] getUnseenLatentTagCounts() {
        return unseenLatentTagCounts;
    }

    public double getTotalUnseenTokens() {
        return totalUnseenTokens;
    }

    public int getNumNodes() {
        return numNodes;
    }

    protected enum OOVHandler {

        simple, heuristic
    }

    public void setOovHandler(OOVHandler oovHandler) {
        this.oovHandler = oovHandler;
    }

    public boolean isRuleManagerUpdated() {
        return false;
    }

    public void setupArray() {
        totalTokens = wordCountsMap.getCount();

        latentTagWordCounts = new Map[numNodes];
        for (Entry<Integer, HashMap<String, DoubleArray>> biEntry : latentTagWordCountsMap.entrySet()) {
            latentTagWordCounts[biEntry.getKey()] = biEntry.getValue();
        }
        latentTagWordCountsMap = null;
        latentTagWordProbsMap = null;

        tagWordCounts = new UniCounter[numNodes];
        for (Entry<Integer, UniCounter<String>> biEntry : tagWordCountsMap.entrySet()) {
            tagWordCounts[biEntry.getKey()] = biEntry.getValue();
        }
        tagWordCountsMap = null;

        tagCounts = new double[numNodes];
        for (Entry<Integer, Double> uniEntry : tagCountsMap.entrySet()) {
            tagCounts[uniEntry.getKey()] = uniEntry.getValue();
        }

        totalUnseenTokens = 0;
        for (Entry<String, Double> uniEntry : wordCountsMap.entrySet()) {
            double count = uniEntry.getValue();
            if (count < rareWordThreshold) {
                totalUnseenTokens += count;
            }
        }

        unseenTagCounts = new double[numNodes];
        for (int ni = 0; ni < numNodes; ni++) {
            if (tagWordCounts[ni] == null) {
                continue;
            }
            for (Entry<String, Double> uniEntry : tagWordCounts[ni].entrySet()) {
                String word = uniEntry.getKey();
                double count = uniEntry.getValue();
                if (wordCountsMap.getCount(word) < rareWordThreshold) {
                    unseenTagCounts[ni] += count;
                }
            }
        }

        unseenLatentTagCounts = new double[numNodes][];
        for (int ni = 0; ni < numNodes; ni++) {
            if (latentTagWordCounts[ni] == null) {
                continue;
            }
            unseenLatentTagCounts[ni] = new double[numStates[ni]];
            for (Entry<String, DoubleArray> uniEntry : latentTagWordCounts[ni].entrySet()) {
                String word = uniEntry.getKey();
                double[] counts = uniEntry.getValue().getArray();
                if (wordCountsMap.getCount(word) < rareWordThreshold) {
                    for (int si = 0; si < numStates[ni]; si++) {
                        unseenLatentTagCounts[ni][si] += counts[si];
                    }
                }
            }
        }
    }

    public void checkTrainedWithPunc() {
        if (!trainedWithPuncChecked) {
            trainedWithPuncChecked = true;
            trainedWithPunc = false;
            if (nodeMap.containsKey("PU")) {
                trainedWithPunc = true;
            }
        }
    }

    public double getRareWordThreshold() {
        return rareWordThreshold;
    }

    public void setRareWordThreshold(double rareWordThreshold) {
        this.rareWordThreshold = rareWordThreshold;
    }

    /**
     * Add lexical rule and count words
     *
     * @param tag
     * @param word
     */
    public synchronized void addLexicalRule(int tag, String word, double weight) {
        wordCountsMap.incrementCount(word, weight);
        tagCountsMap.incrementCount(tag, weight);
        tagWordCountsMap.incrementCount(tag, word, weight);
    }

    public void tallyNodeCounts() {
        for (Entry<Integer, HashMap<String, DoubleArray>> biEntry : latentTagWordCountsMap.entrySet()) {
            int tag = biEntry.getKey();
            int stateNum = numStates[tag];
            // update tagCountMap
            double[] stateCounts = nodeCounts[tag];
            Arrays.fill(stateCounts, 0);
            for (DoubleArray uniValue : biEntry.getValue().values()) {
                double[] twCounts = uniValue.getArray();
                for (int si = 0; si < stateNum; si++) {
                    stateCounts[si] += twCounts[si];
                }
            }
        }
    }

    /**
     * Starting from the counts in tagWordCountMap, compute the counts for
     * tagCountMap, and then doMStep the latentTagWordCounts to obtain
     * latentTagWordProbs.
     */
    public void doMStep() {
        tieRareWordCounts();

        for (Entry<Integer, HashMap<String, DoubleArray>> biEntry : latentTagWordCountsMap.entrySet()) {
            // doMStep tagWordCountMap to get tagWordProbMap

            int tag = biEntry.getKey();
            int tagStateNum = numStates[tag];
            double[] stateCounts = nodeCounts[tag];

            HashMap<String, DoubleArray> twProbMap = latentTagWordProbsMap.get(tag);

            for (Entry<String, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
                String word = uniEntry.getKey();
                double[] twCounts = uniEntry.getValue().getArray();
                DoubleArray twProbDA = twProbMap.get(word);
                if (twProbDA == null) {
                    twProbDA = new DoubleArray(new double[tagStateNum]);
                    twProbMap.put(word, twProbDA);
                }
                double[] twProbs = twProbDA.getArray();
                if (!ArrayMath.hasSize(twProbs, tagStateNum)) {
                    twProbs = new double[tagStateNum];
                    twProbDA.setArray(twProbs);
                }
                for (int si = 0; si < tagStateNum; si++) {
                    twProbs[si] = twCounts[si] / stateCounts[si];
                }
            }
        }
    }

    public void doMStep(double logLikelihood) {
        doMStep();
    }

    /**
     * tie rare word count if not using the feature rich lexical model
     */
    public void tieRareWordCounts() {
        for (Entry<Integer, HashMap<String, DoubleArray>> biEntry : latentTagWordCountsMap.entrySet()) {
            int tag = biEntry.getKey();
            int tagStateNum = numStates[tag];

            double[] tagRareWordCounts = new double[tagStateNum];
            for (Entry<String, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
                String word = uniEntry.getKey();

                if (wordCountsMap.getCount(word) < rareWordThreshold) {
                    double[] twCounts = uniEntry.getValue().getArray();
                    for (int si = 0; si < tagStateNum; si++) {
                        tagRareWordCounts[si] += twCounts[si];
                    }
                }
            }
            double totalTagRareWordCount = 0;
            for (int si = 0; si < tagStateNum; si++) {
                totalTagRareWordCount += tagRareWordCounts[si];
            }

            for (Entry<String, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
                String word = uniEntry.getKey();
                if (wordCountsMap.getCount(word) < rareWordThreshold) {
                    double[] wCounts = uniEntry.getValue().getArray();
                    double totalTagWordCount = 0;
                    for (int si = 0; si < tagStateNum; si++) {
                        totalTagWordCount += wCounts[si];
                    }
                    for (int si = 0; si < tagStateNum; si++) {
                        wCounts[si] = tagRareWordCounts[si] / totalTagRareWordCount * totalTagWordCount;
                    }
                }
            }
        }
    }

    public void setWordCounter(UniCounter<String> wordCounter) {
        this.wordCountsMap = wordCounter;
    }

    public UniCounter<String> getWordCountsMap() {
        return wordCountsMap;
    }

    public UniCounter<Integer> getTagCountsMap() {
        return tagCountsMap;
    }

    public BiCounter<Integer, String> getTagWordCountsMap() {
        return tagWordCountsMap;
    }

    public UniCounter<String>[] getTagWordCounts() {
        return tagWordCounts;
    }

    public HashMap<Integer, HashMap<String, DoubleArray>> getLatentTagWordCountsMap() {
        return latentTagWordCountsMap;
    }

    public void setTagWordCountMap(BiMap<Integer, String, DoubleArray> tagWordCountMap) {
        this.latentTagWordCountsMap = tagWordCountMap;
    }

    public void setTagWordProbMap(BiMap<Integer, String, DoubleArray> tagWordProbMap) {
        this.latentTagWordProbsMap = tagWordProbMap;
    }

    public void addCounts(int node, String word, double count) {
        assert numStates[node] == 1;
        double[] counts = new double[1];
        counts[0] = count;
        addCounts(node, word, counts);
    }

    public void addCounts(int node, String word, double[] counts) {
        DoubleArray wordArray = latentTagWordCountsMap.get(node).get(word);
        wordArray.add(counts);
    }

    public void setupCoarseLexiconManager(LexiconManager fineLexiconManager) {
        int[] fineNumStates = fineLexiconManager.numStates;
        int[][] ffine2coarseMapping = fineLexiconManager.fine2coarseMapping;
        for (Entry<Integer, HashMap<String, DoubleArray>> biEntry : fineLexiconManager.getLatentTagWordCountsMap().entrySet()) {
            int tag = biEntry.getKey();

            HashMap<String, DoubleArray> coarserTwCountMap = new HashMap<String, DoubleArray>();
            latentTagWordCountsMap.put(tag, coarserTwCountMap);

            HashMap<String, DoubleArray> coarserTwProbMap = new HashMap<String, DoubleArray>();
            latentTagWordProbsMap.put(tag, coarserTwProbMap);

            int fineStateNum = fineNumStates[tag];
            int coarseStateNum = numStates[tag];
            for (Entry<String, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
                String word = uniEntry.getKey();
                double[] twCounts = uniEntry.getValue().getArray();
                double[] coarseTwCounts = new double[coarseStateNum];
                for (int si = 0; si < fineStateNum; si++) {
                    int csi = ffine2coarseMapping[tag][si];
                    coarseTwCounts[csi] += twCounts[si];
                }
                coarserTwCountMap.put(word, new DoubleArray(coarseTwCounts));
                coarserTwProbMap.put(word, new DoubleArray(new double[coarseStateNum]));
            }
        }
        wordCountsMap = fineLexiconManager.getWordCountsMap();
        tagCountsMap = fineLexiconManager.getTagCountsMap();
        tagWordCountsMap = fineLexiconManager.getTagWordCountsMap();
        rareWordThreshold = fineLexiconManager.getRareWordThreshold();
    }

    public void initParams() {
        for (Entry<Integer, UniCounter<String>> biEntry : tagWordCountsMap.entrySet()) {
            int tag = biEntry.getKey();
            UniCounter<String> wordCounter = biEntry.getValue();
            HashMap<String, DoubleArray> twCountMap = new HashMap<String, DoubleArray>();
            HashMap<String, DoubleArray> twProbMap = new HashMap<String, DoubleArray>();
            for (Entry<String, Double> uniEntry : wordCounter.entrySet()) {
                String word = uniEntry.getKey();
                double count = uniEntry.getValue();
                double[] counts = new double[1];
                counts[0] = count;
                twCountMap.put(word, new DoubleArray(counts));
                twProbMap.put(word, new DoubleArray(new double[1]));
            }
            latentTagWordCountsMap.put(tag, twCountMap);
            latentTagWordProbsMap.put(tag, twProbMap);
        }
    }

    /**
     * Reset latentTagWordCounts to zero and make sure array length matches
     * (after splitting and merging)
     */
    public void resetCounts() {
        for (Entry<Integer, UniCounter<String>> biEntry : tagWordCountsMap.entrySet()) {
            int tag = biEntry.getKey();
            int tagStateNum = numStates[tag];
            UniCounter<String> wordCounter = biEntry.getValue();
            HashMap<String, DoubleArray> twCountMap = latentTagWordCountsMap.get(tag);
            if (twCountMap == null) {
                twCountMap = new HashMap<String, DoubleArray>();
                latentTagWordCountsMap.put(tag, twCountMap);
            }
            for (String word : wordCounter.keySet()) {
                DoubleArray array = twCountMap.get(word);
                if (array == null) {
                    array = new DoubleArray(new double[tagStateNum]);
                    twCountMap.put(word, array);
                } else {
                    double[] counts = array.getArray();
                    if (counts.length == tagStateNum) {
                        Arrays.fill(counts, 0);
                    } else {
                        array.setArray(new double[tagStateNum]);
                    }
                }
            }
        }
    }

    public void setSmoothingMode(boolean smoothingMode) {
        this.smoothingMode = smoothingMode;
    }

    /**
     * Initialize latentTagWordProbs after splitting. All other counts are not
     * changed
     */
    public void splitStates(int[] newNumStates) {
//        if (featureRichLexicon != null) {
        //TODO check
//            featureRichLexicon.splitStates();
//        }
        for (Entry<Integer, HashMap<String, DoubleArray>> biEntry : latentTagWordProbsMap.entrySet()) {
            int tag = biEntry.getKey();
            int oldStateNum = numStates[tag];
            int newStateNum = newNumStates[tag];
            int splitFactor = newStateNum / oldStateNum;

            HashMap<String, DoubleArray> twProbMap = biEntry.getValue();
            for (DoubleArray twProbDA : twProbMap.values()) {
                double[] probs = twProbDA.getArray();
                double[] newProbs = new double[newStateNum];
                for (int osi = 0; osi < probs.length; osi++) {
                    for (int si = 0; si < splitFactor; si++) {
                        int nsi = osi * splitFactor + si;
                        newProbs[nsi] = probs[osi];
                    }
                }
                twProbDA.setArray(newProbs);
            }
        }
    }

    //TODO: double check state.setCount line to make sure it is needed.
    public void accumNodeEntropy(double[][] nodeEntropy) {
        for (Entry<Integer, HashMap<String, DoubleArray>> biEntry : latentTagWordProbsMap.entrySet()) {
            int tag = biEntry.getKey();
            double[] stateEntropy = nodeEntropy[tag];
            int tagStateNum = numStates[tag];
            for (DoubleArray twProbDA : biEntry.getValue().values()) {
                double[] twProbs = twProbDA.getArray();
                for (int si = 0; si < tagStateNum; si++) {
                    double prob = twProbs[si];
                    if (prob < Grammar.ruleFilteringThreshold) {
                        continue;
                    }
                    stateEntropy[si] += -prob * Math.log(prob);
                }
            }
        }
    }

    /**
     * Return the word emission probabilities
     *
     * @param tag
     * @param word
     * @return
     */
    public double[] getProbs(int tag, String word) {
        double[] probs = new double[numStates[tag]];
        System.arraycopy(latentTagWordProbsMap.get(tag).get(word).getArray(), 0, probs, 0, numStates[tag]);
        if (smoothingMode) {
            double[][] smoothingWeights = smoothingMatrix[tag];
            double newProbs[] = new double[numStates[tag]];
            for (int i = 0; i < numStates[tag]; i++) {
                for (int j = 0; j < numStates[tag]; j++) {
                    newProbs[i] += smoothingWeights[i][j] * probs[j];
                }
            }
            probs = newProbs;
        }
        return probs;
    }

    public double[] getProbs(int tag, String word, boolean viterbi) {
        double[] probs = getProbs(tag, word);
        if (viterbi) {
            for (int si = 0; si < probs.length; si++) {
                probs[si] = Math.log(probs[si]);
            }
        }
        return probs;
    }

    /**
     * Only merge and update tagWordCountMap and do not change anything else
     */
    public void mergeStates(int[] newNumStates) {
        for (Entry<Integer, HashMap<String, DoubleArray>> biEntry : latentTagWordCountsMap.entrySet()) {
            Integer tag = biEntry.getKey();
            int oldStateNum = numStates[tag];
            int newStateNum = newNumStates[tag];
            for (Entry<String, DoubleArray> uniEntry : biEntry.getValue().entrySet()) {
                DoubleArray countsArray = uniEntry.getValue();
                double[] oldCounts = countsArray.getArray();
                double[] newCounts = new double[newStateNum];
                for (int osi = 0; osi < oldStateNum; osi++) {
                    int nsi = fine2coarseMapping[tag][osi];
                    newCounts[nsi] += oldCounts[osi];
                }
                countsArray.setArray(newCounts);
            }
        }
    }

    public void reportTopWords(int top, Set<String> wordSet) {
        Comparator<Pair<String, Double>> pairComparator = new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                return Double.compare(o2.getSecond(), o1.getSecond());
            }
        };
        for (Entry<Integer, HashMap<String, DoubleArray>> biEntry : latentTagWordProbsMap.entrySet()) {
            int tag = biEntry.getKey();
            HashMap<String, DoubleArray> wordEntry = biEntry.getValue();
            if (wordEntry.isEmpty()) {
                continue;
            }
            int tagStateNum = numStates[tag];
            List<Pair<String, Double>>[] wordCountLists = new ArrayList[tagStateNum];
            for (int si = 0; si < tagStateNum; si++) {
                wordCountLists[si] = new ArrayList<Pair<String, Double>>();
            }
            for (Entry<String, DoubleArray> uniEntry : wordEntry.entrySet()) {
                String word = uniEntry.getKey();
                if (!wordSet.contains(word)) {
                    continue;
                }
                double[] counts = uniEntry.getValue().getArray();
                for (int si = 0; si < tagStateNum; si++) {
                    wordCountLists[si].add(new Pair(word, counts[si]));
                }
            }
            for (int si = 0; si < tagStateNum; si++) {
                if (wordCountLists[si].isEmpty()) {
                    continue;
                }
                Collections.sort(wordCountLists[si], pairComparator);
                System.out.print(tag + ":" + si);
                int min = Math.min(top, wordCountLists[si].size());
                for (int wi = 0; wi < min; wi++) {
                    Pair<String, Double> pair = wordCountLists[si].get(wi);
                    System.out.printf(" %s_%.2f", pair.getFirst(), pair.getSecond());
                }
                System.out.println();
            }
        }
    }
}
