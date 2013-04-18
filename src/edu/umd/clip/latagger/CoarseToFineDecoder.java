/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.util.ArrayUtil;
import edu.umd.clip.util.BiSet;
import edu.umd.clip.util.NBestArrayList;
import edu.umd.clip.util.Numberer;
import edu.umd.clip.util.ScalingTools;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class CoarseToFineDecoder {

    private static final long serialVersionUID = 1L;
    private TaggerModelList modelList;
    private boolean[][][] allowedStates;
    private boolean[][] allowedTags;
    private double[][][] forwardScores;
    private double[][][] backwardScores;
    private int[][] forwardScales;
    private int[][] backwardScales;
    private double[] scoresToAdd;
    private int[] numStates;
    private double[] thresholds = {-12, -12, -11, -12, -12, -14, -14, -14, -14, -14, -14, -14, -14, -14};
    private double[][] maxScore;
    private int[][] maxPrevTag;
    private int[][][] viterbiPrevTagState;
    private int[][][] viterbiPrevTag;
    private TaggerModel model;
    private LatentEmission emission;
    private LatentTransition transition;
    private int[][] stateMapping;
    private int length;
    private int level;
    private int numLevels;
    private int numTags;
    private NBestArrayList[][] maxNBestList;
    private int nbestSize;
    private List<String> sentence;
    private List<String> taggedSentence;
    private List<List<String>> nbestTaggedSentences;
    private List<Set<Integer>> tagsList;
    private List<Integer> tagList;
    private Numberer tagNumberer;
    private BiSet<String, Integer> wordTagSet;
    private int decoder;

    public void setNbestSize(int nbestSize) {
        this.nbestSize = nbestSize;
    }

    public Set<Integer> getWordTagSet(String word) {
        Set<Integer> tagSet = wordTagSet.get(word);
        if (tagSet == null) {
            throw new Error("Words with unknown tag set encounterred:" + word);
        }
        return tagSet;
    }

    public CoarseToFineDecoder(TaggerModelList taggerModelList, int decoder) {
        modelList = taggerModelList;
        this.decoder = decoder;
        wordTagSet = modelList.get(0).getEmission().getWordTagSet();
        numLevels = modelList.size();
        tagNumberer = Numberer.getGlobalNumberer("tags");
        initModel(0);
    }

    public void clearArrays() {
        forwardScores = null;
        backwardScores = null;
        allowedStates = null;
        allowedTags = null;
    }

    public void initModel(int level) {
        this.level = level;
        model = modelList.get(level);
        emission = model.getEmission();
        transition = model.getTransition();
        numTags = model.getNumTags();
        numStates = model.getNumStates();
        stateMapping = model.getFineToCoarseMapping();
    }

    public void doPreTagging() {
        clearArrays();
        for (level = 0; level < numLevels - 1; level++) {
            initModel(level);
            createArrays(level == 0, Double.NEGATIVE_INFINITY);
            doViterbiForwardScores();
            if (viterbiFailed()) {
                return;
            }
            doViterbiBackwardScores();
            pruneLattice();
        }
    }

    public void initTags(List<String> tagSequence) {
        tagList = new ArrayList<Integer>();
        tagList.add(tagNumberer.number("SOS"));
        for (String tag : tagSequence) {
            tagList.add(tagNumberer.number(tag));
        }
        tagList.add(tagNumberer.number("EOS"));
    }

    public void initSentence(List<String> stringSentence) {
        length = stringSentence.size() + 2;
        sentence = new ArrayList<String>();
        tagsList = new ArrayList<Set<Integer>>();

        level = 0;
        sentence.add("***SOS_WORD***");
        tagsList.add(Collections.singleton(tagNumberer.number("SOS")));

        for (int i = 0; i < stringSentence.size(); i++) {
            String word = stringSentence.get(i);
            if (!emission.hasSeenWord(word)) {
                word = "***UNK_WORD***";
            }
            sentence.add(word);
            tagsList.add(getWordTagSet(word));
        }

        sentence.add("***EOS_WORD***");
        tagsList.add(Collections.singleton(tagNumberer.number("EOS")));
    }

    public void setupScaling() {
        // create arrays for scaling coefficients
        forwardScales = new int[length][];
        backwardScales = new int[length][];

        for (int loc = 0; loc < length; loc++) {
            forwardScales[loc] = new int[numTags];
            backwardScales[loc] = new int[numTags];
            Arrays.fill(forwardScales[loc], Integer.MIN_VALUE);
            Arrays.fill(backwardScales[loc], Integer.MIN_VALUE);
        }
    }

    public double getLogLikelihood(List<String> stringSentence) {
        initSentence(stringSentence);
        doPreTagging();
        initModel(level);
        if (viterbiFailed()) {
            return Double.NEGATIVE_INFINITY;
        }
//        if (level == 0) {
//            throw new UnsupportedOperationException("level = 0 not implemented yet");
//        } else {
            createArrays(level == 0, 0);
            setupScaling();
            doScaledForwardScores();
            if (variationFailed()) {
                return Double.NEGATIVE_INFINITY;
            }
            return getLogSentenceScore();
//        }
    }

    public List<String> getBestTags(List<String> stringSentence) {
        initSentence(stringSentence);
        doPreTagging();
        initModel(level);
        if (level == 0) {
            createArrays(level == 0, Double.NEGATIVE_INFINITY);
            doViterbiTagging();
            if (viterbiFailed()) {
                return null;
            }
            taggedSentence = getViterbiPOSTags(stringSentence);
        } else {
            if (viterbiFailed()) {
                return null;
            }
            if (decoder == 0) { // vitebi decoding
                createArrays(level == 0, Double.NEGATIVE_INFINITY);
                doViterbiForwardScores();
                if (viterbiFailed()) {
                    return null;
                }
                doViterbiBackwardScores();
                doViterbiTagging();
                taggedSentence = getViterbiPOSTags(stringSentence);
            } else { // max rule product decoding
                createArrays(level == 0, 0);
                setupScaling();
                doScaledForwardScores();
                if (variationFailed()) {
                    return null;
                }
                doScaledBackwardScores();
                doMaxScore();
                taggedSentence = getPOSTags(stringSentence);
            }
        }
        return taggedSentence;
    }

    public List<List<String>> getNBestTags(List<String> stringSentence) {
        initSentence(stringSentence);
        doPreTagging();
        initModel(level);
        if (level == 0) {
            throw new RuntimeException("I cannot supoort level 0 in nbest tagging right now...");
        }
        if (viterbiFailed()) {
            return null;
        }
        // max rule product decoding
        createArrays(level == 0, 0);
        setupScaling();
        doScaledForwardScores();
        if (variationFailed()) {
            return null;
        }
        doScaledBackwardScores();
        doNBestMaxScore();
        nbestTaggedSentences = getNBestPOSTags(stringSentence);
        return nbestTaggedSentences;
    }

    public double getMaxScore() {
        return maxScore[length - 1][tagNumberer.number("EOS")];
    }

    public double getIBestMaxScore(int i) {
        NBestItem eosItem = (NBestItem) maxNBestList[length - 1][tagNumberer.number("EOS")].get(i);
        return eosItem.getScore();
    }

    private double getLogSentenceScore() {
        int eosTag = tagNumberer.number("EOS");
        double score = forwardScores[length - 1][eosTag][0];
        int scale = forwardScales[length - 1][eosTag];
        return Math.log(score) + ScalingTools.getLogScale(scale);
    }

    public boolean variationFailed() {
        int eos = tagNumberer.number("EOS");
        if (forwardScores[length - 1][eos][0] == 0) {
            return true;
        } else {
            return false;
        }

    }

    public boolean viterbiFailed() {
        int eos = tagNumberer.number("EOS");
        if (level != 0 && forwardScores[length - 1][eos][0] == Double.NEGATIVE_INFINITY) {
            return true;
        } else {
            return false;
        }

    }

    public void doMaxScore() {
        maxScore = new double[length][numTags];
        maxPrevTag = new int[length][numTags];

        double initVal = Double.NEGATIVE_INFINITY;
        ArrayUtil.fill(maxScore, initVal);

        int sosTag = tagNumberer.number("SOS");
        int eosTag = tagNumberer.number("EOS");

        double sentenceScore = forwardScores[length - 1][eosTag][0];
        double sentenceScale = forwardScales[length - 1][eosTag];
        double[][] bestScores = new double[numTags][numTags];
        maxScore[0][sosTag] = 0;
        for (int index = 1; index < length; index++) {
            String currWord = sentence.get(index);
            Set<Integer> currTagSet = tagsList.get(index);
            Set<Integer> prevTagSet = tagsList.get(index - 1);
            for (int currTag : currTagSet) {
                if (!allowedTags[index][currTag]) {
                    continue;
                }

                int currTagStateNum = numStates[currTag];
                double[] emissionProb = emission.getLatentProb(currTag, currTagStateNum, currWord, false);
                for (int prevTag : prevTagSet) {
                    if (!allowedTags[index - 1][prevTag]) {
                        continue;
                    }

                    int prevTagStateNum = numStates[prevTag];
                    double[][] transitionProb = transition.getLatentProb(prevTag, prevTagStateNum, currTag, currTagStateNum, false);
                    double scaleFactor = ScalingTools.calcScaleFactor(forwardScales[index - 1][prevTag] + backwardScales[index][currTag] - sentenceScale);
                    double score = 0;
                    for (int pts = 0; pts < prevTagStateNum; pts++) {
                        if (!allowedStates[index - 1][prevTag][pts] ||
                                forwardScores[index - 1][prevTag][pts] == initVal) {
                            continue;
                        }

                        for (int cts = 0; cts < currTagStateNum; cts++) {
                            if (!allowedStates[index][currTag][cts] ||
                                    backwardScores[index][currTag][cts] == initVal) {
                                continue;
                            }

                            score += forwardScores[index - 1][prevTag][pts] *
                                    transitionProb[pts][cts] * emissionProb[cts] *
                                    backwardScores[index][currTag][cts];
                        }
                    }
                    score = Math.log(score / sentenceScore * scaleFactor) + maxScore[index - 1][prevTag];
                    if (score > maxScore[index][currTag]) {
                        maxScore[index][currTag] = score;
                        maxPrevTag[index][currTag] = prevTag;
                    }

                }
            }
        }
    }

    public void doNBestMaxScore() {
        maxNBestList = new NBestArrayList[length][numTags];
        double initVal = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < length; index++) {
            for (int ni = 0; ni < numTags; ni++) {
                maxNBestList[index][ni] = new NBestArrayList(nbestSize, new NBestItem.Comparator());
            }

        }

        int sosTag = tagNumberer.number("SOS");
        int eosTag = tagNumberer.number("EOS");

        double sentenceScore = forwardScores[length - 1][eosTag][0];
        double sentenceScale = forwardScales[length - 1][eosTag];

        NBestItem sosItem = new NBestItem(-1, -1, 0);
        maxNBestList[0][sosTag].add(sosItem);
        for (int index = 1; index < length; index++) {
            String currWord = sentence.get(index);
            Set<Integer> currTagSet = tagsList.get(index);
            Set<Integer> prevTagSet = tagsList.get(index - 1);

            for (int currTag : currTagSet) {
                if (!allowedTags[index][currTag]) {
                    continue;
                }

                int currTagStateNum = numStates[currTag];
                double[] emissionProb = emission.getLatentProb(currTag, currTagStateNum, currWord, false);
                for (int prevTag : prevTagSet) {
                    if (!allowedTags[index - 1][prevTag]) {
                        continue;
                    }

                    int prevTagStateNum = numStates[prevTag];
                    double[][] transitionProb = transition.getLatentProb(prevTag, prevTagStateNum, currTag, currTagStateNum, false);
                    double scaleFactor = ScalingTools.calcScaleFactor(forwardScales[index - 1][prevTag] + backwardScales[index][currTag] - sentenceScale);
                    double score = 0;
                    for (int pts = 0; pts < prevTagStateNum; pts++) {
                        if (!allowedStates[index - 1][prevTag][pts] ||
                                forwardScores[index - 1][prevTag][pts] == initVal) {
                            continue;
                        }

                        for (int cts = 0; cts < currTagStateNum; cts++) {
                            if (!allowedStates[index][currTag][cts] ||
                                    backwardScores[index][currTag][cts] == initVal) {
                                continue;
                            }

                            score += forwardScores[index - 1][prevTag][pts] *
                                    transitionProb[pts][cts] * emissionProb[cts] *
                                    backwardScores[index][currTag][cts];
                        }

                    }
                    NBestArrayList prevNBestList = maxNBestList[index - 1][prevTag];
                    score = Math.log(score / sentenceScore * scaleFactor);
                    for (int pni = 0; pni < prevNBestList.size(); pni++) {
                        NBestItem prevItem = (NBestItem) prevNBestList.get(pni);
                        double totalScore = score + prevItem.getScore();
                        if (totalScore == initVal) {
                            continue;
                        }

                        maxNBestList[index][currTag].add(new NBestItem(prevTag, pni, totalScore));
                    }

                }
            }
        }
    }

    public List<List<String>> getNBestPOSTags(List<String> sentence) {
        nbestTaggedSentences = new ArrayList<List<String>>();
        int eos = tagNumberer.number("EOS");
        NBestArrayList eosList = maxNBestList[length - 1][eos];
        for (int bi = 0; bi < eosList.size(); bi++) {
            NBestItem nextItem = (NBestItem) eosList.get(bi);
            List<String> ibestTaggedSentence = new ArrayList<String>();
            for (int index = length - 2; index >= 1; index--) {
                int tag = nextItem.getPrevTag();
                int ibest = nextItem.getPrevNBest();
                ibestTaggedSentence.add(0, sentence.get(index - 1) + "/" + tagNumberer.object(tag));
                nextItem = (NBestItem) maxNBestList[index][tag].get(ibest);
            }
            nbestTaggedSentences.add(ibestTaggedSentence);
        }
        return nbestTaggedSentences;
    }

    public List<String> getPOSTags(List<String> sentence) {
        taggedSentence = new ArrayList<String>();
        int nextTag = tagNumberer.number("EOS");
        for (int index = length - 2; index >= 1; index--) {
            int currTag = maxPrevTag[index + 1][nextTag];
            taggedSentence.add(0, sentence.get(index - 1) + "/" + tagNumberer.object(currTag));
            nextTag = currTag;
        }
        return taggedSentence;
    }

    public List<String> getViterbiPOSTags(List<String> stringSentence) {
        taggedSentence = new ArrayList<String>();
        int nextTag = tagNumberer.number("EOS");
        int nextState = 0;
        for (int index = length - 2; index >= 1; index--) {
            int currTag = viterbiPrevTag[index + 1][nextTag][nextState];
            int currState = viterbiPrevTagState[index + 1][nextTag][nextState];
            taggedSentence.add(0, stringSentence.get(index - 1) + "/" + tagNumberer.object(currTag));
            nextTag = currTag;
            nextState = currState;
        }

        return taggedSentence;
    }

    public void pruneLattice() {
        int esoTag = tagNumberer.number("EOS");
        double sentenceProb = forwardScores[length - 1][esoTag][0];
        for (int index = 0; index < length; index++) {
            Set<Integer> tagSet = tagsList.get(index);
            for (Integer tag : tagSet) {
                if (!allowedTags[index][tag]) {
                    continue;
                }

                boolean nonPossible = true;
                for (int state = 0; state < numStates[tag]; state++) {
                    if (!allowedStates[index][tag][state]) {
                        continue;
                    }

                    double fS = forwardScores[index][tag][state];
                    double bS = backwardScores[index][tag][state];
                    if (fS == Double.NEGATIVE_INFINITY || bS == Double.NEGATIVE_INFINITY) {
                        allowedStates[index][tag][state] = false;
                        continue;

                    }

                    double posterior = fS + bS - sentenceProb;
                    if (posterior > thresholds[level]) {
                        allowedStates[index][tag][state] = true;
                        nonPossible = false;
                    } else {
                        allowedStates[index][tag][state] = false;
                    }

                }
                if (nonPossible) {
                    allowedTags[index][tag] = false;
                }

            }
        }
    }

    public void doScaledForwardScores() {
        double initVal = 0;
        int sosTag = tagNumberer.number("SOS");

        forwardScores[0][sosTag][0] = 1;
        forwardScales[0][sosTag] = 0;
        for (int index = 1; index < length; index++) {
            String currWord = sentence.get(index);
            Set<Integer> currTagSet = tagsList.get(index);
            Set<Integer> prevTagSet = tagsList.get(index - 1);
            for (int currTag : currTagSet) {
                if (!allowedTags[index][currTag]) {
                    continue;
                }

                int currTagStateNum = numStates[currTag];
                double[] emissionProb = emission.getLatentProb(currTag, currTagStateNum, currWord, false);

                for (int prevTag : prevTagSet) {
                    if (!allowedTags[index - 1][prevTag]) {
                        continue;
                    }

                    int prevTagStateNum = numStates[prevTag];
                    double[][] transitionProb = transition.getLatentProb(prevTag, prevTagStateNum, currTag, currTagStateNum, false);
                    for (int pts = 0; pts < prevTagStateNum; pts++) {
                        if (!allowedStates[index - 1][prevTag][pts] || forwardScores[index - 1][prevTag][pts] == initVal) {
                            continue;
                        }

                        for (int cts = 0; cts < currTagStateNum; cts++) {
                            if (!allowedStates[index][currTag][cts]) {
                                continue;
                            }
                            scoresToAdd[cts] += forwardScores[index - 1][prevTag][pts] * transitionProb[pts][cts] * emissionProb[cts];
                        }
                    }
                    int currScale = forwardScales[index - 1][prevTag];
                    currScale = ScalingTools.scaleArray(scoresToAdd, currScale);
                    if (forwardScales[index][currTag] != currScale) {
                        if (forwardScales[index][currTag] == Integer.MIN_VALUE) {
                            forwardScales[index][currTag] = currScale;
                        } else {
                            int newScale = Math.max(forwardScales[index][currTag], currScale);
                            ScalingTools.scaleArrayToScale(scoresToAdd, currScale, newScale);
                            ScalingTools.scaleArrayToScale(forwardScores[index][currTag], forwardScales[index][currTag], newScale);
                            forwardScales[index][currTag] = newScale;
                        }

                    }
                    for (int si = 0; si < currTagStateNum; si++) {
                        forwardScores[index][currTag][si] += scoresToAdd[si];
                    }
                    Arrays.fill(scoresToAdd, initVal);
                }
            }
        }
    }

    public void doScaledBackwardScores() {
        double initVal = 0;
        int eosTag = tagNumberer.number("EOS");

        backwardScores[length - 1][eosTag][0] = 1;
        backwardScales[length - 1][eosTag] = 0;
        for (int index = length - 2; index >= 0; index--) {
            String nextWord = sentence.get(index + 1);
            Set<Integer> currTagSet = tagsList.get(index);
            Set<Integer> nextTagSet = tagsList.get(index + 1);
            for (int nextTag : nextTagSet) {
                if (!allowedTags[index + 1][nextTag]) {
                    continue;
                }

                int nextTagStateNum = numStates[nextTag];
                double[] emissionProb = emission.getLatentProb(nextTag, nextTagStateNum, nextWord, false);

                for (int currTag : currTagSet) {
                    if (!allowedTags[index][currTag]) {
                        continue;
                    }

                    int currTagStateNum = numStates[currTag];
                    double[][] transitionProb = transition.getLatentProb(currTag, currTagStateNum, nextTag, nextTagStateNum, false);
                    for (int nts = 0; nts < nextTagStateNum; nts++) {
                        if (!allowedStates[index + 1][nextTag][nts] ||
                                backwardScores[index + 1][nextTag][nts] == initVal) {
                            continue;
                        }

                        for (int cts = 0; cts < currTagStateNum; cts++) {
                            if (!allowedStates[index][currTag][cts]) {
                                continue;
                            }
                            scoresToAdd[cts] += backwardScores[index + 1][nextTag][nts] * emissionProb[nts] * transitionProb[cts][nts];
                        }

                    }
                    int currScale = backwardScales[index + 1][nextTag];
                    currScale = ScalingTools.scaleArray(scoresToAdd, currScale);
                    if (backwardScales[index][currTag] != currScale) {
                        if (backwardScales[index][currTag] == Integer.MIN_VALUE) {
                            backwardScales[index][currTag] = currScale;
                        } else {
                            int newScale = Math.max(backwardScales[index][currTag], currScale);
                            ScalingTools.scaleArrayToScale(scoresToAdd, currScale, newScale);
                            ScalingTools.scaleArrayToScale(backwardScores[index][currTag], backwardScales[index][currTag], newScale);
                            backwardScales[index][currTag] = newScale;
                        }

                    }
                    for (int si = 0; si < currTagStateNum; si++) {
                        backwardScores[index][currTag][si] += scoresToAdd[si];
                    }

                    Arrays.fill(scoresToAdd, initVal);
                }

            }
        }
    }

    public void doViterbiTagging() {
        viterbiPrevTagState = new int[length][numTags][];
        viterbiPrevTag = new int[length][numTags][];
        for (int si = 0; si < length; si++) {
            for (int tag = 0; tag < numTags; tag++) {
                viterbiPrevTagState[si][tag] = new int[numStates[tag]];
                viterbiPrevTag[si][tag] = new int[numStates[tag]];
                Arrays.fill(viterbiPrevTagState[si][tag], -1);
                Arrays.fill(viterbiPrevTag[si][tag], -1);
            }

        }
        int sosTag = tagNumberer.number("SOS");

        double initVal = Double.NEGATIVE_INFINITY;
        forwardScores[0][sosTag][0] = 0;
        for (int index = 1; index < length; index++) {
            String currWord = sentence.get(index);
            Set<Integer> currTagSet = tagsList.get(index);
            Set<Integer> prevTagSet = tagsList.get(index - 1);
            for (int currTag : currTagSet) {
                if (!allowedTags[index][currTag]) {
                    continue;
                }

                int currTagStateNum = numStates[currTag];
                double[] emissionProb = emission.getLatentProb(currTag, currTagStateNum, currWord, true);
                for (int prevTag : prevTagSet) {
                    if (!allowedTags[index - 1][prevTag]) {
                        continue;
                    }

                    int prevTagStateNum = numStates[prevTag];
                    double[][] transitionProb = transition.getLatentProb(prevTag, prevTagStateNum, currTag, currTagStateNum, true);
                    for (int pts = 0; pts < prevTagStateNum; pts++) {
                        if (!allowedStates[index - 1][prevTag][pts] || forwardScores[index - 1][prevTag][pts] == initVal) {
                            continue;
                        }

                        for (int cts = 0; cts < currTagStateNum; cts++) {
                            if (!allowedStates[index][currTag][cts]) {
                                continue;
                            }

                            double tempScore = forwardScores[index - 1][prevTag][pts] + transitionProb[pts][cts] + emissionProb[cts];
                            if (tempScore > forwardScores[index][currTag][cts]) {
                                forwardScores[index][currTag][cts] = tempScore;
                                viterbiPrevTagState[index][currTag][cts] = pts;
                                viterbiPrevTag[index][currTag][cts] = prevTag;
                            }

                        }
                    }
                }
            }
        }
    }

    public void doViterbiForwardScores() {
        int sosTag = tagNumberer.number("SOS");

        double initVal = Double.NEGATIVE_INFINITY;
        forwardScores[0][sosTag][0] = 0;
        for (int index = 1; index < length; index++) {
            String currWord = sentence.get(index);
            Set<Integer> currTagSet = tagsList.get(index);
            Set<Integer> prevTagSet = tagsList.get(index - 1);
            for (int currTag : currTagSet) {
                if (!allowedTags[index][currTag]) {
                    continue;
                }

                int currTagStateNum = numStates[currTag];
                double[] emissionProb = emission.getLatentProb(currTag, currTagStateNum, currWord, true);

                for (int prevTag : prevTagSet) {
                    if (!allowedTags[index - 1][prevTag]) {
                        continue;
                    }

                    int prevTagStateNum = numStates[prevTag];
                    double[][] transitionProb = transition.getLatentProb(prevTag, prevTagStateNum, currTag, currTagStateNum, true);
                    for (int pts = 0; pts < prevTagStateNum; pts++) {
                        if (!allowedStates[index - 1][prevTag][pts] || forwardScores[index - 1][prevTag][pts] == initVal) {
                            continue;
                        }

                        for (int cts = 0; cts < currTagStateNum; cts++) {
                            if (!allowedStates[index][currTag][cts]) {
                                continue;
                            }

                            double tempScore = forwardScores[index - 1][prevTag][pts] +
                                    transitionProb[pts][cts] + emissionProb[cts];
                            if (tempScore > forwardScores[index][currTag][cts]) {
                                forwardScores[index][currTag][cts] = tempScore;
                            }
                        }
                    }
                }
            }
        }
    }

    public void doViterbiBackwardScores() {
        int eosTag = tagNumberer.number("EOS");

        double initVal = Double.NEGATIVE_INFINITY;

        backwardScores[length - 1][eosTag][0] = 0;
        for (int index = length - 2; index >= 0; index--) {
            String nextWord = sentence.get(index + 1);
            Set<Integer> currTagSet = tagsList.get(index);
            Set<Integer> nextTagSet = tagsList.get(index + 1);
            for (int nextTag : nextTagSet) {
                if (!allowedTags[index + 1][nextTag]) {
                    continue;
                }

                int nextTagStateNum = numStates[nextTag];
                double[] emissionProb = emission.getLatentProb(nextTag, nextTagStateNum, nextWord, true);

                for (int currTag : currTagSet) {
                    if (!allowedTags[index][currTag]) {
                        continue;
                    }

                    int currTagStateNum = numStates[currTag];
                    double[][] transitionProb = transition.getLatentProb(currTag, currTagStateNum, nextTag, nextTagStateNum, true);
                    for (int nts = 0; nts < nextTagStateNum; nts++) {
                        if (!allowedStates[index + 1][nextTag][nts] || backwardScores[index + 1][nextTag][nts] == initVal) {
                            continue;
                        }

                        for (int cts = 0; cts < currTagStateNum; cts++) {
                            if (!allowedStates[index][currTag][cts]) {
                                continue;
                            }

                            double tempScore = backwardScores[index + 1][nextTag][nts] +
                                    emissionProb[nts] + transitionProb[cts][nts];
                            if (tempScore > backwardScores[index][currTag][cts]) {
                                backwardScores[index][currTag][cts] = tempScore;
                            }
                        }
                    }
                }
            }
        }
    }

    public void createArrays(boolean firstTime, double initVal) {
        if (firstTime) {
            forwardScores = new double[length][][];
            backwardScores = new double[length][][];
            allowedTags = new boolean[length][];
            allowedStates = new boolean[length][][];
        }

        int maxStateNum = 0;
        for (int tag = 0; tag < numTags; tag++) {
            if (maxStateNum < numStates[tag]) {
                maxStateNum = numStates[tag];
            }

        }
        scoresToAdd = new double[maxStateNum];

        for (int index = 0; index < length; index++) {
            forwardScores[index] = new double[numTags][];
            backwardScores[index] = new double[numTags][];
            boolean[][] newAllowedStates = null;
            boolean[] newAllowedTags = null;
            if (firstTime) {
                allowedTags[index] = new boolean[numTags];
                allowedStates[index] = new boolean[numTags][];
            } else {
                newAllowedTags = new boolean[numTags];
                newAllowedStates = new boolean[numTags][];
            }

            for (int tag = 0; tag < numTags; tag++) {
                forwardScores[index][tag] = new double[numStates[tag]];
                backwardScores[index][tag] = new double[numStates[tag]];
                Arrays.fill(forwardScores[index][tag], initVal);
                Arrays.fill(backwardScores[index][tag], initVal);
                if (firstTime) {
                    allowedTags[index][tag] = true;
                    allowedStates[index][tag] = new boolean[numStates[tag]];
                    Arrays.fill(allowedStates[index][tag], true);
                } else {
                    boolean allowOne = false;
                    newAllowedStates[tag] = new boolean[numStates[tag]];
                    for (int state = 0; state < numStates[tag]; state++) {
                        int coarserState = stateMapping[tag][state];
                        if (allowedStates[index][tag][coarserState]) {
                            newAllowedStates[tag][state] = true;
                            allowOne = true;
                        } else {
                            newAllowedStates[tag][state] = false;
                        }

                    }
                    if (allowOne) {
                        newAllowedTags[tag] = true;
                    } else {
                        newAllowedTags[tag] = false;
                    }

                }
            }
            if (!firstTime) {
                allowedTags[index] = newAllowedTags;
                allowedStates[index] = newAllowedStates;
            }
        }
    }
}

