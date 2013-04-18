/*
 * SentenceLatentUnigram.java
 * 
 * Created on May 23, 2007, 10:07:35 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.latagger;

/**
 *
 * @author zqhuang
 */

public class SentenceLatentUnigram {
    private static final long serialVersionUID = 1L;
    private int tagStateNum;
    
    private double[] unigramScores;
    
    /** Creates a new instance of SentenceLatentBigram
     * @param alphaBetaItem
     * @param sentenceScore
     * @param sentenceScale
     */
    public SentenceLatentUnigram(AlphaBetaItem alphaBetaItem, double sentenceScore, double sentenceScale) {
        tagStateNum = alphaBetaItem.getTagStateNum();
        unigramScores = new double[tagStateNum];
        double currentScale = alphaBetaItem.getAlphaScale() + alphaBetaItem.getBetaScale() - sentenceScale;
        double scaleFactor = Math.pow(LatentTrainer.SCALE, currentScale);
        double[] alphaScores = alphaBetaItem.getAlphaScores();
        double[] betaScores = alphaBetaItem.getBetaScores();
        for (int cts = 0; cts < tagStateNum; cts++) {
            unigramScores[cts] = scaleFactor * 
                    alphaScores[cts] *
                    betaScores[cts] /
                    sentenceScore;
        }
    }
    
    public double getUnigramProb() {
        double score = 0;
        for (int cts = 0; cts < tagStateNum; cts++)
            score += unigramScores[cts];
        return score;
    }

    public int getTagStateNum() {
        return tagStateNum;
    }
    
    public double getUnigramScore(int cts) {
        return unigramScores[cts];
    }
    
    public double[] getUnigramScore() {
        return unigramScores;
    }
}