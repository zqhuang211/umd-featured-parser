/*
 * SentenceLatentBigram.java
 *
 * Created on May 22, 2007, 11:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.latagger;

/**
 *
 * @author Zhongqiang Huang
 */

public class SentenceLatentBigram {
    private static final long serialVersionUID = 1L;
    private int prevTagStateNum;
    private int currTagStateNum;
    
    private double[][] bigramScores;
    
    public SentenceLatentBigram(AlphaBetaItem prevAlphaBetaItem, AlphaBetaItem currAlphaBetaItem,
            double[] emissionProb, double[][] transitionProb, double sentenceScore, double sentenceScale) {
        prevTagStateNum = prevAlphaBetaItem.getTagStateNum();
        currTagStateNum = currAlphaBetaItem.getTagStateNum();
        bigramScores = new double[prevTagStateNum][currTagStateNum];
        double[] prevAlphaScores = prevAlphaBetaItem.getAlphaScores();
        double[] currBetaScores = currAlphaBetaItem.getBetaScores();
        double currentScale = prevAlphaBetaItem.getAlphaScale() + currAlphaBetaItem.getBetaScale() - sentenceScale;
        double scaleFactor = Math.pow(LatentTrainer.SCALE, currentScale);
        for (int pts = 0; pts < prevTagStateNum; pts++) {
            for (int cts = 0; cts < currTagStateNum; cts++) {
                bigramScores[pts][cts] = scaleFactor *
                        prevAlphaScores[pts] *
                        transitionProb[pts][cts] *
                        emissionProb[cts] *
                        currBetaScores[cts] /
                        sentenceScore;
            }
        }
    }
    
    public double getBigramProb() {
        double score = 0;
        for (int pts = 0; pts < prevTagStateNum; pts++)
            for (int cts = 0; cts < currTagStateNum; cts++)
                score += bigramScores[pts][cts];
        return score;
    }
    
    public int getPrevTagStateNum() {
        return prevTagStateNum;
    }
    
    public int getCurrTagStateNum() {
        return currTagStateNum;
    }
    
    public double[][] getBigramScore() {
        return bigramScores;
    }
    
    public double getBigramScore(int prevTagState, int currTagState) {
        return bigramScores[prevTagState][currTagState];
    }
}