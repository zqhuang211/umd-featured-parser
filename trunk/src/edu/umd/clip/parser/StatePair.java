/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import java.util.List;

/**
 *
 * @author zqhuang
 */
public class StatePair implements Comparable<StatePair> {

    private int node;
    private int state1;
    private int state2;
    private double score;

    public StatePair(int node, int state1, int state2) {
        this.node = node;
        if (state1 < state2) {
            this.state1 = state1;
            this.state2 = state2;
        } else {
            this.state1 = state2;
            this.state2 = state1;
        }
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getNode() {
        return node;
    }

    public double getScore() {
        return score;
    }

    public int getState1() {
        return state1;
    }

    public int getState2() {
        return state2;
    }

    public synchronized void addScore(double amount) {
        score += amount;
    }

    public int compareTo(StatePair t) {
        return (int) Math.signum(score - t.getScore());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StatePair other = (StatePair) obj;
        if (this.node != other.node) {
            return false;
        }
        if (this.state1 != other.state1) {
            return false;
        }
        if (this.state2 != other.state2) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.node;
        hash = 89 * hash + this.state1;
        hash = 89 * hash + this.state2;
        return hash;
    }

    public String toString(List<String> nodeList) {
        StringBuilder sb = new StringBuilder();
        sb.append(nodeList.get(node));
        sb.append("_");
        sb.append(state1);
        sb.append("+");
        sb.append(state2);
        sb.append(": ");
        sb.append(score);
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(node);
        sb.append("_");
        sb.append(state1);
        sb.append("+");
        sb.append(state2);
        sb.append(": ");
        sb.append(score);
        return sb.toString();
    }
}
