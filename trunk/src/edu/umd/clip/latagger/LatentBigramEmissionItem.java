/*
 * LatentBigramEmissionItem.java
 *
 * Created on May 22, 2007, 9:37 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.math.Operator;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Zhongqiang Huang
 */
public class LatentBigramEmissionItem implements Serializable {

    private static final long serialVersionUID = 1L;
    private double[] latentScore;
    private int tagStateNum;
    private double surfaceScore;

    public LatentBigramEmissionItem(double surfaceScore) {
        this.surfaceScore = surfaceScore;
        tagStateNum = 1;
        latentScore = new double[tagStateNum];
        latentScore[0] = surfaceScore;
    }

    public void clearScores() {
        ArrayMath.fill(latentScore, 0.0);
    }

    public LatentBigramEmissionItem(LatentBigramEmissionItem anotherItem) {
        tagStateNum = anotherItem.tagStateNum;
        surfaceScore = anotherItem.surfaceScore;
        latentScore = new double[tagStateNum];
        for (int ts = 0; ts < tagStateNum; ts++) {
            latentScore[ts] = anotherItem.latentScore[ts];
        }
    }

    public LatentBigramEmissionItem(int tagStateNum) {
        this.surfaceScore = 0;
        this.tagStateNum = tagStateNum;
        latentScore = new double[tagStateNum];
        Arrays.fill(latentScore, 0.0);
    }

    public void applyOperator(Operator operator) {
        for (int ts = 0; ts < tagStateNum; ts++) {
            latentScore[ts] = (Double) operator.applyOperator(latentScore[ts]);
        }
    }

    public void increase(int tagState, double amount) {
        latentScore[tagState] += amount;
    }

    public void splitStates() {
        splitStates(2);
    }

    public double getLatentScore(int tagState) {
        return latentScore[tagState];
    }

    public void setLatentScore(int tagState, double score) {
        latentScore[tagState] = score;
    }

    public int getTagStateNum() {
        return tagStateNum;
    }

    public void normalize(double[] normalizationFactor) {
        for (int ts = 0; ts < tagStateNum; ts++) {
            if (latentScore[ts] != 0) {
                latentScore[ts] /= normalizationFactor[ts];
            }
            if (Double.isInfinite(latentScore[ts]) || Double.isNaN(latentScore[ts])) {
                latentScore[ts] = 1.0 / LatentTrainer.SCALE;
            }
        }
    }

    public void splitStates(int tagSplitFactor) {
        double[] newLatentScore = new double[tagStateNum * tagSplitFactor];
        for (int tagOld = 0; tagOld < tagStateNum; tagOld++) {
            double oldScore = latentScore[tagOld];
            for (int i = 0; i < tagSplitFactor; i++) {
                int tagNew = tagOld * tagSplitFactor + i;
                newLatentScore[tagNew] = oldScore;
            }
        }

        latentScore = newLatentScore;
        tagStateNum *= tagSplitFactor;
    }

    public void setLatentScore(double[] latentScore) {
        this.latentScore = latentScore;
    }

    public double[] getLatentScores() {
        return latentScore;
    }

    public void setSurfaceScore(double surfaceScore) {
        this.surfaceScore = surfaceScore;
    }

    public double getSurfaceScore() {
        return surfaceScore;
    }

    public void simplySplitStates() {
        simplySplitStates(2);
    }

    public void simplySplitStates(int tagSplitFactor) {
        tagStateNum *= tagSplitFactor;
        latentScore = new double[tagStateNum];
        Arrays.fill(latentScore, 0.0);
    }

    public void smooth(double smoothingParam) {
        double sum = 0;
        for (int ts = 0; ts < tagStateNum; ts++) {
            sum += latentScore[ts];
        }
        double mean = sum / tagStateNum;
        for (int ts = 0; ts < tagStateNum; ts++) {
            latentScore[ts] = (1 - smoothingParam) * latentScore[ts] + smoothingParam * mean;
        }
    }

    public void smooth(double smoothingParam, double unigram) {
        for (int ts = 0; ts < tagStateNum; ts++) {
            latentScore[ts] = (1-smoothingParam) * latentScore[ts] + smoothingParam * unigram;
        }
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
            newLatentScore[nts] += latentScore[ts];
        }
        tagStateNum = newTagStateNum;
        latentScore = newLatentScore;
    }

    public void mergeStates(List<Tree<Integer>> splitTreeList, double[] finerScores) {
        if (splitTreeList.size() != tagStateNum) {
            throw new RuntimeException("Length does not match...");
        }
        latentScore = new double[tagStateNum];
        for (int si = 0; si < tagStateNum; si++) {
            for (Tree<Integer> child : splitTreeList.get(si).getChildren()) {
                latentScore[si] += finerScores[child.getLabel()];
            }
        }
    }

    public void simplyMergeStates(boolean[] mergeSignal) {
        if (mergeSignal == null) {
            ArrayMath.fill(latentScore, 0.0);
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
        latentScore = new double[tagStateNum];
        ArrayMath.fill(latentScore, 0.0);
    }
}
