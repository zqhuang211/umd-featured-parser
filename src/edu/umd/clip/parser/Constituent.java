/*
 * Constituent.java
 * 
 * Created on Nov 2, 2007, 12:02:43 AM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.math.ArrayMath;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class Constituent implements Cloneable {

    private static final long serialVersionUID = 1L;
    private int node;
    private int iScale;
    private int oScale;
    private double[] iScores;
    private double[] oScores;
    private double[] giScores;
    private double[] goScores;
    private String word;
    private int from;
    private int to;
    private int viterbiState = -1;

    public void setViterbiState(int viterbiState) {
        this.viterbiState = viterbiState;
    }

    public int getViterbiState() {
        return viterbiState;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public void setGiScores(double[] giScores) {
        this.giScores = giScores;
    }

    public void setGoScores(double[] goScores) {
        this.goScores = goScores;
    }

    public double[] getGiScores() {
        return giScores;
    }

    public double[] getGoScores() {
        return goScores;
    }

    public enum SplitType {

        TAG, UNARY, BINARY
    }
    private double score;
    int split;
    private Constituent leftItem;
    private Constituent rightItem;
    private Constituent childItem;
    private SplitType splitType;

    public double getScore() {
        return score;
    }

    public int getSplit() {
        return split;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Constituent getLeftItem() {
        return leftItem;
    }

    public Constituent getRightItem() {
        return rightItem;
    }

    public Constituent getChildItem() {
        return childItem;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setSplit(int split) {
        this.split = split;
    }

    public void setLeftItem(Constituent leftItem) {
        this.leftItem = leftItem;
    }

    public void setRightItem(Constituent rightItem) {
        this.rightItem = rightItem;
    }

    public void setChildItem(Constituent childItem) {
        this.childItem = childItem;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public Constituent(int node, String word, int from, int to) {
        this.node = node;
        this.word = word;
        this.from = from;
        this.to = to;
        resetScores();
        this.score = Double.NEGATIVE_INFINITY;
    }

    public Constituent(int node, String word) {
        this.node = node;
        this.word = word;
        resetScores();
        this.score = Double.NEGATIVE_INFINITY;
    }

    public Constituent(int node) {
        this.node = node;
        resetScores();
        this.score = Double.NEGATIVE_INFINITY;
    }

    private void resetScores() {
//        int stateNum = node.getStateNum();
//        iScores = new double[stateNum];
//        oScores = new double[stateNum];
//        ArrayMath.fill(iScores, 0.0);
//        ArrayMath.fill(oScores, 0.0);
        iScale = 0;
        oScale = 0;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public void setIScores(double[] iScores) {
        this.iScores = iScores;
    }

    public void setIScore(int index, double score) {
        iScores[index] = score;
    }

    public void setIScale(int iScale) {
        this.iScale = iScale;
    }

    public void setOScores(double[] oScores) {
        this.oScores = oScores;
    }

    public void setOScore(int index, double score) {
        oScores[index] = score;
    }

    public void setOScale(int oScale) {
        this.oScale = oScale;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public int getNode() {
        return node;
    }

    public String getWord() {
        return word;
    }

    public double[] getIScores() {
        return iScores;
    }

    public double getIScore(int i) {
        return iScores[i];
    }

    public double[] getOScores() {
        return oScores;
    }

    public double getOScore(int i) {
        return oScores[i];
    }

    public int getIScale() {
        return iScale;
    }

    public int getOScale() {
        return oScale;
    }

    public boolean isPreterminal() {
        return word != null;
    }

    public void scaleIScores(int previousScale) {
        int logScale = 0;
        double scale = 1.0;
        double max = ArrayMath.max(iScores);
        while (max > Grammar.SCALE) {
            max /= Grammar.SCALE;
            scale *= Grammar.SCALE;
            logScale += 1;
        }
        while (max > 0.0 && max < 1.0 / Grammar.SCALE) {
            max *= Grammar.SCALE;
            scale /= Grammar.SCALE;
            logScale -= 1;
        }
        if (logScale != 0) {
            for (int i = 0; i < iScores.length; i++) {
                iScores[i] /= scale;
            }
        }
        if ((max != 0) && ArrayMath.max(iScores) == 0) {
            throw new Error("Underflow when scaling iScores!");
        }
        iScale = previousScale + logScale;
    }

    public void scaleOScores(int previousScale) {
        int logScale = 0;
        double scale = 1.0;
        double max = ArrayMath.max(oScores);
        while (max > Grammar.SCALE) {
            max /= Grammar.SCALE;
            scale *= Grammar.SCALE;
            logScale += 1;
        }
        while (max > 0.0 && max < 1.0 / Grammar.SCALE) {
            max *= GrammarTrainer.SCALE;
            scale /= Grammar.SCALE;
            logScale -= 1;
        }
        if (logScale != 0) {
            for (int i = 0; i < oScores.length; i++) {
                oScores[i] /= scale;
            }
        }
        if ((max != 0) && ArrayMath.max(oScores) == 0) {
            throw new Error("Underflow when scaling oScores!");
        }
        oScale = previousScale + logScale;
    }

    public String toString(List<String> nodeList) {
        if (word == null) {
            if (viterbiState == -1) {
                return nodeList.get(node);
            } else {
                return nodeList.get(node) + "-" + viterbiState;
            }
        } else if (node == -1) {
            return word.toString();
        } else {
            if (viterbiState == -1) {
                return nodeList.get(node) + ":" + word;
            } else {
                return nodeList.get(node) + "-" + viterbiState + ":" + word;
            }
        }
    }

    @Override
    public String toString() {
        if (word == null) {
            if (viterbiState == -1) {
                return String.valueOf(node);
            } else {
                return node + "-" + viterbiState;
            }
        } else if (node == -1) {
            return word.toString();
        } else {
            if (viterbiState == -1) {
                return node + ":" + word;
            } else {
                return node + "-" + viterbiState + ":" + word;
            }
        }
    }
}
