/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.lvlm;

import edu.umd.clip.util.ScalingTools;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class Token {

    private static final long serialVersionUID = 1L;
    private Word word;
    private double[] forwardScores;
    private double[] backwordScores;
    private int forwardScale;
    private int backwardScale;

    public List<State> getStateList() {
        return word.getStateList();
    }

    public int getStateNum() {
        return word.getStateNum();
    }

    public Word getWord() {
        return word;
    }

    /**
     * Tally bigram transition counts in array representation
     * @param nextToken
     * @param sentenceScore
     * @param sentenceScale
     */
    public void tallyTransitionCount(Token prevToken, Token nextToken, double sentenceScore, int sentenceScale) {
        double scalingFactor = ScalingTools.calcScaleFactor(forwardScale + nextToken.getBackwardScale() - sentenceScale) / sentenceScore;
        word.addBigramTransitionCounts(prevToken.getWord(), nextToken.getWord(), scalingFactor, forwardScores, nextToken.getBackwardScores());
    }

    /**
     * Tally trigram transition counts in map representation
     * 
     * @param prevToken
     * @param nextToken
     * @param sentenceScore
     * @param sentenceScale
     */
    public void tallyTransitionCount(Token prev2Token, Token prevToken, Token nextToken, double sentenceScore, int sentenceScale) {
        double scalingFactor = ScalingTools.calcScaleFactor(prevToken.getForwardScale() + nextToken.getBackwardScale() - sentenceScale) / sentenceScore;
        word.tallyTransitionCount(prev2Token.getWord(), prevToken.getWord(), nextToken.getWord(), scalingFactor, prevToken.getForwardScores(), nextToken.getBackwardScores());
    }

    public Token(Word word) {
        this.word = word;
        reset();
    }

    public void reset() {
        forwardScale = 0;
        backwardScale = 0;
        forwardScores = null;
        backwordScores = null;
    }

    public void setForwardScores(double[] forwardScores) {
        this.forwardScores = forwardScores;
    }

    public void setForwardScale(int forwardScale) {
        this.forwardScale = forwardScale;
    }

    public void setBackwordScores(double[] backwordScores) {
        this.backwordScores = backwordScores;
    }

    public void setBackwardScale(int backwardScale) {
        this.backwardScale = backwardScale;
    }

    public int getForwardScale() {
        return forwardScale;
    }

    public double[] getForwardScores() {
        return forwardScores;
    }

    public int getBackwardScale() {
        return backwardScale;
    }

    public double[] getBackwardScores() {
        return backwordScores;
    }

    @Override
    public String toString() {
        return word.toString();
    }
}
