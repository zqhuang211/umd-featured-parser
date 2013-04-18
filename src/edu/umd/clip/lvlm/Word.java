/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.lvlm;

import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.util.BiCounter;
import edu.umd.clip.util.UniCounter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author zqhuang
 */
public class Word implements Serializable {

    private static final long serialVersionUID = 1L;
    private double wordCount;
    private List<State> stateList = new ArrayList<State>();
    private String identity;
    private double[] latentKneserNeyLambda;
    private double[] stateCounts;
    private double[] stateProbs;
    private Map<Word, double[][]> transitionCountsMap = new HashMap<Word, double[][]>();
    private Map<Word, double[][]> transitionProbsMap = new HashMap<Word, double[][]>();
    private double kneserNeyUnigramProb = 0; // unigram modified Kneser-Ney probability
    private UniCounter<Word> kneserNeyBigramDiscountedFraction = null; // Fraction of bigrams that remain after Kneser-Ney discounting
    private UniCounter<Word> wordTransitionCounts = new UniCounter<Word>();
    private BiCounter<Word, Word> trigramWordTransitionProbs = new BiCounter<Word, Word>();
    private UniCounter<Word> kneserNeyBigramLambdas = new UniCounter<Word>();
    private double kneserNeyUnigramLambda = 0;

    public BiCounter<Word, Word> getTrigramWordTransitionProbs() {
        return trigramWordTransitionProbs;
    }

    public double getTrigramWordTransitionProb(Word prevWord, Word nextWord) {
//        return trigramWordTransitionProbs.getCount(prevWord, nextWord);
        return 0;
    }

    public synchronized void addTrigramWordTransition(Word prevWord, Word nextWord) {
        trigramWordTransitionProbs.incrementCount(prevWord, nextWord, 1);
    }

    public static class CountComparator implements Comparator<Word> {

        public int compare(Word o1, Word o2) {
            return (int) Math.signum(o2.getWordCount() - o1.getWordCount());
        }
    }

    public void calcKneserNeyBigramDiscountedFraction(double[] kneserNeyDiscounts) {
        kneserNeyBigramDiscountedFraction = new UniCounter<Word>();
        if (wordTransitionCounts.getCount() == 0) {
            return;
        }
        if (Math.abs(wordCount - wordTransitionCounts.getCount()) > 0.001) {
            throw new Error("Word count mismatch");
        }
        kneserNeyUnigramLambda = 0;
        for (Entry<Word, Double> entry : wordTransitionCounts.entrySet()) {
            Word nextWord = entry.getKey();
            double count = Math.round(entry.getValue());
            int i = (int) Math.round(count);
            double discountedCount = count;
            if (i <= 2) {
                discountedCount -= kneserNeyDiscounts[i];

            } else {
                discountedCount -= kneserNeyDiscounts[3];

            }
            kneserNeyBigramDiscountedFraction.incrementCount(nextWord, discountedCount / count);
            kneserNeyUnigramLambda += discountedCount;
        }
        kneserNeyUnigramLambda = 1 - kneserNeyUnigramLambda / wordCount;
    }

    public void calcKneserNeyTrigramDiscountedProb(double[] kneserNeyDiscounts) {
        UniCounter<Word> historyCounts = new UniCounter<Word>();
        if (trigramWordTransitionProbs.getCount() != wordTransitionCounts.getCount()) {
            throw new RuntimeException("error");
        }
        for (Entry<Word, UniCounter<Word>> entry : trigramWordTransitionProbs.entrySet()) {
            historyCounts.incrementCount(entry.getKey(), entry.getValue().getCount());
        }

        for (Entry<Word, UniCounter<Word>> biEntry : trigramWordTransitionProbs.entrySet()) {
            Word prevWord = biEntry.getKey();
            double historyCount = historyCounts.getCount(prevWord);
            double trigramProbSum = 0;
            for (Entry<Word, Double> uniEntry : biEntry.getValue().entrySet()) {
                double count = uniEntry.getValue();
                int i = (int) Math.round(count);
                double discountedCount = count;
                if (i <= 2) {
                    discountedCount -= kneserNeyDiscounts[i];
                } else {
                    discountedCount -= kneserNeyDiscounts[3];
                }
                double prob = discountedCount / historyCount;
                uniEntry.setValue(prob);
                trigramProbSum += prob;
            }
            kneserNeyBigramLambdas.incrementCount(prevWord, 1 - trigramProbSum);
        }
    }

    public void calcLatentKneserNeyLambda() {
        int currStateNum = getStateNum();
        latentKneserNeyLambda = ArrayMath.initArray(latentKneserNeyLambda, currStateNum);
        for (Entry<Word, double[][]> entry : transitionProbsMap.entrySet()) {
            Word nextWord = entry.getKey();
            double[][] discountedBigramProb = entry.getValue();
            double kneserNeyDiscountedFraction = kneserNeyBigramDiscountedFraction.getCount(nextWord);
            int nextStateNum = nextWord.getStateNum();
            for (int csi = 0; csi < currStateNum; csi++) {
                for (int nsi = 0; nsi < nextStateNum; nsi++) {
                    latentKneserNeyLambda[csi] += kneserNeyDiscountedFraction * discountedBigramProb[csi][nsi];
                }
            }
        }

        for (int csi = 0; csi < currStateNum; csi++) {
            latentKneserNeyLambda[csi] = (1 - kneserNeyUnigramLambda) / latentKneserNeyLambda[csi];
        }
    }

    public String getLatentKneserNeyLambdaString() {
        int currStateNum = getStateNum();
        StringBuilder sb = new StringBuilder();
        sb.append(identity + ": ");
        for (int csi = 0; csi < currStateNum; csi++) {
            sb.append(latentKneserNeyLambda[csi] + " ");
        }
        return sb.toString();
    }

    public String getStateProbString() {
        int currStateNum = getStateNum();
        StringBuilder sb = new StringBuilder();
        sb.append(identity + ": ");
        for (int csi = 0; csi < currStateNum; csi++) {
            sb.append(stateProbs[csi] + " ");
        }
        return sb.toString();
    }

    public void setUnigramKNProb(double unigramKNProb) {
        this.kneserNeyUnigramProb = unigramKNProb;
    }

    public synchronized void addTransition(Word word) {
        wordTransitionCounts.incrementCount(word, 1);
    }

    public UniCounter<Word> getTransitionCounts() {
        return wordTransitionCounts;
    }

    public String getIdentity() {
        return identity;
    }

    public int indexOfState(State state) {
        return stateList.indexOf(state);
    }

    /**
     * Reset counts
     */
    public void resetCounts() {
        int currStateNum = stateList.size();
        stateCounts = ArrayMath.initArray(stateCounts, currStateNum);
        for (Entry<Word, double[][]> entry : transitionCountsMap.entrySet()) {
            Word nextWord = entry.getKey();
            int nextStateNum = nextWord.getStateNum();
            double[][] counts = entry.getValue();
            counts = ArrayMath.initArray(counts, currStateNum, nextStateNum);
            entry.setValue(counts);
        }
    }

    public double[] getStateProbs() {
        return stateProbs;
    }

    public double[] getLatentKneserNeyLambda() {
        return latentKneserNeyLambda;
    }

    public double getKneserNeyUnigramLambda() {
        return kneserNeyUnigramLambda;
    }

    public double getKneserNeyUnigramProb() {
        return kneserNeyUnigramProb;
    }

    public double[][] getTransitionCounts(Word nextWord) {
        return transitionCountsMap.get(nextWord);
    }

    public double getKneserNeyBigramDiscountedFraction(Word nextWord) {
        return kneserNeyBigramDiscountedFraction.getCount(nextWord);
    }

    public double getKneserNeyBigramLambda(Word prevWord) {
//        Double lambda = kneserNeyBigramLambdas.getCount(prevWord);
//        if (lambda == 0) {
//            lambda = 1.0;
//        }
//        return lambda;
        return 1;
    }

    public double[] getStateCounts() {
        return stateCounts;
    }

    public void setStateCounts(double[] stateCounts) {
        this.stateCounts = stateCounts;
    }

    /**
     * Add bigram transition counts
     *
     * @param nextWord
     * @param scalingFactor
     * @param currForwardScores
     * @param nextBackwardScores
     */
    public synchronized void addBigramTransitionCounts(Word prevWord, Word nextWord, double scalingFactor, double[] currForwardScores, double[] nextBackwardScores) {
        int currStateNum = getStateNum();
        int nextStateNum = nextWord.getStateNum();

        double[][] transititionCounts = transitionCountsMap.get(nextWord);
        if (transititionCounts == null) {
            transititionCounts = new double[currStateNum][nextStateNum];
            transitionCountsMap.put(nextWord, transititionCounts);
        }
        double[][] transitionProbs = transitionProbsMap.get(nextWord);
        if (transitionProbs == null) {
            throw new RuntimeException("uninitialized transition probabilities");
        }
        double kneserNeyDiscountedFraction = getKneserNeyBigramDiscountedFraction(nextWord);

        double[] nextStateCounts = nextWord.getStateCounts();
        if (nextStateCounts == null) {
            nextStateCounts = new double[nextStateNum];
            nextWord.setStateCounts(nextStateCounts);
        }
        double[] nextStateProbs = nextWord.getStateProbs();
        if (nextStateProbs == null) {
            throw new RuntimeException("uninitialized state probabilities");
        }
        double nextWordKneserNeyUnigramProb = nextWord.getKneserNeyUnigramProb();

        double kneserNeyBigramLambda = getKneserNeyBigramLambda(prevWord);
//        double trigramWordProbs = getTrigramWordTransitionProb(prevWord, nextWord);

        for (int csi = 0; csi < currStateNum; csi++) {
            for (int nsi = 0; nsi < nextStateNum; nsi++) {
//                double bigramScore = currForwardScores[csi] *
//                        (trigramWordProbs + kneserNeyBigramLambda *
//                        (kneserNeyDiscountedFraction * transitionProbs[csi][nsi] +
//                        latentKneserNeyLambda[csi] * nextWordKneserNeyUnigramProb * nextStateProbs[nsi])) *
//                        nextBackwardScores[nsi] * scalingFactor;
                double bigramScore = currForwardScores[csi] * kneserNeyBigramLambda * latentKneserNeyLambda[csi] * kneserNeyDiscountedFraction * transitionProbs[csi][nsi] * nextBackwardScores[nsi] * scalingFactor;
                if (bigramScore >= LatentLM.MINIMUM_PROB) {
                    transititionCounts[csi][nsi] += bigramScore;
                }

//                double unigramScore = bigramScore;
                double unigramScore = currForwardScores[csi] * kneserNeyBigramLambda * kneserNeyUnigramLambda * nextWordKneserNeyUnigramProb * nextStateProbs[nsi] * nextBackwardScores[nsi] * scalingFactor;
                if (unigramScore >= LatentLM.MINIMUM_PROB) {
                    nextStateCounts[nsi] += unigramScore;
                }
            }
        }
    }

    /**
     * Tally trigram transition counts in map representation
     *
     * @param prevToken
     * @param nextToken
     * @param sentenceScore
     * @param sentenceScale
     */
    public void tallyTransitionCount(Word prev2Word, Word prevWord, Word nextWord, double scalingFactor, double[] prevForwardScores, double[] nextBackwardScores) {

        List<State> prevStateList = prevWord.stateList;
        List<State> nextStateList = nextWord.getStateList();

        int currStateNum = stateList.size();
        int nextStateNum = nextStateList.size();
        int prevStateNum = prevStateList.size();

        double[][] prevTransitionProbs = prevWord.getTransitionProbs(this);
        double prevKneserNeyDiscountedFraction = prevWord.getKneserNeyBigramDiscountedFraction(this);
        double[][] currTransitionProbs = getTransitionProbs(nextWord);
        double currKneserNeyDiscountedFraction = getKneserNeyBigramDiscountedFraction(nextWord);
        double[] prevLatentKneserNeyLambdas = prevWord.getLatentKneserNeyLambda();
        double nextKneserNeyUnigramProb = nextWord.getKneserNeyUnigramProb();

        double prevKneserNeyBigramLambda = prevWord.getKneserNeyBigramLambda(prev2Word);
        double prevTrigramWordProbs = prevWord.getTrigramWordTransitionProb(prev2Word, this);
        double prevKneserNeyUnigramLambda = prevWord.getKneserNeyUnigramLambda();

        double[] nextStateProbs = nextWord.getStateProbs();

        double currKneserNeyBigramLambda = getKneserNeyBigramLambda(prevWord);
        double currTrigramWordProbs = getTrigramWordTransitionProb(prevWord, nextWord);

        for (int psi = 0; psi < prevStateNum; psi++) {
            State prevState = prevStateList.get(psi);
            for (int csi = 0; csi < currStateNum; csi++) {
                State currState = stateList.get(csi);
                for (int nsi = 0; nsi < nextStateNum; nsi++) {
                    State nextState = nextStateList.get(nsi);
                    double score = prevForwardScores[psi] *
                            (prevTrigramWordProbs + prevKneserNeyBigramLambda *
                            (prevKneserNeyDiscountedFraction * prevLatentKneserNeyLambdas[psi] * prevTransitionProbs[psi][csi] +
                            prevKneserNeyUnigramLambda * kneserNeyUnigramProb * stateProbs[csi])) *
                            (currTrigramWordProbs + currKneserNeyBigramLambda *
                            (currKneserNeyDiscountedFraction * latentKneserNeyLambda[csi] * currTransitionProbs[csi][nsi] +
                             kneserNeyUnigramLambda * nextKneserNeyUnigramProb * nextStateProbs[nsi])) *
                            nextBackwardScores[nsi] *
                            scalingFactor;
                    if (score >= LatentLM.MINIMUM_PROB) {
                        currState.addTransitionCount(prevState, nextState, score);
                    }
                }
            }
        }
    }

    public double getWordCount() {
        return wordCount;
    }

    public int getStateNum() {
        return stateList.size();
    }

    public List<State> getStateList() {
        return stateList;
    }

    public Word(String identity) {
        this.identity = identity.intern();
    }

    /**
     * Return transition probabilities. Transition probabilities need to be
     * computed on the fly for unknown word bigrams.
     * 
     * @param nextWord
     * @return
     */
    public double[][] getTransitionProbs(Word nextWord) {
        return transitionProbsMap.get(nextWord);
    }

//    @Override
//    public int hashCode() {
//        int hash = 7;
//        hash = 67 * hash + (this.identity != null ? this.identity.hashCode() : 0);
//        return hash;
//    }
    public synchronized void addWordCount(double count) {
        this.wordCount += count;
    }

    public synchronized void addState(State state) {
        if (!stateList.contains(state)) {
            stateList.add(state);
        }
    }

    public synchronized void removeState(State state) {
        stateList.remove(state);
    }

    public State getStateOfIndex(int index) {
        if (index >= stateList.size()) {
            throw new RuntimeException(index + " exceeds array length " + stateList.size());
        }
        return stateList.get(index);
    }

    public int getIndexOfState(State state) {
        int index = stateList.indexOf(state);
        if (index == -1) {
            throw new RuntimeException("I cannot find state (" + state + ") in the state list of " + this);
        }
        return index;
    }

    @Override
    public String toString() {
        return identity;
    }

    /**
     * Update the map counts with array counts
     */
    public void updateMapCounts() {
        int currStateNum = stateList.size();

        for (int csi = 0; csi < currStateNum; csi++) {
            State currState = stateList.get(csi);
            currState.addUnigramStateCount(stateCounts[csi]);
        }

        for (Entry<Word, double[][]> entry : transitionCountsMap.entrySet()) {
            Word nextWord = entry.getKey();
            double[][] counts = entry.getValue();
            List<State> nextStateList = nextWord.getStateList();
            int nextStateNum = nextStateList.size();
            for (int csi = 0; csi < currStateNum; csi++) {
                State currState = stateList.get(csi);
                for (int nsi = 0; nsi < nextStateNum; nsi++) {
                    State nextState = nextStateList.get(nsi);
                    currState.addTransitionCount(nextState, counts[csi][nsi]);
                }
            }
        }
    }

    /**
     * Update array probabilities with map probabilities
     */
    public void updateArrayProbs() {
        int currStateNum = stateList.size();

        for (Word nextWord : wordTransitionCounts.keySet()) {
            List<State> nextStateList = nextWord.getStateList();
            int nextStateNum = nextStateList.size();

            double[][] transitionProbs = transitionProbsMap.get(nextWord);
            transitionProbs = ArrayMath.initArray(transitionProbs, currStateNum, nextStateNum);
            transitionProbsMap.put(nextWord, transitionProbs);

            for (int csi = 0; csi < currStateNum; csi++) {
                State currState = stateList.get(csi);
                for (int nsi = 0; nsi < nextStateNum; nsi++) {
                    State nextState = nextStateList.get(nsi);
                    transitionProbs[csi][nsi] = currState.getTransitionProb(nextState);
                }
            }
        }

        stateProbs = ArrayMath.initArray(stateProbs, currStateNum);
        stateCounts = ArrayMath.initArray(stateCounts, currStateNum);
        if (currStateNum == 1) {
            stateCounts[0] = wordCount;
            stateProbs[0] = 1;
        } else {
            double totalCount = 0;
            for (int si = 0; si < currStateNum; si++) {
                State state = stateList.get(si);
                stateCounts[si] = state.getUnigramStateCount();
                totalCount += stateCounts[si];
            }
            for (int si = 0; si < currStateNum; si++) {
                stateProbs[si] = stateCounts[si] / totalCount;
            }
        }

        calcLatentKneserNeyLambda();
    }
//    public void updateArrayCounts() {
//        int currStateNum = stateList.size();
//        if (currStateNum != 1) {
//            throw new RuntimeException("There should only be one state per word");
//        }
//        State currState = stateList.get(0);
//
//        for (Entry<Word, Double> entry : wordTransitionCounts.entrySet()) {
//            Word nextWord = entry.getKey();
//            if (nextWord.getStateNum() != 1) {
//                throw new RuntimeException("There should only be one state per word");
//            }
//            State nextState = nextWord.getStateList().get(0);
//            entry.setValue(currState.getTransitionCount(nextState));
//        }
//    }
}
