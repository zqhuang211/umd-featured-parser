/*
 * NBestItem.java
 *
 * Created on July 03, 2007, 3:37:07 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

/**
 *
 * @author Zhongqiang Huang
 */
public class NBestItem {
    
    private static final long serialVersionUID = 1L;
    private double score;
    private int prevTag;
    private int prevNBest;
    static java.util.Comparator Comparator;

    public NBestItem() {
        prevTag = -1;
        prevNBest = -1;
        score = Double.NEGATIVE_INFINITY;
    }

    public NBestItem(int prevTag, int prevNBest, double score) {
        this.prevTag = prevTag;
        this.prevNBest = prevNBest;
        this.score = score;
    }

    public void setNBest(int prevTag, int prevNBest, double score) {
        this.prevTag = prevTag;
        this.prevNBest = prevNBest;
        this.score = score;
    }

//    public static class Comparator implements java.util.Comparator {
//
//        public int compare(Object item, Object anotherItem) {
//            if (!(item instanceof NBestItem) || !(anotherItem instanceof NBestItem))
//                throw new ClassCastException("In NBestItem: both NBestItem objects expected.");
//            double score = ((NBestItem) item).getScore();
//            double anotherScore = ((NBestItem) anotherItem).getScore();
//            
//            if (score < anotherScore)
//                return 1;
//            else if (score > anotherScore)
//                return -1;
//            else
//                return 0;
//        }
//    }
    public double getScore() {
        return score;
    }

    public int getPrevTag() {
        return prevTag;
    }

    public int getPrevNBest() {
        return prevNBest;
    }

    public static class Comparator implements java.util.Comparator {

        public int compare(Object item, Object anotherItem) throws ClassCastException {
            if (!(item instanceof NBestItem) || !(anotherItem instanceof NBestItem)) {
                throw new ClassCastException("Both MaxCItem objects expected.");
            }
            double score1 = ((NBestItem) item).getScore();
            double score2 = ((NBestItem) anotherItem).getScore();
            if (score1 < score2) {
                return 1;
            } else if (score1 > score2) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}