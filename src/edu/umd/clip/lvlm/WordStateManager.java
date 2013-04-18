/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.lvlm;

import edu.umd.clip.util.BiCounter;
import edu.umd.clip.util.StringUtils;
import edu.umd.clip.util.UniCounter;
import edu.umd.clip.jobs.Job;
import edu.umd.clip.jobs.JobGroup;
import edu.umd.clip.jobs.JobManager;
import edu.umd.clip.lvlm.State.StateType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class WordStateManager implements Serializable {

    private static final long serialVersionUID = 1L;
    private HashMap<Integer, State> stateMap = new HashMap<Integer, State>();
    private HashMap<String, Word> wordMap = new HashMap<String, Word>();
    private boolean locked = false;
    private boolean testMode = false;
    private boolean smoothMode = false;
    private Integer nextStateIndex = 0;
    private double splittingRate = 0.5;

    /**
     * Add a universal state to be shared my all words except SOS and EOS words.
     * This state is not splitable and is aimed to capture some universal
     * propertities that are useful for modeling unknown bigram dependencies.
     */
//    public void addUniversalStateToWords() {
//        Word uniWord = wordMap.get("##UNI_WORD##");
//        State uniState = uniWord.getStateOfIndex(0);
//        double totalWordCount = 0;
//        for (Word word : wordMap.values()) {
//            String identity = word.getIdentity();
//            if (!identity.equals("##SOS_WORD##") && !identity.equals("##EOS_WORD##")) {
//                word.addState(uniState);
//                totalWordCount += word.getWordCount();
//            }
//        }
//        UniCounter<Word> universalStateEmissionProbs = new UniCounter<Word>();
//        for (Word word : wordMap.values()) {
//            String identity = word.getIdentity();
//            if (!identity.equals("##SOS_WORD##") && !identity.equals("##EOS_WORD##")) {
//                universalStateEmissionProbs.incrementCount(word, word.getWordCount() / totalWordCount);
//            }
//        }
//        uniState.setDefaultEmissionProbs(universalStateEmissionProbs);
//    }
    public void setSplittingRate(double splittingRate) {
        this.splittingRate = splittingRate;
    }

    public void reportLatentKneserNeyLambda() {
        List<Word> wordList = new ArrayList<Word>();
        for (Word word : wordMap.values()) {
            wordList.add(word);
        }
        System.out.println();
        Collections.sort(wordList, new Word.CountComparator());
        for (int i = 0; i < 20; i++) {
            System.out.println(wordList.get(i).getLatentKneserNeyLambdaString());
        }
        System.out.println();
    }

    public void reportStateProbs() {
        List<Word> wordList = new ArrayList<Word>();
        for (Word word : wordMap.values()) {
            wordList.add(word);
        }
        System.out.println();
        Collections.sort(wordList, new Word.CountComparator());
        for (int i = 0; i < 20; i++) {
            System.out.println(wordList.get(i).getStateProbString());
        }
        System.out.println();
    }

    public double getSplittingRate() {
        return splittingRate;
    }

    public void setNextStateIndex(Integer nextStateIndex) {
        this.nextStateIndex = nextStateIndex;
    }

    public void printStates() {
        Set<String> stateNameSet = new HashSet<String>();
        for (Entry<Integer, State> entry : stateMap.entrySet()) {
            stateNameSet.add(entry.getValue().toString());
        }
        List<String> stateNameList = new ArrayList<String>();
        stateNameList.addAll(stateNameSet);
        Collections.sort(stateNameList);
        for (String stateName : stateNameList) {
            System.out.println(stateName);
        }
    }

    public void resetCounts() {
        for (State state : stateMap.values()) {
            state.resetCounts();
        }

        for (Word word : wordMap.values()) {
            word.resetCounts();
        }
    }

    public void updateCounts(boolean updateMapCounts) {
        if (updateMapCounts) {
            updateMapCounts();
        }
        updateArrayProbs();
//        reportLatentKneserNeyLambda();
    }

//    public void setUniStateParam() {
//        double prob = 1.0 / (getStateNum() - 1);
//        for (State state : stateMap.values()) {
//            state.setUniStateProb(prob);
//        }
//    }
    public int getStateNum() {
        return stateMap.size();
    }

    public void setSmoothMode(boolean smoothMode) {
        this.smoothMode = smoothMode;
    }

    public boolean isSmoothMode() {
        return smoothMode;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public boolean isTestMode() {
        return testMode;
    }

    /**
     * Create a new state
     *
     * @param isSplitable specify whether the state is splitable or not
     * @return a new state
     */
    public State createState(boolean isSplitable, StateType stateType) {
        if (locked) {
            throw new Error("The stateManager is locked to create new states...");
        }
        State state = new State(nextStateIndex, isSplitable, stateType);
        stateMap.put(nextStateIndex, state);
        nextStateIndex++;
        return state;
    }

    /**
     * Create a word object with the specified identity
     *
     * @param identity the identity of the word
     * @param splitable indicates whether its initial state is splitable or not
     * @return
     */
    public Word getOrCreateWord(String identity, boolean splitable, StateType stateType) {
        Word word = wordMap.get(identity);
        if (word == null) {
            if (locked) {
                throw new Error("The wordManager is locked to create new words...");
            }
            word = new Word(identity);
            State state = createState(splitable, stateType);
            word.addState(state);
            state.setWord(word);
            wordMap.put(identity, word);
        }
        return word;
    }

    public Word getWord(String identity) {
        Word word = wordMap.get(identity);
        if (word == null) {
            word = wordMap.get("##UNK_WORD##");
            if (word == null) {
                throw new RuntimeException("The UNK word should have been created during training");
            }
        }
        return word;
    }

    /**
     * Split some of the splitable states into two. The splitting crition is
     * based on the difference between trigram conditional entropy and bigram
     * conditional entropy, weighted by the frequency of the state of interest.
     * Only these states that benefit most of extending bigram to trigram and
     * accumulatively contribute splittingRate part of the overall difference
     * are split into two.
     *
     * The counts associated with original states are also equally distrubted to
     * the split states with some randomness in the transition counts.
     *
     * The states that are split into two are removed from the stateMap.
     */
    public void doSplitting() {
        System.out.println("\n" + getStateNum() + " states before splitting");

        // prepare splitting
        List<State> oriStateList = new ArrayList<State>();
        for (State state : stateMap.values()) {
            state.setSubStates();
            oriStateList.add(state);
        }

        final List<State> splitableStateList = new ArrayList<State>();
        double totalBigramCounts = 0;
        double totalTrigramCounts = 0;
        for (State state : stateMap.values()) {
            if (!state.isSplitable()) {
                continue;
            }
            splitableStateList.add(state);
            totalBigramCounts += state.getTotalBigramCount();
            totalTrigramCounts += state.getTotalTrigramCount();
        }

        // cacluate conditional entropy in parallel
        JobManager jobManager = JobManager.getInstance();
        JobGroup grp = jobManager.createJobGroup("calc conditional entropy");
        int i = 0;
        final double finalTotalBigramCounts = totalBigramCounts;
        final double finalTotalTrigramCounts = totalTrigramCounts;
        for (final State state : stateMap.values()) {
            if (!state.isSplitable()) {
                continue;
            }
            i++;
            Job job = new Job(
                    new Runnable() {

                        public void run() {
                            state.calcBigramConditionalEntropy(finalTotalBigramCounts);
//                            state.calcTrigramConditionalEntropy(finalTotalTrigramCounts);
                        }
                    },
                    String.valueOf(i) + "-th state");
            job.setPriority(i);
            jobManager.addJob(grp, job);
        }
        grp.join();

        Collections.sort(splitableStateList, new State.ConditionalEntropyDiffComparator());

        double totalDiff = 0;
        for (State state : splitableStateList) {
            totalDiff += state.getConditionalEntropyDiff();
        }
        System.out.format("Total bigram entropy - trigram entropy = %.6f\n", totalDiff);
        double thresholdDiff = totalDiff * splittingRate;
        System.out.format("I am going to split states that contribute %.2f of the difference\n\n", splittingRate);

        double partialDiff = 0;
        for (State state : splitableStateList) {
            Word word = state.getWord();
            StringBuilder sb = new StringBuilder();
            sb.append(word + "_" + word.indexOfState(state) + ",");
            sb.deleteCharAt(sb.length() - 1);
            System.out.format("%s: %s - %s = %s\n", StringUtils.stripOrPad(sb.toString(), 15),
                    StringUtils.stripOrPad(String.format("%.6f", state.getBigramConditionalEntropy()), 15),
                    StringUtils.stripOrPad(String.format("%.6f", state.getTrigramConditionalEntropy()), 15),
                    StringUtils.stripOrPad(String.format("%.6f", state.getConditionalEntropyDiff()), 15));

            splitState(state);
            partialDiff += state.getConditionalEntropyDiff();
            if (partialDiff > thresholdDiff) {
                break;
            }
        }

        // split counts in parallel
        grp = jobManager.createJobGroup("splitting counts");
        i = 0;
        for (final State state : oriStateList) {
            i++;
            Job job = new Job(
                    new Runnable() {

                        public void run() {
                            state.splitCounts();
                        }
                    },
                    String.valueOf(i) + "-th state");
            job.setPriority(i);
            jobManager.addJob(grp, job);
        }
        grp.join();


        // all new counts are now in maps
        updateCounts(false);
        for (State state : stateMap.values()) {
            state.resetTrigramCounts();
        }

        System.out.println("\n" + getStateNum() + " states after splitting");
    }

    public void splitState(State state) {
        if (!state.isSplitable()) {
            throw new RuntimeException("This state is not splitable");
        }
        Word word = state.getWord();

        State.StateType stateType = state.getStateType();
        State substate0 = createState(true, stateType);
        substate0.setStateType(stateType);
        substate0.setWord(word);

        State substate1 = createState(true, stateType);
        substate1.setStateType(stateType);
        substate1.setWord(word);

        state.setSubStates(substate0, substate1);
        removeState(state);
    }

    public State getState(Integer stateIndex) {
        State state = stateMap.get(stateIndex);
        if (state == null) {
            throw new Error("There is no state with index " + stateIndex);
        }
        return state;
    }

    public void removeState(State state) {
        stateMap.remove(state.getStateIndex());
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    public void updateMapCounts() {
        JobManager jobManager = JobManager.getInstance();
        JobGroup grp = jobManager.createJobGroup("update map counts");
        int i = 0;
        for (final Word word : wordMap.values()) {
            i++;
            Job job = new Job(
                    new Runnable() {

                        public void run() {
                            word.updateMapCounts();
                        }
                    },
                    String.valueOf(i) + "-th state");
            job.setPriority(i);
            jobManager.addJob(grp, job);
        }
        grp.join();
    }

    public void updateArrayProbs() {
        JobManager jobManager = JobManager.getInstance();
        JobGroup grp = jobManager.createJobGroup("update map counts");
        int i = 0;
        for (final Word word : wordMap.values()) {
            i++;
            Job job = new Job(
                    new Runnable() {

                        public void run() {
                            word.updateArrayProbs();
                        }
                    },
                    String.valueOf(i) + "-th state");
            job.setPriority(i);
            jobManager.addJob(grp, job);
        }
        grp.join();
    }

    public void calcKneserNeySmoothingParams() {
        // calculate unigram modified Kneser Ney probabilities
        UniCounter<Word> uniqueLeftWordCount = new UniCounter<Word>();
        for (Word currWord : wordMap.values()) {
            UniCounter<Word> transitionCounts = currWord.getTransitionCounts();
            for (Word nextWord : transitionCounts.keySet()) {
                uniqueLeftWordCount.incrementCount(nextWord, 1);
            }
        }
        uniqueLeftWordCount.normalize();
        for (Entry<Word, Double> entry : uniqueLeftWordCount.entrySet()) {
            entry.getKey().setUnigramKNProb(entry.getValue());
        }

        // calculate bigram discouting weights, ns[0] is not used
        double[] ns = new double[5];
        for (Word currWord : wordMap.values()) {
            UniCounter<Word> transitionCounts = currWord.getTransitionCounts();
            for (Entry<Word, Double> entry : transitionCounts.entrySet()) {
                int count = (int) Math.round(entry.getValue());
                if (count == 0) {
                    throw new RuntimeException("bigram count must be greater than 0");
                }
                if (count <= 4) {
                    ns[count]++;
                }
            }
        }
        double[] kneserNeyDiscounts = new double[4];
        double y = 0;
        if (ns[1] > 0) {
            y = ns[1] / (ns[1] + 2 * ns[2]);
        }
        for (int i = 1; i <= 3; i++) {
            kneserNeyDiscounts[i] = i;
            if (ns[i] > 0) {
                kneserNeyDiscounts[i] -= (i + 1) * y * ns[i + 1] / ns[i];
            }
        }

        for (Word word : wordMap.values()) {
            word.calcKneserNeyBigramDiscountedFraction(kneserNeyDiscounts);
        }

        // calculate trigram discounts, ns[0] not used
        ns = new double[5];
        for (Word currWord : wordMap.values()) {
            BiCounter<Word, Word> trigramCounts = currWord.getTrigramWordTransitionProbs();
            for (Entry<Word, UniCounter<Word>> biEntry : trigramCounts.entrySet()) {
                for (Entry<Word, Double> uniEntry : biEntry.getValue().entrySet()) {
                    int count = (int) Math.round(uniEntry.getValue());
                    if (count == 0) {
                        throw new RuntimeException("trigram count must be greater than 0");
                    }
                    if (count <= 4) {
                        ns[count]++;
                    }
                }
            }
        }
        kneserNeyDiscounts = new double[4];
        y = 0;
        if (ns[1] > 0) {
            y = ns[1] / (ns[1] + 2 * ns[2]);
        }
        for (int i = 1; i <= 3; i++) {
            kneserNeyDiscounts[i] = i;
            if (ns[i] > 0) {
                kneserNeyDiscounts[i] -= (i + 1) * y * ns[i + 1] / ns[i];
            }
        }
        for (Word word : wordMap.values()) {
            word.calcKneserNeyTrigramDiscountedProb(kneserNeyDiscounts);
        }
    }
}
