/*
 * LatentUnigramTransitionItem.java
 *
 * Created on May 23, 2007, 2:53 PM
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
public class LatentUnigramTransitionItem implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private double[] latentScores;
    private int tagStateNum;
    private double surfaceScore;

    public LatentUnigramTransitionItem(double surfaceScore) {
        this.surfaceScore = surfaceScore;
        tagStateNum = 1;
        latentScores = new double[tagStateNum];
        latentScores[0] = surfaceScore;
    }

    public LatentUnigramTransitionItem(int tagStateNum) {
        surfaceScore = 0;
        this.tagStateNum = tagStateNum;
        latentScores = new double[tagStateNum];
        Arrays.fill(latentScores, 0.0);
    }

    public LatentUnigramTransitionItem(LatentUnigramTransitionItem anotherItem) {
        tagStateNum = anotherItem.tagStateNum;
        surfaceScore = anotherItem.surfaceScore;
        latentScores = new double[tagStateNum];
        for (int ts = 0; ts < tagStateNum; ts++) {
            latentScores[ts] = anotherItem.latentScores[ts];
        }
    }

    public void applyOperator(Operator operator) {
        for (int ts = 0; ts < tagStateNum; ts++) {
            latentScores[ts] = (Double) operator.applyOperator(latentScores[ts]);
        }
    }

    public void increase(int tagState, double amount) {
        latentScores[tagState] += amount;
    }

    public void increase(double[] amount) {
        for (int i = 0; i < latentScores.length; i++) {
            latentScores[i] += amount[i];
        }
    }

    public void splitStates() {
        splitStates(2);
    }

    public int getTagStateNum() {
        return tagStateNum;
    }

    public void normalize(double normalizationFactor) {
        for (int ts = 0; ts < tagStateNum; ts++) {
            if (latentScores[ts] != 0) {
                latentScores[ts] /= normalizationFactor;
                if (Double.isInfinite(latentScores[ts]) || Double.isNaN(latentScores[ts])) {
                    latentScores[ts] = 1.0 / LatentTrainer.SCALE;
                }
            }
        }
    }

    public void splitStates(int tagSplitFactor) {
        double[] newLatentScore = new double[tagStateNum * tagSplitFactor];
        for (int tagOld = 0; tagOld < tagStateNum; tagOld++) {
            double oldScore = latentScores[tagOld];
            double randomnessComponent = 0;
            if (tagSplitFactor > 1) {
                randomnessComponent = oldScore / tagSplitFactor * RandomDisturbance.generateRandomDisturbance();
            }
            for (int i = 0; i < tagSplitFactor; i++) {
                if (i == 1) {
                    randomnessComponent *= -1;
                }
                int tagNew = tagOld * tagSplitFactor + i;
                newLatentScore[tagNew] = oldScore / tagSplitFactor + randomnessComponent;
            }
        }
        latentScores = newLatentScore;
        tagStateNum *= tagSplitFactor;
    }

    public void setLatentScore(double[] latentScore) {
        this.latentScores = latentScore;
    }

    public double[] getLatentScores() {
        return latentScores;
    }

    public double getLatentScore(int tagState) {
        return latentScores[tagState];
    }

    public void setLatentScore(int tagState, double score) {
        latentScores[tagState] = score;
    }

    public void setSurfaceScore(double surfaceScore) {
        this.surfaceScore = surfaceScore;
    }

    public double getSurfaceScore() {
        return surfaceScore;
    }

    public void clearScores() {
        Arrays.fill(latentScores, 0.0);
    }

    public void simplySplitStates(int tagSplitFactor) {
        tagStateNum *= tagSplitFactor;
        latentScores = new double[tagStateNum];
        ArrayMath.fill(latentScores, 0.0);
    }

    public void mergeStates(boolean[] mergeSignal) {
        if (mergeSignal == null) {
            return;
        }
        int newTagStateNum = 0;
        int[] stateMapping = new int[tagStateNum];
        if (mergeSignal.length * 2 != tagStateNum) {
            throw new RuntimeException("Error: latent state number does not match.");
        }
        for (int ots = 0; ots < mergeSignal.length; ots++) {
            if (mergeSignal[ots]) {
                stateMapping[2 * ots] = newTagStateNum;
                stateMapping[2 * ots + 1] = newTagStateNum;
                newTagStateNum++;
            } else {
                stateMapping[2 * ots] = newTagStateNum;
                newTagStateNum++;
                stateMapping[2 * ots + 1] = newTagStateNum;
                newTagStateNum++;
            }
        }

        double[] newLatentScore = new double[newTagStateNum];
        ArrayMath.fill(newLatentScore, 0.0);
        for (int ts = 0; ts < tagStateNum; ts++) {
            int nts = stateMapping[ts];
            newLatentScore[nts] += latentScores[ts];
        }
        tagStateNum = newTagStateNum;
        latentScores = newLatentScore;
    }

    public void mergeStates(List<Tree<Integer>> currSplitTreeList, double[] finerLatentScores) {
        if (tagStateNum != currSplitTreeList.size()) {
            throw new RuntimeException("Error: latent state number does not match...");
        }
        latentScores = new double[tagStateNum];
        for (int si = 0; si < tagStateNum; si++) {
            for (Tree<Integer> child : currSplitTreeList.get(si).getChildren()) {
                latentScores[si] += finerLatentScores[child.getLabel()];
            }
        }
    }

    public void simplyMergeStates(boolean[] mergeSignal) {
        if (mergeSignal == null) {
            ArrayMath.fill(latentScores, 0.0);
            return;
        }

        int newTagStateNum = 0;
        if (mergeSignal.length * 2 != tagStateNum) {
            throw new RuntimeException("Error: latent state number does not match.");
        }
        for (int ots = 0; ots < mergeSignal.length; ots++) {
            if (mergeSignal[ots]) {
                newTagStateNum += 1;
            } else {
                newTagStateNum += 2;
            }
        }

        tagStateNum = newTagStateNum;
        latentScores = new double[tagStateNum];
        ArrayMath.fill(latentScores, 0.0);
    }

    public void smoothing(double smoothingParam) {
        double sum = 0;
        for (int ts = 0; ts < tagStateNum; ts++) {
            sum += latentScores[ts];
        }
        double mean = sum / tagStateNum;
        for (int ts = 0; ts < tagStateNum; ts++) {
            latentScores[ts] = (1 - smoothingParam) * latentScores[ts] +
                    smoothingParam * mean;
        }
    }

    public void smoothing(double smoothingParam, double unigram) {
        for (int ts = 0; ts < tagStateNum; ts++) {
            latentScores[ts] = (1 - smoothingParam) * latentScores[ts] +
                    smoothingParam * unigram / tagStateNum;
        }
    }
}
