/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.clip.parser;

/*
 * MaxCItem.java
 *
 * Created on May 4, 2007, 12:01 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */


/**
 * Each instance holds the values of the following arrays in
 * CoarseToFindMaxRuleParser:
 * <p>
 *     protected double[][][] maxcScore;  // start, end, state --> logProb
 *     protected int[][][] maxcSplit;  // start, end, state -> split position
 *     protected int[][][] maxcChild;  // start, end, state -> unary child (if any)
 *     protected int[][][] maxcLeftChild;  // start, end, state -> left child
 *     protected int[][][] maxcRightChild;  // start, end, state -> right child
 * <p>
 * @author zqhuang
 */
public class NBestItem implements Cloneable {

    protected double score;
    protected int scale;
    protected int split;
    protected int child;
    protected int ibest;
    protected int leftChild;
    protected int leftIBest;
    protected int rightChild;
    protected int rightIBest;
    static java.util.Comparator Comparator;

    /** Creates a new instance of MaxCItem with default values. */
    public NBestItem() {
        this.score = 0;
        this.scale = 0;
        this.split = -1;
        this.child = -1;
        this.ibest = -1;
        this.leftIBest = -1;
        this.rightIBest = -1;
        this.leftChild = -1;
        this.rightChild = -1;
    }

    /** Creates a new instance of MaxCitem with specified values. */
    public NBestItem(double score, int split, int child, int ibest, int leftChild, int leftIBest, int rightChild, int rightIBest) {
        this.score = score;
        this.scale = 0;
        this.split = split;
        this.child = child;
        this.ibest = ibest;
        this.leftChild = leftChild;
        this.leftIBest = leftIBest;
        this.rightChild = rightChild;
        this.rightIBest = rightIBest;
    }

//    public NBestItem(double score, int scale, int split, int child, int ibest, int leftChild, int leftIBest, int rightChild, int rightIBest) {
//        this.score = score;
//        this.scale = scale;
//        this.split = split;
//        this.child = child;
//        this.ibest = ibest;
//        this.leftChild = leftChild;
//        this.leftIBest = leftIBest;
//        this.rightChild = rightChild;
//        this.rightIBest = rightIBest;
//    }

    public void setIbest(int ibest) {
        this.ibest = ibest;
    }

    public int getScale() {
        return scale;
    }

/** Returns 1 if this.maxScore > O.maxScore, -1 if this.maxsScore < O.maxScore, 0 otherwise. */
    public static class Comparator implements java.util.Comparator {

        public int compare(Object item, Object anotherItem) throws ClassCastException {
            if (!(item instanceof NBestItem) || !(anotherItem instanceof NBestItem)) {
                throw new ClassCastException("Both MaxCItem objects expected.");
            }
            NBestItem maxCItem0 = (NBestItem) item;
            NBestItem maxCItem1 = (NBestItem) anotherItem;
            if ((maxCItem0.getScale() < maxCItem1.getScale()) || (maxCItem0.getScale() == maxCItem1.getScale() && maxCItem0.getScore() < maxCItem1.getScore())) {
                return 1;
            } else if ((maxCItem0.getScale() > maxCItem1.getScale()) || (maxCItem0.getScale() == maxCItem1.getScale() && maxCItem0.getScore() > maxCItem1.getScore())) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    public double getScore() {
        return score;
    }

    public int getSplit() {
        return split;
    }

    public int getChild() {
        return child;
    }

    public int getRightChild() {
        return rightChild;
    }

    public int getLeftChild() {
        return leftChild;
    }

    public int getRightIBest() {
        return rightIBest;
    }

    public int getLeftIBest() {
        return leftIBest;
    }

    public int getIbest() {
        return ibest;
    }
}
