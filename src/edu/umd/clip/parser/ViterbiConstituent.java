/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.clip.parser;

/**
 *
 * @author zqhuang
 */
public class ViterbiConstituent {
    private static final long serialVersionUID = 1L;
    private int node = -1;
    private String nodeName = null;
    private double[] iScores = null;
    private double[] oScores = null;
    private String word = null;
    private int viterbiState = -1;
    private int from = -1;
    private int to = -1;

    public int getViterbiState() {
        return viterbiState;
    }

    public void setViterbiState(int viterbiState) {
        this.viterbiState = viterbiState;
    }

    public double getIScore(int index) {
        return iScores[index];
    }

    public void setIScores(double[] iScores) {
        this.iScores = iScores;
    }

    public double[] getIScores() {
        return iScores;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public ViterbiConstituent(int node) {
        this.node = node;
    }

    public ViterbiConstituent(String word) {
        this.word = word;
    }

    public int getNode() {
        return node;
    }

    public String getWord() {
        return word;
    }

    public String toString() {
        if (node != -1)
            return String.valueOf(node);
        else if (word != null)
            return word;
        else
            throw new RuntimeException("incorrect viterbi constituent");
    }
}
