/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.util.ArrayUtil;
import edu.umd.clip.util.NBestArrayList;
import edu.umd.clip.util.ScalingTools;
import edu.umd.clip.util.BiSet;
import edu.umd.clip.util.Pair;
import edu.umd.clip.util.TriSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author zqhuang
 */
public class ParseDecoder implements Serializable {

    private static final long serialVersionUID = 1L;
    private boolean[][][][] allowedStates;
    private boolean[][][] allowedNodes;
    private List<Grammar> grammarList;
    private List<String> nodeList;
    private Map<String, Integer> nodeMap;
    private double[][][][] iScore;
    private double[][][][] oScore;
    private int[] numStates;
    private Tree<String> bestTree = null;
    private double[] splittingThresholds = {5.75, 7.19, 6.95, 9.34, 9.34, 6.25, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private double[] thresholds = {-12, -12, -11, -12, -12, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14}; //{-9.75, -10, -9.6, -9.66, -8.01};
    private double[] largeThresholds = {-12, -12, -11, -12, -12, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14, -14}; //{-9.75, -10, -9.6, -9.66, -8.01};
    private double[] smallThresholds = {-20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20, -20}; //{-9.75, -10, -9.6, -9.66, -8.01};
    private double[] verySmallThreaholds = {-50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50};
    private double[] insideThresholds = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    private double[][][] maxScore;
    private int[][][] maxSplit;
    private int[][][] maxChild;
    private int[][][] maxLeftChild;
    private int[][][] maxRightChild;
    private NBestArrayList[][][] maxNBestList;
    private int level;
    private Grammar grammar;
    private RuleManager ruleManager;
    private LexiconManager lexiconManager;
    private int[][] fine2coarseMapping;
    private int numNodes;
    private boolean[] phrasalNodes;
    private int length;
    private int[][] narrowRExtent;
    private int[][] narrowLExtent;
    private int[][] wideRExtent;
    private int[][] wideLExtent;
    private int[][][] iScale;
    private int[][][] oScale;
    private double[] scoresToAdd;
    private double[] scoresToAddL;
    private double[] scoresToAddR;
    private double[][] scoresAfterUnaries;
    private boolean[] changedAfterUnaries;
    private int numLevels;
    private double maxParsingTime = Double.POSITIVE_INFINITY;
    private Date startTime = null;
    private Date endTime = null;
    private boolean exceededTimeLimit = false;
    private Tree<ViterbiConstituent> viterbiTree = null;
    protected int nbestSize = 5;
    List<Tree<String>> nbestTrees;
    private double pruningScale = Double.POSITIVE_INFINITY;
    private int pruningMethod = 0;
    private int minSplitLen = 70;

    public void setPruningScale(double pruningScale) {
        this.pruningScale = pruningScale;
    }

    public void setPruningMethod(int pruningMethod) {
        this.pruningMethod = pruningMethod;
    }

    public int getNumLevels() {
        return numLevels;
    }

    public void setNbestSize(int nbestSize) {
        this.nbestSize = nbestSize;
    }

    public void setMaxParsingTime(double maxParsingMin) {
        maxParsingTime = maxParsingMin * 60000;
        if (maxParsingTime == 0) {
            maxParsingTime = Double.POSITIVE_INFINITY;
        }
    }

    public void useSmallThreasholds() {
        thresholds = smallThresholds;
    }

    public void useLargeThreasholds() {
        thresholds = largeThresholds;
    }

    public void useVerySmallThreaholds() {
        thresholds = verySmallThreaholds;
    }

    public int[] getNumStates() {
        return numStates;
    }

    public List<String> getNodeList() {
        return nodeList;
    }

    public ParseDecoder(List<Grammar> grammarList) {
        this.grammarList = grammarList;
        numLevels = grammarList.size();
    }

    public void doPreParses(List<String> sentence) {
        length = sentence.size();
        clearArrays();
        for (level = 0; level < numLevels - 2; level++) {
            initParsingModel(level);
            createArrays(level == 0, Double.NEGATIVE_INFINITY);
            initChart(sentence, true);
            if (level == 0) {
                doViterbiInsideScores0();
            } else {
                doViterbiInsideScores();
            }
            if (exceededTimeLimit) {
                return;
            }
            pruneChart2(true);
            if (level == 0) {
                doViterbiOutsideScores0();
            } else {
                doViterbiOutsideScores();
            }
            if (exceededTimeLimit) {
                return;
            }
            pruneChart();
        }
    }

//    public double doSingleParse(List<String> sentence, boolean parseXBar, boolean onlyXBar,
//            boolean[][][] anotherAllowedNodes,
//            Numberer anotherNumberer) {
//        resetOperations();
//        length = sentence.size();
//        startTime = new Date();
//        exceededTimeLimit = false;
//
//        clearArrays();
//        for (level = 0; level < numLevels - 2; level++) {
//            if (level == 0 && !parseXBar) {
//                continue;
//            }
//            initParsingModel(level);
//            if (level == 1 && anotherAllowedNodes != null) {
//                createArrays(anotherAllowedNodes, anotherNumberer, Double.NEGATIVE_INFINITY);
//            } else {
//                createArrays(level == 0, Double.NEGATIVE_INFINITY);
//            }
//            initChart(sentence, true);
//            if (level == 0) {
//                doViterbiInsideScores0();
//            } else {
//                doViterbiInsideScores();
//            }
//            if (exceededTimeLimit) {
//                return Double.NEGATIVE_INFINITY;
//            }
//            pruneChart2(true);
//            if (level == 0) {
//                doViterbiOutsideScores0();
//            } else {
//                doViterbiOutsideScores();
//            }
//            if (exceededTimeLimit) {
//                return Double.NEGATIVE_INFINITY;
//            }
//            pruneChart();
//            if (level == 0 && onlyXBar) {
//                return iScore[0][length][0][0];
//            }
//        }
//
//        initParsingModel(numLevels - 1);
//        if (level == 2 && anotherAllowedNodes != null) {
//            createArrays(anotherAllowedNodes, anotherNumberer, 0);
//        } else {
//            createArrays(level == 1, 0);
//        }
//        setupScaling();
//        initChart(sentence, false);
//        doScaledInsideScores();
//        if (exceededTimeLimit || iScore[0][length][0][0] == 0) {
//            return Double.NEGATIVE_INFINITY;
//        }
//        pruneChart2(false);
//        doScaledOutsideScores();
//        if (exceededTimeLimit) {
//            return Double.NEGATIVE_INFINITY;
//        }
//        return Math.log(iScore[0][length][0][0])
//                + ScalingTools.getLogScale(iScale[0][length][0]);
//    }
    protected void pruneCrossing(int start, int end, int split) {
        for (int i = start; i < split; i++) {
            for (int j = split + 1; j <= end; j++) {
                if (i == start && j == end) {
                    continue;
                }
                Arrays.fill(allowedNodes[i][j], false);
            }
        }
    }

    protected void pruneInsideChart(int diff) {
        if (pruningMethod == 0) {
            return;
        }
        double maxLogProb = Double.NEGATIVE_INFINITY;
        for (int start = 0; start < (length - diff + 1); start++) {
            int end = start + diff;
            double spanMaxLogProb = Double.NEGATIVE_INFINITY;
            for (int node = 0; node < numNodes; node++) {
                if (diff > 1 && !phrasalNodes[node]) {
                    allowedNodes[start][end][node] = false;
                    ArrayMath.fill(allowedStates[start][end][node], false);
                    continue;
                }
                if (!allowedNodes[start][end][node]) {
                    continue;
                }
                for (int state = 0; state < numStates[node]; state++) {
                    if (!allowedStates[start][end][node][state]) {
                        continue;
                    }
                    double iS = iScore[start][end][node][state];

                    if (iS == Double.NEGATIVE_INFINITY) {
                        allowedStates[start][end][node][state] = false;
                        continue;
                    }
                    if (iS > maxLogProb) {
                        maxLogProb = iS;
                    }
                    if (iS > spanMaxLogProb) {
                        spanMaxLogProb = iS;
                    }
                }
            }
            if (pruningMethod == 1) {
                for (int node = 0; node < numNodes; node++) {
                    if (diff > 1 && !phrasalNodes[node]) {
                        //TODO: check whether need to add something like
                        allowedNodes[start][end][node] = false;
                        ArrayMath.fill(allowedStates[start][end][node], false);
                        continue;
                    }
                    if (!allowedNodes[start][end][node]) {
                        continue;
                    }
                    boolean nonePossible = true;
                    for (int state = 0; state < numStates[node]; state++) {
                        if (!allowedStates[start][end][node][state]) {
                            continue;
                        }
                        double iS = iScore[start][end][node][state];

                        if (iS == Double.NEGATIVE_INFINITY) {
                            allowedStates[start][end][node][state] = false;
                            continue;
                        }
                        double posterior = iS - spanMaxLogProb;
                        if (posterior > insideThresholds[level] * pruningScale) {
                            allowedStates[start][end][node][state] = true;
                            nonePossible = false;
                        } else {
                            allowedStates[start][end][node][state] = false;
                        }
                    }
                    if (nonePossible) {
                        allowedNodes[start][end][node] = false;
                    }
                }
            }
        }

        if (pruningMethod == 2) {
            for (int start = 0; start < (length - diff + 1); start++) {
                int end = start + diff;
                for (int node = 0; node < numNodes; node++) {
                    if (diff > 1 && !phrasalNodes[node]) {
                        //TODO: check whether need to add something like
                        allowedNodes[start][end][node] = false;
                        ArrayMath.fill(allowedStates[start][end][node], false);
                        continue;
                    }
                    if (!allowedNodes[start][end][node]) {
                        continue;
                    }
                    boolean nonePossible = true;
                    for (int state = 0; state < numStates[node]; state++) {
                        if (!allowedStates[start][end][node][state]) {
                            continue;
                        }
                        double iS = iScore[start][end][node][state];

                        if (iS == Double.NEGATIVE_INFINITY) {
                            allowedStates[start][end][node][state] = false;
                            continue;
                        }
                        double posterior = iS - maxLogProb;
                        if (posterior > insideThresholds[level] * pruningScale) {
                            allowedStates[start][end][node][state] = true;
                            nonePossible = false;
                        } else {
                            allowedStates[start][end][node][state] = false;
                        }
                    }
                    if (nonePossible) {
                        allowedNodes[start][end][node] = false;
                    }
                }
            }
        }
    }

    protected void pruneChart() {
        double sentenceProb = iScore[0][length][0][0];
        for (int diff = 1; diff <= length; diff++) {
            for (int start = 0; start < (length - diff + 1); start++) {
                int end = start + diff;
                for (int node = 0; node < numNodes; node++) {
                    if (diff > 1 && !phrasalNodes[node]) {
                        //TODO: check whether need to add something like
                        allowedNodes[start][end][node] = false;
                        ArrayMath.fill(allowedStates[start][end][node], false);
                        continue;
                    }
                    if (!allowedNodes[start][end][node]) {
                        continue;
                    }
                    boolean nonePossible = true;
                    for (int state = 0; state < numStates[node]; state++) {
                        if (!allowedStates[start][end][node][state]) {
                            continue;
                        }
                        double iS = iScore[start][end][node][state];
                        double oS = oScore[start][end][node][state];

                        if (iS == Double.NEGATIVE_INFINITY || oS == Double.NEGATIVE_INFINITY) {
                            allowedStates[start][end][node][state] = false;
                            continue;
                        }
                        double posterior = iS + oS - sentenceProb;
                        if (posterior > thresholds[level]) {
                            allowedStates[start][end][node][state] = true;
                            nonePossible = false;
                        } else {
                            allowedStates[start][end][node][state] = false;
                        }
                    }
                    if (nonePossible) {
                        allowedNodes[start][end][node] = false;
                    }
                }
            }
        }
    }

    protected void pruneChart2(boolean viterbi) {
        double zeroVal = 0;
        if (viterbi) {
            zeroVal = Double.NEGATIVE_INFINITY;
        }
        for (int diff = 1; diff <= length; diff++) {
            for (int start = 0; start < (length - diff + 1); start++) {
                int end = start + diff;
                for (int node = 0; node < numNodes; node++) {
                    if (diff > 1 && !phrasalNodes[node]) {
                        //TODO: check whether need to add something like
                        allowedNodes[start][end][node] = false;
                        ArrayMath.fill(allowedStates[start][end][node], false);
                        continue;
                    }
                    if (!allowedNodes[start][end][node]) {
                        continue;
                    }
                    boolean nonePossible = true;
                    for (int state = 0; state < numStates[node]; state++) {
                        if (!allowedStates[start][end][node][state]) {
                            continue;
                        }
                        double iS = iScore[start][end][node][state];
                        if (iS == zeroVal) {
                            allowedStates[start][end][node][state] = false;
                        } else {
                            nonePossible = false;
                        }
                    }
                    if (nonePossible) {
                        allowedNodes[start][end][node] = false;
                    }
                }
            }
        }
    }

//    public void createArrays(boolean[][][] anotherAllowedNodes, Numberer anotherNumberer, double initVal) {
//        iScore = new double[length][length + 1][][];
//        oScore = new double[length][length + 1][][];
//        allowedNodes = new boolean[length][length + 1][];
//        allowedStates = new boolean[length][length + 1][][];
//
//        int maxStateNum = 0;
//        for (int node = 0; node < numNodes; node++) {
//            if (maxStateNum < numStates[node]) {
//                maxStateNum = numStates[node];
//            }
//        }
//        scoresToAdd = new double[maxStateNum];
//        scoresToAddL = new double[maxStateNum];
//        scoresToAddR = new double[maxStateNum];
//
//        scoresAfterUnaries = new double[numNodes][];
//        changedAfterUnaries = new boolean[numNodes];
//        for (int ni = 0; ni < numNodes; ni++) {
//            scoresAfterUnaries[ni] = new double[numStates[ni]];
//        }
//
//        for (int start = 0; start < length; start++) {
//            for (int end = start + 1; end <= length; end++) {
//                iScore[start][end] = new double[numNodes][];
//                oScore[start][end] = new double[numNodes][];
//
//                allowedNodes[start][end] = new boolean[numNodes];
//                allowedStates[start][end] = new boolean[numNodes][];
//
//                for (int node = 0; node < numNodes; node++) {
//                    iScore[start][end][node] = new double[numStates[node]];
//                    oScore[start][end][node] = new double[numStates[node]];
//                    Arrays.fill(iScore[start][end][node], initVal);
//                    Arrays.fill(oScore[start][end][node], initVal);
//
//                    String nodeName = nodeList.get(node);
//                    boolean allow = true;
//                    if (anotherNumberer.hasSeen(nodeName)) {
//                        int anotherNode = anotherNumberer.number(nodeName);
//                        allow = anotherAllowedNodes[start][end][anotherNode];
//                    }
//                    allowedNodes[start][end][node] = allow;
//                    allowedStates[start][end][node] = new boolean[numStates[node]];
//                    Arrays.fill(allowedStates[start][end][node], allow);
//                }
//            }
//        }
//        narrowRExtent = new int[length + 1][numNodes];
//        narrowLExtent = new int[length + 1][numNodes];
//        wideRExtent = new int[length + 1][numNodes];
//        wideLExtent = new int[length + 1][numNodes];
//        for (int loc = 0; loc <= length; loc++) {
//            Arrays.fill(narrowLExtent[loc], -1);
//            Arrays.fill(wideLExtent[loc], length + 1);
//            Arrays.fill(narrowRExtent[loc], length + 1);
//            Arrays.fill(wideRExtent[loc], -1);
//        }
//    }
    public void clearArrays() {
        iScore = null;
        oScore = null;
        maxChild = null;
        maxLeftChild = null;
        maxRightChild = null;
        maxScore = null;
        maxSplit = null;
        allowedNodes = null;
        allowedStates = null;
        narrowRExtent = null;
        narrowLExtent = null;
        wideRExtent = null;
        wideLExtent = null;
    }

    public double computeBinaryRuleScore(int pNode, int lNode, int rNode, int start, int split, int end) {
        if (!allowedNodes[start][end][pNode] || !allowedNodes[start][split][lNode] || !allowedNodes[split][end][rNode]) {
            return Double.NEGATIVE_INFINITY;
        }

        BinaryRule br = ruleManager.getBinaryRule(pNode, lNode, rNode);
        if (br == null) {
            return Double.NEGATIVE_INFINITY;
        }
        double sentenceScore = iScore[0][length][0][0];
        double[][][] scores = br.getRuleProbs();
        int nParentStates = numStates[pNode];
        int nLeftChildStates = numStates[lNode];
        int nRightChildStates = numStates[rNode];
        double ruleScore = 0;
        double gScore = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][split][lNode] + iScale[split][end][rNode] - iScale[0][length][0]);
        for (int lsi = 0; lsi < nLeftChildStates; lsi++) {
            if (!allowedStates[start][split][lNode][lsi]) {
                continue;
            }
            if (scores[lsi] == null) {
                continue;
            }
            double lIS = iScore[start][split][lNode][lsi];
            if (lIS == 0) {
                continue;
            }
            for (int rsi = 0; rsi < nRightChildStates; rsi++) {
                if (!allowedStates[split][end][rNode][rsi]) {
                    continue;
                }
                if (scores[lsi][rsi] == null) {
                    continue;
                }
                double rIS = iScore[split][end][rNode][rsi];
                if (rIS == 0) {
                    continue;
                }
                for (int psi = 0; psi < nParentStates; psi++) {
                    if (!allowedStates[start][end][pNode][psi]) {
                        continue;
                    }
                    double rS = scores[lsi][rsi][psi];
                    if (rS == 0) {
                        continue;
                    }
                    double pOS = oScore[start][end][pNode][psi];
                    if (pOS == 0) {
                        continue;
                    }
                    ruleScore += (pOS * rS * lIS * rIS) / sentenceScore;
                }
            }
        }
        gScore += Math.log(ruleScore);
        return gScore;
    }

    public double computeUnryRuleScore(int pNode, int cNode, int start, int end) {
        if (!allowedNodes[start][end][pNode] || !allowedNodes[start][end][cNode]) {
            return Double.NEGATIVE_INFINITY;
        }
        UnaryRule ur = ruleManager.getUnaryRule(pNode, cNode);
        if (ur == null) {
            return Double.NEGATIVE_INFINITY;
        }
        double sentenceScore = iScore[0][length][0][0];
        double[][] probs = ur.getRuleProbs();
        int nParentStates = numStates[pNode];
        int nChildStates = numStates[cNode];
        double ruleScore = 0;
        double gScore = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][end][cNode] - iScale[0][length][0]);
        for (int csi = 0; csi < nChildStates; csi++) {
            if (!allowedStates[start][end][cNode][csi]) {
                continue;
            }
            if (probs[csi] == null) {
                continue;
            }
            double cIS = iScore[start][end][cNode][csi];
            if (cIS == 0) {
                continue;
            }
            for (int psi = 0; psi < nParentStates; psi++) {
                if (!allowedStates[start][end][pNode][psi]) {
                    continue;
                }
                double rS = probs[csi][psi];
                if (rS == 0) {
                    continue;
                }
                double pOS = oScore[start][end][pNode][psi];
                if (pOS == 0) {
                    continue;
                }
                ruleScore += (pOS * rS * cIS) / sentenceScore;
            }

        }
        gScore += Math.log(ruleScore);
        return gScore;
    }

    public double computeNodeScore(int pNode, int start, int end) {
        if (!allowedNodes[start][end][pNode]) {
            return Double.NEGATIVE_INFINITY;
        }
        double sentenceScore = iScore[0][length][0][0];
        int nParentStates = numStates[pNode];
        double ruleScore = 0;
        double gScore = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][end][pNode] - iScale[0][length][0]);
        for (int si = 0; si < nParentStates; si++) {
            double oS = oScore[start][end][pNode][si];
            double iS = iScore[start][end][pNode][si];
            ruleScore += (oS * iS) / sentenceScore;
        }
        gScore += Math.log(ruleScore);
        return gScore;
    }

    void initChart(List<String> sentence, boolean viterbi) {
        double zeroVal = 0;
        if (viterbi) {
            zeroVal = Double.NEGATIVE_INFINITY;
        }
        for (int start = 0; start < length; start++) {
            int end = start + 1;
            String word = sentence.get(start);
            for (int tag = 0; tag < numNodes; tag++) {
                if (!allowedNodes[start][end][tag]) {
                    continue;
                }
                if (phrasalNodes[tag]) {
                    continue;
                }
                narrowRExtent[start][tag] = end;
                wideRExtent[start][tag] = end;
                narrowLExtent[end][tag] = start;
                wideLExtent[end][tag] = start;
                double[] lexiconScores = lexiconManager.getProbs(tag, word, viterbi);
                boolean allZero = true;
                for (int state = 0; state < numStates[tag]; state++) {
                    if (!allowedStates[start][end][tag][state]) {
                        continue;
                    }
                    double prob = lexiconScores[state];
                    if (prob != zeroVal) {
                        iScore[start][end][tag][state] = prob;
                        if (!viterbi) {
                            iScale[start][end][tag] = 0;
                        }
                        allZero = false;
                    } else {
                        allowedStates[start][end][tag][state] = false;
                    }
                }
                if (allZero) {
                    allowedNodes[start][end][tag] = false;
                }
            }
        }
    }

    public void createIOScores(double initVal) {
        for (int start = 0; start < length; start++) {
            for (int end = start + 1; end <= length; end++) {
                iScore[start][end] = new double[numNodes][];
                oScore[start][end] = new double[numNodes][];
                for (int node = 0; node < numNodes; node++) {
                    iScore[start][end][node] = new double[numStates[node]];
                    oScore[start][end][node] = new double[numStates[node]];
                    Arrays.fill(iScore[start][end][node], initVal);
                    Arrays.fill(oScore[start][end][node], initVal);
                }
            }
        }
    }

    public void createArrays(boolean firstTime, double initVal) {
        if (firstTime) {
            iScore = new double[length][length + 1][][];
            oScore = new double[length][length + 1][][];
            allowedNodes = new boolean[length][length + 1][];
            allowedStates = new boolean[length][length + 1][][];
        }

        int maxStateNum = 0;
        for (int node = 0; node < numNodes; node++) {
            if (maxStateNum < numStates[node]) {
                maxStateNum = numStates[node];
            }
        }
        scoresToAdd = new double[maxStateNum];
        scoresToAddL = new double[maxStateNum];
        scoresToAddR = new double[maxStateNum];

        scoresAfterUnaries = new double[numNodes][];
        changedAfterUnaries = new boolean[numNodes];
        for (int ni = 0; ni < numNodes; ni++) {
            scoresAfterUnaries[ni] = new double[numStates[ni]];
        }

        for (int start = 0; start < length; start++) {
            for (int end = start + 1; end <= length; end++) {
                iScore[start][end] = new double[numNodes][];
                oScore[start][end] = new double[numNodes][];
                boolean[][] newAllowedStates = null;
                boolean[] newAllowedNodes = null;
                if (firstTime) {
                    allowedNodes[start][end] = new boolean[numNodes];
                    allowedStates[start][end] = new boolean[numNodes][];
                } else {
                    newAllowedNodes = new boolean[numNodes];
                    newAllowedStates = new boolean[numNodes][];
                }
                for (int node = 0; node < numNodes; node++) {
                    iScore[start][end][node] = new double[numStates[node]];
                    oScore[start][end][node] = new double[numStates[node]];
                    Arrays.fill(iScore[start][end][node], initVal);
                    Arrays.fill(oScore[start][end][node], initVal);
                    if (firstTime) {
                        allowedNodes[start][end][node] = true;
                        allowedStates[start][end][node] = new boolean[numStates[node]];
                        Arrays.fill(allowedStates[start][end][node], true);
                    } else {
                        boolean allowOne = false;
                        newAllowedStates[node] = new boolean[numStates[node]];
                        Arrays.fill(newAllowedStates[node], false);
                        if (allowedNodes[start][end][node]) {
                            for (int state = 0; state < numStates[node]; state++) {
                                int coarserNode = node;
                                int coarserState = fine2coarseMapping[node][state];
                                if (allowedStates[start][end][coarserNode][coarserState]) {
                                    newAllowedStates[node][state] = true;
                                    allowOne = true;
                                } else {
                                    newAllowedStates[node][state] = false;
                                }
                            }
                        }
                        if (allowOne) {
                            newAllowedNodes[node] = true;
                        } else {
                            newAllowedNodes[node] = false;
                        }
                    }
                }
                if (!firstTime) {
                    allowedNodes[start][end] = newAllowedNodes;
                    allowedStates[start][end] = newAllowedStates;
                }
            }
        }
        narrowRExtent = new int[length + 1][numNodes];
        narrowLExtent = new int[length + 1][numNodes];
        wideRExtent = new int[length + 1][numNodes];
        wideLExtent = new int[length + 1][numNodes];
        for (int loc = 0; loc <= length; loc++) {
            Arrays.fill(narrowLExtent[loc], -1);
            Arrays.fill(wideLExtent[loc], length + 1);
            Arrays.fill(narrowRExtent[loc], length + 1);
            Arrays.fill(wideRExtent[loc], -1);
        }
    }

    public void createArrays(boolean[][][] anotherAllowedNodes, Map<String, Integer> anotherNodeMap, double initVal) {
        iScore = new double[length][length + 1][][];
        oScore = new double[length][length + 1][][];
        allowedNodes = new boolean[length][length + 1][];
        allowedStates = new boolean[length][length + 1][][];

        int maxStateNum = 0;
        for (int node = 0; node < numNodes; node++) {
            if (maxStateNum < numStates[node]) {
                maxStateNum = numStates[node];
            }
        }
        scoresToAdd = new double[maxStateNum];
        scoresToAddL = new double[maxStateNum];
        scoresToAddR = new double[maxStateNum];

        scoresAfterUnaries = new double[numNodes][];
        changedAfterUnaries = new boolean[numNodes];
        for (int ni = 0; ni < numNodes; ni++) {
            scoresAfterUnaries[ni] = new double[numStates[ni]];
        }

        for (int start = 0; start < length; start++) {
            for (int end = start + 1; end <= length; end++) {
                iScore[start][end] = new double[numNodes][];
                oScore[start][end] = new double[numNodes][];

                allowedNodes[start][end] = new boolean[numNodes];
                allowedStates[start][end] = new boolean[numNodes][];

                for (int node = 0; node < numNodes; node++) {
                    iScore[start][end][node] = new double[numStates[node]];
                    oScore[start][end][node] = new double[numStates[node]];
                    Arrays.fill(iScore[start][end][node], initVal);
                    Arrays.fill(oScore[start][end][node], initVal);

                    String nodeName = nodeList.get(node);
                    boolean allow = true;
                    if (anotherNodeMap.containsKey(nodeName)) {
                        int anotherNode = anotherNodeMap.get(nodeName);
                        allow = anotherAllowedNodes[start][end][anotherNode];
                    }
                    allowedNodes[start][end][node] = allow;
                    allowedStates[start][end][node] = new boolean[numStates[node]];
                    Arrays.fill(allowedStates[start][end][node], allow);
                }
            }
        }
        narrowRExtent = new int[length + 1][numNodes];
        narrowLExtent = new int[length + 1][numNodes];
        wideRExtent = new int[length + 1][numNodes];
        wideLExtent = new int[length + 1][numNodes];
        for (int loc = 0; loc <= length; loc++) {
            Arrays.fill(narrowLExtent[loc], -1);
            Arrays.fill(wideLExtent[loc], length + 1);
            Arrays.fill(narrowRExtent[loc], length + 1);
            Arrays.fill(wideRExtent[loc], -1);
        }
    }

    public boolean hasExceededTimeLimit() {
        if (exceededTimeLimit) {
            return true;
        }
        if (Double.isInfinite(maxParsingTime)) {
            return false;
        }
        endTime = new Date();
        double dur = endTime.getTime() - startTime.getTime();
        if (dur > maxParsingTime) {
            exceededTimeLimit = true;
            System.err.println("Parsing time " + dur / 1000 + " seconds exceeds limit: " + maxParsingTime / 1000 + " seconds. Start: " + startTime + "; End: " + endTime);
        }
        return exceededTimeLimit;
    }

    public TriSet<Integer, Integer, Integer> getSplittingSet(Tree<String> tree) {
        if (tree.isPreTerminal()) {
            return new TriSet<Integer, Integer, Integer>();
        }
        List<Tree<String>> children = tree.getChildren();
        TriSet<Integer, Integer, Integer> splitSet = new TriSet<Integer, Integer, Integer>();
        switch (children.size()) {
            case 1: {
                break;
            }
            case 2: {
                splitSet.add(tree.getStart(), tree.getEnd(), children.get(0).getEnd());
                break;
            }
            default: {
                throw new Error();
            }
        }
        for (Tree<String> child : children) {
            TriSet<Integer, Integer, Integer> childSplitSet = getSplittingSet(child);
            for (Entry<Integer, BiSet<Integer, Integer>> biEntry : childSplitSet.entrySet()) {
                Integer start = biEntry.getKey();
                for (Entry<Integer, HashSet<Integer>> uniEntry : biEntry.getValue().entrySet()) {
                    Integer end = uniEntry.getKey();
                    for (Integer split : uniEntry.getValue()) {
                        splitSet.add(start, end, split);
                    }
                }
            }
        }

        return splitSet;
    }

    public static void setSpans(Tree<String> tree) {
        List<Tree<String>> terminals = tree.getTerminals();
        setSpansHelper(tree, terminals);
    }

    public static void setSpansHelper(Tree<String> tree, List<Tree<String>> terminals) {
        if (tree.isLeaf()) {
            int index = terminals.indexOf(tree);
            tree.setStart(index);
            tree.setEnd(index + 1);
            return;
        }

        List<Tree<String>> children = tree.getChildren();
        for (Tree<String> child : children) {
            setSpansHelper(child, terminals);
        }
        tree.setStart(children.get(0).getStart());
        tree.setEnd(children.get(children.size() - 1).getEnd());
    }

    public double doSingleParse(List<String> sentence, boolean parseXBar, boolean onlyXBar,
            boolean[][][] anotherAllowedNodes, Map<String, Integer> anotherNodeMap) {
        length = sentence.size();
        startTime = new Date();
        exceededTimeLimit = false;

        clearArrays();
        for (level = 0; level < numLevels - 2; level++) {
            if (level == 0 && !parseXBar) {
                continue;
            }
            initParsingModel(level);
            if (level == 1 && anotherAllowedNodes != null) {
                createArrays(anotherAllowedNodes, anotherNodeMap, Double.NEGATIVE_INFINITY);
            } else {
                createArrays(level == 0, Double.NEGATIVE_INFINITY);
            }
            initChart(sentence, true);
            if (level == 0) {
                doViterbiInsideScores0();
            } else {
                doViterbiInsideScores();
            }
            if (exceededTimeLimit) {
                return Double.NEGATIVE_INFINITY;
            }
            pruneChart2(true);
            if (level == 0) {
                doViterbiOutsideScores0();
            } else {
                doViterbiOutsideScores();
            }
            if (exceededTimeLimit) {
                return Double.NEGATIVE_INFINITY;
            }
            pruneChart();
            if (level == 0 && onlyXBar) {
                return iScore[0][length][0][0];
            }
        }

        initParsingModel(numLevels - 1);
        if (level == 2 && anotherAllowedNodes != null) {
            createArrays(anotherAllowedNodes, anotherNodeMap, 0);
        } else {
            createArrays(level == 1, 0);
        }
        setupScaling();
        initChart(sentence, false);
        doScaledInsideScores();
        if (exceededTimeLimit || iScore[0][length][0][0] == 0) {
            return Double.NEGATIVE_INFINITY;
        }
        pruneChart2(false);
        doScaledOutsideScores();
        if (exceededTimeLimit) {
            return Double.NEGATIVE_INFINITY;
        }
//        doMaxScore();
        return Math.log(iScore[0][length][0][0])
                + ScalingTools.getLogScale(iScale[0][length][0]);
    }

    public Tree<String> getBestParse(List<String> sentence) {
        startTime = new Date();
        if (grammarList.size() == 2) {
            return getRawGrammarParse(sentence);
        }
        bestTree = null;
        exceededTimeLimit = false;
        length = sentence.size();

        doPreParses(sentence);
        if (exceededTimeLimit) {
            return null;
        }

        initParsingModel(numLevels - 1);
        createArrays(level == 1, 0);
        setupScaling();

        initChart(sentence, false);
        doScaledInsideScores();
        if (exceededTimeLimit || iScore[0][length][0][0] == 0) {
            return null;
        }

        pruneChart2(false);
        doScaledOutsideScores();
        if (exceededTimeLimit) {
            return null;
        }

        doMaxScore();
        bestTree = extractBestMaxRuleParse(0, length, sentence);
        return bestTree;
    }

    public Tree<String> getBestParseWithCrossPruning(List<String> sentence) {
        startTime = new Date();
        if (grammarList.size() == 2) {
            return getRawGrammarParse(sentence);
        }
        bestTree = null;
        exceededTimeLimit = false;
        length = sentence.size();
        doPreParses(sentence);
        if (exceededTimeLimit) {
            return null;
        }
        initParsingModel(numLevels - 1);
        createArrays(level == 1, 0);
        setupScaling();
        initChart(sentence, false);
        doScaledInsideScores();
        if (exceededTimeLimit || iScore[0][length][0][0] == 0) {
            return null;
        }
        pruneChart2(false);
        doScaledOutsideScores();
        if (exceededTimeLimit) {
            return null;
        }

//        splitByPostProb(sentence, 0, length, 0.5);
        doMaxScore();
        bestTree = extractBestMaxRuleParse(0, length, sentence);

        return bestTree;
    }

//    public boolean splitByPostProb(List<String> sentence, int start, int end, double threshold) {
//        double[] splitPostProb = doSplittingPostProb(start, end);
//        if (splitPostProb == null) {
//            return false;
//        }
//        for (int i = 0; i < length; i++) {
//            System.out.printf("%s/%.2f ", sentence.get(i), splitPostProb[i + 1]);
//        }
//        System.out.println();
//
//        int split = ArrayMath.argmax(splitPostProb);
//        if (splitPostProb[split] < threshold) {
//            return false;
//        }
//        splitByPostProb(sentence, start, split, threshold);
//        splitByPostProb(sentence, split, end, threshold);
//        return true;
//    }
    public boolean[][][] getAllowedNodes() {
        return allowedNodes;
    }

    public List<Tree<String>> getNBestParse(List<String> sentence) {
        if (grammarList.size() == 2) {
            throw new RuntimeException("Nbest parsing of the raw grammar is not supported yet");
        }
        bestTree = null;
        startTime = new Date();
        exceededTimeLimit = false;
        doPreParses(sentence);
        if (exceededTimeLimit) {
            return null;
        }
        initParsingModel(++level);
        createArrays(level == 0, 0);
        setupScaling();
        initChart(sentence, false);
        doScaledInsideScores();
        if (exceededTimeLimit || iScore[0][length][0][0] == 0) {
            return null;
        }
        pruneChart2(false);
        doScaledOutsideScores();
        if (exceededTimeLimit) {
            return null;
        }
        doNBestMaxScores(sentence);
        nbestTrees = extractNBestMaxRuleParses(0, length, sentence);
        return nbestTrees;
    }

    public Tree<String> getBestViterbiParse(List<String> sentence) {
        bestTree = null;
        startTime = new Date();
        exceededTimeLimit = false;
        length = sentence.size();
        if (exceededTimeLimit) {
            return null;
        }
        initParsingModel(numLevels - 2);
        level = 0;
        createArrays(level == 0, Double.NEGATIVE_INFINITY);
        initChart(sentence, true);
        doViterbiInsideScores();
        if (exceededTimeLimit || iScore[0][length][0][0] == Double.NEGATIVE_INFINITY) {
            return null;
        }
        bestTree = extractBestViterbiParse(0, 0, 0, length, sentence);
        return bestTree;
    }

    public Tree<String> getRawGrammarParse(List<String> sentence) {
        bestTree = null;
        length = sentence.size();
        initParsingModel(0);
        createArrays(true, Double.NEGATIVE_INFINITY);
        initChart(sentence, true);
        doViterbiInsideScores0();
        if (iScore[0][length][0][0] == Double.NEGATIVE_INFINITY) {
            return null;
        }
        bestTree = extractBestViterbiParse(0, 0, 0, length, sentence);
        return bestTree;
    }

    public double getMaxScore() {
        if (bestTree == null) {
            return Double.NEGATIVE_INFINITY;
        } else {
            if (level == 0) {
                return iScore[0][length][0][0];
            } else {
                return maxScore[0][length][0];
            }
        }
    }

    public double getMaxScore(int ibest) {
        NBestItem item = (NBestItem) maxNBestList[0][length][0].get(ibest);
        if (item == null) {
            throw new RuntimeException("Unexpected error");
        }
        return item.getScore();
    }

    public double getLL() {
        return Math.log(iScore[0][length][0][0]) + ScalingTools.getLogScale(iScale[0][length][0]);
    }

    public void setupScaling() {
        // create arrays for scaling coefficients
        iScale = new int[length][length + 1][];
        oScale = new int[length][length + 1][];

        for (int start = 0; start < length; start++) {
            for (int end = start + 1; end <= length; end++) {
                iScale[start][end] = new int[numNodes];
                oScale[start][end] = new int[numNodes];
                Arrays.fill(iScale[start][end], Integer.MIN_VALUE);
                Arrays.fill(oScale[start][end], Integer.MIN_VALUE);
            }
        }
    }

    public void initParsingModel(int level) {
        this.level = level;
        grammar = grammarList.get(level);
        ruleManager = grammar.getRuleManager();
        lexiconManager = grammar.getLexiconManager();
        numNodes = grammar.getNumNodes();
        numStates = grammar.getNumStates();
        phrasalNodes = grammar.getIsPhrasalNode();
        fine2coarseMapping = grammar.getFine2coarseMapping();
        nodeList = grammar.getNodeList();
        nodeMap = grammar.getNodeMap();
    }

    void doScaledInsideScores() {
        double initVal = 0;
        for (int diff = 1; diff <= length; diff++) {
            for (int start = 0; start < (length - diff + 1); start++) {
                if (hasExceededTimeLimit()) {
                    return;
                }
                int end = start + diff;
                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (diff == 1) {
                        continue; // there are no binary rules that span over 1 symbol only

                    }
                    if (!allowedNodes[start][end][pNode]) {
                        continue;
                    }

                    if (start == 6 && end == 8 && pNode == 23) {
                        boolean check = true;
                    }
                    BinaryRule[] binaryRules = ruleManager.getBinaryRulesWithP(pNode);
                    int nParentStates = numStates[pNode];
                    boolean somethingChanged = false;
                    for (int r = 0; r < binaryRules.length; r++) {
                        BinaryRule br = binaryRules[r];
                        int lNode = br.getLeftChild();
                        int rNode = br.getRightChild();

                        int narrowR = narrowRExtent[start][lNode];
                        boolean iPossibleL = narrowR < end; // can this left constituent leave space for a right constituent?

                        if (!iPossibleL) {
                            continue;
                        }

                        int narrowL = narrowLExtent[end][rNode];
                        boolean iPossibleR = narrowL >= narrowR; // can this right constituent fit next to the left constituent?

                        if (!iPossibleR) {
                            continue;
                        }

                        int min1 = narrowR;
                        int min2 = wideLExtent[end][rNode];
                        int min = min1 > min2 ? min1 : min2; // can this right constituent stretch far enough to reach the left constituent?

                        if (min > narrowL) {
                            continue;
                        }

                        int max1 = wideRExtent[start][lNode];
                        int max2 = narrowL;
                        int max = max1 < max2 ? max1 : max2; // can this left constituent stretch far enough to reach the right constituent?

                        if (min > max) {
                            continue;
                        }

                        // TODO switch order of loops for efficiency
                        double[][][] ruleProbs = br.getRuleProbs();
                        int nLeftChildStates = numStates[lNode];
                        int nRightChildStates = numStates[rNode];

                        for (int split = min; split <= max; split++) {
                            boolean changeThisRound = false;
                            if (!allowedNodes[start][split][lNode]) {
                                continue;
                            }
                            if (!allowedNodes[split][end][rNode]) {
                                continue;
                            }
                            if (start == 6 && split == 8 && end == 11 && lNode == 23 && rNode == 1 && pNode == 6) {
                                boolean check = true;
                            }

                            for (int lsi = 0; lsi < nLeftChildStates; lsi++) {
                                if (!allowedStates[start][split][lNode][lsi]) {
                                    continue;
                                }
                                double lIS = iScore[start][split][lNode][lsi];
                                if (lIS == initVal) {
                                    continue;
                                }
                                if (ruleProbs[lsi] == null) {
                                    continue;
                                }
                                for (int rsi = 0; rsi < nRightChildStates; rsi++) {
                                    if (ruleProbs[lsi][rsi] == null) {
                                        continue;
                                    }
                                    if (!allowedStates[split][end][rNode][rsi]) {
                                        continue;
                                    }
                                    double rIS = iScore[split][end][rNode][rsi];
                                    if (rIS == initVal) {
                                        continue;
                                    }
                                    for (int psi = 0; psi < nParentStates; psi++) {
                                        if (!allowedStates[start][end][pNode][psi]) {
                                            continue;
                                        }
                                        double rS = ruleProbs[lsi][rsi][psi];
                                        if (rS == initVal) {
                                            continue;
                                        }
                                        double thisRound = rS * lIS * rIS;
                                        scoresToAdd[psi] += thisRound;
                                        somethingChanged = true;
                                        changeThisRound = true;
                                    }
                                }
                            }
                            if (!changeThisRound) {
                                continue;
                            }
                            //boolean firstTime = false;
                            int parentScale = iScale[start][end][pNode];
                            int currentScale = iScale[start][split][lNode] + iScale[split][end][rNode];
                            currentScale = ScalingTools.scaleArray(scoresToAdd, currentScale);

                            if (parentScale != currentScale) {
                                if (parentScale == Integer.MIN_VALUE) {
                                    // first time to build this span
                                    iScale[start][end][pNode] = currentScale;
                                } else {
                                    int newScale = Math.max(currentScale, parentScale);
                                    ScalingTools.scaleArrayToScale(scoresToAdd, currentScale, newScale);
                                    ScalingTools.scaleArrayToScale(iScore[start][end][pNode], parentScale, newScale);
                                    iScale[start][end][pNode] = newScale;
                                }
                            }
                            for (int psi = 0; psi < nParentStates; psi++) {
                                iScore[start][end][pNode][psi] += scoresToAdd[psi];
                            }
                            Arrays.fill(scoresToAdd, initVal);
                        }
                    }
                    if (!somethingChanged) {
                        continue;
                    }

                    if (start > narrowLExtent[end][pNode]) {
                        narrowLExtent[end][pNode] = start;
                    }
                    if (start < wideLExtent[end][pNode]) {
                        wideLExtent[end][pNode] = start;
                    }
                    if (end < narrowRExtent[start][pNode]) {
                        narrowRExtent[start][pNode] = end;
                    }
                    if (end > wideRExtent[start][pNode]) {
                        wideRExtent[start][pNode] = end;
                    }
                }
                // now do the unaries
                Arrays.fill(changedAfterUnaries, false);
                int[] parentScales = new int[numNodes];
                Arrays.fill(parentScales, Integer.MIN_VALUE);
                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (start == 6 && end == 8 && pNode == 23) {
                        boolean check = true;
                    }
                    if (!allowedNodes[start][end][pNode]) {
                        continue;
                    }
                    if (pNode == 0) {
                        continue;
                    }
                    // Should be: Closure under sum-product:
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
                    //UnaryRule[] unaries = grammar.getUnaryRulesByParent(pState).toArray(new UnaryRule[0]);
                    if (unaryRules.length == 0) {
                        continue;
                    }
                    int nParentStates = numStates[pNode]; //ruleScores[0].length;

                    int parentScale = parentScales[pNode];

                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int cNode = ur.getChild();
                        if (pNode == cNode) {
                            continue;
                        }
                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }
                        if (narrowLExtent[end][cNode] < start && wideLExtent[end][cNode] > start) {
                            continue;
                        }
                        if (narrowRExtent[start][cNode] > end && wideRExtent[start][cNode] < end) {
                            continue;
                        }

                        double[][] ruleProbs = ur.getRuleProbs();
                        final int nChildStates = numStates[cNode]; //ruleScores[0].length;

                        boolean changeThisRound = false;
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }
                            if (ruleProbs[csi] == null) {
                                continue;
                            }
                            double cIS = iScore[start][end][cNode][csi];
                            if (cIS == initVal) {
                                continue;
                            }
                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }
                                double rS = ruleProbs[csi][psi];
                                if (rS == initVal) {
                                    continue;
                                }
                                double thisRound = rS * cIS;
                                scoresToAdd[psi] += thisRound;
                                changeThisRound = true;
                            }
                        }
                        if (!changeThisRound) {
                            continue;
                        }
                        if (!changedAfterUnaries[pNode]) {
                            Arrays.fill(scoresAfterUnaries[pNode], initVal);
                            changedAfterUnaries[pNode] = true;
                        }

                        int currentScale = iScale[start][end][cNode];
                        currentScale = ScalingTools.scaleArray(scoresToAdd, currentScale);
                        if (parentScale != currentScale) {
                            if (parentScale == Integer.MIN_VALUE) {
                                parentScale = currentScale;
                            } else {
                                int newScale = Math.max(currentScale, parentScale);
                                ScalingTools.scaleArrayToScale(scoresToAdd, currentScale, newScale);
                                ScalingTools.scaleArrayToScale(scoresAfterUnaries[pNode], parentScale, newScale);
                                parentScale = newScale;
                            }
                        }
                        for (int psi = 0; psi < nParentStates; psi++) {
                            scoresAfterUnaries[pNode][psi] += scoresToAdd[psi];
                        }
                        Arrays.fill(scoresToAdd, initVal);
                        parentScales[pNode] = parentScale;
                    }
                }
                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (start == 6 && end == 8 && pNode == 23) {
                        boolean check = true;
                    }
                    if (!changedAfterUnaries[pNode]) {
                        continue;
                    }
                    int scaleBeforeUnaries = iScale[start][end][pNode];
                    int parentScale = parentScales[pNode];
                    if (scaleBeforeUnaries != parentScale) {
                        if (parentScale == Integer.MIN_VALUE) {
                            throw new Error("It is impossible");
                        } else if (scaleBeforeUnaries == Integer.MIN_VALUE) {
                            iScale[start][end][pNode] = parentScale;
                        } else {
                            int newScale = Math.max(scaleBeforeUnaries, parentScale);
                            ScalingTools.scaleArrayToScale(iScore[start][end][pNode], scaleBeforeUnaries, newScale);
                            ScalingTools.scaleArrayToScale(scoresAfterUnaries[pNode], parentScale, newScale);
                            iScale[start][end][pNode] = newScale;
                        }
                    }
                    // in any case copy/add the ruleScores from before
                    int nParentStates = numStates[pNode];
                    for (int psi = 0; psi < nParentStates; psi++) {
                        double val = scoresAfterUnaries[pNode][psi];
                        if (val > 0) {
                            iScore[start][end][pNode][psi] += val;
                        }
                    }

                    if (start > narrowLExtent[end][pNode]) {
                        narrowLExtent[end][pNode] = start;
                    }
                    if (start < wideLExtent[end][pNode]) {
                        wideLExtent[end][pNode] = start;
                    }
                    if (end < narrowRExtent[start][pNode]) {
                        narrowRExtent[start][pNode] = end;
                    }
                    if (end > wideRExtent[start][pNode]) {
                        wideRExtent[start][pNode] = end;
                    }
                }

                if (start == 0 && diff == length) {
                    int pNode = 0;
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
                    int nParentStates = numStates[pNode]; //ruleScores[0].length;
                    int parentScale = iScale[start][end][pNode];
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int cNode = ur.getChild();
                        if (pNode == cNode) {
                            continue;
                        }
                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }
                        if (narrowLExtent[end][cNode] < start && wideLExtent[end][cNode] > start) {
                            continue;
                        }
                        if (narrowRExtent[start][cNode] > end && wideRExtent[start][cNode] < end) {
                            continue;
                        }

                        double[][] ruleProbs = ur.getRuleProbs();
                        final int nChildStates = numStates[cNode]; //ruleScores[0].length;

                        boolean changeThisRound = false;
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }
                            if (ruleProbs[csi] == null) {
                                continue;
                            }
                            double cIS = iScore[start][end][cNode][csi];
                            if (cIS == initVal) {
                                continue;
                            }
                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }
                                double rS = ruleProbs[csi][psi];
                                if (rS == initVal) {
                                    continue;
                                }
                                double thisRound = rS * cIS;
                                scoresToAdd[psi] += thisRound;
                                changeThisRound = true;
                            }
                        }
                        if (!changeThisRound) {
                            continue;
                        }
                        int currentScale = iScale[start][end][cNode];
                        currentScale = ScalingTools.scaleArray(scoresToAdd, currentScale);
                        if (parentScale != currentScale) {
                            if (parentScale == Integer.MIN_VALUE) {
                                parentScale = currentScale;
                            } else {
                                int newScale = Math.max(currentScale, parentScale);
                                ScalingTools.scaleArrayToScale(scoresToAdd, currentScale, newScale);
                                ScalingTools.scaleArrayToScale(iScore[start][end][pNode], parentScale, newScale);
                                parentScale = newScale;
                            }
                        }
                        for (int psi = 0; psi < nParentStates; psi++) {
                            iScore[start][end][pNode][psi] += scoresToAdd[psi];
                        }
                        Arrays.fill(scoresToAdd, initVal);
                        iScale[start][end][pNode] = parentScale;
                    }
                }
            }
        }
    }

    public Tree<String> doViterbiSplitProb(int start, int end) {
        if (end - start < minSplitLen) {
            return new Tree<String>(String.format("(%d_%d_NAN)", start, end), new ArrayList<Tree<String>>());
        }
        double initVal = Double.NEGATIVE_INFINITY;

        double firstSplitProb = Double.NEGATIVE_INFINITY;
        int firstSplit = -1;

        double secondSplitProb = Double.NEGATIVE_INFINITY;
        int secondSplit = -1;

        for (int pNode = 0; pNode < numNodes; pNode++) {
            if (!allowedNodes[start][end][pNode]) {
                continue;
            }
            BinaryRule[] binaryRules = ruleManager.getBinaryRulesWithP(pNode);
            if (binaryRules.length == 0) {
                continue;
            }
            final int nParentStates = numStates[pNode];
            final int numRules = binaryRules.length;
            for (int r = 0; r < numRules; r++) {
                BinaryRule br = binaryRules[r];
                int lNode = br.getLeftChild();
                int rNode = br.getRightChild();
                int narrowR = narrowRExtent[start][lNode];
                boolean iPossibleL = narrowR < end; // can this left constituent leave space for a right constituent?

                if (!iPossibleL) {
                    continue;
                }

                int narrowL = narrowLExtent[end][rNode];
                boolean iPossibleR = narrowL >= narrowR; // can this right constituent fit next to the left constituent?

                if (!iPossibleR) {
                    continue;
                }

                int min1 = narrowR;
                int min2 = wideLExtent[end][rNode];
                int min = min1 > min2 ? min1 : min2; // can this right constituent stretch far enough to reach the left constituent?

                if (min > narrowL) {
                    continue;
                }

                int max1 = wideRExtent[start][lNode];
                int max2 = narrowL;
                final int max = max1 < max2 ? max1 : max2; // can this left constituent stretch far enough to reach the right constituent?

                if (min > max) {
                    continue;
                }
                // TODO switch order of loops for efficiency
                double[][][] probs = br.getRuleProbs();
                final int nLeftChildStates = numStates[lNode];
                final int nRightChildStates = numStates[rNode];
                for (int split = min; split <= max; split++) {
                    if (!allowedNodes[start][split][lNode]) {
                        continue;
                    }
                    if (!allowedNodes[split][end][rNode]) {
                        continue;
                    }

                    for (int lsi = 0; lsi < nLeftChildStates; lsi++) {
                        if (!allowedStates[start][split][lNode][lsi]) {
                            continue;
                        }
                        if (probs[lsi] == null) {
                            continue;
                        }
                        double lIS = iScore[start][split][lNode][lsi];
                        if (lIS == initVal) {
                            continue;
                        }
                        for (int rsi = 0; rsi < nRightChildStates; rsi++) {
                            if (!allowedStates[split][end][rNode][rsi]) {
                                continue;
                            }
                            if (probs[lsi][rsi] == null) {
                                continue;
                            }
                            double rIS = iScore[split][end][rNode][rsi];
                            if (rIS == initVal) {
                                continue;
                            }
                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }

                                double pOS = oScore[start][end][pNode][psi];
                                if (pOS == initVal) {
                                    continue;
                                }

                                double rS = probs[lsi][rsi][psi];
                                if (rS == initVal) {
                                    continue;
                                }

                                double splitProb = pOS + rS + lIS + rIS;
                                if (splitProb > firstSplitProb) {
                                    firstSplitProb = splitProb;
                                    firstSplit = split;
                                }
                                if (splitProb > secondSplitProb && split != firstSplit) {
                                    secondSplitProb = splitProb;
                                    secondSplit = split;
                                }
                            }
                        }
                    }
                }
            }
        }

        double diff = firstSplitProb - secondSplitProb;
        List<Tree<String>> children = new ArrayList<Tree<String>>();
        Tree<String> splitTree = new Tree<String>(String.format("(%d_%d_%d_%.1f)", start, firstSplit, end, diff), children);
        splitTree.setStart(start);
        splitTree.setEnd(end);

//        if (diff > splittingThresholds[level] * splittingThresholdScale) {
        pruneCrossing(start, end, firstSplit);
        children.add(doViterbiSplitProb(start, firstSplit));
        children.add(doViterbiSplitProb(firstSplit, end));
//        }

        return splitTree;
    }

    void doViterbiInsideScores0() {
        double initVal = Double.NEGATIVE_INFINITY;
        for (int diff = 1; diff <= length; diff++) {
            for (int start = 0; start < (length - diff + 1); start++) {
                if (hasExceededTimeLimit()) {
                    return;
                }
                int end = start + diff;
                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (diff == 1) {
                        continue; // there are no binary rules that span over 1 symbol only
                    }
                    if (!phrasalNodes[pNode]) {
                        continue;
                    }
                    if (!allowedNodes[start][end][pNode]) {
                        continue;
                    }
                    double oldIScore = iScore[start][end][pNode][0];
                    double bestIScore = oldIScore;
                    BinaryRule[] binaryRules = ruleManager.getBinaryRulesWithP(pNode);
                    for (int r = 0; r < binaryRules.length; r++) {
                        BinaryRule br = binaryRules[r];
                        int lNode = br.getLeftChild();
                        int rNode = br.getRightChild();

                        int narrowR = narrowRExtent[start][lNode];
                        boolean iPossibleL = narrowR < end; // can this left constituent leave space for a right constituent?

                        if (!iPossibleL) {
                            continue;
                        }

                        int narrowL = narrowLExtent[end][rNode];
                        boolean iPossibleR = narrowL >= narrowR; // can this right constituent fit next to the left constituent?

                        if (!iPossibleR) {
                            continue;
                        }

                        int min1 = narrowR;
                        int min2 = wideLExtent[end][rNode];
                        int min = min1 > min2 ? min1 : min2; // can this right constituent stretch far enough to reach the left constituent?

                        if (min > narrowL) {
                            continue;
                        }

                        int max1 = wideRExtent[start][lNode];
                        int max2 = narrowL;
                        final int max = max1 < max2 ? max1 : max2; // can this left constituent stretch far enough to reach the right constituent?

                        if (min > max) {
                            continue;
                        }
                        // TODO switch order of loops for efficiency
                        double[][][] probs = br.getRuleProbs();
                        double rS = probs[0][0][0];
                        if (rS == initVal) {
                            continue;
                        }
//                        final int nLeftChildStates = numStates[lNode];
//                        final int nRightChildStates = numStates[rNode];
                        for (int split = min; split <= max; split++) {
                            if (!allowedNodes[start][split][lNode]) {
                                continue;
                            }
                            if (!allowedNodes[split][end][rNode]) {
                                continue;
                            }

//                            numLoop1[level]++;
                            double lIS = iScore[start][split][lNode][0];
                            if (lIS == initVal) {
                                continue;
                            }
//                            numLoop2[level]++;
                            double rIS = iScore[split][end][rNode][0];
                            if (rIS == initVal) {
                                continue;
                            }
//                            numLoop3[level]++;
//                            numOperations[level]++;
                            double thisRound = rS + lIS + rIS;
                            if (thisRound > bestIScore) {
                                bestIScore = thisRound;
                            }
                        }
                    }
                    if (bestIScore > oldIScore) {
                        iScore[start][end][pNode][0] = bestIScore;

                        if (start > narrowLExtent[end][pNode]) {
                            narrowLExtent[end][pNode] = start;
                        }
                        if (start < wideLExtent[end][pNode]) {
                            wideLExtent[end][pNode] = start;
                        }
                        if (end < narrowRExtent[start][pNode]) {
                            narrowRExtent[start][pNode] = end;
                        }
                        if (end > wideRExtent[start][pNode]) {
                            wideRExtent[start][pNode] = end;
                        }
                    }
                }
                Arrays.fill(changedAfterUnaries, false);
                boolean somethingChanged = false;
                for (int cNode = 0; cNode < numNodes; cNode++) {
                    if (!allowedNodes[start][end][cNode]) {
                        continue;
                    }
                    double cIS = iScore[start][end][cNode][0];
                    if (cIS == initVal) {
                        continue;
                    }
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithC(cNode);
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int pNode = ur.getParent();
                        if (pNode == cNode) {
                            continue;
                        }
                        if (pNode == 0) {
                            continue;
                        }
                        double[][] ruleProbs = ur.getRuleProbs();
                        double rS = ruleProbs[0][0];
                        double thisRound = cIS + rS;
                        if (!changedAfterUnaries[pNode] && thisRound > iScore[start][end][pNode][0]) {
                            Arrays.fill(scoresAfterUnaries[pNode], initVal);
                            scoresAfterUnaries[pNode][0] = thisRound;
                            changedAfterUnaries[pNode] = true;
                            somethingChanged = true;
                        } else if (thisRound > scoresAfterUnaries[pNode][0]) {
                            scoresAfterUnaries[pNode][0] = thisRound;
                        }
                    }
                }
                if (somethingChanged) {
                    for (int pNode = 0; pNode < numNodes; pNode++) {
                        if (!changedAfterUnaries[pNode]) {
                            continue;
                        }
                        iScore[start][end][pNode][0] = scoresAfterUnaries[pNode][0];
                        if (start > narrowLExtent[end][pNode]) {
                            narrowLExtent[end][pNode] = start;
                        }
                        if (start < wideLExtent[end][pNode]) {
                            wideLExtent[end][pNode] = start;
                        }
                        if (end < narrowRExtent[start][pNode]) {
                            narrowRExtent[start][pNode] = end;
                        }
                        if (end > wideRExtent[start][pNode]) {
                            wideRExtent[start][pNode] = end;
                        }
                    }
                }
                if (start == 0 && diff == length) {
                    int pNode = 0;
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int cNode = ur.getChild();
                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }
                        double[][] ruleProbs = ur.getRuleProbs();
                        double rS = ruleProbs[0][0];
                        double cIS = iScore[start][end][cNode][0];
                        if (rS == initVal || cIS == initVal) {
                            continue;
                        }
                        double thisRound = cIS + rS;
                        if (thisRound > iScore[start][end][pNode][0]) {
                            iScore[start][end][pNode][0] = thisRound;
                        }
                    }
                    if (start > narrowLExtent[end][pNode]) {
                        narrowLExtent[end][pNode] = start;
                    }
                    if (start < wideLExtent[end][pNode]) {
                        wideLExtent[end][pNode] = start;
                    }
                    if (end < narrowRExtent[start][pNode]) {
                        narrowRExtent[start][pNode] = end;
                    }
                    if (end > wideRExtent[start][pNode]) {
                        wideRExtent[start][pNode] = end;
                    }
                }
            }
            pruneInsideChart(diff);
        }
    }

    void doViterbiInsideScores() {
        double initVal = Double.NEGATIVE_INFINITY;
        for (int diff = 1; diff <= length; diff++) {
            for (int start = 0; start < (length - diff + 1); start++) {
                if (hasExceededTimeLimit()) {
                    return;
                }
                int end = start + diff;
                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (diff == 1) {
                        continue; // there are no binary rules that span over 1 symbol only
                    }
                    if (!allowedNodes[start][end][pNode]) {
                        continue;
                    }
                    BinaryRule[] binaryRules = ruleManager.getBinaryRulesWithP(pNode);
                    if (binaryRules.length == 0) {
                        continue;
                    }
                    final int nParentStates = numStates[pNode];
                    boolean somethingChanged = false;
                    final int numRules = binaryRules.length;
                    for (int r = 0; r < numRules; r++) {
                        BinaryRule br = binaryRules[r];
                        int lNode = br.getLeftChild();
                        int rNode = br.getRightChild();
                        int narrowR = narrowRExtent[start][lNode];
                        boolean iPossibleL = narrowR < end; // can this left constituent leave space for a right constituent?

                        if (!iPossibleL) {
                            continue;
                        }

                        int narrowL = narrowLExtent[end][rNode];
                        boolean iPossibleR = narrowL >= narrowR; // can this right constituent fit next to the left constituent?

                        if (!iPossibleR) {
                            continue;
                        }

                        int min1 = narrowR;
                        int min2 = wideLExtent[end][rNode];
                        int min = min1 > min2 ? min1 : min2; // can this right constituent stretch far enough to reach the left constituent?

                        if (min > narrowL) {
                            continue;
                        }

                        int max1 = wideRExtent[start][lNode];
                        int max2 = narrowL;
                        final int max = max1 < max2 ? max1 : max2; // can this left constituent stretch far enough to reach the right constituent?

                        if (min > max) {
                            continue;
                        }
                        // TODO switch order of loops for efficiency
                        double[][][] probs = br.getRuleProbs();
                        final int nLeftChildStates = numStates[lNode];
                        final int nRightChildStates = numStates[rNode];
                        for (int split = min; split <= max; split++) {
                            if (!allowedNodes[start][split][lNode]) {
                                continue;
                            }
                            if (!allowedNodes[split][end][rNode]) {
                                continue;
                            }

                            for (int lsi = 0; lsi < nLeftChildStates; lsi++) {
//                                numLoop1[level]++;
                                if (!allowedStates[start][split][lNode][lsi]) {
                                    continue;
                                }
                                if (probs[lsi] == null) {
                                    continue;
                                }
                                double lIS = iScore[start][split][lNode][lsi];
                                if (lIS == initVal) {
                                    continue;
                                }
                                for (int rsi = 0; rsi < nRightChildStates; rsi++) {
//                                    numLoop2[level]++;
                                    if (!allowedStates[split][end][rNode][rsi]) {
                                        continue;
                                    }
                                    if (probs[lsi][rsi] == null) {
                                        continue;
                                    }
                                    double rIS = iScore[split][end][rNode][rsi];
                                    if (rIS == initVal) {
                                        continue;
                                    }
                                    for (int psi = 0; psi < nParentStates; psi++) {
//                                        numLoop3[level]++;
                                        if (!allowedStates[start][end][pNode][psi]) {
                                            continue;
                                        }
                                        double rS = probs[lsi][rsi][psi];
                                        if (rS == initVal) {
                                            continue;
                                        }
//                                        numOperations[level]++;
                                        double thisRound = rS + lIS + rIS;
                                        iScore[start][end][pNode][psi] = Math.max(thisRound, iScore[start][end][pNode][psi]);
//                                        scoresToAdd[psi] = Math.max(thisRound, scoresToAdd[psi]);
                                        somethingChanged = true;
                                    }
                                }
                            }
                        }
                    }
                    if (somethingChanged) {
                        if (start > narrowLExtent[end][pNode]) {
                            narrowLExtent[end][pNode] = start;
                        }
                        if (start < wideLExtent[end][pNode]) {
                            wideLExtent[end][pNode] = start;
                        }
                        if (end < narrowRExtent[start][pNode]) {
                            narrowRExtent[start][pNode] = end;
                        }
                        if (end > wideRExtent[start][pNode]) {
                            wideRExtent[start][pNode] = end;
                        }
                    }
                }
                Arrays.fill(changedAfterUnaries, false);
                boolean somethingChanged = false;
                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (pNode == 0) {
                        continue;
                    }
                    if (!allowedNodes[start][end][pNode]) {
                        continue;
                    }
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
                    final int nParentStates = numStates[pNode]; //ruleScores[0].length;
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int cNode = ur.getChild();
                        if (pNode == cNode) {
                            continue; // && (np == cp))continue;
                        }
                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }
                        double[][] probs = ur.getRuleProbs();
                        final int nChildStates = numStates[cNode]; //ruleScores[0].length;
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }
                            if (probs[csi] == null) {
                                continue;
                            }
                            double cIS = iScore[start][end][cNode][csi];
                            if (cIS == initVal) {
                                continue;
                            }
                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }
                                double rS = probs[csi][psi];
                                if (rS == initVal) {
                                    continue;
                                }
                                if (!changedAfterUnaries[pNode]) {
                                    Arrays.fill(scoresAfterUnaries[pNode], initVal);
                                    changedAfterUnaries[pNode] = true;
                                }
//                                numOperations[level]++;
                                double thisRound = rS + cIS;
                                scoresAfterUnaries[pNode][psi] = Math.max(thisRound, scoresAfterUnaries[pNode][psi]);
                                somethingChanged = true;
                            }
                        }
                    }
                }
                if (somethingChanged) {
                    for (int pNode = 0; pNode < numNodes; pNode++) {
                        if (!changedAfterUnaries[pNode]) {
                            continue;
                        }
                        final int nParentStates = numStates[pNode];
                        double[] thisCell = scoresAfterUnaries[pNode];
                        for (int psi = 0; psi < nParentStates; psi++) {
                            if (thisCell[psi] > initVal) {
                                iScore[start][end][pNode][psi] = Math.max(iScore[start][end][pNode][psi], thisCell[psi]);
                            }
                        }
                        if (start > narrowLExtent[end][pNode]) {
                            narrowLExtent[end][pNode] = start;
                        }
                        if (start < wideLExtent[end][pNode]) {
                            wideLExtent[end][pNode] = start;
                        }
                        if (end < narrowRExtent[start][pNode]) {
                            narrowRExtent[start][pNode] = end;
                        }
                        if (end > wideRExtent[start][pNode]) {
                            wideRExtent[start][pNode] = end;
                        }
                    }
                }
                if (start == 0 && diff == length) {
                    int pNode = 0;
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
                    final int nParentStates = numStates[pNode];
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int cNode = ur.getChild();
                        if (pNode == cNode) {
                            continue;
                        }
                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }
                        double[][] probs = ur.getRuleProbs();
                        final int nChildStates = numStates[cNode]; //ruleScores[0].length;
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }
                            if (probs[csi] == null) {
                                continue;
                            }
                            double cIS = iScore[start][end][cNode][csi];
                            if (cIS == initVal) {
                                continue;
                            }
                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }
                                double rS = probs[csi][psi];
                                if (rS == initVal) {
                                    continue;
                                }
                                double thisRound = rS + cIS;
                                iScore[start][end][pNode][psi] = Math.max(thisRound, iScore[start][end][pNode][psi]);
                            }
                        }
                    }
                    if (start > narrowLExtent[end][pNode]) {
                        narrowLExtent[end][pNode] = start;
                    }
                    if (start < wideLExtent[end][pNode]) {
                        wideLExtent[end][pNode] = start;
                    }
                    if (end < narrowRExtent[start][pNode]) {
                        narrowRExtent[start][pNode] = end;
                    }
                    if (end > wideRExtent[start][pNode]) {
                        wideRExtent[start][pNode] = end;
                    }
                }
            }
        }
    }

    void doViterbiOutsideScores0() {
        double zeroVal = Double.NEGATIVE_INFINITY;
        oScore[0][length][0][0] = 0;
        for (int diff = length; diff >= 1; diff--) {
            for (int start = 0; start + diff <= length; start++) {
                if (hasExceededTimeLimit()) {
                    return;
                }
                int end = start + diff;
                if (start == 0 && diff == length) {
                    int pNode = 0;
                    double pOS = oScore[start][end][pNode][0];
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int cNode = ur.getChild();

                        if (pNode == cNode) {
                            continue;
                        }

                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }

                        double[][] ruleProbs = ur.getRuleProbs();
                        double rS = ruleProbs[0][0];
                        double thisRound = pOS + rS;
                        if (thisRound > iScore[start][end][cNode][0]) {
                            oScore[start][end][cNode][0] = thisRound;
                        }
                    }
                }
                // do unaries
                Arrays.fill(changedAfterUnaries, false);
                boolean somethingChanged = false;
                for (int cNode = 0; cNode < numNodes; cNode++) {
                    if (!allowedNodes[start][end][cNode]) {
                        continue;
                    }
                    if (diff > 1 && !phrasalNodes[cNode]) {
                        continue;
                    }
                    // the oldOScore is always Double.NEG_INFINITI
                    double oldOScore = oScore[start][end][cNode][0];
                    double bestOScore = oldOScore;

                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithC(cNode);
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int pNode = ur.getParent();

                        if (pNode == cNode || pNode == 0) {
                            continue;
                        }

                        if (!allowedNodes[start][end][pNode]) {
                            continue;
                        }
                        double pOS = oScore[start][end][pNode][0];

                        if (pOS == zeroVal) {
                            continue;
                        }

                        double[][] ruleProbs = ur.getRuleProbs();
                        double rS = ruleProbs[0][0];
                        double thisRound = pOS + rS;
                        if (thisRound > bestOScore) {
                            bestOScore = thisRound;
                        }
                    }
                    if (bestOScore > oldOScore) {
                        Arrays.fill(scoresAfterUnaries[cNode], zeroVal);
                        scoresAfterUnaries[cNode][0] = bestOScore;
                        changedAfterUnaries[cNode] = true;
                        somethingChanged = true;
                    }
                }
                if (somethingChanged) {
                    for (int cNode = 0; cNode < numNodes; cNode++) {
                        if (!changedAfterUnaries[cNode]) {
                            continue;
                        }

                        oScore[start][end][cNode][0] = scoresAfterUnaries[cNode][0];
                    }
                }

                // do binaries
                if (diff == 1) {
                    continue;
                }
                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (!phrasalNodes[pNode]) {
                        continue;
                    }
                    if (!allowedNodes[start][end][pNode]) {
                        continue;
                    }
                    double pOS = oScore[start][end][pNode][0];
                    if (pOS == zeroVal) {
                        continue;
                    }
                    BinaryRule[] binaryRules = ruleManager.getBinaryRulesWithP(pNode);
                    for (int r = 0; r < binaryRules.length; r++) {
                        BinaryRule br = binaryRules[r];
                        int lNode = br.getLeftChild();
                        int rNode = br.getRightChild();

                        int narrowR = narrowRExtent[start][lNode];
                        boolean iPossibleL = narrowR < end; // can this left constituent leave space for a right constituent?
                        if (!iPossibleL) {
                            continue;
                        }

                        int narrowL = narrowLExtent[end][rNode];
                        boolean iPossibleR = narrowL >= narrowR; // can this right constituent fit next to the left constituent?

                        if (!iPossibleR) {
                            continue;
                        }

                        int min1 = narrowR;
                        int min2 = wideLExtent[end][rNode];
                        int min = min1 > min2 ? min1 : min2; // can this right constituent stretch far enough to reach the left constituent?

                        if (min > narrowL) {
                            continue;
                        }

                        int max1 = wideRExtent[start][lNode];
                        int max2 = narrowL;
                        final int max = max1 < max2 ? max1 : max2; // can this left constituent stretch far enough to reach the right constituent?

                        if (min > max) {
                            continue;
                        }

                        double[][][] ruleProbs = br.getRuleProbs();
                        double rS = ruleProbs[0][0][0];
                        for (int split = min; split <= max; split++) {
                            if (!allowedNodes[start][split][lNode]) {
                                continue;
                            }
                            if (!allowedNodes[split][end][rNode]) {
                                continue;
                            }

                            double lIS = iScore[start][split][lNode][0];
                            if (lIS == zeroVal) {
                                continue;
                            }

                            double rIS = iScore[split][end][rNode][0];
                            if (rIS == zeroVal) {
                                continue;
                            }

                            double thisRoundL = pOS + rS + rIS;
                            double thisRoundR = pOS + rS + lIS;
                            if (thisRoundL > oScore[start][split][lNode][0]) {
                                oScore[start][split][lNode][0] = thisRoundL;
                            }
                            if (thisRoundR > oScore[split][end][rNode][0]) {
                                oScore[split][end][rNode][0] = thisRoundR;
                            }
                        }
                    }
                }
            }
            pruneInsideChart(diff);
        }
    }

    void doViterbiOutsideScores() {
        double zeroVal = Double.NEGATIVE_INFINITY;
//        int operationCount = 0;
        oScore[0][length][0][0] = 0;
        for (int diff = length; diff >= 1; diff--) {
            for (int start = 0; start + diff <= length; start++) {
                if (hasExceededTimeLimit()) {
                    return;
                }
                int end = start + diff;
                if (start == 0 && diff == length) {
                    int pNode = 0;
                    final int nParentStates = numStates[pNode];
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int cNode = ur.getChild();
                        if (pNode == cNode) {
                            continue;
                        }
                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }
                        double[][] ruleProbs = ur.getRuleProbs();
                        final int nChildStates = numStates[cNode];
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (ruleProbs[csi] == null) {
                                continue;
                            }
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }
                            for (int psi = 0; psi < nParentStates; psi++) {
                                double rS = ruleProbs[csi][psi];
                                if (rS == zeroVal) {
                                    continue;
                                }
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }
                                double pOS = oScore[start][end][pNode][psi];
                                if (pOS == zeroVal) {
                                    continue;
                                }
                                double thisRound = pOS + rS;
                                oScore[start][end][cNode][csi] = Math.max(thisRound, oScore[start][end][cNode][csi]);
                            }
                        }
                    }
                }

                // do unaries
                Arrays.fill(changedAfterUnaries, false);
                boolean somethingChanged = false;
                for (int cNode = 0; cNode < numNodes; cNode++) {
                    if (diff > 1 && !phrasalNodes[cNode]) {
                        continue;
                    }
                    if (!allowedNodes[start][end][cNode]) {
                        continue;
                    }
                    final int nChildStates = numStates[cNode];
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithC(cNode);
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int pNode = ur.getParent();
                        if (pNode == cNode || pNode == 0) {
                            continue;
                        }
                        if (!allowedNodes[start][end][pNode]) {
                            continue;
                        }
                        double[][] ruleScores = ur.getRuleProbs();
                        final int nParentStates = numStates[pNode];
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (ruleScores[csi] == null) {
                                continue;
                            }
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }
                            for (int psi = 0; psi < nParentStates; psi++) {
                                double rS = ruleScores[csi][psi];
                                if (rS == zeroVal) {
                                    continue;
                                }
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }
                                double pOS = oScore[start][end][pNode][psi];
                                if (pOS == zeroVal) {
                                    continue;
                                }
                                double thisRound = pOS + rS;
//                                numOperations[level]++;
                                if (!changedAfterUnaries[cNode]) {
                                    Arrays.fill(scoresAfterUnaries[cNode], zeroVal);
                                    changedAfterUnaries[cNode] = true;
                                }
                                scoresAfterUnaries[cNode][csi] = Math.max(thisRound, scoresAfterUnaries[cNode][csi]);
                                somethingChanged = true;
                            }
                        }
                    }
                }
                if (somethingChanged) {
                    for (int cNode = 0; cNode < numNodes; cNode++) {
                        if (!changedAfterUnaries[cNode]) {
                            continue;
                        }

                        double[] thisCell = scoresAfterUnaries[cNode];
                        for (int csi = 0; csi < numStates[cNode]; csi++) {
                            if (thisCell[csi] > zeroVal) {
                                oScore[start][end][cNode][csi] = Math.max(oScore[start][end][cNode][csi], thisCell[csi]);
                            }
                        }
                    }
                }

                // do binaries
                if (diff == 1) {
                    continue;
                }

                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (!allowedNodes[start][end][pNode]) {
                        continue;
                    }

                    final int nParentChildStates = numStates[pNode];
                    //if (!allowedStates[start][end][pState]) continue;
                    BinaryRule[] binaryRules = ruleManager.getBinaryRulesWithP(pNode);
                    if (binaryRules.length == 0) {
                        continue;
                    }

                    final int numRules = binaryRules.length;
                    for (int r = 0; r < numRules; r++) {
                        BinaryRule br = binaryRules[r];
                        int lNode = br.getLeftChild();
                        int rNode = br.getRightChild();
                        int narrowR = narrowRExtent[start][lNode];
                        boolean iPossibleL = narrowR < end; // can this left constituent leave space for a right constituent?

                        if (!iPossibleL) {
                            continue;
                        }

                        int narrowL = narrowLExtent[end][rNode];
                        boolean iPossibleR = narrowL >= narrowR; // can this right constituent fit next to the left constituent?

                        if (!iPossibleR) {
                            continue;
                        }

                        int min1 = narrowR;
                        int min2 = wideLExtent[end][rNode];
                        int min = min1 > min2 ? min1 : min2; // can this right constituent stretch far enough to reach the left constituent?

                        if (min > narrowL) {
                            continue;
                        }

                        int max1 = wideRExtent[start][lNode];
                        int max2 = narrowL;
                        final int max = max1 < max2 ? max1 : max2; // can this left constituent stretch far enough to reach the right constituent?

                        if (min > max) {
                            continue;
                        }

                        double[][][] probs = br.getRuleProbs();
                        final int nLeftChildStates = numStates[lNode];
                        final int nRightChildStates = numStates[rNode];
                        for (int split = min; split <= max; split++) {
                            if (!allowedNodes[start][split][lNode]) {
                                continue;
                            }

                            if (!allowedNodes[split][end][rNode]) {
                                continue;
                            }
                            somethingChanged = false;
                            for (int lsi = 0; lsi < nLeftChildStates; lsi++) {
                                if (!allowedStates[start][split][lNode][lsi]) {
                                    continue;
                                }

                                if (probs[lsi] == null) {
                                    continue;
                                }

                                double lIS = iScore[start][split][lNode][lsi];
                                if (lIS == zeroVal) {
                                    continue;
                                }

                                for (int rsi = 0; rsi < nRightChildStates; rsi++) {
                                    if (!allowedStates[split][end][rNode][rsi]) {
                                        continue;
                                    }

                                    if (probs[lsi][rsi] == null) {
                                        continue;
                                    }

                                    double rIS = iScore[split][end][rNode][rsi];
                                    if (rIS == zeroVal) {
                                        continue;
                                    }

                                    for (int psi = 0; psi < nParentChildStates; psi++) {
                                        if (!allowedStates[start][end][pNode][psi]) {
                                            continue;
                                        }

                                        double pOS = oScore[start][end][pNode][psi];
                                        if (pOS == zeroVal) {
                                            continue;
                                        }

                                        double rS = probs[lsi][rsi][psi];
                                        if (rS == zeroVal) {
                                            continue;
                                        }

                                        double thisRoundL = pOS + rS + rIS;
                                        double thisRoundR = pOS + rS + lIS;
                                        oScore[start][split][lNode][lsi] = Math.max(thisRoundL, oScore[start][split][lNode][lsi]);
                                        oScore[split][end][rNode][rsi] = Math.max(thisRoundR, oScore[split][end][rNode][rsi]);
                                        somethingChanged = true;
                                    }
                                }
                            }
                            if (!somethingChanged) {
                                continue;
                            }
                        }
                    }
                }
            }
        }
    }

    void doScaledOutsideScores() {
        double initVal = 0;
        Arrays.fill(scoresToAddL, 0);
        Arrays.fill(scoresToAddR, 0);
        Arrays.fill(scoresToAdd, 0);
        oScore[0][length][0][0] = 1;
        oScale[0][length][0] = 0;
        for (int diff = length; diff >= 1; diff--) {
            for (int start = 0; start + diff <= length; start++) {
                int end = start + diff;
                if (start == 0 && diff == length) {
                    int pNode = 0;
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
                    final int nParentStates = numStates[pNode];
                    final int numRules = unaryRules.length;
                    for (int r = 0; r < numRules; r++) {
                        UnaryRule ur = unaryRules[r];
                        int cNode = ur.getChild();
                        if (cNode == pNode) {
                            continue;
                        }
                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }
                        double[][] probs = ur.getRuleProbs();
                        final int nChildStates = numStates[cNode];
                        int childScale = oScale[start][end][cNode];
                        boolean changeThisRound = false;
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }

                            if (probs[csi] == null) {
                                continue;
                            }

                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }

                                double rS = probs[csi][psi];
                                if (rS == initVal) {
                                    continue;
                                }

                                double pOS = oScore[start][end][pNode][psi];
                                if (pOS == initVal) {
                                    continue;
                                }

                                double thisRound = pOS * rS;
                                scoresToAdd[csi] += thisRound;
                                changeThisRound = true;
                            }

                        }
                        if (!changeThisRound) {
                            continue;
                        }
                        int currentScale = oScale[start][end][pNode];
                        currentScale = ScalingTools.scaleArray(scoresToAdd, currentScale);
                        if (childScale != currentScale) {
                            if (childScale == Integer.MIN_VALUE) {
                                childScale = currentScale;
                            } else {
                                int newScale = Math.max(currentScale, childScale);
                                ScalingTools.scaleArrayToScale(scoresToAdd, currentScale, newScale);
                                ScalingTools.scaleArrayToScale(oScore[start][end][cNode], childScale, newScale);
                                childScale = newScale;
                            }

                        }
                        for (int csi = 0; csi < nChildStates; csi++) {
                            oScore[start][end][cNode][csi] += scoresToAdd[csi];
                        }

                        Arrays.fill(scoresToAdd, initVal);
                        oScale[start][end][cNode] = childScale;
                    }
                }

                Arrays.fill(changedAfterUnaries, false);
                int[] childScales = new int[numNodes];
                Arrays.fill(childScales, Integer.MIN_VALUE);
                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (diff > 1 && !phrasalNodes[pNode]) {
                        continue;
                    }
                    if (pNode == 0) {
                        continue;
                    }
                    if (!allowedNodes[start][end][pNode]) {
                        continue;
                    }

                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
                    if (unaryRules.length == 0) {
                        continue;
                    }

                    final int nParentStates = numStates[pNode];
                    final int numRules = unaryRules.length;
                    for (int r = 0; r < numRules; r++) {
                        UnaryRule ur = unaryRules[r];
                        int cNode = ur.getChild();
                        if (cNode == pNode) {
                            continue; // && (np == cp))continue;
                        }

                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }

                        double[][] probs = ur.getRuleProbs();
                        final int nChildStates = numStates[cNode];
                        int childScale = childScales[cNode];
                        boolean changeThisRound = false;
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }

                            if (probs[csi] == null) {
                                continue;
                            }

                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }

                                double rS = probs[csi][psi];
                                if (rS == initVal) {
                                    continue;
                                }

                                double pOS = oScore[start][end][pNode][psi];
                                if (pOS == initVal) {
                                    continue;
                                }

//                                numOperations[level]++;
                                double thisRound = pOS * rS;
                                scoresToAdd[csi] += thisRound;
                                changeThisRound = true;
                            }

                        }
                        if (!changeThisRound) {
                            continue;
                        }

                        if (!changedAfterUnaries[cNode]) {
                            Arrays.fill(scoresAfterUnaries[cNode], initVal);
                            changedAfterUnaries[cNode] = true;
                        }

                        int currentScale = oScale[start][end][pNode];
                        currentScale = ScalingTools.scaleArray(scoresToAdd, currentScale);
                        if (childScale != currentScale) {
                            if (childScale == Integer.MIN_VALUE) {
                                childScale = currentScale;
                            } else {
                                int newScale = Math.max(currentScale, childScale);
                                ScalingTools.scaleArrayToScale(scoresToAdd, currentScale, newScale);
                                ScalingTools.scaleArrayToScale(scoresAfterUnaries[cNode], childScale, newScale);
                                childScale =
                                        newScale;
                            }

                        }
                        for (int csi = 0; csi < nChildStates; csi++) {
                            scoresAfterUnaries[cNode][csi] += scoresToAdd[csi];
                        }

                        Arrays.fill(scoresToAdd, initVal);
                        childScales[cNode] = childScale;
                    }

                }
                for (int cNode = 0; cNode < numNodes; cNode++) {
                    if (!changedAfterUnaries[cNode]) {
                        continue;
                    }

                    int scaleBeforeUnaries = oScale[start][end][cNode];
                    int childScale = childScales[cNode];
                    if (scaleBeforeUnaries != childScale) {
                        if (childScale == Integer.MIN_VALUE) {
                        } else if (scaleBeforeUnaries == Integer.MIN_VALUE) {
                            oScale[start][end][cNode] = childScale;
                        } else {
                            int newScale = Math.max(scaleBeforeUnaries, childScale);
                            ScalingTools.scaleArrayToScale(oScore[start][end][cNode], scaleBeforeUnaries, newScale);
                            ScalingTools.scaleArrayToScale(scoresAfterUnaries[cNode], childScale, newScale);
                            oScale[start][end][cNode] = newScale;
                        }

                    }
                    // copy/add the entries where the unaries where not useful
                    int nChildStates = numStates[cNode];
                    for (int csi = 0; csi < nChildStates; csi++) {
                        double val = scoresAfterUnaries[cNode][csi];
                        if (val > 0) {
                            oScore[start][end][cNode][csi] += val;
                        }

                    }
                }

//                System.out.println("Do binaries");
                // do binaries
                if (diff == 1) {
                    continue; // there is no space for a binary

                }

                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (!allowedNodes[start][end][pNode]) {
                        continue;
                    }

                    final int nParentChildStates = numStates[pNode];
                    //if (!allowedStates[start][end][pState]) continue;
                    BinaryRule[] binaryRules = ruleManager.getBinaryRulesWithP(pNode);
                    if (binaryRules.length == 0) {
                        continue;
                    }

                    final int numRules = binaryRules.length;
                    for (int r = 0; r < numRules; r++) {
                        BinaryRule br = binaryRules[r];
                        int lNode = br.getLeftChild();
                        int rNode = br.getRightChild();
                        int narrowR = narrowRExtent[start][lNode];
                        boolean iPossibleL = narrowR < end; // can this left constituent leave space for a right constituent?

                        if (!iPossibleL) {
                            continue;
                        }

                        int narrowL = narrowLExtent[end][rNode];
                        boolean iPossibleR = narrowL >= narrowR; // can this right constituent fit next to the left constituent?

                        if (!iPossibleR) {
                            continue;
                        }

                        int min1 = narrowR;
                        int min2 = wideLExtent[end][rNode];
                        int min = min1 > min2 ? min1 : min2; // can this right constituent stretch far enough to reach the left constituent?

                        if (min > narrowL) {
                            continue;
                        }

                        int max1 = wideRExtent[start][lNode];
                        int max2 = narrowL;
                        final int max = max1 < max2 ? max1 : max2; // can this left constituent stretch far enough to reach the right constituent?

                        if (min > max) {
                            continue;
                        }

                        double[][][] probs = br.getRuleProbs();
                        final int nLeftChildStates = numStates[lNode];
                        final int nRightChildStates = numStates[rNode];
                        for (int split = min; split <= max; split++) {
                            if (!allowedNodes[start][split][lNode]) {
                                continue;
                            }

                            if (!allowedNodes[split][end][rNode]) {
                                continue;
                            }

                            boolean changedThisRound = false;
                            for (int lsi = 0; lsi < nLeftChildStates; lsi++) {
                                if (!allowedStates[start][split][lNode][lsi]) {
                                    continue;
                                }

                                if (probs[lsi] == null) {
                                    continue;
                                }

                                double lIS = iScore[start][split][lNode][lsi];
                                if (lIS == initVal) {
                                    continue;
                                }

                                for (int rsi = 0; rsi < nRightChildStates; rsi++) {
                                    if (!allowedStates[split][end][rNode][rsi]) {
                                        continue;
                                    }

                                    if (probs[lsi][rsi] == null) {
                                        continue;
                                    }

                                    double rIS = iScore[split][end][rNode][rsi];
                                    if (rIS == initVal) {
                                        continue;
                                    }

                                    for (int psi = 0; psi < nParentChildStates; psi++) {
                                        if (!allowedStates[start][end][pNode][psi]) {
                                            continue;
                                        }

                                        double rS = probs[lsi][rsi][psi];
                                        if (rS == initVal) {
                                            continue;
                                        }

                                        double pOS = oScore[start][end][pNode][psi];
                                        if (pOS == initVal) {
                                            continue;
                                        }

//                                        numOperations[level]++;
                                        double thisRoundL = pOS * rS * rIS;
                                        double thisRoundR = pOS * rS * lIS;
                                        scoresToAddL[lsi] += thisRoundL;
                                        scoresToAddR[rsi] += thisRoundR;
                                        changedThisRound = true;
                                    }

                                }
                            }
                            if (!changedThisRound) {
                                continue;
                            }

                            if (ArrayMath.max(scoresToAddL) != 0) {
                                //oScale[start][end][pState]!=Integer.MIN_VALUE && iScale[split][end][rState]!=Integer.MIN_VALUE){
                                int leftScale = oScale[start][split][lNode];
                                int currentScale = oScale[start][end][pNode] + iScale[split][end][rNode];
                                currentScale =
                                        ScalingTools.scaleArray(scoresToAddL, currentScale);
                                if (leftScale != currentScale) {
                                    if (leftScale == Integer.MIN_VALUE) {
                                        // first time to build this span
                                        oScale[start][split][lNode] = currentScale;
                                    } else {
                                        int newScale = Math.max(currentScale, leftScale);
                                        ScalingTools.scaleArrayToScale(scoresToAddL, currentScale, newScale);
                                        ScalingTools.scaleArrayToScale(oScore[start][split][lNode], leftScale, newScale);
                                        oScale[start][split][lNode] = newScale;
                                    }

                                }
                                for (int csi = 0; csi < nLeftChildStates; csi++) {
                                    if (scoresToAddL[csi] > initVal) {
                                        oScore[start][split][lNode][csi] += scoresToAddL[csi];
                                    }

                                }
                                Arrays.fill(scoresToAddL, 0);
                            }

                            if (ArrayMath.max(scoresToAddR) != 0) {
                                //oScale[start][end][pState]!=Integer.MIN_VALUE && iScale[start][split][lState]!=Integer.MIN_VALUE){
                                int rightScale = oScale[split][end][rNode];
                                int currentScale = oScale[start][end][pNode] + iScale[start][split][lNode];
                                currentScale =
                                        ScalingTools.scaleArray(scoresToAddR, currentScale);
                                if (rightScale != currentScale) {
                                    if (rightScale == Integer.MIN_VALUE) {
                                        // first time to build this span
                                        oScale[split][end][rNode] = currentScale;
                                    } else {
                                        int newScale = Math.max(currentScale, rightScale);
                                        ScalingTools.scaleArrayToScale(scoresToAddR, currentScale, newScale);
                                        ScalingTools.scaleArrayToScale(oScore[split][end][rNode], rightScale, newScale);
                                        oScale[split][end][rNode] = newScale;
                                    }

                                }
                                for (int csi = 0; csi < nRightChildStates; csi++) {
                                    if (scoresToAddR[csi] > initVal) {
                                        oScore[split][end][rNode][csi] += scoresToAddR[csi];
                                    }

                                }
                                Arrays.fill(scoresToAddR, 0);
                            }

                        }
                    }
                }
            }
        }
    }

    void doNBestMaxScores(List<String> sentence) {
        maxNBestList = new NBestArrayList[length][length + 1][numNodes];
        double zeroVal = Double.NEGATIVE_INFINITY;
        double sentenceNormalizer = iScore[0][length][0][0];
        for (int diff = 1; diff <= length; diff++) {
            for (int start = 0; start < (length - diff + 1); start++) {
                int end = start + diff;
                for (int pNode = 0; pNode < numNodes; pNode++) {
                    maxNBestList[start][end][pNode] = new NBestArrayList(nbestSize, new NBestItem.Comparator());
                }
                // TODO modify the following code to incorporate nbest information.
                if (diff > 1) {
                    // diff > 1: Try binary rules
                    for (int pNode = 0; pNode < numNodes; pNode++) {
                        if (!allowedNodes[start][end][pNode]) {
                            continue;
                        }

                        BinaryRule[] parentRules = ruleManager.getBinaryRulesWithP(pNode);
                        int nParentStates = numStates[pNode];

                        for (int i = 0; i < parentRules.length; i++) {
                            BinaryRule br = parentRules[i];
                            int lNode = br.getLeftChild();
                            int rNode = br.getRightChild();

                            int narrowR = narrowRExtent[start][lNode];
                            boolean iPossibleL = narrowR < end; // can this left constituent leave space for a right constituent?
                            if (!iPossibleL) {
                                continue;
                            }

                            int narrowL = narrowLExtent[end][rNode];
                            boolean iPossibleR = narrowL >= narrowR; // can this right constituent fit next to the left constituent?
                            if (!iPossibleR) {
                                continue;
                            }

                            int min1 = narrowR;
                            int min2 = wideLExtent[end][rNode];
                            int min = min1 > min2 ? min1 : min2; // can this right constituent stretch far enough to reach the left constituent?
                            if (min > narrowL) {
                                continue;
                            }

                            int max1 = wideRExtent[start][lNode];
                            int max2 = narrowL;
                            int max = max1 < max2 ? max1 : max2; // can this left constituent stretch far enough to reach the right constituent?
                            if (min > max) {
                                continue;
                            }
                            // TODO switch order of loops for efficiency
                            double[][][] probs = br.getRuleProbs();
                            int nLeftChildStates = numStates[lNode];
                            int nRightChildStates = numStates[rNode];
                            for (int split = min; split <= max; split++) {
                                NBestArrayList lNBestList = maxNBestList[start][split][lNode];
                                NBestArrayList rNBestList = maxNBestList[split][end][rNode];
                                if (lNBestList.size() == 0 || rNBestList.size() == 0) {
                                    continue;
                                }

                                if (!allowedNodes[start][split][lNode]) {
                                    continue;
                                }
                                if (!allowedNodes[split][end][rNode]) {
                                    continue;
                                }

                                double ruleScore = 0;

                                for (int lsi = 0; lsi < nLeftChildStates; lsi++) {
                                    if (!allowedStates[start][split][lNode][lsi]) {
                                        continue;
                                    }
                                    if (probs[lsi] == null) {
                                        continue;
                                    }
                                    double lIS = iScore[start][split][lNode][lsi];
                                    if (lIS == 0) {
                                        continue;
                                    }
                                    for (int rsi = 0; rsi < nRightChildStates; rsi++) {
                                        if (!allowedStates[split][end][rNode][rsi]) {
                                            continue;
                                        }

                                        if (probs[lsi][rsi] == null) {
                                            continue;
                                        }
                                        double rIS = iScore[split][end][rNode][rsi];
                                        if (rIS == 0) {
                                            continue;
                                        }
                                        for (int psi = 0; psi < nParentStates; psi++) {
                                            if (!allowedStates[start][end][pNode][psi]) {
                                                continue;
                                            }

                                            double rS = probs[lsi][rsi][psi];
                                            if (rS == 0) {
                                                continue;
                                            }
                                            double pOS = oScore[start][end][pNode][psi];
                                            if (pOS == 0) {
                                                continue;
                                            }

                                            ruleScore += (pOS * rS * lIS * rIS) / sentenceNormalizer;
                                        }
                                    }
                                }
                                if (ruleScore == 0) {
                                    continue;
                                }
                                /*
                                 if (doVariational){
                                 double norm = 0;
                                 for (int np = 0; np < nParentStates; np++) {
                                 norm += oScore[start][end][pState][np]/logNormalizer*iScore[start][end][pState][np];
                                 }
                                 ruleScore /= norm;
                                 }
                                 */

                                double scalingFactor = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][split][lNode] + iScale[split][end][rNode] - iScale[0][length][0]);

                                ruleScore = Math.log(ruleScore) + scalingFactor;

                                for (int lib = 0; lib < lNBestList.size(); lib++) {
                                    for (int rib = 0; rib < rNBestList.size(); rib++) {
                                        NBestItem leftChildItem = (NBestItem) lNBestList.get(lib);
                                        NBestItem rightChildItem = (NBestItem) rNBestList.get(rib);
                                        double leftChildScore = leftChildItem.getScore();
                                        double rightChildScore = rightChildItem.getScore();
                                        double gScore = ruleScore + leftChildScore + rightChildScore;
                                        if (gScore == zeroVal) {
                                            continue;
                                        }
                                        maxNBestList[start][end][pNode].add(new NBestItem(gScore, split, -1, -1, lNode, lib, rNode, rib));
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // diff == 1
                    // We treat TAG --> word exactly as if it was a unary rule, except the score of the rule is
                    // given by the lexicon rather than the grammar and that we allow another unary on top of it.
                    //for (int tag : lexicon.getAllTags()){
                    for (int tag = 0; tag < numNodes; tag++) {
                        if (phrasalNodes[tag]) {
                            continue;
                        }
                        if (!allowedNodes[start][end][tag]) {
                            continue;
                        }
                        int nTagStates = numStates[tag];
                        double[] lexiconScoreArray = iScore[start][end][tag];
                        double lexiconScore = 0;
                        for (int tsi = 0; tsi < nTagStates; tsi++) {
                            double oS = oScore[start][end][tag][tsi];
                            double iS = lexiconScoreArray[tsi];
                            lexiconScore += (oS * iS) / sentenceNormalizer; // The inside score of a word is 0.0f
                        }
                        double scalingFactor = ScalingTools.getLogScale(oScale[start][end][tag] - iScale[0][length][0]);
                        lexiconScore = Math.log(lexiconScore) + scalingFactor;
                        if (lexiconScore == zeroVal) {
                            continue;
                        }
                        maxNBestList[start][end][tag].add(new NBestItem(lexiconScore, -1, -1, 0, -1, -1, -1, -1));
                    }
                }
                // Try unary rules
                // Replacement for maxcScore[start][end], which is updated in batch
                NBestArrayList[] maxNBestListStartEnd = new NBestArrayList[numNodes];
                for (int node = 0; node < numNodes; node++) {
                    maxNBestListStartEnd[node] = new NBestArrayList(nbestSize, new NBestItem.Comparator());
                    for (Object item : maxNBestList[start][end][node]) {
                        maxNBestListStartEnd[node].add(item);
                    }
                }
                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (pNode == 0 || !allowedNodes[start][end][pNode]) {
                        continue;
                    }
                    int nParentStates = numStates[pNode];
                    UnaryRule[] unaries = ruleManager.getUnaryRulesWithP(pNode);
                    for (int r = 0; r < unaries.length; r++) {
                        UnaryRule ur = unaries[r];
                        int cNode = ur.getChild();
                        if (pNode == cNode) {
                            continue; // && (np == cp))continue;
                        }
                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }

                        if (maxNBestList[start][end][cNode].size() == 0) {
                            continue;
                        }

                        double[][] probs = ur.getRuleProbs();
                        int nChildStates = numStates[cNode]; // == scores.length;
                        double ruleScore = 0;
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }
                            if (probs[csi] == null) {
                                continue;
                            }
                            double cIS = iScore[start][end][cNode][csi];
                            if (cIS == 0) {
                                continue;
                            }
                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }

                                double rS = probs[csi][psi];
                                if (rS == 0) {
                                    continue;
                                }
                                double pOS = oScore[start][end][pNode][psi];
                                if (pOS == 0) {
                                    continue;
                                }

                                ruleScore += (pOS * rS * cIS) / sentenceNormalizer;
                            }
                        }
                        if (ruleScore == 0) {
                            continue;
                        }
                        double scalingFactor = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][end][cNode] - iScale[0][length][0]);
                        ruleScore = Math.log(ruleScore) + scalingFactor;
                        NBestArrayList childNBest = maxNBestList[start][end][cNode];
                        for (int cnb = 0; cnb < childNBest.size(); cnb++) {
                            NBestItem childItem = (NBestItem) childNBest.get(cnb);
                            if (childItem.getChild() != -1) { // it should never happen because we only do one level of unary rule expansion
                                continue;
                            }
                            double childScore = childItem.getScore();
                            double gScore = ruleScore + childScore;
                            if (gScore == zeroVal) {
                                continue;
                            }
                            maxNBestListStartEnd[pNode].add(new NBestItem(gScore, -1, cNode, cnb, -1, -1, -1, -1));
                        }
                    }
                }
                for (int pNode = 0; pNode < numNodes; pNode++) {
                    for (Object object : maxNBestListStartEnd[pNode]) {
                        NBestItem pItem = (NBestItem) object;
                        int cNode = pItem.getChild();
                        if (cNode == -1) {
                            continue;
                        }
                        int cIbest = pItem.getIbest();
                        NBestItem cItem = (NBestItem) maxNBestList[start][end][cNode].get(cIbest);
                        pItem.setIbest(maxNBestListStartEnd[cNode].indexOf(cItem));
                    }
                }
                maxNBestList[start][end] = maxNBestListStartEnd;

                if (start == 0 && diff == length) {
                    int pNode = 0;
                    int nParentStates = numStates[pNode];
                    UnaryRule[] unaries = ruleManager.getUnaryRulesWithP(pNode);
                    for (int r = 0; r < unaries.length; r++) {
                        UnaryRule ur = unaries[r];
                        int cNode = ur.getChild();
                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }
                        if (maxNBestList[start][end][cNode].size() == 0) {
                            continue;
                        }

                        double[][] scores = ur.getRuleProbs();
                        int nChildStates = numStates[cNode]; // == scores.length;
                        double ruleScore = 0;
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }
                            if (scores[csi] == null) {
                                continue;
                            }
                            double cIS = iScore[start][end][cNode][csi];
                            if (cIS == 0) {
                                continue;
                            }
                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }

                                double rS = scores[csi][psi];
                                if (rS == 0) {
                                    continue;
                                }
                                double pOS = oScore[start][end][pNode][psi];
                                if (pOS == 0) {
                                    continue;
                                }

                                ruleScore += (pOS * rS * cIS) / sentenceNormalizer;
                            }
                        }
                        if (ruleScore == 0) {
                            continue;
                        }
                        double scalingFactor = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][end][cNode] - iScale[0][length][0]);
                        ruleScore = Math.log(ruleScore) + scalingFactor;
                        NBestArrayList childNBest = maxNBestList[start][end][cNode];
                        for (int cnb = 0; cnb < childNBest.size(); cnb++) {
                            NBestItem childItem = (NBestItem) childNBest.get(cnb);
                            if (childItem.getChild() != -1) { // it should never happen because we only do one level of unary rule expansion
                                continue;
                            }
                            double childScore = childItem.getScore();
                            double gScore = ruleScore + childScore;
                            if (gScore == zeroVal) {
                                continue;
                            }
                            maxNBestList[start][end][pNode].add(new NBestItem(gScore, -1, cNode, cnb, -1, -1, -1, -1));
                        }
                    }
                }
            }
        }
    }

    public void doSplittingPostProb(int start, int end) {
        if (end - start < minSplitLen) {
            return;
        }
        double[] splitPostProb = new double[length + 1];
        double sentenceNormalizer = iScore[0][length][0][0];
        for (int pNode = 0; pNode < numNodes; pNode++) {
            if (!allowedNodes[start][end][pNode]) {
                continue;
            }
            BinaryRule[] parentRules = ruleManager.getBinaryRulesWithP(pNode);
            int nParentStates = numStates[pNode]; // == ruleScores[0][0].length;

            for (int r = 0; r < parentRules.length; r++) {
                BinaryRule br = parentRules[r];
                int lNode = br.getLeftChild();
                int rNode = br.getRightChild();

                int narrowR = narrowRExtent[start][lNode];
                boolean iPossibleL = narrowR < end; // can this left constituent leave space for a right constituent?

                if (!iPossibleL) {
                    continue;
                }

                int narrowL = narrowLExtent[end][rNode];
                boolean iPossibleR = narrowL >= narrowR; // can this right constituent fit next to the left constituent?

                if (!iPossibleR) {
                    continue;
                }

                int min1 = narrowR;
                int min2 = wideLExtent[end][rNode];
                int min = min1 > min2 ? min1 : min2; // can this right constituent stretch far enough to reach the left constituent?

                if (min > narrowL) {
                    continue;
                }

                int max1 = wideRExtent[start][lNode];
                int max2 = narrowL;
                int max = max1 < max2 ? max1 : max2; // can this left constituent stretch far enough to reach the right constituent?

                if (min > max) {
                    continue;
                }

                double[][][] probs = br.getRuleProbs();
                int nLeftChildStates = numStates[lNode]; // == ruleScores.length;

                int nRightChildStates = numStates[rNode]; // == ruleScores[0].length;

                for (int split = min; split <= max; split++) {
                    double ruleScore = 0;
                    if (!allowedNodes[start][split][lNode]) {
                        continue;
                    }

                    if (!allowedNodes[split][end][rNode]) {
                        continue;
                    }

                    double scalingFactor = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][split][lNode] + iScale[split][end][rNode] - iScale[0][length][0]);
                    scalingFactor = Math.exp(scalingFactor);
                    for (int lsi = 0; lsi < nLeftChildStates; lsi++) {
                        if (!allowedStates[start][split][lNode][lsi]) {
                            continue;
                        }

                        if (probs[lsi] == null) {
                            continue;
                        }

                        double lIS = iScore[start][split][lNode][lsi];
                        if (lIS == 0) {
                            continue;
                        }

                        for (int rsi = 0; rsi < nRightChildStates; rsi++) {
                            if (!allowedStates[split][end][rNode][rsi]) {
                                continue;
                            }

                            if (probs[lsi][rsi] == null) {
                                continue;
                            }

                            double rIS = iScore[split][end][rNode][rsi];
                            if (rIS == 0) {
                                continue;
                            }

                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }

                                double rS = probs[lsi][rsi][psi];
                                if (rS == 0) {
                                    continue;
                                }

                                double pOS = oScore[start][end][pNode][psi];
                                if (pOS == 0) {
                                    continue;
                                }

                                ruleScore += (pOS * rS * lIS * rIS) / sentenceNormalizer * scalingFactor;
                            }

                        }
                    }
                    if (ruleScore == 0) {
                        continue;
                    }

                    splitPostProb[split] += ruleScore;
                }
            }
        }
        int firstSplit = ArrayMath.argmax(splitPostProb);
        double firstSplitProb = splitPostProb[firstSplit];

        pruneCrossing(start, end, firstSplit);
        doSplittingPostProb(start, firstSplit);
        doSplittingPostProb(firstSplit, end);
    }

    void doMaxScore() {
        maxScore = new double[length][length + 1][numNodes];
        maxSplit = new int[length][length + 1][numNodes];
        maxChild = new int[length][length + 1][numNodes];
        maxLeftChild = new int[length][length + 1][numNodes];
        maxRightChild = new int[length][length + 1][numNodes];
        double initVal = Double.NEGATIVE_INFINITY;
        ArrayUtil.fill(maxScore, initVal);

        double sentenceNormalizer = iScore[0][length][0][0];
        //	    double thresh2 = threshold*logNormalizer;
        for (int diff = 1; diff <= length; diff++) {
            //System.out.print(diff + " ");
            for (int start = 0; start < (length - diff + 1); start++) {
                int end = start + diff;
                Arrays.fill(maxSplit[start][end], -1);
                Arrays.fill(maxChild[start][end], -1);
                Arrays.fill(maxLeftChild[start][end], -1);
                Arrays.fill(maxRightChild[start][end], -1);
                if (diff > 1) {
                    // diff > 1: Try binary rules
                    for (int pNode = 0; pNode < numNodes; pNode++) {
                        if (!allowedNodes[start][end][pNode]) {
                            continue;
                        }

                        BinaryRule[] parentRules = ruleManager.getBinaryRulesWithP(pNode);
                        int nParentStates = numStates[pNode]; // == ruleScores[0][0].length;

                        for (int r = 0; r < parentRules.length; r++) {
                            BinaryRule br = parentRules[r];
                            int lNode = br.getLeftChild();
                            int rNode = br.getRightChild();

                            int narrowR = narrowRExtent[start][lNode];
                            boolean iPossibleL = narrowR < end; // can this left constituent leave space for a right constituent?

                            if (!iPossibleL) {
                                continue;
                            }

                            int narrowL = narrowLExtent[end][rNode];
                            boolean iPossibleR = narrowL >= narrowR; // can this right constituent fit next to the left constituent?

                            if (!iPossibleR) {
                                continue;
                            }

                            int min1 = narrowR;
                            int min2 = wideLExtent[end][rNode];
                            int min = min1 > min2 ? min1 : min2; // can this right constituent stretch far enough to reach the left constituent?

                            if (min > narrowL) {
                                continue;
                            }

                            int max1 = wideRExtent[start][lNode];
                            int max2 = narrowL;
                            int max = max1 < max2 ? max1 : max2; // can this left constituent stretch far enough to reach the right constituent?

                            if (min > max) {
                                continue;
                            }

                            double[][][] probs = br.getRuleProbs();
                            int nLeftChildStates = numStates[lNode]; // == ruleScores.length;

                            int nRightChildStates = numStates[rNode]; // == ruleScores[0].length;

                            double scoreToBeat = maxScore[start][end][pNode];
                            for (int split = min; split <= max; split++) {
                                double ruleScore = 0;
                                if (!allowedNodes[start][split][lNode]) {
                                    continue;
                                }

                                if (!allowedNodes[split][end][rNode]) {
                                    continue;
                                }

                                double leftChildScore = maxScore[start][split][lNode];
                                double rightChildScore = maxScore[split][end][rNode];
                                if (leftChildScore == initVal || rightChildScore == initVal) {
                                    continue;
                                }

                                double scalingFactor = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][split][lNode] + iScale[split][end][rNode] - iScale[0][length][0]);

                                double gScore = leftChildScore + scalingFactor + rightChildScore;

                                if (gScore < scoreToBeat) {
                                    continue; // no chance of finding a better derivation

                                }

                                for (int lsi = 0; lsi < nLeftChildStates; lsi++) {
                                    if (!allowedStates[start][split][lNode][lsi]) {
                                        continue;
                                    }

                                    if (probs[lsi] == null) {
                                        continue;
                                    }

                                    double lIS = iScore[start][split][lNode][lsi];
                                    if (lIS == 0) {
                                        continue;
                                    }

                                    for (int rsi = 0; rsi < nRightChildStates; rsi++) {
                                        if (!allowedStates[split][end][rNode][rsi]) {
                                            continue;
                                        }

                                        if (probs[lsi][rsi] == null) {
                                            continue;
                                        }

                                        double rIS = iScore[split][end][rNode][rsi];
                                        if (rIS == 0) {
                                            continue;
                                        }

                                        for (int psi = 0; psi < nParentStates; psi++) {
                                            if (!allowedStates[start][end][pNode][psi]) {
                                                continue;
                                            }

                                            double rS = probs[lsi][rsi][psi];
                                            if (rS == 0) {
                                                continue;
                                            }

                                            double pOS = oScore[start][end][pNode][psi];
                                            if (pOS == 0) {
                                                continue;
                                            }

                                            ruleScore += (pOS * rS * lIS * rIS) / sentenceNormalizer;
                                        }

                                    }
                                }
                                if (ruleScore == 0) {
                                    continue;
                                }

                                gScore += Math.log(ruleScore);
                                if (gScore > scoreToBeat) {
                                    scoreToBeat = gScore;
                                    maxScore[start][end][pNode] = gScore;
                                    maxSplit[start][end][pNode] = split;
                                    maxLeftChild[start][end][pNode] = lNode;
                                    maxRightChild[start][end][pNode] = rNode;
                                }

                            }
                        }
                    }
                } else {
                    // diff == 1
                    // We treat TAG --> word exactly as if it was a unary rule, except the score of the rule is
                    // given by the lexicon rather than the grammar and that we allow another unary on top of it.
                    //for (int tag : lexicon.getAllTags()){
                    for (int tag = 0; tag < numNodes; tag++) {
                        if (phrasalNodes[tag]) {
                            continue;
                        }

                        if (!allowedNodes[start][end][tag]) {
                            continue;
                        }

                        int nTagStates = numStates[tag];
                        double[] lexiconScoreArray = iScore[start][end][tag];
                        double lexiconScores = 0;
                        for (int tsi = 0; tsi < nTagStates; tsi++) {
                            double oS = oScore[start][end][tag][tsi];
                            double iS = lexiconScoreArray[tsi];
                            lexiconScores +=
                                    (oS * iS) / sentenceNormalizer; // The inside score of a word is 0.0f

                        }

                        double scalingFactor = ScalingTools.getLogScale(oScale[start][end][tag] - iScale[0][length][0]);
                        maxScore[start][end][tag] = Math.log(lexiconScores) + scalingFactor;
                    }

                }
                // Try unary rules
                // Replacement for maxScore[start][end], which is updated in batch
                double[] maxScoreStartEnd = new double[numNodes];
                System.arraycopy(maxScore[start][end], 0, maxScoreStartEnd, 0, numNodes);

                for (int pNode = 0; pNode < numNodes; pNode++) {
                    if (!allowedNodes[start][end][pNode] || pNode == 0) {
                        continue;
                    }
                    int nParentStates = numStates[pNode];
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int cNode = ur.getChild();
                        if (pNode == cNode) {
                            continue; // && (np == cp))continue;

                        }

                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }

                        double childScore = maxScore[start][end][cNode];
                        if (childScore == initVal) {
                            continue;
                        }

                        double scalingFactor = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][end][cNode] - iScale[0][length][0]);
                        double gScore = scalingFactor + childScore;
                        if (gScore < maxScoreStartEnd[pNode]) {
                            continue;
                        }

                        double[][] probs = ur.getRuleProbs();
                        int nChildStates = numStates[cNode]; // == ruleScores.length;

                        double ruleScore = 0;
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }

                            if (probs[csi] == null) {
                                continue;
                            }

                            double cIS = iScore[start][end][cNode][csi];
                            if (cIS == 0) {
                                continue;
                            }

                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }

                                double rS = probs[csi][psi];
                                if (rS == 0) {
                                    continue;
                                }

                                double pOS = oScore[start][end][pNode][psi];
                                if (pOS == 0) {
                                    continue;
                                }

                                ruleScore += (pOS * rS * cIS) / sentenceNormalizer;
                            }

                        }
                        if (ruleScore == 0) {
                            continue;
                        }

                        gScore += Math.log(ruleScore);
                        if (gScore > maxScoreStartEnd[pNode]) {
                            maxScoreStartEnd[pNode] = gScore;
                            maxChild[start][end][pNode] = cNode;
                        }

                    }
                }
                maxScore[start][end] = maxScoreStartEnd;
                if (start == 0 && diff == length) {
                    int pNode = 0;
                    int nParentStates = numStates[pNode];
                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        int cNode = ur.getChild();
                        if (pNode == cNode) {
                            continue; // && (np == cp))continue;
                        }

                        if (!allowedNodes[start][end][cNode]) {
                            continue;
                        }

                        double childScore = maxScore[start][end][cNode];
                        if (childScore == initVal) {
                            continue;
                        }

                        double scalingFactor = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][end][cNode] - iScale[0][length][0]);
                        double gScore = scalingFactor + childScore;
                        if (gScore < maxScoreStartEnd[pNode]) {
                            continue;
                        }

                        double[][] probs = ur.getRuleProbs();
                        int nChildStates = numStates[cNode]; // == ruleScores.length;

                        double ruleScore = 0;
                        for (int csi = 0; csi < nChildStates; csi++) {
                            if (!allowedStates[start][end][cNode][csi]) {
                                continue;
                            }

                            if (probs[csi] == null) {
                                continue;
                            }

                            double cIS = iScore[start][end][cNode][csi];
                            if (cIS == 0) {
                                continue;
                            }

                            for (int psi = 0; psi < nParentStates; psi++) {
                                if (!allowedStates[start][end][pNode][psi]) {
                                    continue;
                                }

                                double rS = probs[csi][psi];
                                if (rS == 0) {
                                    continue;
                                }

                                double pOS = oScore[start][end][pNode][psi];
                                if (pOS == 0) {
                                    continue;
                                }

                                ruleScore += (pOS * rS * cIS) / sentenceNormalizer;
                            }

                        }
                        if (ruleScore == 0) {
                            continue;
                        }

                        gScore += Math.log(ruleScore);
                        if (gScore > maxScore[start][end][pNode]) {
                            maxScore[start][end][pNode] = gScore;
                            maxChild[start][end][pNode] = cNode;
                        }
                    }
                }
            }
        }
    }

    public BinaryRule[] getBinaryRuleArrayWithP(String pNodeName) {
        return ruleManager.getBinaryRulesWithP(nodeMap.get(pNodeName));
    }

    public BinaryRule getBinaryRule(String pNodeName, String lNodeName, String rNodeName) {
        return ruleManager.getBinaryRule(nodeMap.get(pNodeName), nodeMap.get(lNodeName), nodeMap.get(rNodeName));
    }

    public UnaryRule[] getUnaryRuleArrayWithP(String pNodeName) {
        return ruleManager.getUnaryRulesWithP(nodeMap.get(pNodeName));
    }

    public UnaryRule getUnaryRule(String pNodeName, String cNodeName) {
        return ruleManager.getUnaryRule(nodeMap.get(pNodeName), nodeMap.get(cNodeName));
    }

    public String getNodeName(int node) {
        return nodeList.get(node);
    }

    public Pair<Integer, Integer> getMinMax(int start, int end, String lNodeName, String rNodeName) {
        int lNode = nodeMap.get(lNodeName);
        int rNode = nodeMap.get(rNodeName);

        int narrowR = narrowRExtent[start][lNode];
        boolean iPossibleL = narrowR < end; // can this left constituent leave space for a right constituent?

        if (!iPossibleL) {
            return null;
        }

        int narrowL = narrowLExtent[end][rNode];
        boolean iPossibleR = narrowL >= narrowR; // can this right constituent fit next to the left constituent?

        if (!iPossibleR) {
            return null;
        }

        int min1 = narrowR;
        int min2 = wideLExtent[end][rNode];
        int min = min1 > min2 ? min1 : min2; // can this right constituent stretch far enough to reach the left constituent?

        if (min > narrowL) {
            return null;
        }

        int max1 = wideRExtent[start][lNode];
        int max2 = narrowL;
        int max = max1 < max2 ? max1 : max2; // can this left constituent stretch far enough to reach the right constituent?

        if (min > max) {
            return null;
        }
        return new Pair<Integer, Integer>(min, max);
    }

    public double getPostProb(int start, int end, int split, String pNodeName, String lNodeName, String rNodeName) {
        int pNode = nodeMap.get(pNodeName);
        int lNode = nodeMap.get(lNodeName);
        int rNode = nodeMap.get(rNodeName);

        double sentenceNormalizer = iScore[0][length][0][0];
        double scalingFactor = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][split][lNode] + iScale[split][end][rNode] - iScale[0][length][0]);
        double ruleScore = 0;

        if (!allowedNodes[start][split][lNode]) {
            return Double.NEGATIVE_INFINITY;
        }

        if (!allowedNodes[split][end][rNode]) {
            return Double.NEGATIVE_INFINITY;
        }

        int nParentStates = numStates[pNode];
        int nLeftChildStates = numStates[lNode];
        int nRightChildStates = numStates[rNode];

        BinaryRule br = ruleManager.getBinaryRule(pNode, lNode, rNode);
        if (br == null) {
            return Double.NEGATIVE_INFINITY;
        }

        double[][][] probs = br.getRuleProbs();

        for (int lsi = 0; lsi < nLeftChildStates; lsi++) {
            if (!allowedStates[start][split][lNode][lsi]) {
                continue;
            }

            if (probs[lsi] == null) {
                continue;
            }

            double lIS = iScore[start][split][lNode][lsi];
            if (lIS == 0) {
                continue;
            }

            for (int rsi = 0; rsi < nRightChildStates; rsi++) {
                if (!allowedStates[split][end][rNode][rsi]) {
                    continue;
                }

                if (probs[lsi][rsi] == null) {
                    continue;
                }

                double rIS = iScore[split][end][rNode][rsi];
                if (rIS == 0) {
                    continue;
                }

                for (int psi = 0; psi < nParentStates; psi++) {
                    if (!allowedStates[start][end][pNode][psi]) {
                        continue;
                    }

                    double rS = probs[lsi][rsi][psi];
                    if (rS == 0) {
                        continue;
                    }

                    double pOS = oScore[start][end][pNode][psi];
                    if (pOS == 0) {
                        continue;
                    }

                    ruleScore += (pOS * rS * lIS * rIS);
                }

            }
        }
        ruleScore /= sentenceNormalizer;
        if (ruleScore == 0) {
            return Double.NEGATIVE_INFINITY;
        }

        double gScore = Math.log(ruleScore) + scalingFactor;
        return gScore;
    }

    public double getPostProb(int start, int end, String pNodeName, String cNodeName) {
        int pNode = nodeMap.get(pNodeName);
        int cNode = nodeMap.get(cNodeName);

        double sentenceNormalizer = iScore[0][length][0][0];
        double scalingFactor = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][end][cNode] - iScale[0][length][0]);
        double ruleScore = 0;

        int nParentStates = numStates[pNode];
        int nChildStates = numStates[cNode]; // == ruleScores.length;

        UnaryRule ur = ruleManager.getUnaryRule(pNode, cNode);
        if (ur == null) {
            return Double.NEGATIVE_INFINITY;
        }
        double[][] probs = ur.getRuleProbs();

        for (int csi = 0; csi < nChildStates; csi++) {
            if (!allowedStates[start][end][cNode][csi]) {
                continue;
            }

            if (probs[csi] == null) {
                continue;
            }

            double cIS = iScore[start][end][cNode][csi];
            if (cIS == 0) {
                continue;
            }

            for (int psi = 0; psi < nParentStates; psi++) {
                if (!allowedStates[start][end][pNode][psi]) {
                    continue;
                }

                double rS = probs[csi][psi];
                if (rS == 0) {
                    continue;
                }

                double pOS = oScore[start][end][pNode][psi];
                if (pOS == 0) {
                    continue;
                }

                ruleScore += (pOS * rS * cIS);
            }

        }

        ruleScore /= sentenceNormalizer;
        if (ruleScore == 0) {
            return Double.NEGATIVE_INFINITY;
        }

        double gScore = Math.log(ruleScore) + scalingFactor;
        return gScore;
    }

    public double getPostProb(int start, int end, String tagName) {
        int tag = nodeMap.get(tagName);
        int nTagStates = numStates[tag];

        double sentenceNormalizer = iScore[0][length][0][0];
        double[] lexiconScoreArray = iScore[start][end][tag];
        double lexiconScore = 0;
        for (int tsi = 0; tsi < nTagStates; tsi++) {
            double oS = oScore[start][end][tag][tsi];
            double iS = lexiconScoreArray[tsi];
            lexiconScore += (oS * iS) / sentenceNormalizer; // The inside score of a word is 0.0f
        }

        double scalingFactor = ScalingTools.getLogScale(oScale[start][end][tag] - iScale[0][length][0]);
        lexiconScore = Math.log(lexiconScore) + scalingFactor;
        return lexiconScore;
    }

    public boolean isPhrasalNode(String tag) {
        return phrasalNodes[nodeMap.get(tag)];
    }

//    public void addPostRuleProbs(MultiParseDecoder multiParseDecoder) {
//        double initVal = Double.NEGATIVE_INFINITY;
//
//        double sentenceNormalizer = iScore[0][length][0][0];
//        for (int diff = 1; diff <= length; diff++) {
//            for (int start = 0; start < (length - diff + 1); start++) {
//                int end = start + diff;
//                if (diff > 1) {
//                    // diff > 1: Try binary rules
//                    for (int pNode = 0; pNode < numNodes; pNode++) {
//                        if (!allowedNodes[start][end][pNode]) {
//                            continue;
//                        }
//
//                        BinaryRule[] parentRules = ruleManager.getBinaryRulesWithP(pNode);
//                        int nParentStates = numStates[pNode];
//
//                        for (int r = 0; r < parentRules.length; r++) {
//                            BinaryRule br = parentRules[r];
//                            int lNode = br.getLeftChild();
//                            int rNode = br.getRightChild();
//
//                            int narrowR = narrowRExtent[start][lNode];
//                            boolean iPossibleL = narrowR < end;
//
//                            if (!iPossibleL) {
//                                continue;
//                            }
//
//                            int narrowL = narrowLExtent[end][rNode];
//                            boolean iPossibleR = narrowL >= narrowR;
//
//                            if (!iPossibleR) {
//                                continue;
//                            }
//
//                            int min1 = narrowR;
//                            int min2 = wideLExtent[end][rNode];
//                            int min = min1 > min2 ? min1 : min2;
//
//                            if (min > narrowL) {
//                                continue;
//                            }
//
//                            int max1 = wideRExtent[start][lNode];
//                            int max2 = narrowL;
//                            int max = max1 < max2 ? max1 : max2;
//
//                            if (min > max) {
//                                continue;
//                            }
//
//                            double[][][] probs = br.getRuleProbs();
//                            int nLeftChildStates = numStates[lNode];
//                            int nRightChildStates = numStates[rNode];
//
//                            for (int split = min; split <= max; split++) {
//                                double ruleScore = 0;
//                                if (!allowedNodes[start][split][lNode]) {
//                                    continue;
//                                }
//
//                                if (!allowedNodes[split][end][rNode]) {
//                                    continue;
//                                }
//
//                                double scalingFactor = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][split][lNode] + iScale[split][end][rNode] - iScale[0][length][0]);
//
//                                for (int lsi = 0; lsi < nLeftChildStates; lsi++) {
//                                    if (!allowedStates[start][split][lNode][lsi]) {
//                                        continue;
//                                    }
//
//                                    if (probs[lsi] == null) {
//                                        continue;
//                                    }
//
//                                    double lIS = iScore[start][split][lNode][lsi];
//                                    if (lIS == 0) {
//                                        continue;
//                                    }
//
//                                    for (int rsi = 0; rsi < nRightChildStates; rsi++) {
//                                        if (!allowedStates[split][end][rNode][rsi]) {
//                                            continue;
//                                        }
//
//                                        if (probs[lsi][rsi] == null) {
//                                            continue;
//                                        }
//
//                                        double rIS = iScore[split][end][rNode][rsi];
//                                        if (rIS == 0) {
//                                            continue;
//                                        }
//
//                                        for (int psi = 0; psi < nParentStates; psi++) {
//                                            if (!allowedStates[start][end][pNode][psi]) {
//                                                continue;
//                                            }
//
//                                            double rS = probs[lsi][rsi][psi];
//                                            if (rS == 0) {
//                                                continue;
//                                            }
//
//                                            double pOS = oScore[start][end][pNode][psi];
//                                            if (pOS == 0) {
//                                                continue;
//                                            }
//
//                                            ruleScore += (pOS * rS * lIS * rIS) / sentenceNormalizer;
//                                        }
//
//                                    }
//                                }
//                                if (ruleScore == 0) {
//                                    continue;
//                                }
//
//                                double gScore = Math.log(ruleScore) + scalingFactor;
//                                multiParseDecoder.addPostProb(start, end, split,
//                                        nodeList.get(pNode),
//                                        nodeList.get(lNode),
//                                        nodeList.get(rNode),
//                                        gScore);
//                            }
//                        }
//                    }
//                } else {
//                    // diff == 1
//                    for (int tag = 0; tag < numNodes; tag++) {
//                        if (phrasalNodes[tag]) {
//                            continue;
//                        }
//
//                        if (!allowedNodes[start][end][tag]) {
//                            continue;
//                        }
//
//                        int nTagStates = numStates[tag];
//                        double[] lexiconScoreArray = iScore[start][end][tag];
//                        double lexiconScores = 0;
//                        for (int tsi = 0; tsi < nTagStates; tsi++) {
//                            double oS = oScore[start][end][tag][tsi];
//                            double iS = lexiconScoreArray[tsi];
//                            lexiconScores +=
//                                    (oS * iS) / sentenceNormalizer; // The inside score of a word is 0.0f
//
//                        }
//
//                        double scalingFactor = ScalingTools.getLogScale(oScale[start][end][tag] - iScale[0][length][0]);
//                        double gScore = Math.log(lexiconScores) + scalingFactor;
//                        multiParseDecoder.addPostProb(start, nodeList.get(tag), gScore);
//                    }
//
//                }
//                // Try unary rules
//
//                for (int pNode = 0; pNode < numNodes; pNode++) {
//                    if (!allowedNodes[start][end][pNode]) {
//                        continue;
//                    }
//                    int nParentStates = numStates[pNode];
//                    UnaryRule[] unaryRules = ruleManager.getUnaryRulesWithP(pNode);
//                    for (int r = 0; r < unaryRules.length; r++) {
//                        UnaryRule ur = unaryRules[r];
//                        int cNode = ur.getChild();
//                        if (pNode == cNode) {
//                            continue; // && (np == cp))continue;
//                        }
//
//                        if (!allowedNodes[start][end][cNode]) {
//                            continue;
//                        }
//
//                        double scalingFactor = ScalingTools.getLogScale(oScale[start][end][pNode] + iScale[start][end][cNode] - iScale[0][length][0]);
//
//                        double[][] probs = ur.getRuleProbs();
//                        int nChildStates = numStates[cNode]; // == ruleScores.length;
//
//                        double ruleScore = 0;
//                        for (int csi = 0; csi < nChildStates; csi++) {
//                            if (!allowedStates[start][end][cNode][csi]) {
//                                continue;
//                            }
//
//                            if (probs[csi] == null) {
//                                continue;
//                            }
//
//                            double cIS = iScore[start][end][cNode][csi];
//                            if (cIS == 0) {
//                                continue;
//                            }
//
//                            for (int psi = 0; psi < nParentStates; psi++) {
//                                if (!allowedStates[start][end][pNode][psi]) {
//                                    continue;
//                                }
//
//                                double rS = probs[csi][psi];
//                                if (rS == 0) {
//                                    continue;
//                                }
//
//                                double pOS = oScore[start][end][pNode][psi];
//                                if (pOS == 0) {
//                                    continue;
//                                }
//
//                                ruleScore += (pOS * rS * cIS) / sentenceNormalizer;
//                            }
//
//                        }
//                        if (ruleScore == 0) {
//                            continue;
//                        }
//
//                        double gScore = Math.log(ruleScore) + scalingFactor;
//                        int iNode = ur.getIntermediate();
//                        String iNodeName = null;
//                        if (iNode != -1) {
//                            iNodeName = nodeList.get(iNode);
//                        }
//                        multiParseDecoder.addPostProb(start, end,
//                                nodeList.get(pNode),
//                                nodeList.get(cNode),
//                                iNodeName,
//                                gScore);
//                    }
//                }
//            }
//        }
//    }
    protected static boolean matches(double x, double y) {
        return Math.abs(x - y) / (Math.abs(x) + Math.abs(y) + 1e-10) < 1.0E-5;
    }

    public Tree<String> extractBestViterbiParse(int pNode, int pState, int start, int end, List<String> sentence) {
        double zeroVal = Double.NEGATIVE_INFINITY;
        double bestScore = iScore[start][end][pNode][pState];
        String nodeStr = nodeList.get(pNode);
        nodeStr = nodeStr + "-" + pState;
        if (end - start == 1) {
            if (!phrasalNodes[pNode]) {
                List<Tree<String>> child = new ArrayList<Tree<String>>();
                child.add(new Tree<String>(sentence.get(start)));
                return new Tree<String>(nodeStr, child);
            } else {
                UnaryRule[] unaries = ruleManager.getUnaryRulesWithP(pNode);
                for (int ri = 0; ri < unaries.length; ri++) {
                    UnaryRule ur = unaries[ri];
                    int cNode = ur.getChild();
                    if (cNode == pNode) {
                        continue;
                    }
                    if (!allowedNodes[start][end][cNode]) {
                        continue;
                    }
                    double[][] ruleProbs = ur.getRuleProbs();
                    for (int csi = 0; csi < ruleProbs.length; csi++) {
                        if (ruleProbs[csi] == null) {
                            continue;
                        }
                        double newScore = iScore[start][end][cNode][csi] + ruleProbs[csi][pState];
                        if (matches(newScore, bestScore)) {
                            Tree<String> childTree = extractBestViterbiParse(cNode, csi, start, end, sentence);
                            List<Tree<String>> children = new ArrayList<Tree<String>>();
                            children.add(childTree);
                            Tree<String> result = new Tree<String>(nodeStr, children);
                            return result;
                        }
                    }
                }
                throw new RuntimeException("I am not able to find the viterbi parse");
            }
        }

        BinaryRule[] binaries = ruleManager.getBinaryRulesWithP(pNode);
        for (int split = start + 1; split < end; split++) {
            for (int ri = 0; ri < binaries.length; ri++) {
                BinaryRule br = binaries[ri];

                int lNode = br.getLeftChild();
                if (!allowedNodes[start][split][lNode] || iScore[start][split][lNode] == null) {
                    continue;
                }
                int rNode = br.getRightChild();
                if (!allowedNodes[split][end][rNode] || iScore[split][end][rNode] == null) {
                    continue;
                }
                double[][][] ruleProbs = br.getRuleProbs();
                for (int lcsi = 0; lcsi < numStates[lNode]; lcsi++) {
                    if (ruleProbs[lcsi] == null || iScore[start][split][lNode][lcsi] == zeroVal) {
                        continue;
                    }
                    for (int rcsi = 0; rcsi < numStates[rNode]; rcsi++) {
                        if (ruleProbs[lcsi][rcsi] == null || iScore[split][end][rNode][rcsi] == zeroVal) {
                            continue;
                        }
                        double score = ruleProbs[lcsi][rcsi][pState] + iScore[start][split][lNode][lcsi] + iScore[split][end][rNode][rcsi];
                        if (matches(score, bestScore)) {
                            Tree<String> leftChildTree = extractBestViterbiParse(lNode, lcsi, start, split, sentence);
                            Tree<String> rightchildTree = extractBestViterbiParse(rNode, rcsi, split, end, sentence);
                            List<Tree<String>> children = new ArrayList<Tree<String>>();
                            children.add(leftChildTree);
                            children.add(rightchildTree);
                            Tree<String> result = new Tree<String>(nodeStr, children);
                            return result;
                        }
                    }
                }
            }
        }

        UnaryRule[] unaries = ruleManager.getUnaryRulesWithP(pNode);
        for (int ri = 0; ri < unaries.length; ri++) {
            UnaryRule ur = unaries[ri];
            int cNode = ur.getChild();
            if (cNode == pNode) {
                continue;
            }
            if (!allowedNodes[start][end][cNode] || iScore[start][end][cNode] == null) {
                continue;
            }
            double[][] ruleProbs = ur.getRuleProbs();
            for (int csi = 0; csi < ruleProbs.length; csi++) {
                if (ruleProbs[csi] == null) {
                    continue;
                }
                double score = ruleProbs[csi][pState] + iScore[start][end][cNode][csi];
                if (matches(score, bestScore)) {
                    Tree<String> childTree = extractBestViterbiParse(cNode, csi, start, end, sentence);
                    List<Tree<String>> children = new ArrayList<Tree<String>>();
                    children.add(childTree);
                    Tree<String> result = new Tree<String>(nodeStr, children);
                    return result;
                }
            }
        }
        throw new RuntimeException("I cannot find the viterbi parse...");
    }

    /**
     * Returns the best parse, the one with maximum expected labelled recall.
     * Assumes that the maxc* arrays have been filled.
     */
    public Tree<String> extractBestMaxRuleParse(int start, int end, List<String> sentence) {
        return extractBestMaxRuleParse1(start, end, 0, sentence);
    }

    public List<Tree<String>> extractNBestMaxRuleParses(int start, int end, List<String> sentence) {
        List<Tree<String>> nbestParsedTrees = new ArrayList<Tree<String>>();
        NBestArrayList nbestList = maxNBestList[start][end][0];
        for (int ibest = 0; ibest < nbestList.size(); ibest++) {
            nbestParsedTrees.add(extractIBestMaxRuleParse1(start, end, 0, ibest, sentence));
        }
        return nbestParsedTrees;
    }

    public Tree<String> extractIBestMaxRuleParse1(int start, int end, int pNode, int ibest, List<String> sentence) {
        int cNode = ((NBestItem) maxNBestList[start][end][pNode].get(ibest)).getChild();

        if (cNode == -1) {
            return extractIBestMaxRuleParse2(start, end, pNode, ibest, sentence);
        } else {
            int cIBest = ((NBestItem) maxNBestList[start][end][pNode].get(ibest)).getIbest();
            List<Tree<String>> child = new ArrayList<Tree<String>>();
            child.add(extractIBestMaxRuleParse2(start, end, cNode, cIBest, sentence));

            UnaryRule unaryRule = ruleManager.getUnaryRule(pNode, cNode);
            int iNode = unaryRule.getIntermediate();
            if (iNode != -1) {
                String stateStr = nodeList.get(iNode);
                Tree<String> tree = new Tree<String>(stateStr, child);
                child = new ArrayList<Tree<String>>();
                child.add(tree);
            }

            String stateStr = nodeList.get(pNode);
            return new Tree<String>(stateStr, child);
        }
    }

    public Tree<String> extractIBestMaxRuleParse2(int start, int end, int pNode, int ibest, List<String> sentence) {
        List<Tree<String>> children = new ArrayList<Tree<String>>();
        String stateStr = nodeList.get(pNode);

        boolean posLevel = end - start == 1;
        if (posLevel) {
            if (phrasalNodes[pNode]) {
                List<Tree<String>> childs = new ArrayList<Tree<String>>();
                childs.add(new Tree<String>(sentence.get(start)));
                String stateStr2 = nodeList.get(maxChild[start][end][pNode]);
                children.add(new Tree<String>(stateStr2, childs));
            } else {
                children.add(new Tree<String>(sentence.get(start)));
            }

        } else {
            int split = ((NBestItem) maxNBestList[start][end][pNode].get(ibest)).getSplit();
            if (split == -1) {
                System.err.println("Warning: no symbol can generate the span from " + start + " to " + end + ".");
                return null;
            }
            int lNode = ((NBestItem) maxNBestList[start][end][pNode].get(ibest)).getLeftChild();
            int lIbest = ((NBestItem) maxNBestList[start][end][pNode].get(ibest)).getLeftIBest();
            int rNode = ((NBestItem) maxNBestList[start][end][pNode].get(ibest)).getRightChild();
            int rIbest = ((NBestItem) maxNBestList[start][end][pNode].get(ibest)).getRightIBest();
            Tree<String> leftChildTree = extractIBestMaxRuleParse1(start, split, lNode, lIbest, sentence);
            Tree<String> rightChildTree = extractIBestMaxRuleParse1(split, end, rNode, rIbest, sentence);
            children.add(leftChildTree);
            children.add(rightChildTree);
        }

        return new Tree<String>(stateStr, children);
    }

    /**
     * Returns the best parse for state "state", potentially starting with a
     * unary rule
     */
    public Tree<String> extractBestMaxRuleParse1(int start, int end, int node, List<String> sentence) {
        int cNode = maxChild[start][end][node];
        if (cNode == -1) {
            return extractBestMaxRuleParse2(start, end, node, sentence);
        } else {
            List<Tree<String>> children = new ArrayList<Tree<String>>();
            Tree<String> child = extractBestMaxRuleParse1(start, end, cNode, sentence);
            if (child == null) {
                return null;
            }
            children.add(child);

            UnaryRule unaryRule = ruleManager.getUnaryRule(node, cNode);
            int iNode = unaryRule.getIntermediate();
            if (iNode != -1) {

                String stateStr = nodeList.get(iNode);
                Tree<String> tree = new Tree<String>(stateStr, children);
                children = new ArrayList<Tree<String>>();
                children.add(tree);
            }

            String stateStr = nodeList.get(node);
            return new Tree<String>(stateStr, children);
        }

    }

    public Tree<String> extractBestMaxRuleParse2(int start, int end, int node, List<String> sentence) {
        List<Tree<String>> children = new ArrayList<Tree<String>>();
        String stateStr = nodeList.get(node);

        boolean posLevel = end - start == 1;
        if (posLevel) {
            if (phrasalNodes[node]) {
                List<Tree<String>> childs = new ArrayList<Tree<String>>();
                childs.add(new Tree<String>(sentence.get(start)));
                String stateStr2 = nodeList.get(maxChild[start][end][node]);
                children.add(new Tree<String>(stateStr2, childs));
            } else {
                children.add(new Tree<String>(sentence.get(start)));
            }

        } else {
            int split = maxSplit[start][end][node];
            if (split == -1) {
                System.err.println("Warning: no symbol can generate the span from " + start + " to " + end + ".");
                System.err.println("The score is " + maxScore[start][end][node] + " and the state is supposed to be " + stateStr);
                System.err.println("The insideScores are " + Arrays.toString(iScore[start][end][node]) + " and the outsideScores are " + Arrays.toString(oScore[start][end][node]));
                System.err.println("The maxcScore is " + maxScore[start][end][node]);
                return null;
            }

            int lState = maxLeftChild[start][end][node];
            int rState = maxRightChild[start][end][node];
            Tree<String> leftChildTree = extractBestMaxRuleParse1(start, split, lState, sentence);
            Tree<String> rightChildTree = extractBestMaxRuleParse1(split, end, rState, sentence);
            children.add(leftChildTree);
            children.add(rightChildTree);
        }

        return new Tree<String>(stateStr, children);
    }

    public Tree<ViterbiConstituent> stringTreeToConstituentTree(Tree<String> stringTree) {
        if (stringTree.isLeaf()) {
            return new Tree<ViterbiConstituent>(new ViterbiConstituent(stringTree.getLabel()));
        } else {
            String nodeName = stringTree.getLabel();
            int node = nodeMap.get(nodeName);
            Tree<ViterbiConstituent> localViterbiTree = new Tree<ViterbiConstituent>(new ViterbiConstituent(node));
            List<Tree<String>> children = stringTree.getChildren();
            List<Tree<ViterbiConstituent>> viterbiChildren = new ArrayList<Tree<ViterbiConstituent>>();
            for (Tree<String> child : children) {
                viterbiChildren.add(stringTreeToConstituentTree(child));
            }
            localViterbiTree.setChildren(viterbiChildren);
            return localViterbiTree;
        }
    }

    public boolean doViterbiInsideScores(Tree<ViterbiConstituent> tree) {
        double zeroVal = Double.NEGATIVE_INFINITY;
        if (tree.isPreTerminal()) {
            ViterbiConstituent pretermianl = tree.getLabel();
            int tag = pretermianl.getNode();
            ViterbiConstituent terminal = tree.getChildren().get(0).getLabel();
            String word = terminal.getWord();
            double[] lexiconScores = lexiconManager.getProbs(tag, word, true);
            pretermianl.setIScores(lexiconScores);
            return true;
        } else {
            List<Tree<ViterbiConstituent>> children = tree.getChildren();
            for (Tree<ViterbiConstituent> child : children) {
                if (!doViterbiInsideScores(child)) {
                    return false;
                }
            }
            switch (children.size()) {
                case 0:
                    throw new RuntimeException("Malformed tree... " + tree);
                case 1: {
                    ViterbiConstituent parent = tree.getLabel();
                    int pNode = parent.getNode();
                    double[] pIScores = new double[numStates[pNode]];
                    Arrays.fill(pIScores, zeroVal);
                    ViterbiConstituent child = children.get(0).getLabel();
                    int cNode = child.getNode();
                    double[] cIScores = child.getIScores();
                    UnaryRule rule = ruleManager.getUnaryRule(pNode, cNode);
                    if (rule == null) {
                        return false;
                    }
                    double[][] ruleProbs = rule.getRuleProbs();
                    for (int csi = 0; csi < numStates[cNode]; csi++) {
                        if (cIScores[csi] == zeroVal || ruleProbs[csi] == null) {
                            continue;
                        }
                        for (int psi = 0; psi < numStates[pNode]; psi++) {
                            double score = cIScores[csi] + ruleProbs[csi][psi];
                            if (score > pIScores[psi]) {
                                pIScores[psi] = score;
                            }
                        }
                    }
                    parent.setIScores(pIScores);
                    break;
                }
                case 2: {
                    ViterbiConstituent parent = tree.getLabel();
                    int pNode = parent.getNode();
                    double[] pIScores = new double[numStates[pNode]];
                    Arrays.fill(pIScores, zeroVal);
                    ViterbiConstituent leftChild = children.get(0).getLabel();
                    int lNode = leftChild.getNode();
                    double[] lIScores = leftChild.getIScores();
                    ViterbiConstituent rightChild = children.get(1).getLabel();
                    int rNode = rightChild.getNode();
                    double[] rIScores = rightChild.getIScores();
                    BinaryRule rule = ruleManager.getBinaryRule(pNode, lNode, rNode);
                    if (rule == null) {
                        return false;
                    }
                    double[][][] ruleProbs = rule.getRuleProbs();
                    for (int lsi = 0; lsi < numStates[lNode]; lsi++) {
                        if (lIScores[lsi] == zeroVal || ruleProbs[lsi] == null) {
                            continue;
                        }
                        for (int rsi = 0; rsi < numStates[rNode]; rsi++) {
                            if (rIScores[rsi] == zeroVal || ruleProbs[lsi][rsi] == null) {
                                continue;
                            }
                            for (int psi = 0; psi < numStates[pNode]; psi++) {
                                double score = lIScores[lsi] + rIScores[rsi] + ruleProbs[lsi][rsi][psi];
                                if (score > pIScores[psi]) {
                                    pIScores[psi] = score;
                                }
                            }
                        }
                    }
                    parent.setIScores(pIScores);
                    break;
                }
                default:
                    throw new RuntimeException("Malformed tree..." + tree);
            }
            return true;
        }
    }

    public Tree<String> extractViterbiTree(Tree<ViterbiConstituent> tree) {
        if (tree.isLeaf()) {
            String word = tree.getLabel().getWord();
            return new Tree<String>(word);
        } else {
            ViterbiConstituent constituent = tree.getLabel();
            int node = constituent.getNode();
            String tag = nodeList.get(node) + "-" + constituent.getViterbiState();

            List<Tree<ViterbiConstituent>> children = tree.getChildren();
            List<Tree<String>> newChildren = new ArrayList<Tree<String>>();
            switch (children.size()) {
                case 0:
                    throw new RuntimeException("Malformed tree..." + tree);
                case 1: {
                    newChildren.add(extractViterbiTree(children.get(0)));
                    return new Tree<String>(tag, newChildren);
                }
                case 2: {
                    newChildren.add(extractViterbiTree(children.get(0)));
                    newChildren.add(extractViterbiTree(children.get(1)));
                    return new Tree<String>(tag, newChildren);
                }
                default:
                    throw new RuntimeException("Malformed tree..." + tree);
            }
        }
    }

    public void setViterbiStates(Tree<ViterbiConstituent> tree, int viterbiState) {
        double zeroVal = Double.NEGATIVE_INFINITY;
        ViterbiConstituent parent = tree.getLabel();
        int pNode = parent.getNode();
        parent.setViterbiState(viterbiState);

        if (tree.isPreTerminal()) {
            return;
        }

        double pViterbiScore = parent.getIScore(viterbiState);
        List<Tree<ViterbiConstituent>> children = tree.getChildren();
        switch (children.size()) {
            case 0:
                throw new RuntimeException("Incorrent children size...");
            case 1: {
                Tree<ViterbiConstituent> childTree = children.get(0);
                ViterbiConstituent child = childTree.getLabel();
                int cNode = child.getNode();
                int childStateNum = numStates[cNode];
                double[] childIScores = child.getIScores();
                UnaryRule rule = ruleManager.getUnaryRule(pNode, cNode);
                double[][] unaryScores = rule.getRuleProbs();
                int childViterbiState = -1;
                for (int csi = 0; csi < childStateNum; csi++) {
                    if (childIScores[csi] == zeroVal || unaryScores[csi] == null) {
                        continue;
                    }
                    if (childIScores[csi] + unaryScores[csi][viterbiState] == pViterbiScore) {
                        childViterbiState = csi;
                        break;
                    }
                }
                if (childViterbiState == -1) {
                    throw new RuntimeException("I cannot find the viterbi state");
                }
                setViterbiStates(childTree, childViterbiState);
                break;
            }
            case 2: {
                Tree<ViterbiConstituent> leftChildTree = children.get(0);
                ViterbiConstituent leftChild = leftChildTree.getLabel();
                int lNode = leftChild.getNode();
                int leftChildStateNum = numStates[lNode];
                double[] leftChildIScores = leftChild.getIScores();

                Tree<ViterbiConstituent> rightChildTree = children.get(1);
                ViterbiConstituent rightChild = rightChildTree.getLabel();
                int rNode = rightChild.getNode();
                int rightChildStateNum = numStates[rNode];
                double[] rightChildIScores = rightChild.getIScores();

                BinaryRule rule = ruleManager.getBinaryRule(pNode, lNode, rNode);
                double[][][] binaryScores = rule.getRuleProbs();
                int leftChildViterbiState = -1;
                int rightChildViterbiState = -1;
                for (int lcsi = 0; lcsi < leftChildStateNum; lcsi++) {
                    if (leftChildIScores[lcsi] == zeroVal || binaryScores[lcsi] == null) {
                        continue;
                    }
                    for (int rcsi = 0; rcsi < rightChildStateNum; rcsi++) {
                        if (rightChildIScores[rcsi] == zeroVal || binaryScores[lcsi][rcsi] == null) {
                            continue;
                        }
                        if (leftChildIScores[lcsi] + rightChildIScores[rcsi] + binaryScores[lcsi][rcsi][viterbiState] == pViterbiScore) {
                            leftChildViterbiState = lcsi;
                            rightChildViterbiState = rcsi;
                            break;
                        }
                    }
                }
                if (leftChildViterbiState == -1 || rightChildViterbiState == -1) {
                    throw new RuntimeException("I cannot find the viterbi state");
                }
                setViterbiStates(leftChildTree, leftChildViterbiState);
                setViterbiStates(rightChildTree, rightChildViterbiState);
                break;
            }
            default:
                throw new Error("Malformed tree: more than two children");
        }
    }

    public Tree<String> getBestViterbiTree(Tree<String> stringTree) {
        initParsingModel(numLevels - 2);
        viterbiTree = stringTreeToConstituentTree(stringTree);
        if (!doViterbiInsideScores(viterbiTree)) {
            return null;
        }
        if (viterbiTree.getLabel().getIScore(0) == Double.NEGATIVE_INFINITY) {
            return null;
        }
        setViterbiStates(viterbiTree, 0);
        return extractViterbiTree(viterbiTree);
    }

    public double getViterbiScore() {
        double score = viterbiTree.getLabel().getIScore(0);
        if (score == Double.NEGATIVE_INFINITY) {
            return iScore[0][length][0][0];
        } else {
            return score;
        }
    }
}
