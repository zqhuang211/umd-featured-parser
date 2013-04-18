/*
 * LatentTransition.java
 *
 * Created on May 21, 2007, 6:46 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.util.BiCounter;
import edu.umd.clip.util.BiMap;
import edu.umd.clip.util.UniCounter;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements the transition model for latent annotated bigram tagger.
 *
 * @author zqhuang
 */
public class LatentTransition implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    /* smoothing parameters */
    private boolean latentSmoothingFlag = false;
    private double latentSmoothingParam = 0.01;
    private double jmSmoothingParam = 0.01;

    /* holds physical counts. */
    private BiCounter<Integer, Integer> tagBigramCount;
    private BiCounter<Integer, Integer> tagBigramProb;
    private UniCounter<Integer> tagUnigramCount;
    private UniCounter<Integer> tagUnigramProb;

    /* holds latent counts */
    private BiMap<Integer, Integer, LatentBigramTransitionItem> tagLatentBigramCount;
    private HashMap<Integer, LatentUnigramTransitionItem> tagLatentUnigramCount;

    /* holds latent probabilities */
    private BiMap<Integer, Integer, LatentBigramTransitionItem> tagLatentBigramProb;
    private HashMap<Integer, LatentUnigramTransitionItem> tagLatentUnigramProb;

    public void printInfo() {
        int total = 0, zero = 0, nonzero = 0;
        for (HashMap<Integer, LatentBigramTransitionItem> uniMap : tagLatentBigramProb.values()) {
            for (LatentBigramTransitionItem item : uniMap.values()) {
                double[][] scores = item.getLatentScores();
                int psn = item.getPrevTagStateNum();
                int csn = item.getCurrTagStateNum();
                total += psn * csn;
                for (int psi = 0; psi < psn; psi++) {
                    for (int csi = 0; csi < csn; csi++) {
                        if (scores[psi][csi] < 1e-15) {
                            zero++;
                        } else {
                            nonzero++;
                        }
                    }
                }
            }
        }
        System.out.println("total params: " + total + ", zero = " + zero + ", nonzero = " + nonzero + ", %= " + (double) nonzero / total * 100);
    }

    public LatentTransition shallowClone() {
        try {
            return (LatentTransition) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(LatentTransition.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public LatentTransition clone() throws CloneNotSupportedException {
        LatentTransition newLatentTransition = (LatentTransition) super.clone();
        newLatentTransition.tagBigramCount = tagBigramCount.clone();
        newLatentTransition.tagBigramProb = tagBigramProb.clone();
        newLatentTransition.tagUnigramCount = tagUnigramCount.clone();
        newLatentTransition.tagUnigramProb = tagUnigramProb.clone();
        newLatentTransition.tagLatentUnigramCount = copyTransitionHashMap(tagLatentUnigramCount);
        newLatentTransition.tagLatentUnigramProb = copyTransitionHashMap(tagLatentUnigramProb);
        newLatentTransition.tagLatentBigramCount = copyTransitionBiMap(tagLatentBigramCount);
        newLatentTransition.tagLatentBigramProb = copyTransitionBiMap(tagLatentBigramProb);
        return newLatentTransition;
    }

    public void setLatentSmoothingFlag(boolean latentSmoothingFlag) {
        this.latentSmoothingFlag = latentSmoothingFlag;
    }

    public void replace(LatentTransition anotherLatentTransition) {
        latentSmoothingFlag = anotherLatentTransition.latentSmoothingFlag;
        latentSmoothingParam = anotherLatentTransition.latentSmoothingParam;
        jmSmoothingParam = anotherLatentTransition.jmSmoothingParam;
        tagBigramCount = anotherLatentTransition.tagBigramCount;
        tagBigramProb = anotherLatentTransition.tagBigramProb;
        tagUnigramCount = anotherLatentTransition.tagUnigramCount;
        tagUnigramProb = anotherLatentTransition.tagUnigramProb;
        tagLatentUnigramCount = anotherLatentTransition.tagLatentUnigramCount;
        tagLatentUnigramProb = anotherLatentTransition.tagLatentUnigramProb;
        tagLatentBigramCount = anotherLatentTransition.tagLatentBigramCount;
        tagLatentBigramProb = anotherLatentTransition.tagLatentBigramProb;
    }

    public void setLatentSmoothingParam(double latentSmoothingParam) {
        this.latentSmoothingParam = latentSmoothingParam;
    }

    public LatentTransition getCoarserTransition(
            Tree<Integer>[] splitTrees, int numSplits) {
        LatentTransition coarserTransition = new LatentTransition();

        coarserTransition.jmSmoothingParam = jmSmoothingParam;
        coarserTransition.latentSmoothingFlag = latentSmoothingFlag;
        coarserTransition.latentSmoothingParam = latentSmoothingParam;
        coarserTransition.tagBigramCount = tagBigramCount;
        coarserTransition.tagBigramProb = tagBigramProb;
        coarserTransition.tagUnigramCount = tagUnigramCount;
        coarserTransition.tagUnigramProb = tagUnigramProb;

        coarserTransition.tagLatentBigramCount = new BiMap<Integer, Integer, LatentBigramTransitionItem>();
        for (Entry<Integer, HashMap<Integer, LatentBigramTransitionItem>> biEntry : tagLatentBigramCount.entrySet()) {
            Integer prevTag = biEntry.getKey();
            List<Tree<Integer>> prevSplitTreeList = splitTrees[prevTag].getAtDepth(numSplits - 1);
            HashMap<Integer, LatentBigramTransitionItem> uniCount = biEntry.getValue();
            for (Entry<Integer, LatentBigramTransitionItem> uniEntry : uniCount.entrySet()) {
                Integer currTag = uniEntry.getKey();
                List<Tree<Integer>> currSplitTreeList = splitTrees[currTag].getAtDepth(numSplits);
                LatentBigramTransitionItem finerItem = uniEntry.getValue();
                LatentBigramTransitionItem coarserItem = new LatentBigramTransitionItem(prevSplitTreeList.size(), currSplitTreeList.size());
                coarserItem.mergeStates(prevSplitTreeList, currSplitTreeList, finerItem.getLatentScores());
                coarserTransition.tagLatentBigramCount.put(prevTag, currTag, coarserItem);
            }

        }
        coarserTransition.tagLatentBigramProb = copyTransitionBiMap(coarserTransition.tagLatentBigramCount);
        normalizeBigramProb(coarserTransition.tagLatentBigramProb);

        coarserTransition.tagLatentUnigramCount = new HashMap<Integer, LatentUnigramTransitionItem>();
        for (Entry<Integer, LatentUnigramTransitionItem> uniEntry : tagLatentUnigramCount.entrySet()) {
            Integer currTag = uniEntry.getKey();
            List<Tree<Integer>> currSplitTreeList = splitTrees[currTag].getAtDepth(numSplits - 1);
            LatentUnigramTransitionItem finerItem = uniEntry.getValue();
            LatentUnigramTransitionItem coarserItem = new LatentUnigramTransitionItem(currSplitTreeList.size());
            coarserItem.mergeStates(currSplitTreeList, finerItem.getLatentScores());
            coarserTransition.tagLatentUnigramCount.put(currTag, coarserItem);
        }

        coarserTransition.tagLatentUnigramProb = copyTransitionHashMap(coarserTransition.tagLatentUnigramCount);
        normalizeUnigramProb(coarserTransition.tagLatentUnigramProb);
        int numTags = splitTrees.length;

        return coarserTransition;

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

    private BiMap<Integer, Integer, LatentBigramTransitionItem> copyTransitionBiMap(BiMap<Integer, Integer, LatentBigramTransitionItem> biMap) {
        BiMap<Integer, Integer, LatentBigramTransitionItem> newBiMap = new BiMap<Integer, Integer, LatentBigramTransitionItem>();
        for (Integer prevTag : biMap.keySet()) {
            HashMap<Integer, LatentBigramTransitionItem> uniMap = biMap.get(prevTag);
            for (Integer currTag : uniMap.keySet()) {
                LatentBigramTransitionItem transitionItem = uniMap.get(currTag);
                LatentBigramTransitionItem newTransitionItem = new LatentBigramTransitionItem(transitionItem);
                newBiMap.put(prevTag, currTag, newTransitionItem);
            }

        }
        return newBiMap;
    }

    public void tallyCounts(Collection<AlphaBetaSequence> trainingCorpus) {
        tagUnigramCount = new UniCounter<Integer>();
        tagBigramCount = new BiCounter<Integer, Integer>();
        tagUnigramProb = new UniCounter<Integer>();
        tagBigramProb = new BiCounter<Integer, Integer>();
        for (AlphaBetaSequence sequence : trainingCorpus) {
            double weight = sequence.getWeight();
            for (int i = 1; i < sequence.size(); i++) {
                addCounts(sequence.get(i - 1).getWordTagItem(), sequence.get(i).getWordTagItem(), weight);
            }
        }
        computeSurfaceProbability();
    }

    private void addCounts(WordTagItem prevItem, WordTagItem currItem, double weight) {
        tagUnigramCount.incrementCount(currItem.getTag(), weight);
        tagBigramCount.incrementCount(prevItem.getTag(), currItem.getTag(), weight);
    }

    private void computeSurfaceProbability() {
        tagBigramProb = tagBigramCount.clone();
        tagUnigramProb = tagUnigramCount.clone();

        tagBigramProb.normalize();
        tagUnigramProb.normalize();
    }

    public void initializeLatent() {
        initializeLatentCount();
        initializeLatentProbability();

    }

    private void initializeLatentProbability() {
        tagLatentBigramProb = copyTransitionBiMap(tagLatentBigramCount);
        normalizeBigramProb(tagLatentBigramProb);

        tagLatentUnigramProb = copyTransitionHashMap(tagLatentUnigramCount);
        normalizeUnigramProb(tagLatentUnigramProb);
    }

    private void normalizeUnigramProb(HashMap<Integer, LatentUnigramTransitionItem> uniMap) {
        LatentUnigramTransitionCollection collection = new LatentUnigramTransitionCollection();
        collection.setCollection(uniMap.values());
        collection.normalize();
    }

    private void normalizeBigramProb(BiMap<Integer, Integer, LatentBigramTransitionItem> biMap) {
        LatentBigramTransitionCollection collection = new LatentBigramTransitionCollection();
        for (HashMap<Integer, LatentBigramTransitionItem> uniMap : biMap.values()) {
            collection.setCollection(uniMap.values());
            collection.normalize();
        }

    }

    /**
     * Initializes the counters of latent states.
     */
    private void initializeLatentCount() {
        tagLatentUnigramCount = new HashMap<Integer, LatentUnigramTransitionItem>();
        tagLatentBigramCount = new BiMap<Integer, Integer, LatentBigramTransitionItem>();

        for (Entry<Integer, UniCounter<Integer>> biEntry : tagBigramCount.entrySet()) {
            Integer prevTag = biEntry.getKey();
            UniCounter<Integer> uniCounter = biEntry.getValue();
            for (Entry<Integer, Double> uniEntry : uniCounter.entrySet()) {
                Integer currTag = uniEntry.getKey();
                LatentBigramTransitionItem latentItem = new LatentBigramTransitionItem(uniEntry.getValue());
                tagLatentBigramCount.put(prevTag, currTag, latentItem);
            }

        }

        for (Entry<Integer, Double> entry : tagUnigramCount.entrySet()) {
            Integer tag = entry.getKey();
            LatentUnigramTransitionItem latentItem = new LatentUnigramTransitionItem(entry.getValue());
            tagLatentUnigramCount.put(tag, latentItem);
        }

    }

    public void resetLatentCount() {
        tagLatentUnigramCount = new HashMap<Integer, LatentUnigramTransitionItem>();
        tagLatentBigramCount = new BiMap<Integer, Integer, LatentBigramTransitionItem>();

        for (Entry<Integer, UniCounter<Integer>> biEntry : tagBigramCount.entrySet()) {
            Integer prevTag = biEntry.getKey();
            int prevTagStateNum = LatentTagStates.getLatentStateNum(prevTag);
            UniCounter<Integer> uniCounter = biEntry.getValue();
            for (Entry<Integer, Double> uniEntry : uniCounter.entrySet()) {
                Integer currTag = uniEntry.getKey();
                int currTagStateNum = LatentTagStates.getLatentStateNum(currTag);
                LatentBigramTransitionItem latentItem = new LatentBigramTransitionItem(prevTagStateNum, currTagStateNum);
                tagLatentBigramCount.put(prevTag, currTag, latentItem);
            }

        }

        for (Entry<Integer, Double> entry : tagUnigramCount.entrySet()) {
            Integer tag = entry.getKey();
            int tagStateNum = LatentTagStates.getLatentStateNum(tag);
            LatentUnigramTransitionItem latentItem = new LatentUnigramTransitionItem(tagStateNum);
            tagLatentUnigramCount.put(tag, latentItem);
        }

    }

    public boolean containsTransition(int prevTag, int currTag) {
        return tagBigramCount.containsKey(prevTag, currTag);
    }

    public void updateLatentProbability() {
        tagLatentBigramProb = copyTransitionBiMap(tagLatentBigramCount);
        tagLatentUnigramProb = copyTransitionHashMap(tagLatentUnigramCount);
        normalizeBigramProb(tagLatentBigramProb);
        normalizeUnigramProb(tagLatentUnigramProb);
        if (latentSmoothingFlag) {
            smoothBigramProb(tagLatentBigramProb);
            smoothUnigramProb(tagLatentUnigramProb);
        }
        normalizeBigramProb(tagLatentBigramProb);
        normalizeUnigramProb(tagLatentUnigramProb);
    }

    private void smoothBigramProb(BiMap<Integer, Integer, LatentBigramTransitionItem> biMap) {
        for (Entry<Integer, HashMap<Integer, LatentBigramTransitionItem>> biEntry : biMap.entrySet()) {
            Integer prevTag = biEntry.getKey();
            HashMap<Integer, LatentBigramTransitionItem> uniMap = biEntry.getValue();
            for (Entry<Integer, LatentBigramTransitionItem> uniEntry : uniMap.entrySet()) {
                Integer currTag = uniEntry.getKey();
                LatentBigramTransitionItem item = uniEntry.getValue();
                item.smooth(latentSmoothingParam, tagBigramProb.getCount(prevTag, currTag));
            }
        }
    }

    private void smoothUnigramProb(HashMap<Integer, LatentUnigramTransitionItem> uniMap) {
//        System.out.println("smoothing param: "+latentSmoothingParam);
        for (Entry<Integer, LatentUnigramTransitionItem> uniEntry : uniMap.entrySet()) {
            Integer tag = uniEntry.getKey();
            LatentUnigramTransitionItem item = uniEntry.getValue();
            item.smoothing(latentSmoothingParam, tagUnigramProb.getCount(tag));
        }
    }

    private void splitLatentProbability() {
        for (Entry<Integer, HashMap<Integer, LatentBigramTransitionItem>> biEntry : tagLatentBigramProb.entrySet()) {
            Integer prevTag = biEntry.getKey();
            int prevTagSplitFactor = 2;
            if (LatentTagStates.isNotSplitableTag(prevTag)) {
                prevTagSplitFactor = 1;
            }

            HashMap<Integer, LatentBigramTransitionItem> uniMap = biEntry.getValue();
            for (Entry<Integer, LatentBigramTransitionItem> uniEntry : uniMap.entrySet()) {
                Integer currTag = uniEntry.getKey();
                int currTagSplitFactor = 2;
                if (LatentTagStates.isNotSplitableTag(currTag)) {
                    currTagSplitFactor = 1;
                }

                LatentBigramTransitionItem item = uniEntry.getValue();
                item.splitStates(prevTagSplitFactor, currTagSplitFactor);
            }
        }
    }

    public void splitStates() {
        splitLatentCount();
        splitLatentProbability();
    }

    private void splitLatentCount() {
        resetLatentCount();
    }

    public void addLatentCount(Integer prevTag, Integer currTag, double[][] biProb, double[] uniProb) {
        int prevTagStateNum = LatentTagStates.getLatentStateNum(prevTag);
        int currTagStateNum = LatentTagStates.getLatentStateNum(currTag);
        if (currTagStateNum != uniProb.length || prevTagStateNum != biProb.length) {
            throw new RuntimeException("Sorry, array length doesnot match.");
        }

        LatentBigramTransitionItem bigramItem = tagLatentBigramCount.get(prevTag, currTag);
        LatentUnigramTransitionItem unigramItem = tagLatentUnigramCount.get(currTag);


        synchronized (unigramItem) {
            unigramItem.increase(uniProb);
        }

        synchronized (bigramItem) {
            bigramItem.increase(biProb);
        }

    }

    public void mergeStates() {
        mergeLatentCount();
//        updateLatentProbability();
    }

    private void mergeLatentCount() {
        for (Integer currTag : tagLatentUnigramCount.keySet()) {
            boolean[] mergeSignal = LatentTagStates.getLatentStateMergeSignal(currTag);
            tagLatentUnigramCount.get(currTag).mergeStates(mergeSignal);
        }

        for (Integer prevTag : tagLatentBigramCount.keySet()) {
            boolean[] prevTagMergeSignal = LatentTagStates.getLatentStateMergeSignal(prevTag);
            HashMap<Integer, LatentBigramTransitionItem> uniMap = tagLatentBigramCount.get(prevTag);
            for (Integer currTag : uniMap.keySet()) {
                boolean[] currTagMergeSignal = LatentTagStates.getLatentStateMergeSignal(currTag);
                uniMap.get(currTag).mergeStates(prevTagMergeSignal, currTagMergeSignal);
            }

        }
    }

    public double[] getUnigramCount(int tag) {
        return tagLatentUnigramCount.get(tag).getLatentScores();
    }

    /**
     * Returns the latent bigram probability vector for training procedure, for which
     * the returned probabilities are not smoothed.
     *
     * @param prevTag
     * @param currTag
     */
    public double[][] getLatentProb(Integer prevTag, Integer currTag) {
        LatentBigramTransitionItem transitionItem = tagLatentBigramProb.get(prevTag, currTag);
        if (transitionItem == null) {
            throw new RuntimeException("Error: sorry I did not see this tag pair in training data.");
        }

        return transitionItem.getLatentScores();
    }

    /**
     * Returns the latent bigram probability vector for testing and validation, for which
     * the returned probabilities are smooothed by <code>Jelinek-Mercer</code> smoothing.
     *
     * @param prevTag
     * @param currTag
     * @param logarithm
     */
    public double[][] getLatentProb(Integer prevTag, int prevTagStateNum, Integer currTag, int currTagStateNum, boolean logarithm) {
        double[][] finalProb = new double[prevTagStateNum][currTagStateNum];
        ArrayMath.fill(finalProb, 0);
        LatentBigramTransitionItem bigramItem = tagLatentBigramProb.get(prevTag, currTag);
        LatentUnigramTransitionItem unigramItem = tagLatentUnigramProb.get(currTag);
        double[] unigramScores = unigramItem.getLatentScores();
        if (bigramItem != null) {
            double[][] bigramScores = bigramItem.getLatentScores();
            for (int pts = 0; pts < prevTagStateNum; pts++) {
                for (int cts = 0; cts < currTagStateNum; cts++) {
                    finalProb[pts][cts] = (1 - jmSmoothingParam) * bigramScores[pts][cts] + jmSmoothingParam * unigramScores[cts];
                }

            }
        } else {
            for (int pts = 0; pts < prevTagStateNum; pts++) {
                for (int cts = 0; cts <
                        currTagStateNum; cts++) {
                    finalProb[pts][cts] = jmSmoothingParam * unigramScores[cts];
                }

            }
        }

        if (logarithm) {
            for (int pts = 0; pts <
                    prevTagStateNum; pts++) {
                for (int cts = 0; cts <
                        currTagStateNum; cts++) {
                    finalProb[pts][cts] = Math.log(finalProb[pts][cts]);
                }

            }
        }
        return finalProb;
    }

    /**
     * Returns the surface bigram probability for training testing and vlidation, for which
     * the returned probabilities are smoothed.
     *
     * @param prevTag
     * @param currTag
     */
    public double getSurfaceProb(Integer prevTag, Integer currTag, boolean logarithm) {
        double finalProb = (1 - jmSmoothingParam) * tagBigramProb.getCount(prevTag, currTag) + jmSmoothingParam * tagUnigramProb.getCount(currTag);
        if (logarithm) {
            finalProb = Math.log(finalProb);
        }

        return finalProb;
    }

    public void setJMSmoothingParam(double jmSmoothingParam) {
        this.jmSmoothingParam = jmSmoothingParam;
    }
}
