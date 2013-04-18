/*
 * LatentEmission.java
 *
 * Created on May 21, 2007, 1:44 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.util.BiCounter;
import edu.umd.clip.util.BiMap;
import edu.umd.clip.util.BiSet;
import edu.umd.clip.util.UniCounter;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.umd.clip.util.Numberer;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author zqhuang
 */
public class LatentEmission implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private boolean latentSmoothingFlag = false;
    private double latentSmoothingParam = 0.1;
    private double totalTokens;
    private BiSet<String, Integer> wordTagSet;
    /* holds physical counts. */
    private BiCounter<Integer, String> wordBigramCount;
    private BiCounter<Integer, String> wordBigramProb;
    private UniCounter<String> wordUnigramCount;

    /* holds trianing word probability */
    private BiMap<Integer, String, LatentBigramEmissionItem> wordLatentBigramProb;
    private BiMap<Integer, String, LatentBigramEmissionItem> wordLatentBigramCount;

    public void reportTagWordStats(int topk) {
        Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

        BiCounter<String, Integer> wordTagCount = new BiCounter<String, Integer>();
        for (Entry<Integer, UniCounter<String>> biEntry : wordBigramCount.entrySet()) {
            Integer tag = biEntry.getKey();
            UniCounter<String> uniCounter = biEntry.getValue();
            for (Entry<String, Double> uniEntry : uniCounter.entrySet()) {
                String word = uniEntry.getKey();
                Double count = uniEntry.getValue();
                wordTagCount.setCount(word, tag, count);
            }
        }

        Map<String, String> wordTags = new HashMap<String, String>();
        for (Entry<String, UniCounter<Integer>> biEntry : wordTagCount.entrySet()) {
            String word = biEntry.getKey();
            UniCounter<Integer> uniCounter = biEntry.getValue();
            double wordCount = wordTagCount.getCount(word);
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Entry<Integer, Double> uniEntry : uniCounter.entrySet()) {
                Integer tag = uniEntry.getKey();
                double tagCount = uniEntry.getValue();
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(String.format("%s:%.0f", tagNumberer.object(tag), tagCount));
            }
            wordTags.put(word, sb.toString());
        }

        for (Entry<Integer, HashMap<String, LatentBigramEmissionItem>> biEntry : wordLatentBigramCount.entrySet()) {
            Integer tag = biEntry.getKey();
            HashMap<String, LatentBigramEmissionItem> uniMap = biEntry.getValue();
            System.out.println(tagNumberer.object(tag));
            int numStates = LatentTagStates.getLatentStateNum(tag);
            List<WordCount>[] wordCountLists = new List[numStates];
            for (int si = 0; si < numStates; si++) {
                wordCountLists[si] = new ArrayList<WordCount>();
            }
            for (Entry<String, LatentBigramEmissionItem> uniEntry : uniMap.entrySet()) {
                String word = uniEntry.getKey();
                LatentBigramEmissionItem item = uniEntry.getValue();
                double[] scores = item.getLatentScores();
                for (int si = 0; si < numStates; si++) {
                    wordCountLists[si].add(new WordCount(word, scores[si]));
                }
            }
            for (int si = 0; si < numStates; si++) {
                List<WordCount> wordCountList = wordCountLists[si];
                Collections.sort(wordCountList);
                int n = Math.min(topk, wordCountList.size());
                System.out.print("  " + si + ": ");
                for (int wi = 0; wi < n; wi++) {
                    WordCount wordCount = wordCountList.get(wi);
                    System.out.print(wordCount + "_" + wordTags.get(wordCount.getWord()) + " ");
                }
                System.out.println();
            }
        }
    }

    public void saveLexicon(String fileName) {
        PrintWriter outputWriter = null;
        try {
            outputWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), Charset.forName("UTF-8"))));
            for (Entry<String, Double> entry : wordUnigramCount.entrySet()) {
                outputWriter.println(entry.getKey());
            }
            outputWriter.flush();
            outputWriter.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LatentEmission.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            outputWriter.close();
        }
    }

    public LatentEmission shallowClone() {
        try {
            return (LatentEmission) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(LatentEmission.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public LatentEmission clone() throws CloneNotSupportedException {
        LatentEmission newLatentEmission = (LatentEmission) super.clone();

        newLatentEmission.wordTagSet = wordTagSet.clone();
        newLatentEmission.wordUnigramCount = wordUnigramCount.clone();
        newLatentEmission.wordBigramCount = wordBigramCount.clone();
        newLatentEmission.wordBigramProb = wordBigramProb.clone();
        newLatentEmission.wordLatentBigramProb = copyEmissionBiMap(wordLatentBigramProb);
        newLatentEmission.wordLatentBigramCount = copyEmissionBiMap(wordLatentBigramCount);
        return newLatentEmission;
    }

    public LatentEmission getCoarserEmission(Tree<Integer>[] splitTrees, int numSplits) {
        LatentEmission coarserEmission = new LatentEmission();

        coarserEmission.wordTagSet = wordTagSet;
        coarserEmission.wordUnigramCount = wordUnigramCount;
        coarserEmission.wordBigramCount = wordBigramCount;
        coarserEmission.wordBigramProb = wordBigramProb;
        coarserEmission.totalTokens = totalTokens;
        coarserEmission.latentSmoothingFlag = latentSmoothingFlag;
        coarserEmission.latentSmoothingParam = latentSmoothingParam;
        coarserEmission.wordLatentBigramCount = new BiMap<Integer, String, LatentBigramEmissionItem>();
        for (Entry<Integer, HashMap<String, LatentBigramEmissionItem>> biEntry : wordLatentBigramCount.entrySet()) {
            Integer currTag = biEntry.getKey();
            List<Tree<Integer>> splitTreeList = splitTrees[currTag].getAtDepth(numSplits - 1);
            Map<String, LatentBigramEmissionItem> uniCount = biEntry.getValue();
            for (Entry<String, LatentBigramEmissionItem> uniEntry : uniCount.entrySet()) {
                String currWord = uniEntry.getKey();
                LatentBigramEmissionItem finerItem = uniEntry.getValue();
                LatentBigramEmissionItem coarserItem = new LatentBigramEmissionItem(splitTreeList.size());
                coarserItem.mergeStates(splitTreeList, finerItem.getLatentScores());
                coarserEmission.wordLatentBigramCount.put(currTag, currWord, coarserItem);
            }
        }
        coarserEmission.wordLatentBigramProb = copyEmissionBiMap(coarserEmission.wordLatentBigramCount);
        normalizeBigramProb(coarserEmission.wordLatentBigramProb);

        return coarserEmission;
    }

    public boolean hasSeenWord(String word) {
        return wordUnigramCount.containsKey(word);
    }

    /**
     * Indicate whether to use latent smoothing or not, <code>true</code> to activate smoothing,
     * <code>false</code> to deactivate.
     *
     * @param latentSmoothingFlag
     */
    public void setLatentSmoothingFlag(boolean latentSmoothingFlag) {
        this.latentSmoothingFlag = latentSmoothingFlag;
    }

    private BiMap<Integer, String, LatentBigramEmissionItem> copyEmissionBiMap(BiMap<Integer, String, LatentBigramEmissionItem> biMap) {
        BiMap<Integer, String, LatentBigramEmissionItem> newBiMap = new BiMap<Integer, String, LatentBigramEmissionItem>();
        for (Entry<Integer, HashMap<String, LatentBigramEmissionItem>> biEntry : biMap.entrySet()) {
            int tag = biEntry.getKey();
            Map<String, LatentBigramEmissionItem> uniMap = biEntry.getValue();
            for (Entry<String, LatentBigramEmissionItem> uniEntry : uniMap.entrySet()) {
                String word = uniEntry.getKey();
                LatentBigramEmissionItem emissionItem = uniEntry.getValue();
                LatentBigramEmissionItem newEmissionItem = new LatentBigramEmissionItem(emissionItem);
                newBiMap.put(tag, word, newEmissionItem);
            }
        }
        return newBiMap;
    }

    private HashMap<Integer, LatentUnigramTransitionItem> copyTransitionHashMap(HashMap<Integer, LatentUnigramTransitionItem> uniMap) {
        HashMap<Integer, LatentUnigramTransitionItem> newHashMap = new HashMap<Integer, LatentUnigramTransitionItem>();
        for (Integer tag : uniMap.keySet()) {
            LatentUnigramTransitionItem transitionItem = uniMap.get(tag);
            LatentUnigramTransitionItem newTransitionItem = new LatentUnigramTransitionItem(transitionItem);
            newHashMap.put(tag, newTransitionItem);
        }
        return newHashMap;
    }

    public BiSet<String, Integer> getWordTagSet() {
        return wordTagSet;
    }

    public Set<Integer> getWordTagSet(String word) {
        return wordTagSet.get(word);
    }

    public void setLatentSmoothingParam(double latentSmoothingParam) {
        this.latentSmoothingParam = latentSmoothingParam;
    }

    /**
     * Read counts from collection <code>stateCollection</code> and initialize
     * surface word bigram and char bigram probabilities.
     *
     * @param stateCollection
     */
    public void tallyCounts(Collection<AlphaBetaSequence> trainingCorpus) {
        wordTagSet = new BiSet<String, Integer>();
        wordUnigramCount = new UniCounter<String>();
        wordBigramCount = new BiCounter<Integer, String>();
        totalTokens = 0;

        for (AlphaBetaSequence sequence : trainingCorpus) {
            double weight = sequence.getWeight();
            for (int i = 0; i < sequence.size(); i++) {
                addCounts(sequence.get(i).getWordTagItem(), weight);
            }
        }
        computeSurfaceProbability();
    }

    private void computeSurfaceProbability() {
        wordBigramProb = wordBigramCount.clone();
        wordBigramProb.normalize();
    }

    private void addCounts(WordTagItem currItem, double weight) {
        Integer tag = currItem.getTag();
        String word = currItem.getWord();

        wordTagSet.add(word, tag);
        wordUnigramCount.incrementCount(word, weight);
        wordBigramCount.incrementCount(tag, word, weight);
        totalTokens += weight;
    }

    public void initializeLatent() {
        initializeLatentCount();
        initializeLatentProbability();
    }

    private void initializeLatentProbability() {
        wordLatentBigramProb = copyEmissionBiMap(wordLatentBigramCount);
        normalizeBigramProb(wordLatentBigramProb);
    }

    private void normalizeBigramProb(BiMap<Integer, String, LatentBigramEmissionItem> biMap) {
        LatentBigramEmissionCollection collection = new LatentBigramEmissionCollection();
        for (HashMap<String, LatentBigramEmissionItem> uniMap : biMap.values()) {
            collection.setCollection(uniMap.values());
            collection.normalize();
        }
    }

    private void initializeLatentCount() {
        wordLatentBigramCount = new BiMap<Integer, String, LatentBigramEmissionItem>();
        for (Entry<Integer, UniCounter<String>> biEntry : wordBigramCount.entrySet()) {
            Integer currTag = biEntry.getKey();
            UniCounter<String> uniCounter = biEntry.getValue();
            for (Entry<String, Double> uniEntry : uniCounter.entrySet()) {
                String currWord = uniEntry.getKey();
                LatentBigramEmissionItem latentItem = new LatentBigramEmissionItem(uniEntry.getValue());
                wordLatentBigramCount.put(currTag, currWord, latentItem);
            }
        }
    }

    private void splitLatentProbability() {
        for (Entry<Integer, HashMap<String, LatentBigramEmissionItem>> biEntry : wordLatentBigramProb.entrySet()) {
            Integer currTag = biEntry.getKey();
            HashMap<String, LatentBigramEmissionItem> uniMap = biEntry.getValue();
            for (LatentBigramEmissionItem item : uniMap.values()) {
                int currTagSplitFactor = 2;
                if (LatentTagStates.isNotSplitableTag(currTag)) {
                    currTagSplitFactor = 1;
                }
                item.splitStates(currTagSplitFactor);
            }
        }
    }

    public void updateLatentProbability() {
        wordLatentBigramProb = copyEmissionBiMap(wordLatentBigramCount);
        normalizeBigramProb(wordLatentBigramProb);
        if (latentSmoothingFlag) {
            smoothBigramProb(wordLatentBigramProb);
        }
        normalizeBigramProb(wordLatentBigramProb);
    }

    private void smoothBigramProb(BiMap<Integer, String, LatentBigramEmissionItem> biMap) {
        for (Entry<Integer, HashMap<String, LatentBigramEmissionItem>> biEntry : biMap.entrySet()) {
            Integer tag = biEntry.getKey();
            HashMap<String, LatentBigramEmissionItem> uniMap = biEntry.getValue();
            for (Entry<String, LatentBigramEmissionItem> uniEntry : uniMap.entrySet()) {
                String word = uniEntry.getKey();
                LatentBigramEmissionItem item = uniEntry.getValue();
                item.smooth(latentSmoothingParam, wordBigramProb.getCount(tag, word));
            }
        }
    }

    private void splitLatentCount() {
        resetLatentCount();
    }

    public void splitStates() {
        splitLatentCount();
        splitLatentProbability();
    }

    public void resetLatentCount() {
        wordLatentBigramCount = new BiMap<Integer, String, LatentBigramEmissionItem>();
        for (Entry<Integer, UniCounter<String>> biEntry : wordBigramCount.entrySet()) {
            Integer currTag = biEntry.getKey();
            UniCounter<String> uniCounter = biEntry.getValue();
            for (Entry<String, Double> uniEntry : uniCounter.entrySet()) {
                String currWord = uniEntry.getKey();
                LatentBigramEmissionItem latentItem = new LatentBigramEmissionItem(LatentTagStates.getLatentStateNum(currTag));
                wordLatentBigramCount.put(currTag, currWord, latentItem);
            }
        }
    }

    public double[] getLatentProb(Integer currTag, String currWord) {
        LatentBigramEmissionItem emissionItem = wordLatentBigramProb.get(currTag, currWord);
        if (emissionItem != null) {
            return emissionItem.getLatentScores();
        } else {
            throw new RuntimeException("Error: I haven't seen this word/tag pair in training: " + currWord + "/s" + currTag);
        }
    }

    public boolean isLatentSmoothingFlag() {
        return latentSmoothingFlag;
    }

    public double[] getLatentProb(Integer currTag, int tagStateNum, String currWord,
            boolean logarithm) {

        double[] finalProb = new double[tagStateNum];

        if (!hasSeenWord(currWord)) {
            throw new Error("I haven't seen this word before: " + currWord);
        }

        LatentBigramEmissionItem wordBigramItem = wordLatentBigramProb.get(currTag, currWord);
        if (wordBigramItem != null) {
            double[] scores = wordBigramItem.getLatentScores();
            System.arraycopy(scores, 0, finalProb, 0, tagStateNum);
        } else {
            throw new RuntimeException("cannot find word tag bigram: " + currWord + "/" + currTag);
        }

        if (logarithm) {
            for (int cts = 0; cts < tagStateNum; cts++) {
                finalProb[cts] = Math.log(finalProb[cts]);
            }
        }
        return finalProb;
    }

    public void assertSeenWord(String word) {
        if (!hasSeenWord(word)) {
            throw new Error("Haven't seen word: " + word);
        }
    }

    public double getSurfaceProb(Integer currTag, String currWord, boolean logarithm) {
        double finalProb = 0;
        assertSeenWord(currWord);
        finalProb = wordBigramProb.getCount(currTag, currWord);
        if (logarithm) {
            finalProb = Math.log(finalProb);
        }
        return finalProb;
    }

    public void addLatentCount(Integer currTag, String currWord, double[] uniProb) {
        int currTagStateNum = LatentTagStates.getLatentStateNum(currTag);
        if (currTagStateNum != uniProb.length) {
            throw new RuntimeException("Sorry, array length doesnot match.");
        }
        LatentBigramEmissionItem wordBigramItem = wordLatentBigramCount.get(currTag, currWord);
        synchronized (wordBigramItem) {
            double[] latentScores = wordBigramItem.getLatentScores();
            for (int cts = 0; cts < currTagStateNum; cts++) {
                latentScores[cts] += uniProb[cts];
            }
        }
    }

    public void mergeStates() {
        mergeLatentCount();
    }

    private void mergeLatentCount() {
        for (Integer currTag : wordLatentBigramCount.keySet()) {
            HashMap<String, LatentBigramEmissionItem> uniMap = wordLatentBigramCount.get(currTag);
            boolean[] stateMergeSignal = LatentTagStates.getLatentStateMergeSignal(currTag);
            for (String currWord : uniMap.keySet()) {
                uniMap.get(currWord).mergeStates(stateMergeSignal);
            }
        }
    }
}
