/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.lacluster;

import edu.umd.clip.math.ArrayMath;

/**
 *
 * @author zqhuang
 */
public class AlphaBetaItem {

    private static final long serialVersionUID = 1L;
    private String word;
    private String tag;
    private double[] alphaScores;
    private double[] betaScores;
    private double alphaScale;
    private double betaScale;

    public AlphaBetaItem(String word, String tag) {
        this(word);
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public AlphaBetaItem(String word) {
        this.word = word;
        alphaScale = 0;
        betaScale = 0;
    }

    public void initialize(String word, int clusterNum) {
        this.word = word;
        alphaScale = 0;
        betaScale = 0;
        reset(clusterNum);
    }

    public void reset(int clusterNum) {
        alphaScale = 0;
        betaScale = 0;
        alphaScores = ArrayMath.initArray(alphaScores, clusterNum);
        betaScores = ArrayMath.initArray(betaScores, clusterNum);
    }

    public void clear() {
        alphaScale = 0;
        betaScale = 0;
        alphaScores = null;
        betaScores = null;
    }

    public String getWord() {
        return word;
    }

    public double[] getAlphaScores() {
        return alphaScores;
    }

    public double[] getBetaScores() {
        return betaScores;
    }

    public double getAlphaScale() {
        return alphaScale;
    }

    public double getBetaScale() {
        return betaScale;
    }

    public void setAlphaScale(double alphaScale) {
        this.alphaScale = alphaScale;
    }

    public void setBetaScale(double betaScale) {
        this.betaScale = betaScale;
    }

    @Override
    public String toString() {
        return word;
    }
}
