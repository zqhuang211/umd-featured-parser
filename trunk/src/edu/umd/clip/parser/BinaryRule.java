package edu.umd.clip.parser;

import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.math.RandomDisturbance;
import edu.umd.clip.util.ArrayUtil;
import java.io.Serializable;
import java.util.List;

public class BinaryRule extends Rule implements Serializable, Comparable {

    private static final long serialVersionUID = 1L;
    private int leftChild;
    private int rightChild;
    private double[][][] ruleProbs = null;
    private double[][][] ruleCounts = null;

    public void takeLogarithm(int[] numStates) {
        int lcStateNum = numStates[leftChild];
        int rcStateNum = numStates[rightChild];
        int pStateNum = numStates[parent];
        for (int lsi = 0; lsi < lcStateNum; lsi++) {
            if (ruleProbs[lsi] == null) {
                continue;
            }
            for (int rsi = 0; rsi < rcStateNum; rsi++) {
                if (ruleProbs[lsi][rsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < pStateNum; psi++) {
                    ruleProbs[lsi][rsi][psi] = Math.log(ruleProbs[lsi][rsi][psi]);
                }
            }
        }
    }

    public int getZeroRuleNum(int[] numStates) {
        int numZeros = 0;
        int leftChildStateNum = numStates[leftChild];
        int rightChildStateNum = numStates[rightChild];
        int parentStateNum = numStates[parent];
        for (int lsi = 0; lsi < leftChildStateNum; lsi++) {
            if (ruleProbs[lsi] == null) {
                numZeros += rightChildStateNum * parentStateNum;
            } else {
                for (int rsi = 0; rsi < rightChildStateNum; rsi++) {
                    if (ruleProbs[lsi][rsi] == null) {
                        numZeros += parentStateNum;
                    } else {
                        for (int psi = 0; psi < parentStateNum; psi++) {
                            if (ruleProbs[lsi][rsi][psi] == 0) {
                                numZeros++;
                            }
                        }
                    }
                }
            }
        }
        return numZeros;
    }

    public int matrixSize(int[] numStates) {
        int pStateNum = numStates[parent];
        int lcStateNum = numStates[leftChild];
        int rcStateNum = numStates[rightChild];
        int size = 0;
        for (int csi = 0; csi < lcStateNum; csi++) {
            if (ruleProbs[csi] == null) {
                continue;
            }
            for (int rsi = 0; rsi < rcStateNum; rsi++) {
                if (ruleProbs[csi][rsi] == null) {
                    continue;
                }
                size += pStateNum;
            }
        }
        return size;
    }

    public void splitStates(int[] oldNumStates, int[] newNumStates) {
        int pStateNum = oldNumStates[parent];
        int lcStateNum = oldNumStates[leftChild];
        int rcStateNum = oldNumStates[rightChild];

        int pSplitFactor = newNumStates[parent] / oldNumStates[parent];
        int lcSplitFactor = newNumStates[leftChild] / oldNumStates[leftChild];
        int rcSplitFactor = newNumStates[rightChild] / oldNumStates[rightChild];

        double[][][] newRuleProbs = new double[newNumStates[leftChild]][newNumStates[rightChild]][newNumStates[parent]];

        for (int olcsi = 0; olcsi < lcStateNum; olcsi++) {
            if (ruleProbs[olcsi] == null) {
                continue;
            }
            for (int orcsi = 0; orcsi < rcStateNum; orcsi++) {
                if (ruleProbs[olcsi][orcsi] == null) {
                    continue;
                }
                double divFactor = lcSplitFactor * rcSplitFactor;
                for (int opsi = 0; opsi < pStateNum; opsi++) {
                    double oldProb = ruleProbs[olcsi][orcsi][opsi];
                    if (oldProb == 0) {
                        continue;
                    }

                    for (int psi = 0; psi < pSplitFactor; psi++) {
                        double leftRandomnessComponent = oldProb / divFactor * RandomDisturbance.generateRandomDisturbance();
                        if (lcSplitFactor == 1) {
                            leftRandomnessComponent = 0;
                        }
                        leftRandomnessComponent *= -1;
                        for (int lcsi = 0; lcsi < lcSplitFactor; lcsi++) {
                            leftRandomnessComponent *= -1;
                            double rightRandomnessComponent = oldProb / divFactor * RandomDisturbance.generateRandomDisturbance();
                            if (rcSplitFactor == 1) {
                                rightRandomnessComponent = 0;
                            }
                            rightRandomnessComponent *= -1;
                            for (int rcsi = 0; rcsi < rcSplitFactor; rcsi++) {
                                rightRandomnessComponent *= -1;

                                int nlcsi = olcsi * lcSplitFactor + lcsi;
                                int nrcsi = orcsi * rcSplitFactor + rcsi;
                                int npsi = opsi * pSplitFactor + psi;
                                newRuleProbs[nlcsi][nrcsi][npsi] = oldProb / divFactor + leftRandomnessComponent + rightRandomnessComponent;

                            }

                        }

                    }
                }
            }
        }
        ruleProbs = newRuleProbs;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BinaryRule other = (BinaryRule) obj;
        if (this.parent != other.parent) {
            return false;
        }
        if (this.leftChild != other.leftChild) {
            return false;
        }
        if (this.rightChild != other.rightChild) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 29 * hash + this.leftChild;
        hash = 29 * hash + this.rightChild;
        return hash;
    }

//    public final void resetRuleCounts(int[] numStates) {
//        if (ArrayMath.hasSize(ruleCounts, numStates[leftChild], numStates[rightChild], numStates[parent])) {
//            ArrayMath.fill(ruleCounts, 0);
//        } else {
//            ruleCounts = ArrayMath.initArray(ruleCounts, numStates[leftChild], numStates[rightChild], numStates[parent]);
//        }
//    }
//
//    public void resetRuleProbs(int[] numStates) {
//        if (ArrayMath.hasSize(ruleProbs, numStates[leftChild], numStates[rightChild], numStates[parent])) {
//            ArrayMath.fill(ruleProbs, 0);
//        } else {
//            ruleProbs = ArrayMath.initArray(ruleProbs, numStates[leftChild], numStates[rightChild], numStates[parent]);
//        }
//    }
    public synchronized void addCounts(int[] numStates, double[] pOScores, double[] lIScores, double[] rIScores, double treeScore, double scalingFactor) {
        double scalingScore = scalingFactor / treeScore;
        double[][] ruleScores2d, ruleCounts2d;
        double[] ruleScores1d, ruleCounts1d;

        for (int lcsi = 0; lcsi < numStates[leftChild]; lcsi++) {
            double leftChildScore = lIScores[lcsi];
            if (leftChildScore == 0 || ruleProbs[lcsi] == null) {
                continue;
            }
            double tempScore1 = scalingScore * leftChildScore;
            ruleScores2d = ruleProbs[lcsi];
            ruleCounts2d = ruleCounts[lcsi];
            for (int rcsi = 0; rcsi < numStates[rightChild]; rcsi++) {
                double rightChildScore = rIScores[rcsi];
                if (rightChildScore == 0 || ruleScores2d[rcsi] == null) {
                    continue;
                }
                double tempScore2 = tempScore1 * rightChildScore;
                ruleScores1d = ruleScores2d[rcsi];
                ruleCounts1d = ruleCounts2d[rcsi];
                for (int psi = 0; psi < numStates[parent]; psi++) {
                    double parentOScore = pOScores[psi];
                    double ruleScore = ruleScores1d[psi];

                    if (parentOScore == 0 || ruleScore == 0) {
                        continue;
                    }

                    double ruleCount = tempScore2 * ruleScore * parentOScore;
                    ruleCounts1d[psi] += ruleCount;
                }
            }
        }
    }

    public synchronized void addCount(int[] numStates, double count) {
        assert numStates[parent] == 1 && numStates[leftChild] == 1 && numStates[rightChild] == 1;
        ruleCounts[0][0][0] += count;
    }

    public BinaryRule(int parent, int leftChild, int rightChild) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.parent = parent;
        ruleProbs = new double[1][1][1];
    }

    public BinaryRule getCoarseRule(int[] coarseNumStates, int[] numStates, int[][] fine2coarseMapping) {
        BinaryRule coarseRule = new BinaryRule(parent, leftChild, rightChild);

        double[][][] coarseRuleCounts = new double[coarseNumStates[leftChild]][coarseNumStates[rightChild]][coarseNumStates[parent]];
        coarseRule.setRuleCounts(coarseRuleCounts);

        for (int flcsi = 0; flcsi < numStates[leftChild]; flcsi++) {
            if (ruleCounts[flcsi] == null) {
                continue;
            }
            int clcsi = fine2coarseMapping[leftChild][flcsi];
            for (int frcsi = 0; frcsi < numStates[rightChild]; frcsi++) {
                if (ruleCounts[flcsi][frcsi] == null) {
                    continue;
                }
                int crcsi = fine2coarseMapping[rightChild][frcsi];
                for (int fpsi = 0; fpsi < numStates[parent]; fpsi++) {
                    if (ruleCounts[flcsi][frcsi][fpsi] == 0) {
                        continue;
                    }
                    int cpsi = fine2coarseMapping[parent][fpsi];
                    coarseRuleCounts[clcsi][crcsi][cpsi] += ruleCounts[flcsi][frcsi][fpsi];
                }
            }
        }
        return coarseRule;
    }

    public void setRuleProbs(double[][][] ruleProbs) {
        this.ruleProbs = ruleProbs;
    }

    public double[][][] getRuleProbs() {
        return ruleProbs;
    }

    public int getLeftChild() {
        return leftChild;
    }

    public int getRightChild() {
        return rightChild;
    }

    public String toString(List<String> nodeList) {
        return "(" + nodeList.get(parent) + "->" + nodeList.get(leftChild) + " " + nodeList.get(rightChild) + ")";
    }

    @Override
    public String toString() {
        return "(" + parent + "->" + leftChild + " " + rightChild + ")";
    }

    public double[][][] getRuleCounts() {
        return ruleCounts;
    }

    public void setRuleCounts(double[][][] ruleCounts) {
        this.ruleCounts = ruleCounts;
    }

    public void mergeStates(int[] oldNumStates, int[] newNumStates, int[][] fine2coarseMapping) {
        double[][][] newRuleCounts = new double[newNumStates[leftChild]][newNumStates[rightChild]][newNumStates[parent]];

        int opStateNum = oldNumStates[parent];
        int olcStateNum = oldNumStates[leftChild];
        int orcStateNum = oldNumStates[rightChild];

        for (int olcsi = 0; olcsi < olcStateNum; olcsi++) {
            if (ruleCounts[olcsi] == null) {
                continue;
            }
            int nlcsi = fine2coarseMapping[leftChild][olcsi];
            for (int orcsi = 0; orcsi < orcStateNum; orcsi++) {
                if (ruleCounts[olcsi][orcsi] == null) {
                    continue;
                }
                int nrcsi = fine2coarseMapping[rightChild][orcsi];
                for (int opsi = 0; opsi < opStateNum; opsi++) {
                    if (ruleCounts[olcsi][orcsi][opsi] == 0) {
                        continue;
                    }
                    int npsi = fine2coarseMapping[parent][opsi];
                    newRuleCounts[nlcsi][nrcsi][npsi] += ruleCounts[olcsi][orcsi][opsi];
                }
            }
        }
        ruleCounts = newRuleCounts;
    }

    public void filterScores() {
        ruleProbs = ArrayMath.filterMatrix(ruleProbs, Grammar.ruleFilteringThreshold);
    }

    public void filterCounts() {
        ruleCounts = ArrayMath.filterMatrix(ruleCounts, Grammar.ruleFilteringThreshold);
    }

    public int compareTo(Object t) {
        return ((Integer) hashCode()).compareTo(((BinaryRule) t).hashCode());
    }

    /**
     * @param onScores indicate whether the normalization is based on rule
     * scores (otherwise on counts)
     */
    public void normalize(int numStates[], double[][] nodeCounts) {
        double[][][] counts = ruleCounts;
        double[] stateCounts = nodeCounts[parent];
        ruleProbs = ArrayMath.initArray(ruleProbs, ruleCounts);
        for (int lsi = 0; lsi < numStates[leftChild]; lsi++) {
            if (counts[lsi] == null) {
                continue;
            }
            for (int rsi = 0; rsi < numStates[rightChild]; rsi++) {
                if (counts[lsi][rsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < numStates[parent]; psi++) {
                    if (stateCounts[psi] == 0) {
                        ruleProbs[lsi][rsi][psi] = 0;
                    } else {
                        ruleProbs[lsi][rsi][psi] = counts[lsi][rsi][psi] / stateCounts[psi];
                    }
                }
            }
        }
    }

    public void compFeatNum(int[] featNum, int[] numStates) {
        double[][][] counts = ruleCounts;
        for (int lsi = 0; lsi < numStates[leftChild]; lsi++) {
            if (counts[lsi] == null) {
                continue;
            }
            for (int rsi = 0; rsi < numStates[rightChild]; rsi++) {
                if (counts[lsi][rsi] == null) {
                    continue;
                }
                featNum[0] += numStates[parent];
            }
        }
    }

    /**
     * Compute the gradient of the auxiliary function
     *
     * @param gradient
     * @param featIndex
     */
    public void compGradient(int[] numStates, double[][] nodeCounts, double[] gradient, int[] featIndex) {
        double[] stateCounts = nodeCounts[parent];
        double[][][] counts = ruleCounts;
        for (int lcsi = 0; lcsi < numStates[leftChild]; lcsi++) {
            if (counts[lcsi] == null) {
                continue;
            }
            for (int rcsi = 0; rcsi < numStates[rightChild]; rcsi++) {
                if (counts[lcsi][rcsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < numStates[parent]; psi++) {
                    gradient[featIndex[0]] = stateCounts[psi] * ruleProbs[lcsi][rcsi][psi] - counts[lcsi][rcsi][psi];
                    featIndex[0]++;
                }
            }
        }
    }

    public void compObjective(int[] numStates, double[] objectArray) {
        double[][][] counts = ruleCounts;
        double tmp = 0;
        for (int lsi = 0; lsi < numStates[leftChild]; lsi++) {
            if (counts[lsi] == null) {
                continue;
            }
            for (int rsi = 0; rsi < numStates[rightChild]; rsi++) {
                if (counts[lsi][rsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < numStates[parent]; psi++) {
                    if (counts[lsi][rsi][psi] > 0) {
                        tmp += counts[lsi][rsi][psi] * Math.log(ruleProbs[lsi][rsi][psi]);
                    }
                }
            }
        }
        objectArray[0] += tmp;
    }

    public void updateFeatureRichProb(int[] numStates, double[] weight, int[] featIndex, double[][] nodeProbSum) {
        if (nodeProbSum[parent] == null) {
            nodeProbSum[parent] = new double[numStates[parent]];
        }
        double[] denominator = nodeProbSum[parent];
        ruleProbs = ArrayMath.initArray(ruleProbs, ruleCounts);
        for (int lsi = 0; lsi < numStates[leftChild]; lsi++) {
            if (ruleProbs[lsi] == null) {
                continue;
            }
            for (int rsi = 0; rsi < numStates[rightChild]; rsi++) {
                if (ruleProbs[lsi][rsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < numStates[parent]; psi++) {
                    ruleProbs[lsi][rsi][psi] = Math.exp(weight[featIndex[0]]);
                    denominator[psi] += ruleProbs[lsi][rsi][psi];
                    featIndex[0]++;
                }
            }
        }
    }

    public void normFeatureRichProb(int[] numStates, double[][] nodeProbSum) {
        double[] denominator = nodeProbSum[parent];
        if (denominator == null) {
            throw new RuntimeException("denominator = null");
        }
        for (int lcsi = 0; lcsi < numStates[leftChild]; lcsi++) {
            if (ruleProbs[lcsi] == null) {
                continue;
            }
            for (int rcsi = 0; rcsi < numStates[rightChild]; rcsi++) {
                if (ruleProbs[lcsi][rcsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < numStates[parent]; psi++) {
                    ruleProbs[lcsi][rcsi][psi] /= denominator[psi];
                }
            }
        }
    }

    public void compFeatureRichWeightSumCount(int[] numStates, double[][][] stateWeightStats) {
        if (stateWeightStats[parent] == null) {
            stateWeightStats[parent] = new double[numStates[parent]][2];
        }
        double[][] weightStats = stateWeightStats[parent];

        for (int lsi = 0; lsi < numStates[leftChild]; lsi++) {
            if (ruleProbs[lsi] == null) {
                continue;
            }
            for (int rsi = 0; rsi < numStates[rightChild]; rsi++) {
                if (ruleProbs[lsi][rsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < numStates[parent]; psi++) {
                    double tmp = Math.log(ruleProbs[lsi][rsi][psi]);
                    if (tmp < FeaturedLexiconManager.minimumFeatureWeight) {
                        tmp = FeaturedLexiconManager.minimumFeatureWeight;
                    }
                    weightStats[psi][0] += tmp;
                    weightStats[psi][1]++;
                }
            }
        }
    }

    public void initFeatureRichWeight(int[] numStates, double[] weights, int[] featIndex, double[][][] stateWeightStats) {
        double[][] weightStates = stateWeightStats[parent];
        for (int lsi = 0; lsi < numStates[leftChild]; lsi++) {
            if (ruleProbs[lsi] == null) {
                continue;
            }
            for (int rsi = 0; rsi < numStates[rightChild]; rsi++) {
                if (ruleProbs[lsi][rsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < numStates[parent]; psi++) {
                    double tmp = Math.log(ruleProbs[lsi][rsi][psi]);
                    if (tmp < FeaturedLexiconManager.minimumFeatureWeight) {
                        tmp = FeaturedLexiconManager.minimumFeatureWeight;
                        ruleProbs[lsi][rsi][psi] = Math.exp(tmp);
                    }
                    weights[featIndex[0]] = tmp - weightStates[psi][0];
                    featIndex[0]++;
                }
            }
        }
    }

    public void printFeatWeights(int[] numStates, double[][] nodeCounts) {
        System.out.println(toString());
        double[] stateCounts = nodeCounts[parent];
        for (int lsi = 0; lsi < numStates[leftChild]; lsi++) {
            if (ruleProbs[lsi] == null) {
                continue;
            }
            for (int rsi = 0; rsi < numStates[rightChild]; rsi++) {
                if (ruleProbs[lsi][rsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < numStates[parent]; psi++) {
                    double tmp = Math.log(stateCounts[psi] * ruleProbs[lsi][rsi][psi]);
                    System.out.printf("%d->%d,%d %f\n", psi, lsi, rsi, tmp);
                }
            }
        }
    }
    
    public void tallyNodeCounts(int[] numStates, double[][] nodeCounts) {
        int pStateNum = numStates[parent];
        int lcStateNum = numStates[leftChild];
        int rcStateNum = numStates[rightChild];

        double[] stateCounts = nodeCounts[parent];
        for (int lcsi = 0; lcsi < lcStateNum; lcsi++) {
            if (ruleCounts[lcsi] == null) {
                continue;
            }
            for (int rcsi = 0; rcsi < rcStateNum; rcsi++) {
                if (ruleCounts[lcsi][rcsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < pStateNum; psi++) {
                    stateCounts[psi] += ruleCounts[lcsi][rcsi][psi];
                }
            }
        }
    }

    public void tallyNodeEntropy(int[] numStates, double[][] nodeEntropy) {
        int pStateNum = numStates[parent];
        int lcStateNum = numStates[leftChild];
        int rcStateNum = numStates[rightChild];

        double[] stateEntropy = nodeEntropy[parent];
        for (int lcsi = 0; lcsi < lcStateNum; lcsi++) {
            if (ruleProbs[lcsi] == null) {
                continue;
            }
            for (int rcsi = 0; rcsi < rcStateNum; rcsi++) {
                if (ruleProbs[lcsi][rcsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < pStateNum; psi++) {
                    double prob = ruleProbs[lcsi][rcsi][psi];
                    if (prob > Grammar.ruleFilteringThreshold) {
                        stateEntropy[psi] += -prob * Math.log(prob);
                    }
                }
            }
        }
    }

    public void smoothProbs(int[] numStates, double[][][] smoothingMatrix) {
        double[][] stateSmoothingMatrix = smoothingMatrix[parent];

        double[][][] newRuleScores = ArrayMath.initArray(null, ruleProbs);
        for (int lsi = 0; lsi < numStates[leftChild]; lsi++) {
            if (ruleProbs[lsi] == null) {
                continue;
            }
            for (int rsi = 0; rsi < numStates[rightChild]; rsi++) {
                if (ruleProbs[lsi][rsi] == null) {
                    continue;
                }
                for (int psi = 0; psi < numStates[parent]; psi++) {
                    for (int opsi = 0; opsi < numStates[parent]; opsi++) {
                        newRuleScores[lsi][rsi][psi] += stateSmoothingMatrix[psi][opsi] * ruleProbs[lsi][rsi][opsi];
                    }
                }
            }
        }
        if (newRuleScores == null) {
            throw new Error();
        }
        ruleProbs = newRuleScores;
    }

    public BinaryRule copy() {
        BinaryRule rule = new BinaryRule(parent, leftChild, rightChild);
        rule.ruleProbs = ArrayUtil.clone(ruleProbs);
        return rule;
    }
}
