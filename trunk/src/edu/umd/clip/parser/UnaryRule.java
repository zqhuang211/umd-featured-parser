/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.math.RandomDisturbance;
import edu.umd.clip.util.ArrayUtil;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 *
 * @author zqhuang
 */
public class UnaryRule extends Rule implements Serializable, Comparable {

    private static final long serialVersionUID = 1L;
    private int child;
    private double[][] ruleProbs = null;
    private double[][] ruleCounts = null;
    private int intermediate = -1;

    public UnaryRule(int parent, int child) {
        this.parent = parent;
        this.child = child;
        ruleProbs = new double[1][1];
    }

    public void takeLogarithm(int[] numStates) {
        int pStateNum = numStates[parent];
        int cStateNum = numStates[child];
        for (int csi = 0; csi < cStateNum; csi++) {
            if (ruleProbs[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < pStateNum; psi++) {
                ruleProbs[csi][psi] = Math.log(ruleProbs[csi][psi]);
            }
        }
    }

    public void setIntermediate(int intermediate) {
        this.intermediate = intermediate;
    }

    public int getIntermediate() {
        return intermediate;
    }

    public int getZeroRuleNum(int[] numStates) {
        int numZeros = 0;
        for (int csi = 0; csi < numStates[child]; csi++) {
            if (ruleProbs[csi] == null) {
                numZeros += numStates[parent];
            } else {
                for (int psi = 0; psi < numStates[parent]; psi++) {
                    if (ruleProbs[csi][psi] == 0) {
                        numZeros++;
                    }
                }
            }
        }
        return numZeros;
    }

    public int matrixSize(int[] numStates) {
        int cStateNum = numStates[child];
        int pStateNum = numStates[parent];
        int size = 0;
        for (int csi = 0; csi < cStateNum; csi++) {
            if (ruleProbs[csi] == null) {
                continue;
            }
            size += pStateNum;
        }
        return size;
    }

    public void smoothProbs(int[] numStates, double[][][] smoothingMatrix) {
        double[][] stateSmoothingMatrix = smoothingMatrix[parent];
        double[][] newRuleProbs = ArrayMath.initArray(null, ruleProbs);
        for (int csi = 0; csi < numStates[child]; csi++) {
            if (ruleProbs[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < numStates[parent]; psi++) {
                for (int opsi = 0; opsi < numStates[parent]; opsi++) {
                    newRuleProbs[csi][psi] += stateSmoothingMatrix[psi][opsi] * ruleProbs[csi][opsi];
                }
            }
        }
        ruleProbs = newRuleProbs;
    }

    public void normalize(int[] numStates, double[][] nodeCounts) {
        ruleProbs = ArrayMath.initArray(ruleProbs, ruleCounts);
        double[] stateCounts = nodeCounts[parent];
        for (int csi = 0; csi < numStates[child]; csi++) {
            if (ruleCounts[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < numStates[parent]; psi++) {
                double parentCount = stateCounts[psi];
                if (parentCount == 0) {
                    ruleProbs[csi][psi] = 0;
                } else {
                    ruleProbs[csi][psi] = ruleCounts[csi][psi] / parentCount;
                }
            }
        }
    }

    public void compFeatNum(int[] featNum, int[] numStates) {
        double[][] counts = ruleCounts;

        for (int csi = 0; csi < numStates[child]; csi++) {
            if (counts[csi] == null) {
                continue;
            }
            featNum[0] += numStates[parent];
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
        int pnsn = numStates[parent];
        int cnsn = numStates[child];
        double[][] counts = ruleCounts;

        for (int csi = 0; csi < cnsn; csi++) {
            if (counts[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < pnsn; psi++) {
                gradient[featIndex[0]] = stateCounts[psi] * ruleProbs[csi][psi] - counts[csi][psi];
                featIndex[0]++;
            }
        }
    }

    public void compObjective(int[] numStates, double[] objectArray) {
        int pnsn = numStates[parent];
        int cnsn = numStates[child];
        double[][] counts = ruleCounts;
        double tmp = 0;
        for (int csi = 0; csi < cnsn; csi++) {
            if (counts[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < pnsn; psi++) {
                if (counts[csi][psi] > 0) {
                    tmp += counts[csi][psi] * Math.log(ruleProbs[csi][psi]);
                }

            }
        }
//        System.out.println(toString()+"\t"+ tmp);
        objectArray[0] += tmp;
    }

    /**
     * Compute the nominator of the logistic models.
     *
     * @param weight
     * @param featIndex
     */
    public void updateFeatureRichProb(int[] numStates, double[] weight, int[] featIndex, double[][] nodeProbSum) {
        if (nodeProbSum[parent] == null) {
            nodeProbSum[parent] = new double[numStates[parent]];
        }
        double[] denominator = nodeProbSum[parent];
        ruleProbs = ArrayMath.initArray(ruleProbs, ruleCounts);
        for (int csi = 0; csi < numStates[child]; csi++) {
            if (ruleProbs[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < numStates[parent]; psi++) {
                ruleProbs[csi][psi] = Math.exp(weight[featIndex[0]]);
                denominator[psi] += ruleProbs[csi][psi];
                featIndex[0]++;
            }
        }
    }

    public void normFeatureRichProb(int[] numStates, double[][] nodeProbSum) {
        double[] denominator = nodeProbSum[parent];
        if (denominator == null) {
            throw new RuntimeException("denominator = null");
        }
        for (int csi = 0; csi < numStates[child]; csi++) {
            if (ruleProbs[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < numStates[parent]; psi++) {
                ruleProbs[csi][psi] /= denominator[psi];
            }
        }
    }

    public void compFeatureRichWeightSumCount(int[] numStates, double[][][] latenTagWeightStats) {
        if (latenTagWeightStats[parent] == null) {
            latenTagWeightStats[parent] = new double[numStates[parent]][2];
        }
        double[][] weightStats = latenTagWeightStats[parent];
        for (int csi = 0; csi < numStates[child]; csi++) {
            if (ruleProbs[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < numStates[parent]; psi++) {
                double tmp = Math.log(ruleProbs[csi][psi]);
                if (tmp < FeaturedLexiconManager.minimumFeatureWeight) {
                    tmp = FeaturedLexiconManager.minimumFeatureWeight;
                }
                weightStats[psi][0] += tmp;
                weightStats[psi][1]++;
            }
        }
    }

    public void initFeatureRichWeight(int[] numStates, double[] weights, int[] featIndex, double[][][] stateFeatWeightStats) {
        double[][] weightStates = stateFeatWeightStats[parent];
        for (int csi = 0; csi < numStates[child]; csi++) {
            if (ruleProbs[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < numStates[parent]; psi++) {
//                double tmp = Math.log(ruleScores[csi][psi]);

                double tmp = Math.log(ruleProbs[csi][psi]);
                if (tmp < FeaturedLexiconManager.minimumFeatureWeight) {
                    tmp = FeaturedLexiconManager.minimumFeatureWeight;
                    ruleProbs[csi][psi] = Math.exp(tmp);
                }
                weights[featIndex[0]] = tmp - weightStates[psi][0];
                featIndex[0]++;
            }
        }
    }

    public void printFeatWeights(int[] numStates, double[][] nodeCounts) {
        double[] stateCounts = nodeCounts[parent];
        System.out.println(toString());
        for (int csi = 0; csi < numStates[child]; csi++) {
            if (ruleProbs[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < numStates[parent]; psi++) {
                double tmp = Math.log(stateCounts[psi] * ruleProbs[csi][psi]);
                System.out.printf("%d->%d %f\n", psi, csi, tmp);
            }
        }
    }

    public void tallyNodeCounts(int[] numStates, double[][] nodeCounts) {
        double[] stateCounts = nodeCounts[parent];
        int pStateNum = numStates[parent];
        int cStateNum = numStates[child];
        for (int csi = 0; csi < cStateNum; csi++) {
            if (ruleCounts[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < pStateNum; psi++) {
                stateCounts[psi] += ruleCounts[csi][psi];
            }
        }
    }

    public void tallyNodeEntropy(int[] numStates, double[][] nodeEntropy) {
        double[] stateEntropy = nodeEntropy[parent];
        for (int csi = 0; csi < numStates[child]; csi++) {
            if (ruleProbs[csi] == null) {
                continue;
            }
            for (int psi = 0; psi < numStates[parent]; psi++) {
                double prob = ruleProbs[csi][psi];
                if (prob > Grammar.ruleFilteringThreshold) {
                    stateEntropy[psi] += -prob * Math.log(prob);
                }
            }
        }
    }

    public void splitStates(int[] oldNumStates, int[] newNumStates) {
        int pStateNum = oldNumStates[parent];
        int cStateNum = oldNumStates[child];

        int pSplitFactor = newNumStates[parent] / oldNumStates[parent];
        int cSplitFactor = newNumStates[child] / oldNumStates[child];

        double[][] newRuleProbs = new double[newNumStates[child]][newNumStates[parent]];

        for (int ocsi = 0; ocsi < cStateNum; ocsi++) {
            if (ruleProbs[ocsi] == null) {
                continue;
            }
            for (int opsi = 0; opsi < pStateNum; opsi++) {
                double oldProb = ruleProbs[ocsi][opsi];
                if (oldProb == 0) {
                    continue;
                }
                double divFactor = cSplitFactor;
                for (int psi = 0; psi < pSplitFactor; psi++) {
                    double childRandomness = oldProb / divFactor * RandomDisturbance.generateRandomDisturbance();
                    childRandomness *= -1;
                    for (int csi = 0; csi < cSplitFactor; csi++) {
                        childRandomness *= -1;
                        int nlcsi = ocsi * cSplitFactor + csi;
                        int npsi = opsi * pSplitFactor + psi;
                        newRuleProbs[nlcsi][npsi] = oldProb / divFactor + childRandomness;
                    }
                }
            }
        }
        ruleProbs = newRuleProbs;
    }

    public void mergeStates(int[] oldStateNum, int[] newStateNum, int[][] fine2coarseMapping) {
        double[][] newRuleCounts = new double[newStateNum[child]][newStateNum[parent]];
        int opStateNum = oldStateNum[parent];
        int ocStateNum = oldStateNum[child];
        for (int ocsi = 0; ocsi < ocStateNum; ocsi++) {
            if (ruleCounts[ocsi] == null) {
                continue;
            }
            int ncsi = fine2coarseMapping[child][ocsi];
            for (int opsi = 0; opsi < opStateNum; opsi++) {
                if (ruleCounts[ocsi][opsi] == 0) {
                    continue;
                }
                int npsi = fine2coarseMapping[parent][opsi];
                newRuleCounts[ncsi][npsi] += ruleCounts[ocsi][opsi];
            }
        }
        ruleCounts = newRuleCounts;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UnaryRule other = (UnaryRule) obj;
        if (this.parent != other.parent) {
            return false;
        }
        if (this.child != other.child) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.parent;
        hash = 97 * hash + this.child;
        return hash;
    }

    public double[][] getRuleCounts() {
        return ruleCounts;
    }

//    public final void resetRuleCounts(int[] numStates) {
//        if (ArrayMath.hasSize(ruleCounts, numStates[child], numStates[parent])) {
//            ArrayMath.fill(ruleCounts, 0);
//        } else {
//            ruleCounts = ArrayMath.initArray(ruleCounts, numStates[child], numStates[parent]);
//        }
//    }
//
//    public void resetRuleProbs(int[] numStates) {
//        if (ArrayMath.hasSize(ruleProbs, numStates[child], numStates[parent])) {
//            ArrayMath.fill(ruleProbs, 0);
//        } else {
//            ruleProbs = ArrayMath.initArray(ruleProbs, numStates[child], numStates[parent]);
//        }
//    }
    public synchronized void addCounts(int[] numStates, double[] pOScores, double[] cIScores, double treeScore, double scalingFactor) {
        int parentStateNum = numStates[parent];
        int childStateNum = numStates[child];
        double scalingScore = scalingFactor / treeScore;
        double[] ruleScores1d, ruleCounts1d;
        for (int csi = 0; csi < childStateNum; csi++) {
            double childIScore = cIScores[csi];
            if (childIScore == 0 || ruleProbs[csi] == null) {
                continue;
            }
            double tempScore1 = scalingScore * childIScore;
            ruleScores1d = ruleProbs[csi];
            ruleCounts1d = ruleCounts[csi];
            for (int psi = 0; psi < parentStateNum; psi++) {
                double parentOScore = pOScores[psi];
                double ruleScore = ruleScores1d[psi];
                if (parentOScore == 0 || ruleScore == 0) {
                    continue;
                }
                double ruleCount = ruleScore * parentOScore * tempScore1;
                ruleCounts1d[psi] += ruleCount;
            }
        }
    }

    public synchronized void addCounts(int[] numStates, double count) {
        assert numStates[parent] == 1 && numStates[child] == 1;
        ruleCounts[0][0] += count;
    }

    public double[][] getRuleProbs() {
        return ruleProbs;
    }

    public void setRuleProbs(double[][] ruleProbs) {
        this.ruleProbs = ruleProbs;
    }

    public int getChild() {
        return child;
    }

    public String toString(List<String> nodeList) {
        return "(" + nodeList.get(parent) + "->" + nodeList.get(child) + ")";
    }

    @Override
    public String toString() {
        return "(" + parent + "->" + child + ")";
    }

    public int checkZeros(int[] numStates) {
        int zeroCount = 0;
        int parentStateNum = numStates[parent];
        int childStateNum = numStates[child];
        for (int csi = 0; csi < childStateNum; csi++) {
            boolean allZero = true;
            if (ruleProbs[csi] == null) {
                zeroCount += childStateNum;
                continue;
            }
            for (int psi = 0; psi < parentStateNum; psi++) {
                if (ruleProbs[csi][psi] != 0) {
                    allZero = false;
                    break;
                }
            }
            if (allZero) {
                zeroCount += childStateNum;
            }
        }
        return zeroCount;
    }

    public void filterScores() {
        ruleProbs = ArrayMath.filterMatrix(ruleProbs, Grammar.ruleFilteringThreshold);
    }

    public void filterCounts() {
        ruleCounts = ArrayMath.filterMatrix(ruleCounts, Grammar.ruleFilteringThreshold);
    }

    public void setRuleCounts(double[][] ruleCounts) {
        this.ruleCounts = ruleCounts;
    }

    public UnaryRule getCoarseRule(int[] coarseNumStates, int[] numStates, int[][] fine2coarseMapping) {
        UnaryRule coarseRule = new UnaryRule(parent, child);

        double[][] coarseRuleCounts = new double[coarseNumStates[child]][coarseNumStates[parent]];
        coarseRule.setRuleCounts(coarseRuleCounts);
        int pStateNum = numStates[parent];
        int cStateNum = numStates[child];
        for (int fcsi = 0; fcsi < cStateNum; fcsi++) {
            if (ruleCounts[fcsi] == null) {
                continue;
            }
            int ccsi = fine2coarseMapping[child][fcsi];
            for (int fpsi = 0; fpsi < pStateNum; fpsi++) {
                if (ruleCounts[fcsi][fpsi] == 0) {
                    continue;
                }
                int cpsi = fine2coarseMapping[parent][fpsi];
                coarseRuleCounts[ccsi][cpsi] += ruleCounts[fcsi][fpsi];
            }
        }
        return coarseRule;
    }

    public int compareTo(Object t) {
        return ((Integer) hashCode()).compareTo(((UnaryRule) t).hashCode());
    }

    public UnaryRule copy() {
        UnaryRule rule = new UnaryRule(parent, child);
        rule.ruleProbs = ArrayUtil.clone(ruleProbs);
        return rule;
    }
}
