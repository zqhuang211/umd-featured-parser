/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.util.GlobalLogger;
import edu.umd.clip.jobs.Job;
import edu.umd.clip.jobs.JobGroup;
import edu.umd.clip.jobs.JobManager;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author zqhuang
 */
public class Grammar implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    public enum GrammarType {

        pre_raw,
        post_raw
    }

    public enum Language {

        english, chinese, arabic, others
    }
    //
    public static final double SCALE = Math.exp(100);
    public static final double ruleFilteringThreshold = 1.0e-30;
    public static final double rareRuleFilteringThreshold = 1.0e-10;
    public static final double rareRuleCountFilteringThreshold = 1.0e-3;
    //
    public static double ruleSmoothingParam = 0.01;
    public static double wordSmoothingParam = 0.1;
    //
    protected Language lang;
    //
    protected RuleManager ruleManager;
    protected LexiconManager lexiconManager;
    protected FeaturedLexiconManager featureRichLexicon;
    //
    protected double logLikelihood = 0;
    protected int manualTrees = Integer.MAX_VALUE;
    protected double autoWeight = 1;
    protected boolean directOptimization = false;
    //
    protected Map<String, Integer> nodeMap;
    protected List<String> nodeList;
    protected int numNodes;
    protected int[] numStates;
    protected boolean[] isPhrasalNode; // True for phrasal nodes, False for POS nodes
    protected double[][][] smoothingMatrix;
    protected Tree<Integer>[] splitTrees;
    protected List<StatePair>[] statePairList;
    protected int[][] fine2coarseMapping; // map the current state to the coarser state, initialized before coarse-to-fine parsing
    protected double[][] nodeCounts;
    
    protected int numSplits = 0;
    //
    protected double mergingRate = 0.5; // The percentage of splits that are merged back.
    protected double reductionRate = 0.1; // Reduce the number of states by this percentage.
    protected boolean useFeatureLexicon = false;

    @Override
    public Grammar clone() {
        Grammar grammar = null;
        try {
            grammar = (Grammar) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Grammar.class.getName()).log(Level.SEVERE, null, ex);
        }
        grammar.ruleManager = grammar.ruleManager.clone();
//        grammar.lexiconManager = grammar.lexiconManager.checkTrainedWithPunc()
        return grammar;
    }

    public Grammar(Language lang, boolean useFeatureLexicon) {
        nodeMap = new HashMap<String, Integer>();
        nodeList = new ArrayList<String>();
        ruleManager = new RuleManager();
        if (useFeatureLexicon) {
            lexiconManager = new FeaturedLexiconManager(lang);
        } else {
            switch (lang) {
                case english: {
                    lexiconManager = new EnglishLexiconManager();
                    break;
                }
                case chinese: {
                    lexiconManager = new ChineseLexiconManager();
                    break;
                }
                case arabic: {
                    lexiconManager = new ArabicLexiconManager();
                    break;
                }
                default: {
                    GlobalLogger.log(Level.SEVERE, "unsupported language: " + lang);
                    System.exit(1);
                }
            }
        }
        this.lang = lang;
        this.useFeatureLexicon = useFeatureLexicon;
    }

    public void setManualTrees(int manualTrees) {
        this.manualTrees = manualTrees;
    }

    public void setAutoWeight(double autoWeight) {
        this.autoWeight = autoWeight;
    }

    public void setMergingRate(double mergingRate) {
        this.mergingRate = mergingRate;
    }

    public void setReductionRate(double reductionRate) {
        this.reductionRate = reductionRate;
    }

    public int getNumSplits() {
        return numSplits;
    }
    
    public double[][] getNodeCounts() {
        return nodeCounts;
    }

    //TODO feature rich
//    public void useFeatureRichLexicon() {
//        featureRichLexicon = new LatentLexicon();
//        featureRichLexicon.setManagers(lexiconManager, ruleManager);
//        lexiconManager.setFeatureRichLexicon(featureRichLexicon);
//    }
//
//    public void setRegWeights(double lexRegWeight, double synRegWeight) {
//        featureRichLexicon.setRegWeights(lexRegWeight, synRegWeight);
//    }
//
//    public void setDirectOptimization(boolean doDirectOptimization) {
//        this.doDirectOptimization = doDirectOptimization;
//        featureRichLexicon.setDirectOptimization(doDirectOptimization);
//    }
//
//    public void loadWordPredicates(String wordPredFile, String oovPredFile) {
//        featureRichLexicon.loadWordPredicates(wordPredFile);
//        // TODO add oov loglinear lexicon
//    }
    public FeaturedLexiconManager getFeatureRichLexicon() {
        return featureRichLexicon;
    }

    //TODO feature rich
//    public void initDirectOptimization() {
//        featureRichLexicon.initDirectOptimization();
//    }
//
//    public void initIndirectOptimization() {
//        featureRichLexicon.initIndirectOptimization();
//    }
//
//    public void clearFeatureRichLexicon() {
//        featureRichLexicon.clear();
//    }
    public void setSmoothingMode(boolean smoothingMode) {
        ruleManager.setSmoothingMode(smoothingMode);
        lexiconManager.setSmoothingMode(smoothingMode);
//        if (featureRichLexicon != null) {
//            featureRichLexicon.setSmoothingMode(smoothingMode);
//        }
    }

    public List<String> getNodeList() {
        return nodeList;
    }

    public Map<String, Integer> getNodeMap() {
        return nodeMap;
    }

    public double[][][] getSmoothingMatrix() {
        return smoothingMatrix;
    }

    public LexiconManager getLexiconManager() {
        return lexiconManager;
    }

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    public double getMergingRate() {
        return mergingRate;
    }

    public double getReductionRate() {
        return reductionRate;
    }

    public boolean[] getIsPhrasalNode() {
        return isPhrasalNode;
    }

    public Tree<Integer>[] getSplitTreeList() {
        return splitTrees;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public int[] getNumStates() {
        return numStates;
    }

    public int[][] getFine2coarseMapping() {
        return fine2coarseMapping;
    }

    public int[] splitStates() {
        int[] newNumStates = new int[numNodes];
        for (int ni = 0; ni < numNodes; ni++) {
            if (ni == 0) {
                newNumStates[ni] = 1;
                List<Tree<Integer>> terminals = splitTrees[ni].getTerminals();
                assert terminals.size() == 1;
                Tree<Integer> terminal = terminals.get(0);
                List<Tree<Integer>> children = new ArrayList<Tree<Integer>>();
                children.add(new Tree<Integer>(0));
                terminal.setChildren(children);
            } else {
                newNumStates[ni] = numStates[ni] * 2;
                for (Tree<Integer> stateTree : splitTrees[ni].getTerminals()) {
                    int si = stateTree.getLabel();
                    List<Tree<Integer>> children = new ArrayList<Tree<Integer>>();
                    children.add(new Tree<Integer>(si * 2));
                    children.add(new Tree<Integer>(si * 2 + 1));
                    stateTree.setChildren(children);
                }
            }
        }
        return newNumStates;
    }

    public void initMerging(boolean mergeSplitOnly) {
        statePairList = new List[numNodes];
        for (int ni = 0; ni < numNodes; ni++) {
            statePairList[ni] = new ArrayList<StatePair>();
            if (ni == 0) {
                continue;
            }
            if (mergeSplitOnly) {
                assert numStates[ni] % 2 == 0;
                for (int si = 0; si < numStates[ni]; si += 2) {
                    statePairList[ni].add(new StatePair(ni, si, si + 1));
                }
            } else {
                for (int s1 = 0; s1 < numStates[ni] - 1; s1++) {
                    for (int s2 = s1 + 1; s2 < numStates[ni]; s2++) {
                        statePairList[ni].add(new StatePair(ni, s1, s2));
                    }
                }
            }
        }
    }

    public int[] mergeStates() {
        int[] newNumStates = new int[numNodes];
        List<StatePair> allStatePairs = new ArrayList<StatePair>();
        int originalStateNum = 0;
        for (int ni = 0; ni < numNodes; ni++) {
            allStatePairs.addAll(statePairList[ni]);
            fine2coarseMapping[ni] = new int[numStates[ni]];
            for (int si = 0; si < numStates[ni]; si++) {
                fine2coarseMapping[ni][si] = si; // default, mapping to itself
            }
            originalStateNum += numStates[ni];
        }
        GlobalLogger.log(Level.INFO, String.format("Number of candidate merging state pairs: %d", allStatePairs.size()));

        Collections.sort(allStatePairs);

        int targetStateNum = (int) (originalStateNum * (1 - mergingRate / 2.0));

        int currentStateNum = originalStateNum;
        for (StatePair statePair : allStatePairs) {
            int node = statePair.getNode();
            int state1 = statePair.getState1();
            int state2 = statePair.getState2();

            fine2coarseMapping[node][state2] = state1;

            if (currentStateNum-- <= targetStateNum) {
                break;
            }
        }

        // maps index from the merged_split state space to the merged state space
        Map<Integer, Integer> fine2coraseMap = new HashMap<Integer, Integer>();

        for (int ni = 0; ni < numNodes; ni++) {
            fine2coraseMap.clear();
            for (int si = 0; si < numStates[ni]; si++) {
                if (!fine2coraseMap.containsKey(fine2coarseMapping[ni][si])) {
                    fine2coraseMap.put(fine2coarseMapping[ni][si], fine2coraseMap.size());
                }
            }
            // set fine2coarseMapping
            for (int si = 0; si < numStates[ni]; si++) {
                fine2coarseMapping[ni][si] = fine2coraseMap.get(fine2coarseMapping[ni][si]);
            }

            newNumStates[ni] = fine2coraseMap.size();

            // remve merged splits
            for (Tree<Integer> coarserStateTree : splitTrees[ni].getAtHeight(2)) {
                List<Tree<Integer>> children = coarserStateTree.getChildren();
                if (children.size() == 1) {
                    continue;
                }
                Tree<Integer> tree1 = children.get(0);
                Tree<Integer> tree2 = children.get(1);
                tree1.setLabel(fine2coarseMapping[ni][tree1.getLabel()]);
                tree2.setLabel(fine2coarseMapping[ni][tree2.getLabel()]);
                if (tree1.getLabel() == tree2.getLabel()) {
                    children.remove(tree2);
                }
            }
        }
        statePairList = null;
        return newNumStates;
    }

    // TODO: use this simple smoothing or something finer based on the first split
    private void setupSmoothingMatrix() {
        if (smoothingMatrix == null) {
            smoothingMatrix = new double[numNodes][][];
        }

        for (int ni = 0; ni < numNodes; ni++) {
            smoothingMatrix[ni] = new double[numStates[ni]][numStates[ni]];
            if (numStates[ni] == 1) {
                smoothingMatrix[ni][0][0] = 1;
            } else if (isPhrasalNode[ni]) {
                Tree<Integer> tempSplitTree = splitTrees[ni];
                while (tempSplitTree.getChildren().size() == 1) {
                    tempSplitTree = tempSplitTree.getChildren().get(0);
                }
                for (Tree<Integer> branch : tempSplitTree.getChildren()) {
                    List<Integer> statesInBranch = branch.getYield();
                    int stateNumInBranch = statesInBranch.size();
                    double diff = 0;
                    double same = 1;
                    if (stateNumInBranch != 1) {
                        same = 1 - Grammar.ruleSmoothingParam;
                        diff = Grammar.ruleSmoothingParam / (stateNumInBranch - 1);
                    }
                    for (int i : statesInBranch) {
                        for (int j : statesInBranch) {
                            if (i == j) {
                                smoothingMatrix[ni][i][j] = same;
                            } else {
                                smoothingMatrix[ni][i][j] = diff;
                            }
                        }
                    }
                }
            } else {
                double diff = Grammar.wordSmoothingParam / (numStates[ni] - 1);
                double same = 1 - Grammar.wordSmoothingParam;
                for (int i = 0; i < numStates[ni]; i++) {
                    for (int j = 0; j < numStates[ni]; j++) {
                        smoothingMatrix[ni][i][j] = (i == j) ? same : diff;
                    }
                }
            }
        }
    }

    public boolean isPhrasalNode(int node) {
        return isPhrasalNode[node];
    }

    /**
     * Returns the node id of a node name, add the node name is it does not
     * exist
     *
     * @param nodeName
     * @return
     */
    public int getNode(String nodeName) {
        Integer node = nodeMap.get(nodeName);
        if (node == null) {
            node = nodeMap.size();
            nodeList.add(nodeName);
            nodeMap.put(nodeName, node);
        }
        return node;
    }

    /* 
     This function is not right because it uses two different parent-annotated
     nodes of the same base node may each contain a different state. In this 
     case this function will determine the grammara as pre_raw but actually it
     is still post_raw. We use the getGrammarType function of the
     BaseNodeManager instead.
     */
    public GrammarType getGrammarType() {
        GrammarType type = GrammarType.pre_raw;
        for (int ni = 0; ni < numNodes; ni++) {
            if (numStates[ni] > 1) {
                type = GrammarType.post_raw;
                break;
            }
        }
        return type;
    }

    /**
     * return true if any node has more than maxStateNum states, false otherwise
     *
     * @param maxStateNum
     * @return
     */
    public boolean reducible(int maxStateNum) {
        for (int ni = 0; ni < numNodes; ni++) {
            if (splitTrees[ni].getChildren().size() > maxStateNum) {
                return true;
            }
        }
        return false;
    }

    public void doInitStep(ConstituentTreeList treeList) {
        GlobalLogger.log(Level.INFO, "Do initialization step...");

        numNodes = nodeMap.size();
        numStates = new int[numNodes];
        nodeCounts = new double[numNodes][];
        isPhrasalNode = new boolean[numNodes];
        splitTrees = new Tree[numNodes];
        fine2coarseMapping = new int[numNodes][];

        Arrays.fill(numStates, 1);
        Arrays.fill(isPhrasalNode, false);
        for (Entry<String, Integer> entry : nodeMap.entrySet()) {
            String nodeName = entry.getKey();
            int node = entry.getValue();
            if (nodeName.endsWith("^g")) {
                isPhrasalNode[node] = true;
            }
        }

        for (int ni = 0; ni < numNodes; ni++) {
            splitTrees[ni] = new Tree<Integer>(0);
        }

        setupSmoothingMatrix();

        ruleManager.setupGrammar(this);
        lexiconManager.setupGrammar(this);

//        scanRules(treeList);

        GlobalLogger.log(Level.FINER, "begin E-step");
        JobManager jobManager = JobManager.getInstance();
        try {
            JobGroup grp = jobManager.createJobGroup("doEMStep");
            int ti = 0;
            for (final Tree<Constituent> tree : treeList) {
                ti++;
                final int jti = ti;
                Job job = new Job(
                        new Runnable() {
                    @Override
                    public void run() {
                        double weight = jti > manualTrees ? autoWeight : 1.0;
                        tallyRuleCounts(tree, weight);
                    }
                }, String.valueOf(ti) + "-th tree");
                job.setPriority(ti);
                jobManager.addJob(grp, job);
            }
            grp.join();
        } catch (Exception e) {
            System.err.println(e);
        }

        ruleManager.initParams();
        lexiconManager.initParams();
        tallyNodeCounts();
        lexiconManager.doMStep();
        if (!lexiconManager.isRuleManagerUpdated()) {
            ruleManager.doMStep();
        }
        GlobalLogger.log(Level.INFO, "Done");
    }

//    public void setResetFeatureRichLexicon(boolean reset) {
//        if (featureRichLexicon != null) {
//            featureRichLexicon.setResetVariables(reset);
//        }
//    }
    public void doSplittingStep() {
        int[] newNumStates = splitStates();
        ruleManager.splitStates(newNumStates);
        lexiconManager.splitStates(newNumStates);
        System.arraycopy(newNumStates, 0, numStates, 0, numNodes);
        setupSmoothingMatrix();
        numSplits++;

        //TODO check feature rich model
//        if (featureRichLexicon != null) {
//            featureRichLexicon.setResetVariables(true);
//            lexiconManager.setLatentLexiconUpdated(true);
//        }
    }

    protected synchronized void increaseLogLikelihood(double ll) {
        logLikelihood += ll;
    }

    public void clearInsideOutsideScores(Tree<Constituent> tree) {
        if (tree.isLeaf()) {
            return;
        }
        Constituent label = tree.getLabel();
        label.setIScores(null);
        label.setOScores(null);
        for (Tree<Constituent> child : tree.getChildren()) {
            clearInsideOutsideScores(child);
        }
    }

    public void tallyNodeCounts() {
        for (int ni = 0; ni < numNodes; ni++) {
            nodeCounts[ni] = ArrayMath.initArray(nodeCounts[ni], numStates[ni]);
        }
        ruleManager.tallyNodeCounts();
        lexiconManager.tallyNodeCounts();
    }

    public double doEMStep(ConstituentTreeList treeList) {
        GlobalLogger.log(Level.FINER, "initialize EM");

        ruleManager.resetCounts();
        lexiconManager.resetCounts();
        logLikelihood = 0;

        GlobalLogger.log(Level.FINER, "begin E-step");
        JobManager jobManager = JobManager.getInstance();
        try {
            JobGroup grp = jobManager.createJobGroup("doEMStep");
            int ti = 0;
            for (final Tree<Constituent> tree : treeList) {
                ti++;
                final int jti = ti;
                Job job = new Job(
                        new Runnable() {
                    public void run() {
                        double ll = doInsideOutside(tree);
                        double weight = jti > manualTrees ? autoWeight : 1.0;
                        increaseLogLikelihood(ll * weight);
                        tallyPostProbs(tree, weight);
                        clearInsideOutsideScores(tree);
                    }
                }, String.valueOf(ti) + "-th tree");
                job.setPriority(ti);
                jobManager.addJob(grp, job);
            }
            grp.join();
        } catch (Exception e) {
            System.err.println(e);
        }
        GlobalLogger.log(Level.FINER, "begin M-step");
        // TODO: double check the use of direct optimization

        tallyNodeCounts();
//        if (!doDirectOptimization) {

//        }
        lexiconManager.doMStep(logLikelihood);
        if (!lexiconManager.isRuleManagerUpdated()) {
            ruleManager.doMStep();
        }
        //        if (featureRichLexicon != null) {
        //TODO check
//            featureRichLexicon.optimize(logLikelihood);
//            lexiconManager.setLatentLexiconUpdated(true);
//        }
        GlobalLogger.log(Level.FINER, "exit EM");
        return logLikelihood;
    }

    public void doMergingStep(ConstituentTreeList treeList) {
        GlobalLogger.log(Level.FINER, "initialize tying");

        JobManager jobManager = JobManager.getInstance();
        initMerging(true);
        try {
            JobGroup grp = jobManager.createJobGroup("doTyingStep-b");
            int ti = 0;
            for (final Tree<Constituent> tree : treeList) {
                ti++;
                final int jti = ti;
                Job job = new Job(
                        new Runnable() {
                    public void run() {
                        doInsideOutside(tree);
                        double weight = jti > manualTrees ? autoWeight : 1.0;
                        estimateMergingLoss(tree, weight);
                        clearInsideOutsideScores(tree);
                    }
                },
                        String.valueOf(ti) + "-th tree");
                job.setPriority(ti);
                jobManager.addJob(grp, job);
            }
            grp.join();
        } catch (Exception e) {
            System.err.println(e);
        }

        // start merging 
        int[] newNumStates = mergeStates();
        ruleManager.mergeStates(newNumStates);
        lexiconManager.mergeStates(newNumStates);
        System.arraycopy(newNumStates, 0, numStates, 0, numNodes);
        setupSmoothingMatrix();

        tallyNodeCounts();
        ruleManager.doMStep();
        lexiconManager.doMStep();

//        if (featureRichLexicon != null) {
        //TODO check
//            featureRichLexicon.setResetVariables(true);
//            featureRichLexicon.doIndirectOptimization();
//            lexiconManager.setLatentLexiconUpdated(true);
//        }
        GlobalLogger.log(Level.FINER, "exit tying");
    }

//    public void scanRules(ConstituentTreeList treeList) {
//        for (Tree<Constituent> tree : treeList) {
//            scanRules(tree);
//        }
//    }
//    public void scanRules(Tree<Constituent> tree) {
//        if (tree.isPreTerminal()) {
//            int node = tree.getLabel().getNode();
//            String word = tree.getChildren().get(0).getLabel().getWord();
//            lexiconManager.addLexicalRule(node, word);
//            return;
//        }
//        int parentNode = tree.getLabel().getNode();
//        List<Tree<Constituent>> children = tree.getChildren();
//        switch (children.size()) {
//            case 0:
//                throw new Error("It is a malformed tree...\n" + tree);
//            case 1:
//                int childNode = children.get(0).getLabel().getNode();
//                ruleManager.addUnaryRule(parentNode, childNode);
//                break;
//            case 2:
//                int leftNode = children.get(0).getLabel().getNode();
//                int rightNode = children.get(1).getLabel().getNode();
//                ruleManager.addBinaryRule(parentNode, leftNode, rightNode);
//                break;
//            default:
//                throw new Error("It is a malformed tree...\n" + tree);
//        }
//        for (Tree<Constituent> child : children) {
//            scanRules(child);
//        }
//    }
    @SuppressWarnings("static-access")
    public double doInsideOutside(Tree<Constituent> tree) {
        doInsideScores(tree);
        setRootOutsideScore(tree);
        doOutsideScores(tree);

        double localLogLikelihood = 0;
        Constituent root = tree.getLabel();
        localLogLikelihood = Math.log(root.getIScore(0)) + root.getIScale() * Math.log(SCALE);
        return localLogLikelihood;
    }

    @SuppressWarnings("static-access")
    public void doInsideScores(Tree<Constituent> tree) {
        if (tree.isLeaf()) {
            return;
        }
        List<Tree<Constituent>> children = tree.getChildren();
        for (Tree<Constituent> child : children) {
            doInsideScores(child);
        }
        Constituent pConstituent = tree.getLabel();
        int pNode = pConstituent.getNode();
        int pStateNum = numStates[pNode];
        if (tree.isPreTerminal()) {
            Constituent wordLabel = tree.getChildren().get(0).getLabel();
            String word = wordLabel.getWord();
            double[] lexiconScores = lexiconManager.getProbs(pNode, word);
            pConstituent.setIScores(lexiconScores);
            pConstituent.setIScale(0);
        } else {
            switch (children.size()) {
                case 0:
                    throw new Error("Incorrenct children size...");
                case 1: {
                    Constituent cConstituent = children.get(0).getLabel();
                    int cNode = cConstituent.getNode();
                    int cStateNum = numStates[cNode];
                    UnaryRule rule = ruleManager.getUnaryRule(pNode, cNode);
                    double[][] unaryScores = rule.getRuleProbs();
                    double[] iScores = new double[pStateNum];
                    double[] cIScores = cConstituent.getIScores();
                    for (int csi = 0; csi < cStateNum; csi++) {
                        double childScore = cIScores[csi];
                        if (childScore == 0 || unaryScores[csi] == null) {
                            continue;
                        }
                        for (int psi = 0; psi < pStateNum; psi++) {
                            double ruleScore = unaryScores[csi][psi];
                            if (ruleScore == 0) {
                                continue;
                            }
                            iScores[psi] += ruleScore * childScore;
                        }
                    }
                    pConstituent.setIScores(iScores);
                    for (int si = 0; si < iScores.length; si++) {
                        if (Double.isNaN(iScores[si]) || Double.isInfinite(iScores[si])) {
                            throw new Error("Error");
                        }
                    }
                    pConstituent.scaleIScores(cConstituent.getIScale());
                    break;
                }
                case 2: {
                    Constituent lcConstituent = children.get(0).getLabel();
                    int lcNode = lcConstituent.getNode();
                    int lcStateNum = numStates[lcNode];
                    Constituent rcConstituent = children.get(1).getLabel();
                    int rcNode = rcConstituent.getNode();
                    int rcStateNum = numStates[rcNode];
                    BinaryRule rule = ruleManager.getBinaryRule(pNode, lcNode, rcNode);
                    double[][][] binaryScores = rule.getRuleProbs();
                    double[] iScores = new double[pStateNum];
                    double[] rIScores = rcConstituent.getIScores();
                    double[] lIScores = lcConstituent.getIScores();
                    for (int lcsi = 0; lcsi < lcStateNum; lcsi++) {
                        double leftChildScore = lIScores[lcsi];
                        if (leftChildScore == 0 || binaryScores[lcsi] == null) {
                            continue;
                        }
                        for (int rcsi = 0; rcsi < rcStateNum; rcsi++) {
                            double rightChildScore = rIScores[rcsi];
                            if (rightChildScore == 0 || binaryScores[lcsi][rcsi] == null) {
                                continue;
                            }
                            for (int psi = 0; psi < pStateNum; psi++) {
                                double ruleScore = binaryScores[lcsi][rcsi][psi];
                                if (ruleScore == 0) {
                                    continue;
                                }
                                double parentScore = ruleScore * leftChildScore * rightChildScore;
                                iScores[psi] += parentScore;
                            }
                        }
                    }
                    pConstituent.setIScores(iScores);
                    for (int si = 0; si < iScores.length; si++) {
                        if (Double.isNaN(iScores[si]) || Double.isInfinite(iScores[si])) {
                            throw new Error("Error");
                        }
                    }
                    pConstituent.scaleIScores(lcConstituent.getIScale() + rcConstituent.getIScale());
                    break;
                }
                default:
                    throw new Error("Malformed tree: more than two children");
            }
        }
    }

    void setRareWordThreshold(double rare) {
        lexiconManager.setRareWordThreshold(rare - 0.01);
    }

    public void doOutsideScores(Tree<Constituent> tree) {
        if (tree.isLeaf()) {
            return;
        }

        List<Tree<Constituent>> children = tree.getChildren();
        Constituent pConstituent = tree.getLabel();
        int pNode = pConstituent.getNode();
        int pStateNum = numStates[pNode];
        if (tree.isPreTerminal()) {
        } else {
            switch (children.size()) {
                case 0:
                    throw new Error("Incorrect children size...");
                case 1: {
                    Constituent child = children.get(0).getLabel();
                    int cNode = child.getNode();
                    int cStateNum = numStates[cNode];
                    UnaryRule rule = ruleManager.getUnaryRule(pNode, cNode);
                    double[][] unaryScores = rule.getRuleProbs();
                    double[] oScores = new double[cStateNum];
                    double[] pOScores = pConstituent.getOScores();
                    for (int csi = 0; csi < cStateNum; csi++) {
                        if (unaryScores[csi] == null) {
                            continue;
                        }
                        for (int psi = 0; psi < pStateNum; psi++) {
                            double parentScore = pOScores[psi];
                            double ruleScore = unaryScores[csi][psi];
                            if (parentScore == 0 || ruleScore == 0) {
                                continue;
                            }
                            oScores[csi] += ruleScore * parentScore;
                        }
                    }
                    child.setOScores(oScores);
                    child.scaleOScores(pConstituent.getOScale());
                    break;
                }

                case 2: {
                    Constituent lcConstituent = children.get(0).getLabel();
                    int lcNode = lcConstituent.getNode();
                    int lcStateNum = numStates[lcNode];
                    Constituent rightChild = children.get(1).getLabel();
                    int rcNode = rightChild.getNode();
                    int rcStateNum = numStates[rcNode];
                    BinaryRule rule = ruleManager.getBinaryRule(pNode, lcNode, rcNode);
                    double[][][] binaryScores = rule.getRuleProbs();
                    double[] leftOScores = new double[lcStateNum];
                    double[] rightOScores = new double[rcStateNum];
                    double[] pOScores = pConstituent.getOScores();
                    double[] lIScores = lcConstituent.getIScores();
                    double[] rIScores = rightChild.getIScores();
                    for (int lcsi = 0; lcsi < lcStateNum; lcsi++) {
                        double leftIScore = lIScores[lcsi];
                        if (leftIScore == 0 || binaryScores[lcsi] == null) {
                            continue;
                        }
                        for (int rcsi = 0; rcsi < rcStateNum; rcsi++) {
                            double rightIScore = rIScores[rcsi];
                            if (rightIScore == 0 || binaryScores[lcsi][rcsi] == null) {
                                continue;
                            }
                            for (int psi = 0; psi < pStateNum; psi++) {
                                double parentOScore = pOScores[psi];
                                double ruleScore = binaryScores[lcsi][rcsi][psi];
                                if (parentOScore == 0 || ruleScore == 0) {
                                    continue;
                                }
                                leftOScores[lcsi] += ruleScore * parentOScore * rightIScore;
                                rightOScores[rcsi] += ruleScore * parentOScore * leftIScore;
                            }
                        }
                    }
                    lcConstituent.setOScores(leftOScores);
                    lcConstituent.scaleOScores(pConstituent.getOScale() + rightChild.getIScale());
                    rightChild.setOScores(rightOScores);
                    rightChild.scaleOScores(pConstituent.getOScale() + lcConstituent.getIScale());
                    break;
                }
                default:
                    throw new Error("Malformed tree: more than two children");
            }
        }
        for (Tree<Constituent> child : children) {
            doOutsideScores(child);
        }
    }

    public void setRootOutsideScore(Tree<Constituent> tree) {
        Constituent rConstituent = tree.getLabel();
        int rNode = rConstituent.getNode();
        if (rNode != 0 || numStates[rNode] != 1) {
            throw new Error("Root is not 0 or it is split: " + tree.toString());
        }
        double oScores[] = new double[1];
        rConstituent.setOScores(oScores);
        rConstituent.setOScore(0, 1.0);
        rConstituent.setOScale(0);
    }

    public void tallyRuleCounts(Tree<Constituent> tree, double weight) {
        if (tree.isLeaf()) {
            return;
        }

        Constituent pConstituent = tree.getLabel();
        int pNode = pConstituent.getNode();
        if (tree.isPreTerminal()) {
            Constituent terminal = tree.getChildren().get(0).getLabel();
            String word = terminal.getWord();
//            lexiconManager.addCounts(parentNode, word, (1 + RandomDisturbance.generateRandomDisturbance()) * weight);
//            lexiconManager.addCounts(pNode, word, weight);
            lexiconManager.addLexicalRule(pNode, word, weight);
        } else {
            List<Tree<Constituent>> children = tree.getChildren();
            switch (children.size()) {
                case 0:
                    throw new Error("Malformed tree!");
                case 1: {
                    Constituent child = children.get(0).getLabel();
                    int cNode = child.getNode();
                    ruleManager.addUnaryRule(pNode, cNode, weight);
                    break;
                }
                case 2: {
                    Constituent lcConstituent = children.get(0).getLabel();
                    int lcNode = lcConstituent.getNode();
                    Constituent rcConstituent = children.get(1).getLabel();
                    int rcNode = rcConstituent.getNode();
                    ruleManager.addBinaryRule(pNode, lcNode, rcNode, weight);
                    break;
                }
                default:
                    throw new Error("Malformed tree!");
            }

            for (Tree<Constituent> child : children) {
                tallyRuleCounts(child, weight);
            }
        }
    }

    public void tallyPostProbs(Tree<Constituent> tree, double weight) {
        if (tree.isLeaf() || tree.isPreTerminal()) {
            return;
        }

        Constituent rConstituent = tree.getLabel();
        int rNode = rConstituent.getNode();
        if (rNode != 0 || numStates[rNode] != 1) {
            throw new Error("Root is not 0 or it is split: " + tree.toString());
        }

        double treeScore = rConstituent.getIScore(0) / weight;
        double treeScale = rConstituent.getIScale();
        if (treeScore == 0) {
            System.err.println("Something is wrong with this tree. I will skip it.");
            return;
        }

        tallyPostProbs(tree, treeScore, treeScale);
    }

    public void tallyPostProbs(Tree<Constituent> tree, double treeScore, double treeScale) {
        if (tree.isLeaf()) {
            return;
        }
        if (tree.isPreTerminal()) {
            Constituent ptConstituent = tree.getLabel();
            Constituent tConstituent = tree.getChildren().get(0).getLabel();
            String word = tConstituent.getWord();
            int ptNode = ptConstituent.getNode();
            int ptStateNum = numStates[ptNode];
            double[] counts = new double[ptStateNum];
            double scalingFactor = Math.pow(SCALE, ptConstituent.getOScale() + ptConstituent.getIScale() - treeScale);
            if (scalingFactor == 0) {
                System.err.println("O: " + ptConstituent.getOScale() + " I: " + ptConstituent.getIScale() + " T: " + treeScale);
            }
            double[] oScores = ptConstituent.getOScores();
            double[] iScores = ptConstituent.getIScores();
            for (int si = 0; si < ptStateNum; si++) {
                double oScore = oScores[si];
                if (oScore == 0) {
                    continue;
                }

                double iScore = iScores[si];
                if (iScore == 0) {
                    continue;
                }

                double count = iScore / treeScore * scalingFactor * oScore;
                counts[si] += count;
            }
            lexiconManager.addCounts(ptNode, word, counts);
        } else {
            List<Tree<Constituent>> children = tree.getChildren();
            Constituent pConstituent = tree.getLabel();
            int pNode = pConstituent.getNode();
            switch (children.size()) {
                case 0:
                    break;
                case 1: {
                    Constituent cConstituent = children.get(0).getLabel();
                    int cNode = cConstituent.getNode();
                    UnaryRule rule = ruleManager.getUnaryRule(pNode, cNode);
                    double scalingFactor = Math.pow(SCALE, pConstituent.getOScale() + cConstituent.getIScale() - treeScale);
                    if (scalingFactor == 0) {
                        System.err.println("P: " + pConstituent.getOScale() + " C: " + cConstituent.getIScale() + " T: " + treeScale);
                    }
                    double[] pOScores = pConstituent.getOScores();
                    double[] cIScores = cConstituent.getIScores();
                    rule.addCounts(numStates, pOScores, cIScores, treeScore, scalingFactor);
                    break;
                }

                case 2: {
                    Constituent lcConstituent = children.get(0).getLabel();
                    int lcNode = lcConstituent.getNode();
                    Constituent rcConstituent = children.get(1).getLabel();
                    int rcNode = rcConstituent.getNode();
                    BinaryRule rule = ruleManager.getBinaryRule(pNode, lcNode, rcNode);
                    double scalingFactor = Math.pow(SCALE, pConstituent.getOScale() + lcConstituent.getIScale() + rcConstituent.getIScale() - treeScale);
                    if (scalingFactor == 0) {
                        System.err.println("P: " + pConstituent.getOScale() + " LC: " + lcConstituent.getIScale() + " RC: " + rcConstituent.getIScale() + " T: " + treeScale);
                    }
                    double[] pOScores = pConstituent.getOScores();
                    double[] lIScores = lcConstituent.getIScores();
                    double[] rIScores = rcConstituent.getIScores();
                    rule.addCounts(numStates, pOScores, lIScores, rIScores, treeScore, scalingFactor);
                    break;
                }

                default:
                    throw new Error("Malformed tree: more than two children.");
            }

            for (Tree<Constituent> child : children) {
                tallyPostProbs(child, treeScore, treeScale);
            }

        }
    }

    public void estimateMergingLoss(Tree<Constituent> tree, double weight) {
        if (tree.isLeaf()) {
            return;
        }
        if (!tree.isRoot()) {
            Constituent constituent = tree.getLabel();
            int node = constituent.getNode();
            int stateNum = numStates[node];
            double[] iScores = constituent.getIScores();
            double[] oScores = constituent.getOScores();
            double oriScore = 0;
            for (int si = 0; si < stateNum; si++) {
                oriScore += iScores[si] * oScores[si];
            }
            double[] stateCounts = nodeCounts[node];
            // TODO: double check LexicalManager that statecounts of the preterminals are stared in the node

            //            TODO: make sure BaseNode keeps state count in memory...
            for (StatePair mergingStatePair : statePairList[node]) {
                double normalization = 0, combineIScore = 0, combineOScore = 0;
                double seperateScoreSum = 0;

                int s1 = mergingStatePair.getState1();
                int s2 = mergingStatePair.getState2();

                normalization += stateCounts[s1] + stateCounts[s2];
                combineIScore += stateCounts[s1] * iScores[s1] + stateCounts[s2] * iScores[s2];
                combineOScore += oScores[s1] + oScores[s2];
                seperateScoreSum += iScores[s1] * oScores[s1] + iScores[s2] * oScores[s2];
                combineIScore /= normalization;

                double delta = weight * Math.log(oriScore / (oriScore - seperateScoreSum + combineOScore * combineIScore));
                if (Double.isInfinite(delta) || Double.isNaN(delta)) {
                    throw new Error("found error");
                }
                mergingStatePair.addScore(delta);
            }
        }
        List<Tree<Constituent>> children = tree.getChildren();
        for (Tree<Constituent> child : children) {
            estimateMergingLoss(child, weight);
        }
    }

    public boolean save(String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName); // save to file
            GZIPOutputStream gzos = new GZIPOutputStream(fos); // compressed
            ObjectOutputStream out = new ObjectOutputStream(gzos);
            out.writeObject(this);
            out.flush();
            out.close();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            System.out.println("StatckTrace: ");
            return false;
        }

        return true;
    }

    public static Grammar load(
            String fileName) {
        Grammar grammar = null;
        try {
            FileInputStream fis = new FileInputStream(fileName); // load from file
            GZIPInputStream gzis = new GZIPInputStream(fis); // compressed
            ObjectInputStream in = new ObjectInputStream(gzis); // load objects
            grammar = (Grammar) in.readObject(); // read the grammars
            in.close(); // and close the stream.

        } catch (IOException e) {
            System.out.println("IOException\n" + e);
            System.out.println("StackTrace");
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found!");
            return null;
        }

        return grammar;
    }

    public int[] coarseStates() {
        int[] newStateNum = new int[numNodes];
        int maxStateNum = 0;
        boolean[][] stateMerged = new boolean[numNodes][];
        int originalStateNum = 0;
        for (int ni = 0; ni < numNodes; ni++) {
            if (numStates[ni] > maxStateNum) {
                maxStateNum = numStates[ni];
            }
            stateMerged[ni] = new boolean[numStates[ni]];
            Arrays.fill(stateMerged[ni], false);
            fine2coarseMapping[ni] = new int[numStates[ni]];
            for (int si = 0; si < numStates[ni]; si++) {
                fine2coarseMapping[ni][si] = si; // default, mapping to itself
            }
            originalStateNum += numStates[ni];
        }
        System.out.printf("Number of states in the finer grammar: %d\n", originalStateNum);

        int currTotalStateNum = originalStateNum;
        int maxSplitNum = (int) Math.ceil(Math.log(maxStateNum) / Math.log(2));
        int targetMaxStateNum = (int) Math.pow(2, maxSplitNum - 1);

        for (int ni = 0; ni < numNodes; ni++) {
            Collections.sort(statePairList[ni]);
            int nodeStateNum = numStates[ni];
            while (nodeStateNum > targetMaxStateNum) {
                StatePair statePair = statePairList[ni].remove(0);
                int s1 = statePair.getState1();
                int s2 = statePair.getState2();
                if (!stateMerged[ni][s1] && !stateMerged[ni][s2]) {
                    fine2coarseMapping[ni][s2] = s1;
                    stateMerged[ni][s1] = true;
                    stateMerged[ni][s2] = true;
                    nodeStateNum--;
                    currTotalStateNum--;
                }
            }
        }


        List<StatePair> allStatePairs = new ArrayList<StatePair>();
        for (int ni = 0; ni < numNodes; ni++) {
            for (StatePair statePair : statePairList[ni]) {
                int s1 = statePair.getState1();
                int s2 = statePair.getState2();
                if (!stateMerged[ni][s1] && !stateMerged[ni][s2]) {
                    allStatePairs.add(statePair);
                }
            }
        }

        Collections.sort(allStatePairs);
        int targetStateNum = (int) (originalStateNum * (1 - mergingRate / 2.0));
        for (StatePair statePair : allStatePairs) {
            int ni = statePair.getNode();
            int s1 = statePair.getState1();
            int s2 = statePair.getState2();

            if (!stateMerged[ni][s1] && !stateMerged[ni][s2]) {
                fine2coarseMapping[ni][s2] = s1;
                stateMerged[ni][s1] = true;
                stateMerged[ni][s2] = true;
                if (currTotalStateNum-- <= targetStateNum) {
                    break;
                }
            }
        }


        Map<Integer, Integer> fine2coraseMap = new HashMap<Integer, Integer>();
        for (int ni = 0; ni < numNodes; ni++) {
            fine2coraseMap.clear();
            for (int si = 0; si < numStates[ni]; si++) {
                if (!fine2coraseMap.containsKey(fine2coarseMapping[ni][si])) {
                    fine2coraseMap.put(fine2coarseMapping[ni][si], fine2coraseMap.size());
                }
            }
            // set fine2coarseMapping
            for (int si = 0; si < numStates[ni]; si++) {
                fine2coarseMapping[ni][si] = fine2coraseMap.get(fine2coarseMapping[ni][si]);
            }
            newStateNum[ni] = fine2coraseMap.size();

            // remve merged splits
            List<Tree<Integer>> children = new ArrayList<Tree<Integer>>();
            for (int si = 0; si < newStateNum[ni]; si++) {
                children.add(new Tree<Integer>(si, new ArrayList<Tree<Integer>>()));
            }

            for (Tree<Integer> finerStateTree : splitTrees[ni].getChildren()) {
                int fsi = finerStateTree.getLabel();
                int csi = fine2coarseMapping[ni][fsi];

                children.get(csi).getChildren().add(finerStateTree);
            }

            splitTrees[ni].setChildren(children);
        }

        statePairList = null;
        return newStateNum;
    }

//    public void initCoarseGrammar(Grammar fineGrammar) {
//        numNodes = finerNodeManager.getNumNodes();
//        phrasalNodes = finerNodeManager.getIsGrammarTag();
//
//        numStates = new int[numNodes];
//        splitTreeList = new Tree[numNodes];
//        fine2coarseMapping = new int[numNodes][];
//        smoothingWeights = new double[numNodes][][];
//
//        Tree<Integer>[] finerSplitTreeList = finerNodeManager.getSplitTreeList();
//        int[][] finerIndexMapping = finerNodeManager.getFine2coarseMapping();
//        for (int ni = 0; ni < numNodes; ni++) {
//            splitTreeList[ni] = finerSplitTreeList[ni].shallowClone();
//            List<Tree<Integer>> stateSplitTrees = splitTreeList[ni].getPreTerminals();
//            numStates[ni] = stateSplitTrees.size();
//            for (int si = 0; si < numStates[ni]; si++) {
//                for (Tree<Integer> finerStateTree : stateSplitTrees.get(si).getChildren()) {
//                    finerIndexMapping[ni][finerStateTree.getLabel()] = si;
//                }
//            }
//        }
//    }
    public void addUnaryChains() {
        boolean converged = false;
        Map<UnaryRule, Double> chainRuleScoreMap = new HashMap<UnaryRule, Double>();
        for (UnaryRule rule : ruleManager.getUnaryRuleSet()) {
            double[][] probs = rule.getRuleProbs();
            int pStateNum = numStates[rule.getParent()];
            int cStateNum = numStates[rule.getChild()];
            double chainScore = 0;
            for (int ci = 0; ci < cStateNum; ci++) {
                if (probs[ci] == null) {
                    continue;
                }
                for (int pi = 0; pi < pStateNum; pi++) {
                    chainScore += probs[ci][pi];
                }
            }
            chainRuleScoreMap.put(rule, chainScore);
        }


        int loop = 0;
        while (!converged && loop <= 50) {
            converged = true;
            loop++;
            List<UnaryRule>[] unaryRuleListWithP = new List[numNodes];
            List<UnaryRule>[] unaryRuleListWithC = new List[numNodes];
            for (UnaryRule rule : ruleManager.getUnaryRuleSet()) {
                int pNode = rule.getParent();
                int cNode = rule.getChild();
                if (unaryRuleListWithP[pNode] == null) {
                    unaryRuleListWithP[pNode] = new ArrayList<UnaryRule>();
                }
                if (unaryRuleListWithC[cNode] == null) {
                    unaryRuleListWithC[cNode] = new ArrayList<UnaryRule>();
                }
                unaryRuleListWithP[pNode].add(rule);
                unaryRuleListWithC[cNode].add(rule);
            }

            for (int pNode = 0; pNode < numNodes; pNode++) {
                if (unaryRuleListWithP[pNode] == null) {
                    continue;
                }
                int pStateNum = numStates[pNode];
                for (UnaryRule pRule : unaryRuleListWithP[pNode]) {
                    int iNode = pRule.getChild();
                    if (unaryRuleListWithP[iNode] == null) {
                        continue;
                    }
                    int iStateNum = numStates[iNode];
                    double[][] pProbs = pRule.getRuleProbs();
                    for (UnaryRule cRule : unaryRuleListWithP[iNode]) {
                        int cNode = cRule.getChild();
                        int cStateNum = numStates[cNode];
                        double[][] cProbs = cRule.getRuleProbs();
                        double[][] chainProbs = new double[cStateNum][pStateNum];
                        double chainScore = 0;
                        for (int csi = 0; csi < cStateNum; csi++) {
                            if (cProbs[csi] == null) {
                                continue;
                            }
                            for (int psi = 0; psi < pStateNum; psi++) {
                                double iScore = 0;
                                for (int isi = 0; isi < iStateNum; isi++) {
                                    if (pProbs[isi] == null) {
                                        continue;
                                    }
                                    iScore += pProbs[isi][psi] * cProbs[csi][isi];
                                }
                                chainScore += iScore;
                                chainProbs[csi][psi] = iScore;
                            }
                        }
                        chainProbs = ArrayMath.filterMatrix(chainProbs, Grammar.ruleFilteringThreshold);
                        if (chainProbs == null) {
                            continue;
                        }

                        UnaryRule chainRule = ruleManager.getUnaryRule(pNode, cNode);
                        Double bestChainScore = chainRuleScoreMap.get(chainRule);
                        if (bestChainScore == null || chainScore > bestChainScore) {
                            chainRule.setIntermediate(iNode);
                            chainRule.setRuleProbs(chainProbs);
                            chainRuleScoreMap.put(chainRule, chainScore);
                            converged = false;
                        }
                    }
                }
            }
        }
    }

    public void setupFine2CoarseMapping() {
        for (int ni = 0; ni < numNodes; ni++) {
            for (Tree<Integer> coarseStateTree : splitTrees[ni].getAtHeight(2)) {
                for (Tree<Integer> stateTree : coarseStateTree.getChildren()) {
                    fine2coarseMapping[ni][stateTree.getLabel()] = coarseStateTree.getLabel();
                }
            }
        }
    }

    public void setupCoarseGrammar(Grammar fineGrammar, int[] currNumStates) {
        numNodes = fineGrammar.getNumNodes();
        isPhrasalNode = fineGrammar.getIsPhrasalNode();
        mergingRate = fineGrammar.getMergingRate();
        reductionRate = fineGrammar.getReductionRate();
        nodeList = fineGrammar.getNodeList();
        nodeMap = fineGrammar.getNodeMap();
        nodeCounts = new double[numNodes][];
        fine2coarseMapping = new int[numNodes][];

        if (currNumStates == null) {
            splitTrees = new Tree[numNodes];
            numStates = new int[numNodes];
            Tree<Integer>[] fineSplitTrees = fineGrammar.getSplitTreeList();
            for (int ni = 0; ni < numNodes; ni++) {
                splitTrees[ni] = fineSplitTrees[ni].shallowClone();
                List<Tree<Integer>> stateTreeList = splitTrees[ni].getAtHeight(2);
                for (Tree<Integer> preterminal : stateTreeList) {
                    preterminal.getChildren().clear();
                }
                numStates[ni] = stateTreeList.size();
                fine2coarseMapping[ni] = new int[numStates[ni]];
                if (splitTrees[ni].getDepth() > 1) {
                    for (Tree<Integer> coarseStateTree : splitTrees[ni].getAtHeight(2)) {
                        for (Tree<Integer> stateTree : coarseStateTree.getChildren()) {
                            fine2coarseMapping[ni][stateTree.getLabel()] = coarseStateTree.getLabel();
                        }
                    }
                }
            }
            setupSmoothingMatrix();
        } else {
            numStates = currNumStates;
            splitTrees = fineGrammar.getSplitTreeList();
        }

        RuleManager fineRuleManager = fineGrammar.getRuleManager();
        LexiconManager fineLexiconManager = fineGrammar.getLexiconManager();

        ruleManager = new RuleManager();
        ruleManager.setupGrammar(this);
        ruleManager.setupCoarseRuleManager(fineRuleManager);

        try {
            lexiconManager = fineLexiconManager.getClass().newInstance();
        } catch (InstantiationException ex) {
            Logger.getLogger(Grammar.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Grammar.class.getName()).log(Level.SEVERE, null, ex);
        }
        lexiconManager.setupGrammar(this);
        lexiconManager.setupCoarseLexiconManager(fineLexiconManager);

        tallyNodeCounts();

        ruleManager.doMStep();
//        ((LexiconManager) lexiconManager).doMStep();

        setManualTrees(fineGrammar.manualTrees);
        setAutoWeight(fineGrammar.autoWeight);
    }

    public Grammar createCoarserGrammar(ConstituentTreeList treeList) {
        if (getGrammarType() == Grammar.GrammarType.pre_raw) {
            return null;
        }
        GlobalLogger.log(Level.INFO, "beging computing coarsening loss");
        initMerging(false);
        JobManager jobManager = JobManager.getInstance();

        try {
            JobGroup grp = jobManager.createJobGroup("doCoarsen-b");
            int ti = 0;
            for (final Tree<Constituent> tree : treeList) {
                ti++;
                final int jti = ti;
                Job job = new Job(
                        new Runnable() {
                    public void run() {
                        doInsideOutside(tree);
                        double weight = jti > manualTrees ? autoWeight : 1.0;
                        estimateMergingLoss(tree, weight);
                        clearInsideOutsideScores(tree);
                    }
                },
                        String.valueOf(ti) + "-th tree");
                job.setPriority(ti);
                jobManager.addJob(grp, job);
            }
            grp.join();
        } catch (Exception e) {
            System.err.println(e);
        }
        GlobalLogger.log(Level.INFO, "before coarsening operation");
        int[] newNumStates = coarseStates();
        Grammar coarseGrammar = new Grammar(lang, useFeatureLexicon);
        coarseGrammar.setupCoarseGrammar(this, newNumStates);
        GlobalLogger.log(Level.INFO, "exit coarsening");
        return coarseGrammar;
    }

    public void flattenSplitTree() {
        for (int ni = 0; ni < numNodes; ni++) {
            List<Tree<Integer>> children = splitTrees[ni].getChildren();
            children.clear();
            for (int si = 0; si < numStates[ni]; si++) {
                children.add(new Tree<Integer>(si));
            }
        }
    }

    public void initParsingGrammar() {
//        addUnaryChains();
        ruleManager.setupArray();
        lexiconManager.setupArray();
    }

    public List<Grammar> getParsingGrammarList() {
        List<Grammar> grammarList = new ArrayList<Grammar>();
        setupFine2CoarseMapping();
        Grammar fineGrammar = this;
        grammarList.add(0, fineGrammar);
        grammarList.add(0, fineGrammar.clone());
        while (fineGrammar.reducible(1)) {
            Grammar coarseGrammar = new Grammar(lang, useFeatureLexicon);
            coarseGrammar.setupCoarseGrammar(fineGrammar, null);
            grammarList.add(0, coarseGrammar);
            fineGrammar = coarseGrammar;
        }

        int grNum = grammarList.size();
        for (int gi = 0; gi < grNum; gi++) {
            Grammar gr = grammarList.get(gi);
            if (gi != grNum - 1) {
                gr.initParsingGrammar();
                gr.ruleManager.takeLogarithm();
            } else {
                gr.getRuleManager().setupArray();
            }
        }

        return grammarList;
    }

//    public List<Grammar> getParsingGrammarList() {
//        List<Grammar> grammarList = new ArrayList<Grammar>();
//        setupFine2CoarseMapping();
//        Grammar grammar = new Grammar(lang);
//        grammar.setupCoarseGrammar(this, null);
//        grammarList.add(0, grammar);
//        grammarList.add(0, (Grammar) grammar.clone());
//        int grNum = grammarList.size();
//        for (int gi = 0; gi < grNum; gi++) {
//            Grammar gr = grammarList.get(gi);
//            gr.initParsingGrammar();
//            if (gi != grNum - 1) {
//                gr.ruleManager.takeLogarithm();
//            }
//        }
//
//        return grammarList;
//    }
    public void setupSplitTree(ConstituentTreeList trees) {
        flattenSplitTree();
        Grammar tmpGrammar = this;
        while (tmpGrammar.reducible(2)) {
            tmpGrammar = tmpGrammar.createCoarserGrammar(trees);
        }
        setupSmoothingMatrix();
    }

    public void setRootGoutsideScores(Tree<Constituent> tree) {
        Constituent root = tree.getLabel();
        root.setGoScores(root.getOScores());
    }
}
