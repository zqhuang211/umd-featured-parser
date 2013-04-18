/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.util.BiMap;
import edu.umd.clip.util.Pair;
import edu.umd.clip.util.TriMap;
import edu.umd.clip.util.UniCounter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class RuleManager implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private TriMap<Integer, Integer, Integer, BinaryRule> binaryRuleMap = new TriMap<Integer, Integer, Integer, BinaryRule>();
    private BiMap<Integer, Integer, UnaryRule> unaryRuleMap = new BiMap<Integer, Integer, UnaryRule>();
    private UniCounter<Rule> ruleCounts = new UniCounter<Rule>();
    private Set<BinaryRule> binaryRuleSet = new HashSet<BinaryRule>();
    private Set<UnaryRule> unaryRuleSet = new HashSet<UnaryRule>();
    private BinaryRule[][] binaryRulesWithP;
    private UnaryRule[][] unaryRulesWithP;
    private UnaryRule[][] unaryRulesWithC;
    private boolean smoothingMode = false;
    //
    protected Map<String, Integer> nodeMap;
    protected List<String> nodeList;
    protected int numNodes;
    protected int[] numStates;
    protected boolean[] isPhrasalNode; // True for phrasal nodes, False for POS nodes
    protected double[][][] smoothingMatrix;
    protected int[][] fine2coarseMapping; // map the current state to the coarser state, initialized before coarse-to-fine parsing
    protected double[][] nodeCounts;

    @Override
    public RuleManager clone() {
        RuleManager ruleManager = new RuleManager();
        ruleManager.smoothingMode = smoothingMode;

        ruleManager.nodeMap = nodeMap;
        ruleManager.nodeList = nodeList;
        ruleManager.numNodes = numNodes;
        ruleManager.numStates = numStates;
        ruleManager.isPhrasalNode = isPhrasalNode;
        ruleManager.smoothingMatrix = smoothingMatrix;
        ruleManager.fine2coarseMapping = fine2coarseMapping;
        ruleManager.nodeCounts = nodeCounts;

        for (UnaryRule rule : unaryRuleSet) {
            ruleManager.addUnaryRule(rule.copy(), ruleCounts.getCount(rule));
        }

        for (BinaryRule rule : binaryRuleSet) {
            ruleManager.addBinaryRule(rule.copy(), ruleCounts.getCount(rule));
        }
        return ruleManager;
    }

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

    public BinaryRule[] getBinaryRulesWithP(int pNode) {
        return binaryRulesWithP[pNode];
    }

    public UnaryRule[] getUnaryRulesWithP(int pNode) {
        return unaryRulesWithP[pNode];
    }

    public UnaryRule[] getUnaryRulesWithC(int cNode) {
        return unaryRulesWithC[cNode];
    }

    public void takeLogarithm() {
        for (UnaryRule rule : unaryRuleSet) {
            rule.takeLogarithm(numStates);
        }

        for (BinaryRule rule : binaryRuleSet) {
            rule.takeLogarithm(numStates);
        }
    }

    public void setupArray() {
        unaryRulesWithP = new UnaryRule[numNodes][];
        BiMap<Integer, Integer, UnaryRule> unaryRuleCMap = new BiMap<Integer, Integer, UnaryRule>();
        for (Entry<Integer, HashMap<Integer, UnaryRule>> biEntry : unaryRuleMap.entrySet()) {
            int pNode = biEntry.getKey();
            int ri = 0;
            HashMap<Integer, UnaryRule> ruleMap = biEntry.getValue();
            unaryRulesWithP[pNode] = new UnaryRule[ruleMap.size()];
            for (UnaryRule rule : ruleMap.values()) {
                unaryRulesWithP[pNode][ri++] = rule;
                int cNode = rule.getChild();
                unaryRuleCMap.put(cNode, pNode, rule);
            }
        }

        unaryRulesWithC = new UnaryRule[numNodes][];
        for (Entry<Integer, HashMap<Integer, UnaryRule>> biEntry : unaryRuleCMap.entrySet()) {
            int cNode = biEntry.getKey();
            int ri = 0;
            HashMap<Integer, UnaryRule> ruleMap = biEntry.getValue();
            unaryRulesWithC[cNode] = new UnaryRule[ruleMap.size()];
            for (UnaryRule rule : ruleMap.values()) {
                unaryRulesWithC[cNode][ri++] = rule;
            }
        }

        binaryRulesWithP = new BinaryRule[numNodes][];
        for (Entry<Integer, BiMap<Integer, Integer, BinaryRule>> triEntry : binaryRuleMap.entrySet()) {
            int pNode = triEntry.getKey();
            int ri = 0;
            int rn = 0;
            for (Entry<Integer, HashMap<Integer, BinaryRule>> biEntry : triEntry.getValue().entrySet()) {
                rn += biEntry.getValue().values().size();
            }
            binaryRulesWithP[pNode] = new BinaryRule[rn];
            for (Entry<Integer, HashMap<Integer, BinaryRule>> biEntry : triEntry.getValue().entrySet()) {
                for (BinaryRule rule : biEntry.getValue().values()) {
                    binaryRulesWithP[pNode][ri++] = rule;
                }
            }
        }

        for (int ni = 0; ni < numNodes; ni++) {
            if (unaryRulesWithP[ni] == null) {
                unaryRulesWithP[ni] = new UnaryRule[0];
            }
            if (unaryRulesWithC[ni] == null) {
                unaryRulesWithC[ni] = new UnaryRule[0];
            }
            if (binaryRulesWithP[ni] == null) {
                binaryRulesWithP[ni] = new BinaryRule[0];
            }
        }

        for (UnaryRule rule : unaryRuleSet) {
            rule.setRuleCounts(null);
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.setRuleCounts(null);
        }
    }

    public Set<UnaryRule> getUnaryRuleSet() {
        return unaryRuleSet;
    }

    public Set<BinaryRule> getBinaryRuleSet() {
        return binaryRuleSet;
    }

    public void setSmoothingMode(boolean smoothingMode) {
        this.smoothingMode = smoothingMode;
    }

    public TriMap<Integer, Integer, Integer, BinaryRule> getBinaryRuleMap() {
        return binaryRuleMap;
    }

    public BiMap<Integer, Integer, UnaryRule> getUnaryRuleMap() {
        return unaryRuleMap;
    }

    public void setupCoarseRuleManager(RuleManager fineRuleManager) {
        int[] fineNumStates = fineRuleManager.numStates;
        int[][] ffine2coarseMapping = fineRuleManager.fine2coarseMapping;
        for (BinaryRule rule : fineRuleManager.getBinaryRuleSet()) {
            addBinaryRule(rule.getCoarseRule(numStates, fineNumStates, ffine2coarseMapping), ruleCounts.getCount(rule));
        }

        for (UnaryRule rule : fineRuleManager.getUnaryRuleSet()) {
            addUnaryRule(rule.getCoarseRule(numStates, fineNumStates, ffine2coarseMapping), ruleCounts.getCount(rule));
        }
    }

    public BinaryRule getBinaryRule(int pNode, int lNode, int rNode) {
        BinaryRule rule = binaryRuleMap.get(pNode, lNode, rNode);
        if (rule == null) {
            throw new Error("cannot find rule: " + nodeList.get(pNode) + "->" + nodeList.get(lNode) + " " + nodeList.get(rNode));
        }
        return rule;
    }

    public UnaryRule getUnaryRule(Integer pNode, Integer cNode) {
        UnaryRule rule = unaryRuleMap.get(pNode, cNode);
        if (rule == null) {
            throw new Error("cannot find rule: " + nodeList.get(pNode) + "->" + nodeList.get(cNode));
        }
        return rule;
    }

    public synchronized void addBinaryRule(int pNode, int lNode, int rNode, double weight) {
        BinaryRule rule = binaryRuleMap.get(pNode, lNode, rNode);
        if (rule == null) {
            rule = new BinaryRule(pNode, lNode, rNode);
        }
        addBinaryRule(rule, weight);
    }

    public synchronized void addBinaryRule(BinaryRule rule, double count) {
        if (!ruleCounts.containsKey(rule)) {
            binaryRuleMap.put(rule.getParent(), rule.getLeftChild(), rule.getRightChild(), rule);
            binaryRuleSet.add(rule);
        }
        ruleCounts.incrementCount(rule, count);
    }

    public synchronized void addUnaryRule(int pNode, int cNode, double weight) {
        UnaryRule rule = unaryRuleMap.get(pNode, cNode);
        if (rule == null) {
            rule = new UnaryRule(pNode, cNode);
        }
        addUnaryRule(rule, weight);
    }

    public synchronized void addUnaryRule(UnaryRule rule, double count) {
        if (!ruleCounts.containsKey(rule)) {
            unaryRuleMap.put(rule.getParent(), rule.getChild(), rule);
            unaryRuleSet.add(rule);
        }
        ruleCounts.incrementCount(rule, count);
    }

    public void doMStep() {
        for (UnaryRule rule : unaryRuleSet) {
            rule.normalize(numStates, nodeCounts);
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.normalize(numStates, nodeCounts);
        }
        if (smoothingMode) {
            for (UnaryRule rule : unaryRuleSet) {
                rule.smoothProbs(numStates, smoothingMatrix);
            }
            for (BinaryRule rule : binaryRuleSet) {
                rule.smoothProbs(numStates, smoothingMatrix);
            }
        }
        filterRuleScores();
    }

    public void filterRuleScores() {
        for (UnaryRule rule : unaryRuleSet) {
            rule.filterScores();
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.filterScores();
        }
    }

    public void initFeatureRichWeights(double[] weights, int[] featIndex) {
//        accumParentCounts(false);
        double[][][] latentTagWeightStats = new double[numNodes][][];
//        double[] sum_counts = new double[2];

        for (UnaryRule rule : unaryRuleSet) {
            rule.compFeatureRichWeightSumCount(numStates, latentTagWeightStats);
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.compFeatureRichWeightSumCount(numStates, latentTagWeightStats);
        }
        for (int ni = 0; ni < numNodes; ni++) {
            if (latentTagWeightStats[ni] == null) {
                continue;
            }
            for (int si = 0; si < numStates[ni]; si++) {
                latentTagWeightStats[ni][si][0] /= latentTagWeightStats[ni][si][1];
            }
        }


//        System.out.println("shift: " + mean);
        for (UnaryRule rule : unaryRuleSet) {
            rule.initFeatureRichWeight(numStates, weights, featIndex, latentTagWeightStats);
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.initFeatureRichWeight(numStates, weights, featIndex, latentTagWeightStats);
        }

//        accumParentCounts(true);
//        for (UnaryNodeRule rule : unaryRuleSet) {
//            rule.normalize(true);
//        }
//        for (BinaryNodeRule rule : binaryRuleSet) {
//            rule.normalize(true);
//        }
    }

    public void compFeatureRichProbs(double[] weights, int[] featIndex) {
        double[][] nodeProbSum = new double[numNodes][];
        for (UnaryRule rule : unaryRuleSet) {
            rule.updateFeatureRichProb(numStates, weights, featIndex, nodeProbSum);
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.updateFeatureRichProb(numStates, weights, featIndex, nodeProbSum);
        }
        for (UnaryRule rule : unaryRuleSet) {
            rule.normFeatureRichProb(numStates, nodeProbSum);
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.normFeatureRichProb(numStates, nodeProbSum);
        }
    }

    public void compGraident(double[] gradient, int[] featIndex) {
        for (UnaryRule rule : unaryRuleSet) {
            rule.compGradient(numStates, nodeCounts, gradient, featIndex);
        }

        for (BinaryRule rule : binaryRuleSet) {
            rule.compGradient(numStates, nodeCounts, gradient, featIndex);
        }
    }

    public double compObjective() {
        double[] objectiveArray = new double[1];
        for (UnaryRule rule : unaryRuleSet) {
            rule.compObjective(numStates, objectiveArray);
        }

        for (BinaryRule rule : binaryRuleSet) {
            rule.compObjective(numStates, objectiveArray);
        }
        return objectiveArray[0];
    }

    public void getNumFeatures(int[] featNum) {
        for (UnaryRule rule : unaryRuleSet) {
            rule.compFeatNum(featNum, numStates);
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.compFeatNum(featNum, numStates);
        }
    }

    //TODO revisit this   
//    public void setGlobalPhrasalFeatureWeights(int[] numStates, double[] weights, int[] featIndex) {
//        double[][][] stateWeightStats = new double[numStates.length][][];
//        
//        for (UnaryRule rule : unaryRuleSet) {
//            rule.compFeatureRichWeightSumCount(numStates, stateWeightStats);
//        }
//        for (BinaryRule rule : binaryRuleSet) {
//            rule.compFeatureRichWeightSumCount(numStates, stateWeightStats);
//        }
//        
//        for (double[][] weightStats : stateWeightStats.values()) {
//            for (int i = 0; i < weightStats.length; i++) {
//                weightStats[i][0] /= weightStats[i][1];
//            }
//        }
//
////        System.out.println("shift: " + mean);
//        for (UnaryRule rule : unaryRuleSet) {
//            rule.initFeatureRichWeight(weights, featIndex, stateWeightStats);
//        }
//        for (BinaryRule rule : binaryRuleSet) {
//            rule.initFeatureRichWeight(weights, featIndex, stateWeightStats);
//        }
//        accumParentCounts(true);
//        for (UnaryRule rule : unaryRuleSet) {
//            rule.doMStep(true);
//        }
//        for (BinaryRule rule : binaryRuleSet) {
//            rule.doMStep(true);
//        }
//    }
    public void initParams() {
        for (UnaryRule rule : unaryRuleSet) {
            double[][] counts = new double[1][1];
            counts[0][0] = ruleCounts.getCount(rule);
            rule.setRuleCounts(counts);

            double[][] probs = new double[1][1];
            rule.setRuleProbs(probs);
        }
        for (BinaryRule rule : binaryRuleSet) {
            double[][][] counts = new double[1][1][1];
            counts[0][0][0] = ruleCounts.getCount(rule);
            rule.setRuleCounts(counts);

            double[][][] probs = new double[1][1][1];
            rule.setRuleProbs(probs);
        }
    }

    public void resetCounts() {
        for (UnaryRule rule : unaryRuleSet) {
            rule.setRuleCounts(ArrayMath.initArray(rule.getRuleCounts(), rule.getRuleProbs()));
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.setRuleCounts(ArrayMath.initArray(rule.getRuleCounts(), rule.getRuleProbs()));
        }
    }

    public void printFeatWeights() {
        System.out.println("Printing Feature Weights");
        for (UnaryRule rule : unaryRuleSet) {
            rule.printFeatWeights(numStates, nodeCounts);
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.printFeatWeights(numStates, nodeCounts);
        }
    }

    public double[][] accumEntropyCount() {
        double[][] entropy = new double[numStates.length][];
        for (int ni = 0; ni < numStates.length; ni++) {
            entropy[ni] = new double[numStates[ni]];
        }
        for (UnaryRule rule : unaryRuleSet) {
            rule.tallyNodeEntropy(numStates, entropy);
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.tallyNodeEntropy(numStates, entropy);
        }
        return entropy;
    }

    /**
     *
     * @param onScores
     */
    public void tallyNodeCounts() {
        for (BinaryRule rule : binaryRuleSet) {
            rule.tallyNodeCounts(numStates, nodeCounts);
        }
        for (UnaryRule rule : unaryRuleSet) {
            rule.tallyNodeCounts(numStates, nodeCounts);
        }
    }

    public void splitStates(int[] newNumStates) {
        for (BinaryRule rule : binaryRuleSet) {
            rule.splitStates(numStates, newNumStates);
            rule.filterScores();
        }
        for (UnaryRule rule : unaryRuleSet) {
            rule.splitStates(numStates, newNumStates);
            rule.filterScores();

        }
    }

    public void mergeStates(int[] newNumStates) {
        for (UnaryRule rule : unaryRuleSet) {
            rule.mergeStates(numStates, newNumStates, fine2coarseMapping);
            rule.filterCounts();
        }
        for (BinaryRule rule : binaryRuleSet) {
            rule.mergeStates(numStates, newNumStates, fine2coarseMapping);
            rule.filterCounts();
        }
    }

    public Pair<Long, Long> getRuleNum() {
        long totalRuleNum = 0;
        long zeroRuleNum = 0;
        for (UnaryRule rule : unaryRuleSet) {
            totalRuleNum += numStates[rule.getParent()] * numStates[rule.getChild()];
            zeroRuleNum += rule.getZeroRuleNum(numStates);
        }
        for (BinaryRule rule : binaryRuleSet) {
            totalRuleNum += numStates[rule.getParent()] * numStates[rule.getLeftChild()] * numStates[rule.getRightChild()];
            zeroRuleNum += rule.getZeroRuleNum(numStates);
        }
        return new Pair<Long, Long>(totalRuleNum, zeroRuleNum);
    }

    public int getMatrixSize() {
        int size = 0;
        for (UnaryRule rule : unaryRuleSet) {
            size += rule.matrixSize(numStates);
        }
        for (BinaryRule rule : binaryRuleSet) {
            size += rule.matrixSize(numStates);
        }
        return size;
    }
}
