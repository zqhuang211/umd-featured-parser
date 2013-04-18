/*
 * AlphaBetaItem.java
 *
 * Created on May 22, 2007, 12:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.math.ArrayMath;

/**
 *
 * @author zqhuang
 */
public class AlphaBetaItem {

    private static final long serialVersionUID = 1L;
    WordTagItem wordTagItem;
    double[] alphaScores;
    double[] betaScores;
    int tagStateNum;
    int alphaScale;
    int betaScale;

    public edu.umd.clip.latagger.WordTagItem getWordTagItem() {
        return wordTagItem;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String word = wordTagItem.getWord();
        String tag = (String) edu.umd.clip.util.Numberer.object("tags", wordTagItem.getTag());
        sb.append(word + "/" + tag + " ");
        return sb.toString();
    }
    
    public AlphaBetaItem(WordTagItem wordTagItem) {
        this.wordTagItem = wordTagItem;
        if (wordTagItem == null) {
            tagStateNum = 0;
        } else {
            tagStateNum = LatentTagStates.getLatentStateNum(wordTagItem.getTag());
        }
        alphaScores = null;
        betaScores = null;

        alphaScale = 0;
        betaScale = 0;
    }

    public void resetAlphaBetaScores() {
        if (wordTagItem == null) {
            tagStateNum = 0;
        } else {
            tagStateNum = LatentTagStates.getLatentStateNum(wordTagItem.getTag());
        }
        alphaScores = null;
        betaScores = null;
        alphaScale = 0;
        betaScale = 0;
    }

    public void clearAlphaBetaScores() {
        alphaScores = null;
        betaScores = null;
    }

    public void scaleAlphaScores(int previousScale) {
        int logScale = 0;
        double scale = 1.0;
        double max = ArrayMath.max(alphaScores);
        double zero = 1.0 / Double.MAX_VALUE;
        //if (max==0) 	System.out.println("All iScores are 0!");
        while (max > LatentTrainer.SCALE) {
            max /= LatentTrainer.SCALE;
            scale *= LatentTrainer.SCALE;
            logScale += 1;
        }
        while (max > zero && max < 1.0 / LatentTrainer.SCALE) {
            max *= LatentTrainer.SCALE;
            scale /= LatentTrainer.SCALE;
            logScale -= 1;
        }
        if (logScale != 0) {
            ArrayMath.multiplyInPlace(alphaScores, 1 / scale);
        }
        for (int i = 0; i < tagStateNum; i++) {
            if (Double.isInfinite(alphaScores[i])) {
                boolean checkThis = true;
            }
        }

        if ((max != 0) && ArrayMath.max(alphaScores) == 0) {
            System.out.println("Undeflow when scaling alphaScores!");
        }
        alphaScale = previousScale + logScale;
    }

    public void scaleBetaScores(int previousScale) {
        int logScale = 0;
        double scale = 1.0;
        double max = ArrayMath.max(betaScores);
        //if (max==0) 	System.out.println("All iScores are 0!");
        while (max > LatentTrainer.SCALE) {
            max /= LatentTrainer.SCALE;
            scale *= LatentTrainer.SCALE;
            logScale += 1;
        }
        while (max > 0.0 && max < 1.0 / LatentTrainer.SCALE) {
            max *= LatentTrainer.SCALE;
            scale /= LatentTrainer.SCALE;
            logScale -= 1;
        }
        if (logScale != 0) {
            ArrayMath.multiplyInPlace(betaScores, 1 / scale);
        }
        if ((max != 0) && ArrayMath.max(betaScores) == 0) {
            System.out.println("Undeflow when scaling betaScores!");
        }
        betaScale = previousScale + logScale;
    }

    public int getTag() {
        return wordTagItem.getTag();
    }

    public int getTagStateNum() {
        return tagStateNum;
    }

    public String getWord() {
        return wordTagItem.getWord();
    }

    public double getAlphaScore(int cts) {
        return alphaScores[cts];
    }

    public double[] getAlphaScores() {
        return alphaScores;
    }

    public double[] getBetaScores() {
        return betaScores;
    }

    public double getBetaScore(int cts) {
        return betaScores[cts];
    }

    public int getAlphaScale() {
        return alphaScale;
    }

    public int getBetaScale() {
        return betaScale;
    }

    public void setAlphaScores(double[] alphaScores) {
        this.alphaScores = alphaScores;
    }

    public void setBetaScores(double[] betaScores) {
        this.betaScores = betaScores;
    }

    public void setAlphaScore(int cts, double score) {
        alphaScores[cts] = score;
    }

    public void setBetaScore(int cts, double score) {
        betaScores[cts] = score;
    }
}
