/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author zqhuang
 */
public class MultiParseDecoder {

    private List<ParseDecoder> decoderList;
//    private Numberer nodeNumberer;
    private int length;
    private int numNodes;
    private double[][][] maxScore;
    private int[][][] maxSplit;
    private int[][][] maxChild;
    private int[][][] maxLeftChild;
    private int[][][] maxRightChild;
    private boolean[][][] allowedNodes;
    private List<String> nodeList;
    private Map<String, Integer> nodeMap;
    private BiMap<String, String, UnaryRule> unaryRuleMap;
    private TriMap<String, String, String, BinaryRule> binaryRuleMap;
    private BiMap<Integer, Integer, UniCounter<UnaryRule>> unaryRulePostMap;
    private BiMap<Integer, Integer, UniCounter<UnaryRule>> unaryRulePost2Map;
    private BiMap<Integer, Integer, UniCounter<UnaryRule>> unaryRuleCountMap;
    private BiMap<Integer, Integer, UniCounter<UnaryRule>> unaryRulePost3Map;
    private TriMap<Integer, Integer, Integer, UniCounter<BinaryRule>> binaryRulePostMap;
    private TriMap<Integer, Integer, Integer, UniCounter<BinaryRule>> binaryRulePost2Map;
    private TriMap<Integer, Integer, Integer, UniCounter<BinaryRule>> binaryRuleCountMap;
    private TriMap<Integer, Integer, Integer, UniCounter<BinaryRule>> binaryRulePost3Map;
    private HashMap<Integer, UniCounter<Integer>> lexicalProb;
    private HashMap<Integer, UniCounter<Integer>> lexicalCount;
    private boolean reportVarCount = false;
    private double minPostProb = -15;
    private ParseDecoder firstDecoder = null;
    private boolean incrementalPruning = true;

    public void setIncrementalPruning(boolean incrementalPruning) {
        this.incrementalPruning = incrementalPruning;
    }

    public void setMinPostProb(double minPostProb) {
        this.minPostProb = minPostProb;
    }

    public void setReportVarCount(boolean reportVarCount) {
        this.reportVarCount = reportVarCount;
    }

    public MultiParseDecoder(List<ParseDecoder> decoderList) {
        this.decoderList = decoderList;
        nodeList = new ArrayList<String>();
        nodeMap = new HashMap<String, Integer>();
        
        for (ParseDecoder decoder : decoderList) {
            decoder.initParsingModel(0);
            List<String> anotherNodeList = decoder.getNodeList();
            
            if (nodeList.isEmpty()) {
                for (int ni = 0; ni < anotherNodeList.size(); ni++) {
                    nodeList.add(anotherNodeList.get(ni));
                    nodeMap.put(anotherNodeList.get(ni), ni);
                }
            } else {
                for (int ni = 0; ni < anotherNodeList.size(); ni++) {
                    if (!nodeMap.containsKey(anotherNodeList.get(ni))) {
                        nodeList.add(anotherNodeList.get(ni));
                        nodeMap.put(anotherNodeList.get(ni), nodeList.size()-1);
                    }
                }
            }
        }
        numNodes = nodeList.size();

        firstDecoder = decoderList.get(0);
    }

    public void updateAllowedNodes(boolean[][][] anotherAllowedNodes, List<String> anotherNodeList) {
        int numAnotherNodes = anotherNodeList.size();
        for (int start = 0; start < length; start++) {
            for (int end = start + 1; end <= length; end++) {
                for (int anotherNode = 0; anotherNode < numAnotherNodes; anotherNode++) {
                    int node = nodeMap.get(anotherNodeList.get(anotherNode));
                    allowedNodes[start][end][node] &= anotherAllowedNodes[start][end][anotherNode];
                }
            }
        }
    }

    public void init() {
        if (reportVarCount) {
            unaryRulePost2Map = new BiMap<Integer, Integer, UniCounter<UnaryRule>>();
            unaryRulePost3Map = new BiMap<Integer, Integer, UniCounter<UnaryRule>>();
            binaryRulePost2Map = new TriMap<Integer, Integer, Integer, UniCounter<BinaryRule>>();
            binaryRulePost3Map = new TriMap<Integer, Integer, Integer, UniCounter<BinaryRule>>();
        }
        unaryRuleMap = new BiMap<String, String, UnaryRule>();
        binaryRuleMap = new TriMap<String, String, String, BinaryRule>();

        unaryRuleCountMap = new BiMap<Integer, Integer, UniCounter<UnaryRule>>();
        unaryRulePostMap = new BiMap<Integer, Integer, UniCounter<UnaryRule>>();

        binaryRuleCountMap = new TriMap<Integer, Integer, Integer, UniCounter<BinaryRule>>();
        binaryRulePostMap = new TriMap<Integer, Integer, Integer, UniCounter<BinaryRule>>();

        lexicalCount = new HashMap<Integer, UniCounter<Integer>>();
        lexicalProb = new HashMap<Integer, UniCounter<Integer>>();
        allowedNodes = new boolean[length][length + 1][numNodes];
        for (int start = 0; start < length; start++) {
            for (int end = start + 1; end <= length; end++) {
                Arrays.fill(allowedNodes[start][end], true);
            }
        }
    }

    public void cleanup() {
        maxScore = null;
        maxSplit = null;
        maxChild = null;
        maxLeftChild = null;
        maxRightChild = null;
        allowedNodes = null;
        unaryRuleCountMap = null;
        unaryRulePostMap = null;
        unaryRulePost2Map = null;
        unaryRulePost3Map = null;
        binaryRuleCountMap = null;
        binaryRulePostMap = null;
        binaryRulePost2Map = null;
        binaryRulePost3Map = null;
        lexicalCount = null;
        lexicalProb = null;
        unaryRuleMap = null;
        binaryRuleMap = null;
    }

    public Tree<String> tryParse(List<String> sentence, int thresholdLevel) {
        for (ParseDecoder decoder : decoderList) {
            switch (thresholdLevel) {
                case 1: {
                    decoder.useLargeThreasholds();
                    break;
                }
                case 2: {
                    decoder.useSmallThreasholds();
                    break;
                }
                case 3: {
                    decoder.useVerySmallThreaholds();
                    break;
                }
                default: {
                    throw new Error("Invalid threshold level: " + thresholdLevel);
                }
            }
        }
        boolean first = true;
        for (ParseDecoder decoder : decoderList) {
            if (first) {
                double ll = decoder.doSingleParse(sentence, true, true, null, null);
//                double ll = decoder.doSingleParse(sentence, true, false, null, null);
                if (Double.isInfinite(ll)) {
                    return null;
                }
                updateAllowedNodes(decoder.getAllowedNodes(), decoder.getNodeList());
                ll = decoder.doSingleParse(sentence, false, false, allowedNodes, nodeMap);
                if (Double.isInfinite(ll)) {
                    return null;
                }
                first = false;
            } else {
                double ll = decoder.doSingleParse(sentence, false, false, allowedNodes, nodeMap);
                if (Double.isInfinite(ll)) {
                    return null;
                }
                if (incrementalPruning) {
                    updateAllowedNodes(decoder.getAllowedNodes(), decoder.getNodeList());
                }
            }
//            decoder.addPostRuleProbs(this);
//            decoder.clearArrays();
        }
//        normPostProb();
        if (!incrementalPruning) {
            for (ParseDecoder decoder : decoderList) {
                updateAllowedNodes(decoder.getAllowedNodes(), decoder.getNodeList());
            }
        }
        doMaxScoreOri();
//        doMaxScore();
        for (ParseDecoder decoder : decoderList) {
            decoder.clearArrays();
        }
        if (Double.isInfinite(maxScore[0][length][0])) {
            return null;
        }
        Tree<String> tree = extractBestMaxRuleParse(0, length, sentence);
        return tree;
    }

    private boolean hasExceededTimeLimit() {
        for (ParseDecoder decoder : decoderList) {
            if (decoder.hasExceededTimeLimit()) {
                return true;
            }
        }
        return false;
    }

    public Tree<String> getBestParse(final List<String> sentence) {
        length = sentence.size();
        Tree<String> tree = null;
        for (int currPruningLevel = 1; currPruningLevel <= 3; currPruningLevel++) {
            init();
            tree = tryParse(sentence, currPruningLevel);
            cleanup();
            if (tree != null) {
                return tree;
            } else if (hasExceededTimeLimit()) {
                System.err.println("Empty tree for the following sentence is returned because one job has exceeded maximum parsing limit...");
                System.err.println(sentence);
                return tree;
            }
        }
        return tree;
    }

    public void normPostProb() {
        int decoderNum = decoderList.size();
        for (Entry<Integer, UniCounter<Integer>> entry : lexicalProb.entrySet()) {
            int pos = entry.getKey();
            UniCounter<Integer> probCounter = entry.getValue();
            UniCounter<Integer> countCounter = lexicalCount.get(pos);
            for (Entry<Integer, Double> probEntry : probCounter.entrySet()) {
                int tag = probEntry.getKey();
                if (countCounter.getCount(tag) < decoderNum) {
                    probEntry.setValue(Double.NEGATIVE_INFINITY);
                }
            }
        }
        for (Entry<Integer, HashMap<Integer, UniCounter<UnaryRule>>> biEntry : unaryRulePostMap.entrySet()) {
            int start = biEntry.getKey();
            HashMap<Integer, UniCounter<UnaryRule>> uniMap = biEntry.getValue();
            for (Entry<Integer, UniCounter<UnaryRule>> uniEntry : uniMap.entrySet()) {
                int end = uniEntry.getKey();
                UniCounter<UnaryRule> probCounter = uniEntry.getValue();
                UniCounter<UnaryRule> countCounter = unaryRuleCountMap.get(start, end);
                for (Entry<UnaryRule, Double> probEntry : probCounter.entrySet()) {
                    UnaryRule rule = probEntry.getKey();
                    if (countCounter.getCount(rule) < decoderNum) {
                        probEntry.setValue(Double.NEGATIVE_INFINITY);
                    }
                }
            }
        }
        for (Entry<Integer, BiMap<Integer, Integer, UniCounter<BinaryRule>>> triEntry : binaryRulePostMap.entrySet()) {
            int start = triEntry.getKey();
            BiMap<Integer, Integer, UniCounter<BinaryRule>> biMap = triEntry.getValue();
            for (Entry<Integer, HashMap<Integer, UniCounter<BinaryRule>>> biEntry : biMap.entrySet()) {
                int end = biEntry.getKey();
                HashMap<Integer, UniCounter<BinaryRule>> uniMap = biEntry.getValue();
                for (Entry<Integer, UniCounter<BinaryRule>> uniEntry : uniMap.entrySet()) {
                    int split = uniEntry.getKey();
                    UniCounter<BinaryRule> probCounter = uniEntry.getValue();
                    UniCounter<BinaryRule> countCounter = binaryRuleCountMap.get(start, end, split);
                    for (Entry<BinaryRule, Double> probEntry : probCounter.entrySet()) {
                        BinaryRule rule = probEntry.getKey();
                        if (countCounter.getCount(rule) < decoderNum) {
                            probEntry.setValue(Double.NEGATIVE_INFINITY);
                        }
                    }
                }
            }
        }
    }

 
    void doMaxScore() {
        numNodes = nodeList.size();
        maxScore = new double[length][length + 1][numNodes];
        maxSplit = new int[length][length + 1][numNodes];
        maxChild = new int[length][length + 1][numNodes];
        maxLeftChild = new int[length][length + 1][numNodes];
        maxRightChild = new int[length][length + 1][numNodes];
        double initVal = Double.NEGATIVE_INFINITY;
        ArrayUtil.fill(maxScore, initVal);
        for (int diff = 1; diff <= length; diff++) {
            for (int start = 0; start < (length - diff + 1); start++) {
                int end = start + diff;
                Arrays.fill(maxSplit[start][end], -1);
                Arrays.fill(maxChild[start][end], -1);
                Arrays.fill(maxLeftChild[start][end], -1);
                Arrays.fill(maxRightChild[start][end], -1);
                if (diff > 1) {
                    for (int split = start + 1; split < end; split++) {
                        UniCounter<BinaryRule> ruleCounter = binaryRulePostMap.get(start, end, split);
                        if (ruleCounter == null) {
                            continue;
                        }
                        for (Entry<BinaryRule, Double> entry : ruleCounter.entrySet()) {
                            BinaryRule rule = entry.getKey();
                            double rScore = entry.getValue();
                            if (Double.isInfinite(rScore)) {
                                continue;
                            }
                            int pNode = rule.getParent();
                            int lNode = rule.getLeftChild();
                            int rNode = rule.getRightChild();
                            if (Double.isInfinite(maxScore[start][split][lNode]) || Double.isInfinite(maxScore[split][end][rNode])) {
                                continue;
                            }
                            double gScore = rScore + maxScore[start][split][lNode] + maxScore[split][end][rNode];
                            if (gScore > maxScore[start][end][pNode]) {
                                maxScore[start][end][pNode] = gScore;
                                maxSplit[start][end][pNode] = split;
                                maxLeftChild[start][end][pNode] = lNode;
                                maxRightChild[start][end][pNode] = rNode;
                            }
                        }
                    }
                } else {
                    // diff == 1
                    UniCounter<Integer> tagCounter = lexicalProb.get(start);
                    if (tagCounter == null) {
                        throw new Error("cannot find a tag...");
                    }
                    for (Entry<Integer, Double> entry : tagCounter.entrySet()) {
                        int tag = entry.getKey();
                        Double score = entry.getValue();
                        if (Double.isInfinite(score)) {
                            continue;
                        }
                        maxScore[start][end][tag] = score;
                    }
                }
                // Try unary rules
                UniCounter<UnaryRule> ruleCounter = unaryRulePostMap.get(start, end);
                if (ruleCounter == null) {
                    continue;
                } // Replacement for maxScore[start][end], which is updated in batch
                double[] maxScoreStartEnd = new double[numNodes];
                System.arraycopy(maxScore[start][end], 0, maxScoreStartEnd, 0, numNodes);
                for (Entry<UnaryRule, Double> entry : ruleCounter.entrySet()) {
                    UnaryRule rule = entry.getKey();
                    double rScore = entry.getValue();
                    if (Double.isInfinite(rScore)) {
                        continue;
                    }
                    int pNode = rule.getParent();
                    int cNode = rule.getChild();
                    if (Double.isInfinite(maxScore[start][end][cNode])) {
                        continue;
                    }
                    double gScore = rScore + maxScore[start][end][cNode];
                    if (gScore > maxScoreStartEnd[pNode]) {
                        maxScoreStartEnd[pNode] = gScore;
                        maxChild[start][end][pNode] = cNode;
                    }
                }
                maxScore[start][end] = maxScoreStartEnd;
                if (start == 0 && diff == length) {
                    for (Entry<UnaryRule, Double> entry : ruleCounter.entrySet()) {
                        UnaryRule rule = entry.getKey();
                        int pNode = rule.getParent();
                        if (pNode != 0) {
                            continue;
                        }
                        double rScore = entry.getValue();
                        if (Double.isInfinite(rScore)) {
                            continue;
                        }
                        int cNode = rule.getChild();
                        if (Double.isInfinite(maxScore[start][end][cNode])) {
                            continue;
                        }
                        double gScore = rScore + maxScore[start][end][cNode];
                        if (gScore > maxScore[start][end][pNode]) {
                            maxScore[start][end][pNode] = gScore;
                            maxChild[start][end][pNode] = cNode;
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the best parse, the one with maximum expected labelled recall.
     * Assumes that the maxc* arrays have been filled.
     */
    public Tree<String> extractBestMaxRuleParse(int start, int end, List<String> sentence) {
        return extractBestMaxRuleParse1(start, end, 0, sentence);
    }

    /**
     * Returns the best parse for state "state", potentially starting with a
     * unary rule
     */
    public Tree<String> extractBestMaxRuleParse1(int start, int end, int node, List<String> sentence) {
        //System.out.println(start+", "+end+";");
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
//            UnaryRule unaryRule = unaryRuleMap.get((String) nodeNumberer.object(node),
//                    (String) nodeNumberer.object(cNode));
            UnaryRule unaryRule = firstDecoder.getUnaryRule(nodeList.get(node), nodeList.get(cNode));
            int intermedNode = unaryRule.getIntermediate();
            if (intermedNode != -1) {
//                String stateStr = (String) nodeNumberer.object(intermedNode);
                String stateStr = firstDecoder.getNodeName(intermedNode);
                Tree<String> tree = new Tree<String>(stateStr, children);
                children = new ArrayList<Tree<String>>();
                children.add(tree);
            }
            String stateStr = (String) nodeList.get(node);
            return new Tree<String>(stateStr, children);
        }
    }

    public boolean isGrammarTag(int node) {
        String label = nodeList.get(node);
        return label.endsWith("^g");
    }

    public Tree<String> extractBestMaxRuleParse2(int start, int end, int node, List<String> sentence) {
        List<Tree<String>> children = new ArrayList<Tree<String>>();
        String stateStr = (String) nodeList.get(node);
        boolean posLevel = end - start == 1;
        if (posLevel) {
            if (isGrammarTag(node)) {
                List<Tree<String>> childs = new ArrayList<Tree<String>>();
                childs.add(new Tree<String>(sentence.get(start)));
                String stateStr2 = (String) nodeList.get(maxChild[start][end][node]);
                children.add(new Tree<String>(stateStr2, childs));
            } else {
                children.add(new Tree<String>(sentence.get(start)));
            }
        } else {
            int split = maxSplit[start][end][node];
            if (split == -1) {
                System.err.println("Error: no symbol can generate the span from " + start + " to " + end + ".");
                System.err.println("The score is " + maxScore[start][end][node] + " and the state is supposed to be " + stateStr);
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

    public void addPostProb(int pos, String tag, double postProb) {
        if (postProb < minPostProb) {
            return;
        }
        UniCounter<Integer> probCounter = lexicalProb.get(pos);
        if (probCounter == null) {
            probCounter = new UniCounter<Integer>();
            lexicalProb.put(pos, probCounter);
        }
        // multiply posterior probabilities
        probCounter.incrementCount(nodeMap.get(tag), postProb);

        // use the largest posterior probability
//        Integer tagId = nodeNumberer.number(tag);
//        if (!probCounter.containsKey(tagId)) {
//            probCounter.setCount(tagId, postProb);
//        } else {
//            if (probCounter.getCount(tagId) > postProb) {
//                probCounter.setCount(tagId, postProb);
//            }
//        }
        // choose the most-likely parser
//        probCounter.setCount(nodeNumberer.number(tag), postProb);


        UniCounter<Integer> countCounter = lexicalCount.get(pos);
        if (countCounter == null) {
            countCounter = new UniCounter<Integer>();
            lexicalCount.put(pos, countCounter);
        }
        countCounter.incrementCount(nodeMap.get(tag), 1);
    }

    public UnaryRule addUnaryRule(String parentName, String childName, String intermediateName) {
        UnaryRule rule = unaryRuleMap.get(parentName, childName);
        if (rule == null) {
            int parent = nodeMap.get(parentName);
            int child = nodeMap.get(childName);
            rule = new UnaryRule(parent, child);
            unaryRuleMap.put(parentName, childName, rule);
            if (intermediateName != null) {
                int intermediate = nodeMap.get(intermediateName);
                rule.setIntermediate(intermediate);
            }
        }
        return rule;
    }

    public void addPostProb(int start, int end,
            String parentName, String childName, String intermediateName, double postProb) {
        if (postProb < minPostProb) {
            return;
        }
        UnaryRule rule = addUnaryRule(parentName, childName, intermediateName);
        UniCounter<UnaryRule> probCounter = unaryRulePostMap.get(start, end);
        if (probCounter == null) {
            probCounter = new UniCounter<UnaryRule>();
            unaryRulePostMap.put(start, end, probCounter);
        }
        probCounter.incrementCount(rule, postProb);

        if (reportVarCount) {
            UniCounter<UnaryRule> prob2Counter = unaryRulePost2Map.get(start, end);
            if (prob2Counter == null) {
                prob2Counter = new UniCounter<UnaryRule>();
                unaryRulePost2Map.put(start, end, prob2Counter);
            }
            prob2Counter.incrementCount(rule, postProb * postProb);
            UniCounter<UnaryRule> prob3Counter = unaryRulePost3Map.get(start, end);
            if (prob3Counter == null) {
                prob3Counter = new UniCounter<UnaryRule>();
                unaryRulePost3Map.put(start, end, prob3Counter);
            }
            prob3Counter.incrementCount(rule, Math.exp(postProb));
        }

//        if (!probCounter.containsKey(rule)) {
//            probCounter.setCount(rule, postProb);
//        } else {
//            if (probCounter.getCount(rule) > postProb) {
//                probCounter.setCount(rule, postProb);
//            }
//        }
        // choose the most-likely parser
//        probCounter.setCount(rule, postProb);

        UniCounter<UnaryRule> countCounter = unaryRuleCountMap.get(start, end);
        if (countCounter == null) {
            countCounter = new UniCounter<UnaryRule>();
            unaryRuleCountMap.put(start, end, countCounter);
        }
        countCounter.incrementCount(rule, 1);
    }

    public BinaryRule addBinaryRule(String parentName, String leftChildName, String rightChildName) {
        BinaryRule rule = binaryRuleMap.get(parentName, leftChildName, rightChildName);
        if (rule == null) {
            int parent = nodeMap.get(parentName);
            int leftChild = nodeMap.get(leftChildName);
            int rightChild = nodeMap.get(rightChildName);
            rule = new BinaryRule(parent, leftChild, rightChild);
            binaryRuleMap.put(parentName, leftChildName, rightChildName, rule);
        }
        return rule;
    }

    public void addPostProb(int start, int end, int split,
            String parentName, String leftChildName, String rightChildName,
            double postProb) {
        if (postProb < minPostProb) {
            return;
        }
        BinaryRule rule = addBinaryRule(parentName, leftChildName, rightChildName);
        UniCounter<BinaryRule> probCounter = binaryRulePostMap.get(start, end, split);
        if (probCounter == null) {
            probCounter = new UniCounter<BinaryRule>();
            binaryRulePostMap.put(start, end, split, probCounter);
        }
        probCounter.incrementCount(rule, postProb);

        if (reportVarCount) {
            UniCounter<BinaryRule> prob2Counter = binaryRulePost2Map.get(start, end, split);
            if (prob2Counter == null) {
                prob2Counter = new UniCounter<BinaryRule>();
                binaryRulePost2Map.put(start, end, split, prob2Counter);
            }
            prob2Counter.incrementCount(rule, postProb * postProb);

            UniCounter<BinaryRule> prob3Counter = binaryRulePost3Map.get(start, end, split);
            if (prob3Counter == null) {
                prob3Counter = new UniCounter<BinaryRule>();
                binaryRulePost3Map.put(start, end, split, prob3Counter);
            }
            prob3Counter.incrementCount(rule, Math.exp(postProb));
        }
        //        if (!probCounter.containsKey(rule)) {
//            probCounter.setCount(rule, postProb);
//        } else {
//            if (probCounter.getCount(rule) > postProb) {
//                probCounter.setCount(rule, postProb);
//            }
//        }
        // choose the most-likely parser
//        probCounter.setCount(rule, postProb);

        UniCounter<BinaryRule> countCounter = binaryRuleCountMap.get(start, end, split);
        if (countCounter == null) {
            countCounter = new UniCounter<BinaryRule>();
            binaryRuleCountMap.put(start, end, split, countCounter);
        }
        countCounter.incrementCount(rule, 1);
    }

    void doMaxScoreOri() {
        maxScore = new double[length][length + 1][numNodes];
        maxSplit = new int[length][length + 1][numNodes];
        maxChild = new int[length][length + 1][numNodes];
        maxLeftChild = new int[length][length + 1][numNodes];
        maxRightChild = new int[length][length + 1][numNodes];
        double initVal = Double.NEGATIVE_INFINITY;
        ArrayUtil.fill(maxScore, initVal);

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
                        String pNodeName = nodeList.get(pNode);
                        BinaryRule[] parentRules = firstDecoder.getBinaryRuleArrayWithP(pNodeName);

                        for (int r = 0; r < parentRules.length; r++) {
                            BinaryRule br = parentRules[r];

                            String lNodeName = firstDecoder.getNodeName(br.getLeftChild());
                            String rNodeName = firstDecoder.getNodeName(br.getRightChild());
                            int lNode = nodeMap.get(lNodeName);
                            int rNode = nodeMap.get(rNodeName);

                            boolean validSpan = true;
                            int min = Integer.MIN_VALUE;
                            int max = Integer.MAX_VALUE;
                            for (ParseDecoder decoder : decoderList) {
                                Pair<Integer, Integer> minMax = decoder.getMinMax(start, end, lNodeName, rNodeName);
                                if (minMax == null) {
                                    validSpan = false;
                                    break;
                                } else {
                                    if (minMax.getFirst() > min) {
                                        min = minMax.getFirst();
                                    }
                                    if (minMax.getSecond() < max) {
                                        max = minMax.getSecond();
                                    }
                                }

                            }
                            if (!validSpan) {
                                continue;
                            }


                            double scoreToBeat = maxScore[start][end][pNode];

                            for (int split = min; split <= max; split++) {
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

                                double gScore = leftChildScore + rightChildScore;

                                if (gScore < scoreToBeat) {
                                    continue; // no chance of finding a better derivation
                                }

                                for (ParseDecoder decoder : decoderList) {
                                    gScore += decoder.getPostProb(start, end, split, pNodeName, lNodeName, rNodeName);
                                    if (gScore < scoreToBeat) {
                                        break;
                                    }
                                }

                                if (gScore < scoreToBeat) {
                                    continue;
                                }

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
                        String tagName = nodeList.get(tag);
                        if (firstDecoder.isPhrasalNode(tagName)) {
                            continue;
                        }

                        if (!allowedNodes[start][end][tag]) {
                            continue;
                        }

                        double lexiconScore = 0;
                        for (ParseDecoder decoder : decoderList) {
                            lexiconScore += decoder.getPostProb(start, end, tagName);
                            if (Double.isInfinite(lexiconScore)) {
                                break;
                            }
                        }

                        if (Double.isInfinite(lexiconScore)) {
                            continue;
                        }
                        maxScore[start][end][tag] = lexiconScore;
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
                    String pNodeName =nodeList.get(pNode);
                    UnaryRule[] unaryRules = firstDecoder.getUnaryRuleArrayWithP(pNodeName);
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        String cNodeName = firstDecoder.getNodeName(ur.getChild());
                        int cNode = nodeMap.get(cNodeName);
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

                        double gScore = childScore;
                        if (gScore < maxScoreStartEnd[pNode]) {
                            continue;
                        }

                        for (ParseDecoder decoder : decoderList) {
                            gScore += decoder.getPostProb(start, end, pNodeName, cNodeName);
                            if (gScore < maxScoreStartEnd[pNode]) {
                                break;
                            }
                        }
                        if (gScore < maxScoreStartEnd[pNode]) {
                            continue;
                        }

                        if (gScore > maxScoreStartEnd[pNode]) {
                            maxScoreStartEnd[pNode] = gScore;
                            maxChild[start][end][pNode] = cNode;
                        }

                    }
                }
                maxScore[start][end] = maxScoreStartEnd;
                if (start == 0 && diff == length) {
                    int pNode = 0;
                    String pNodeName = nodeList.get(pNode);
                    UnaryRule[] unaryRules = firstDecoder.getUnaryRuleArrayWithP(pNodeName);
                    for (int r = 0; r < unaryRules.length; r++) {
                        UnaryRule ur = unaryRules[r];
                        String cNodeName = firstDecoder.getNodeName(ur.getChild());
                        int cNode = nodeMap.get(cNodeName);
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

                        double gScore = childScore;
                        if (gScore < maxScoreStartEnd[pNode]) {
                            continue;
                        }

                        for (ParseDecoder decoder : decoderList) {
                            gScore += decoder.getPostProb(start, end, pNodeName, cNodeName);
                            if (gScore < maxScoreStartEnd[pNode]) {
                                break;
                            }
                        }
                        if (gScore < maxScoreStartEnd[pNode]) {
                            continue;
                        }

                        if (gScore > maxScoreStartEnd[pNode]) {
                            maxScoreStartEnd[pNode] = gScore;
                            maxChild[start][end][pNode] = cNode;
                        }
                    }
                }
            }
        }
    }
}
