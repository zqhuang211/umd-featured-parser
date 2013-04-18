/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

/**
 *
 * @author zqhuang
 */
public class NodeProbPair implements Comparable<NodeProbPair> {

    private int node;
    private double prob;
    
    public int getNode() {
        return node;
    }

    public double getProb() {
        return prob;
    }

    public NodeProbPair(int node, double prob) {
        this.node = node;
        this.prob = prob;
    }

    @Override
    public int compareTo(NodeProbPair t) {
        return (int) Math.signum(((NodeProbPair) t).prob - prob);
    }

    @Override
    public String toString() {
        return String.format("(%d, %.2f)", node, prob);
    }
}
