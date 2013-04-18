/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.lvlm;

import edu.umd.clip.math.RandomDisturbance;
import edu.umd.clip.util.BiCounter;
import edu.umd.clip.util.UniCounter;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class State implements Serializable, Comparable {

    private static final long serialVersionUID = 1L;
    private Set<State> splitSet = new HashSet<State>(); // the set of states to be split to
    private Set<State> tyingSet = new HashSet<State>(); // the set of states to be tied to
    private boolean splitable = true; // some states are splitable while others are not
    private State tiedState = null; // the resulting tied state
    private Integer stateIndex = -1;
    private StateType stateType = null;
    private Word word = null;
    private UniCounter<State> bigramTransitionCounts = new UniCounter<State>(); // bigramTransition(next_state) = p(next_state|curr_state)
    private BiCounter<State, State> trigramTransitionCounts = new BiCounter<State, State>(); // trigramTransition(prev_state, next_state) = p(next_state|prev_state, curr_state)
    private double bigramConditionalEntropy = 0;
    private double trigramConditionalEntropy = 0;
    private double unigramStateCount = 0;

    public double getTotalBigramCount() {
        return bigramTransitionCounts.getCount();
    }

    public double getTotalTrigramCount() {
        return trigramTransitionCounts.getCount();
    }

    public void setUnigramStateCount(double unigramStateCount) {
        this.unigramStateCount = unigramStateCount;
    }

    public double getUnigramStateCount() {
        return unigramStateCount;
    }

//    public void setUniStateProb(double uniStateProb) {
//        this.uniStateProb = uniStateProb;
//    }
//    public void setUniStateWeight(double uniStateWeight) {
//        this.uniStateWeight = uniStateWeight;
//    }
//    public void setDefaultEmissionProbs(UniCounter<Word> defaultEmissionProbs) {
//        this.defaultEmissionProbs = defaultEmissionProbs;
//    }
    public void setWord(Word word) {
        this.word = word;
    }

    public Word getWord() {
        return word;
    }

    public void resetCounts() {
        unigramStateCount = 0;
        bigramTransitionCounts = new UniCounter<State>();
    }

    public void resetTrigramCounts() {
        trigramTransitionCounts = new BiCounter<State, State>();
    }

    public double getTransitionProb(State state) {
        double totalCounts = bigramTransitionCounts.getCount();
        if (totalCounts == 0) {
            return 0;
        } else {
            double prob = bigramTransitionCounts.getCount(state) / totalCounts;
            return prob;
        }
    }

    public double getTransitionCount(State state) {
        return bigramTransitionCounts.getCount(state);
    }

    public double getBigramConditionalEntropy() {
        return bigramConditionalEntropy;
    }

    public double getTrigramConditionalEntropy() {
        return trigramConditionalEntropy;
    }

    public double getConditionalEntropyDiff() {
        double diff = bigramConditionalEntropy - trigramConditionalEntropy;
        if (diff < 0) {
            diff = 0;
        }
        return diff;
    }

    public static class ConditionalEntropyDiffComparator implements Comparator<State> {

        public int compare(State o1, State o2) {
            return (int) Math.signum(o2.getConditionalEntropyDiff() - o1.getConditionalEntropyDiff());
        }
    }

    public synchronized void addTransitionCount(State state, double count) {
        bigramTransitionCounts.incrementCount(state, count);
    }

    public synchronized void addTransitionCount(State prevState, State nextState, double count) {
        trigramTransitionCounts.incrementCount(prevState, nextState, count);
    }

    public void calcBigramConditionalEntropy(double totalStateCount) {
        bigramConditionalEntropy = bigramTransitionCounts.getCount() / totalStateCount *
                bigramTransitionCounts.calcConditionalEntropy();
    }

    public void calcTrigramConditionalEntropy(double totalStateCount) {
        trigramConditionalEntropy = trigramTransitionCounts.getCount() / totalStateCount *
                trigramTransitionCounts.calcConditionalEntropy();
    }

    public State(Integer stateIndex, boolean splitable, StateType stateType) {
        this.stateIndex = stateIndex;
        this.splitable = splitable;
        this.stateType = stateType;
    }

    public static enum StateType {

        aux, unk, kwn, uni; // aux: for auxiliary sos and eos states, unk for unknown, kwn for known, uni for universal states
    }

    public void clearPreviousStates() {
        splitSet.clear();
        tyingSet.clear();
    }

    public void prepareStateTying() {
        tiedState = this;
        tyingSet.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final State other = (State) obj;
        if (this.stateIndex != other.stateIndex && (this.stateIndex == null || !this.stateIndex.equals(other.stateIndex))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.stateIndex != null ? this.stateIndex.hashCode() : 0);
        return hash;
    }

    public void addTyingState(State state) {
        tyingSet.add(state);
    }

    public Set<State> getTyingStateSet() {
        return tyingSet;
    }

    public void clearTyingSet() {
        tyingSet.clear();
    }

    public void setTiedState(State tiedState) {
        this.tiedState = tiedState;
    }

    public boolean isTied() {
        if (tiedState == this) {
            return false;
        } else {
            return true;
        }
    }

    public State getTiedState() {
        return tiedState;
    }

    public void setSplitable(boolean splitable) {
        this.splitable = splitable;
    }

    public void setStateIndex(Integer stateIndex) {
        this.stateIndex = stateIndex;
    }

    public boolean isSplitable() {
        return splitable;
    }

    public Integer getStateIndex() {
        return stateIndex;
    }

    public Set<State> getSplitSet() {
        return splitSet;
    }

    public StateType getStateType() {
        return stateType;
    }

    public void setStateType(StateType stateType) {
        if (stateType == null) {
            throw new Error("State type is null...");
        }
        this.stateType = stateType;
    }

    public void setSubStates(State subState0, State subState1) {
        splitSet.clear();
        splitSet.add(subState0);
        splitSet.add(subState1);
    }

    public void setSubStates() {
        splitSet.clear();
        splitSet.add(this);
    }

    public void addUnigramStateCount(double count) {
        unigramStateCount += count;
    }

    public void splitCounts() {
        int splitSize = splitSet.size();
        if (splitSize != 1 && splitSize != 2) {
            throw new RuntimeException("I cannot support split size different from 1 or 2");
        }

        double splitUnigramStateCount = unigramStateCount / splitSize;
        double randomness = 0;
        if (splitSize == 2) {
            randomness = RandomDisturbance.generateRandomDisturbance();
        }
        for (State splitState : splitSet) {
            splitState.setUnigramStateCount(splitUnigramStateCount * (1 + randomness));
            randomness *= -1;
        }
        word.removeState(this);
        for (State splitState : splitSet) {
            word.addState(splitState);
        }

        UniCounter<State> oldBigramTransitionCounts = bigramTransitionCounts;
        bigramTransitionCounts = new UniCounter<State>();
        for (Entry<State, Double> entry : oldBigramTransitionCounts.entrySet()) {
            State nextState = entry.getKey();
            double count = entry.getValue();
            Set<State> nextSplitSet = nextState.getSplitSet();
            int nextSplitSize = nextSplitSet.size();
            if (nextSplitSize != 1 && nextSplitSize != 2) {
                throw new RuntimeException("I cannot support split size different from 1 or 2");
            }
            for (State splitState : splitSet) {
                randomness = 0;
                if (nextSplitSize == 2) {
                    randomness =  RandomDisturbance.generateRandomDisturbance();
                }
                for (State nextSplitState : nextState.getSplitSet()) {
                    double splitCount = count / (splitSize * nextSplitSize) * (1 + randomness);
                    splitState.addTransitionCount(nextSplitState, splitCount);
                    randomness *= -1;
                }
            }
        }
    }

    @Override
    public String toString() {
        return String.valueOf(stateIndex);
    }

    public int compareTo(Object o) {
        if (!(o instanceof State)) {
            throw new ClassCastException();
        }
        return ((Integer) hashCode()).compareTo(((State) o).hashCode());
    }
}
