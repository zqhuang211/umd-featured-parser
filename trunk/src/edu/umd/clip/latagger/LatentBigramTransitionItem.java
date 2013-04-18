/*
 * LatentBigramTransitionItem.java
 *
 * Created on May 22, 2007, 9:43 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.math.Operator;
import edu.umd.clip.math.RandomDisturbance;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Zhongqiang Huang
 */
public class LatentBigramTransitionItem implements Serializable {

    private static final long serialVersionUID = 1L;
    private double[][] latentScore;
    private int currTagStateNum;
    private int prevTagStateNum;
    private double surfaceScore;

    public LatentBigramTransitionItem(double surfaceScore) {
        this.surfaceScore = surfaceScore;
        currTagStateNum = 1;
        prevTagStateNum = 1;
        latentScore = new double[prevTagStateNum][currTagStateNum];
        latentScore[0][0] = surfaceScore;
    }

    public LatentBigramTransitionItem(int prevTagStateNum, int currTagStateNum) {
        this.surfaceScore = 0;
        this.prevTagStateNum = prevTagStateNum;
        this.currTagStateNum = currTagStateNum;
        latentScore = new double[prevTagStateNum][currTagStateNum];
        for (int pts = 0; pts < prevTagStateNum; pts++) {
            Arrays.fill(latentScore[pts], 0.0);
        }
    }

    public LatentBigramTransitionItem(LatentBigramTransitionItem anotherItem) {
        prevTagStateNum = anotherItem.prevTagStateNum;
        currTagStateNum = anotherItem.currTagStateNum;
        surfaceScore = anotherItem.surfaceScore;
        latentScore = new double[prevTagStateNum][currTagStateNum];
        for (int pts = 0; pts < prevTagStateNum; pts++) {
            for (int cts = 0; cts < currTagStateNum; cts++) {
                latentScore[pts][cts] = anotherItem.latentScore[pts][cts];
            }
        }
    }

    public void increase(int prevTagState, int currTagState, double amount) {
        latentScore[prevTagState][currTagState] += amount;
    }

    public void increase(double[][] amount) {
        for (int psi = 0; psi < prevTagStateNum; psi++) {
            for (int csi = 0; csi < currTagStateNum; csi++) {
                latentScore[psi][csi] += amount[psi][csi];
            }
        }
    }

    public void applyOperator(Operator operator) {
        for (int pts = 0; pts < prevTagStateNum; pts++) {
            for (int cts = 0; cts < currTagStateNum; cts++) {
                latentScore[pts][cts] = (Double) operator.applyOperator(latentScore[pts][cts]);
            }
        }
    }

    public void splitStates() {
        splitStates(2, 2);
    }

    public int getCurrTagStateNum() {
        return currTagStateNum;
    }

    public int getPrevTagStateNum() {
        return prevTagStateNum;
    }

    public void normalize(double[] normalizationFactor) {
        for (int pts = 0; pts < prevTagStateNum; pts++) {
            for (int cts = 0; cts < currTagStateNum; cts++) {
                if (latentScore[pts][cts] != 0) {
                    latentScore[pts][cts] /= normalizationFactor[pts];
                }
                if (Double.isInfinite(latentScore[pts][cts]) || Double.isNaN(latentScore[pts][cts])) {
                    latentScore[pts][cts] = 1.0 / LatentTrainer.SCALE;
                }
            }
        }
    }

    public void splitStates(int prevTagSplitFactor, int currTagSplitFactor) {
        double[][] newLatentScore = new double[prevTagStateNum * prevTagSplitFactor][currTagStateNum * currTagSplitFactor];
        for (int prevTagOld = 0; prevTagOld < prevTagStateNum; prevTagOld++) {
            for (int currTagOld = 0; currTagOld < currTagStateNum; currTagOld++) {
                double oldScore = latentScore[prevTagOld][currTagOld];
                for (int i = 0; i < prevTagSplitFactor; i++) {
                    double randomnessComponent = 0;
                    if (currTagSplitFactor > 1) {
                        randomnessComponent = oldScore / currTagSplitFactor * RandomDisturbance.generateRandomDisturbance();
                    }
                    for (int j = 0; j < currTagSplitFactor; j++) {
                        if (j == 1) {
                            randomnessComponent *= -1;
                        }
                        int prevTagNew = prevTagOld * prevTagSplitFactor + i;
                        int currTagNew = currTagOld * currTagSplitFactor + j;
                        newLatentScore[prevTagNew][currTagNew] = oldScore / currTagSplitFactor + randomnessComponent;
                    }
                }
            }
        }
        latentScore = newLatentScore;
        prevTagStateNum *= prevTagSplitFactor;
        currTagStateNum *= currTagSplitFactor;
    }

    public void duplicateStates(int prevTagSplitFactor, int currTagSplitFactor) {
        double[][] newLatentScore = new double[prevTagStateNum * prevTagSplitFactor][currTagStateNum * currTagSplitFactor];
        for (int prevTagOld = 0; prevTagOld < prevTagStateNum; prevTagOld++) {
            for (int currTagOld = 0; currTagOld < currTagStateNum; currTagOld++) {
                double oldScore = latentScore[prevTagOld][currTagOld];
                for (int i = 0; i < prevTagSplitFactor; i++) {
                    for (int j = 0; j < currTagSplitFactor; j++) {
                        int prevTagNew = prevTagOld * prevTagSplitFactor + i;
                        int currTagNew = currTagOld * currTagSplitFactor + j;
                        newLatentScore[prevTagNew][currTagNew] = oldScore;
                    }
                }
            }
        }
        latentScore = newLatentScore;
        prevTagStateNum *= prevTagSplitFactor;
        currTagStateNum *= currTagSplitFactor;
    }

    public void setLatentScore(double[][] latentScore) {
        this.latentScore = latentScore;
    }

    public double[][] getLatentScores() {
        return latentScore;
    }

    public double getLatentScore(int prevTagState, int currTagState) {
        return latentScore[prevTagState][currTagState];
    }

    public void setLatentScore(int prevTagState, int currTagState, double score) {
        latentScore[prevTagState][currTagState] = score;
    }

    public void setSurfaceScore(double surfaceScore) {
        this.surfaceScore = surfaceScore;
    }

    public double getSurfaceScore() {
        return surfaceScore;
    }

    public void clearScores() {
        for (int pts = 0; pts < prevTagStateNum; pts++) {
            Arrays.fill(latentScore[pts], 0.0);
        }
    }

    public void simplySplitStates(int prevTagSplitFactor, int currTagSplitFactor) {
        prevTagStateNum *= prevTagSplitFactor;
        currTagStateNum *= currTagSplitFactor;
        latentScore = new double[prevTagStateNum][currTagStateNum];
        ArrayMath.fill(latentScore, 0.0);
    }

    public void smooth(double smoothingParam) {
        for (int cts = 0; cts < currTagStateNum; cts++) {
            double sum = 0;
            for (int pts = 0; pts < prevTagStateNum; pts++) {
                sum += latentScore[pts][cts];
            }
            double mean = sum / prevTagStateNum;
            for (int pts = 0; pts < prevTagStateNum; pts++) {
                latentScore[pts][cts] = (1 - smoothingParam) * latentScore[pts][cts] + smoothingParam * mean;
            }
        }
    }

    public void smooth(double smoothingParam, double unigram) {
        for (int cts = 0; cts < currTagStateNum; cts++) {
            for (int pts = 0; pts < prevTagStateNum; pts++) {
                latentScore[pts][cts] = (1 - smoothingParam) * latentScore[pts][cts] + smoothingParam * unigram / currTagStateNum;
            }
        }
    }

    public void smoothNormalization() {
        double sum = 0;
        for (int pts = 0; pts < prevTagStateNum; pts++) {
            for (int cts = 0; cts < currTagStateNum; cts++) {
                sum += latentScore[pts][cts];
            }
        }
        double mean = sum / (prevTagStateNum * currTagStateNum);
        for (int pts = 0; pts < prevTagStateNum; pts++) {
            for (int cts = 0; cts < currTagStateNum; cts++) {
                latentScore[pts][cts] = (1 - LatentTrainer.SMOOTHING_ALPHA) * latentScore[pts][cts] + LatentTrainer.SMOOTHING_ALPHA * mean;
            }
        }
    }

    public void mergeStates(boolean[] prevTagMergeSignal, boolean[] currTagMergeSignal) {
        int newPrevTagStateNum = 0;
        int[] prevTagStateMapping = new int[prevTagStateNum];
        if (prevTagMergeSignal == null) {
            for (int ts = 0; ts < prevTagStateNum; ts++) {
                prevTagStateMapping[ts] = ts;
            }
            newPrevTagStateNum = prevTagStateNum;
        } else {
            if (prevTagMergeSignal.length * 2 != prevTagStateNum) {
                throw new RuntimeException("Error: latent state number does not match.");
            }
            for (int ots = 0; ots < prevTagMergeSignal.length; ots++) {
                if (prevTagMergeSignal[ots]) {
                    prevTagStateMapping[2 * ots] = newPrevTagStateNum;
                    prevTagStateMapping[2 * ots + 1] = newPrevTagStateNum;
                    newPrevTagStateNum++;
                } else {
                    prevTagStateMapping[2 * ots] = newPrevTagStateNum;
                    newPrevTagStateNum++;
                    prevTagStateMapping[2 * ots + 1] = newPrevTagStateNum;
                    newPrevTagStateNum++;
                }
            }
        }

        int newCurrTagStateNum = 0;
        int[] currTagStateMapping = new int[currTagStateNum];
        if (currTagMergeSignal == null) {
            for (int ts = 0; ts < currTagStateNum; ts++) {
                currTagStateMapping[ts] = ts;
            }
            newCurrTagStateNum = currTagStateNum;
        } else {
            if (currTagMergeSignal.length * 2 != currTagStateNum) {
                throw new RuntimeException("Error: latent state number does not match.");
            }
            for (int ots = 0; ots < currTagMergeSignal.length; ots++) {
                if (currTagMergeSignal[ots]) {
                    currTagStateMapping[2 * ots] = newCurrTagStateNum;
                    currTagStateMapping[2 * ots + 1] = newCurrTagStateNum;
                    newCurrTagStateNum++;
                } else {
                    currTagStateMapping[2 * ots] = newCurrTagStateNum;
                    newCurrTagStateNum++;
                    currTagStateMapping[2 * ots + 1] = newCurrTagStateNum;
                    newCurrTagStateNum++;
                }
            }
        }

        double[][] newLatentScore = new double[newPrevTagStateNum][newCurrTagStateNum];
        ArrayMath.fill(newLatentScore, 0.0);
        for (int pts = 0; pts < prevTagStateNum; pts++) {
            int npts = prevTagStateMapping[pts];
            for (int cts = 0; cts < currTagStateNum; cts++) {
                int ncts = currTagStateMapping[cts];
                newLatentScore[npts][ncts] += latentScore[pts][cts];
            }
        }

        prevTagStateNum = newPrevTagStateNum;
        currTagStateNum = newCurrTagStateNum;
        latentScore = newLatentScore;
    }

    public void mergeStates(List<Tree<Integer>> prevSplitTreeList, List<Tree<Integer>> currSplitTreeList, double[][] finerLatentScores) {
        if (prevTagStateNum != prevSplitTreeList.size() ||
                currTagStateNum != currSplitTreeList.size()) {
            throw new RuntimeException("Error: latent state number does not match.");
        }
        latentScore = new double[prevTagStateNum][currTagStateNum];
        for (int psi = 0; psi < prevTagStateNum; psi++) {
            for (int csi = 0; csi < currTagStateNum; csi++) {
                for (Tree<Integer> finerPrevChild : prevSplitTreeList.get(psi).getChildren()) {
                    for (Tree<Integer> finerCurrChild : currSplitTreeList.get(csi).getChildren()) {
                        latentScore[psi][csi] += finerLatentScores[finerPrevChild.getLabel()][finerCurrChild.getLabel()];
                    }
                }
            }
        }
    }

    public void simplyMergeStates(boolean[] prevTagMergeSignal, boolean[] currTagMergeSignal) {
        int newPrevTagStateNum = 0;
        if (prevTagMergeSignal == null) {
            newPrevTagStateNum = prevTagStateNum;
        } else {
            if (prevTagMergeSignal.length * 2 != prevTagStateNum) {
                throw new RuntimeException("Error: latent state number does not match.");
            }
            for (int ots = 0; ots < prevTagMergeSignal.length; ots++) {
                if (prevTagMergeSignal[ots]) {
                    newPrevTagStateNum += 1;
                } else {
                    newPrevTagStateNum += 2;
                }
            }
        }

        int newCurrTagStateNum = 0;
        if (currTagMergeSignal == null) {
            newCurrTagStateNum = currTagStateNum;
        } else {
            if (currTagMergeSignal.length * 2 != currTagStateNum) {
                throw new RuntimeException("Error: latent state number does not match.");
            }
            for (int ots = 0; ots < currTagMergeSignal.length; ots++) {
                if (currTagMergeSignal[ots]) {
                    newCurrTagStateNum += 1;
                } else {
                    newCurrTagStateNum += 2;
                }
            }
        }

        double[][] newLatentScore = new double[newPrevTagStateNum][newCurrTagStateNum];
        ArrayMath.fill(newLatentScore, 0.0);
    }
}
