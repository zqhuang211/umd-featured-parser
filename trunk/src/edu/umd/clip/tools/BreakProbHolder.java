/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.tools;

/**
 *
 * @author zqhuang
 */
public class BreakProbHolder {

    public BreakProb mean;
    public BreakProb squaredMean;
    public BreakProb var;
    public int count;

    public BreakProbHolder() {
        mean = new BreakProb();
        squaredMean = new BreakProb();
        count = 0;
    }

    public void addProb(BreakProb prob) {
        mean.p1 += prob.p1;
        mean.p4 += prob.p4;
        mean.pp += prob.pp;

        squaredMean.p1 += prob.p1 * prob.p1;
        squaredMean.p4 += prob.p4 * prob.p4;
        squaredMean.pp += prob.pp * prob.pp;

        count++;
    }

    public void normalize() {
        mean.p1 /= count;
        mean.p4 /= count;
        mean.pp /= count;
        squaredMean.p1 /= count;
        squaredMean.p4 /= count;
        squaredMean.pp /= count;
        var = new BreakProb();
        var.p1 = squaredMean.p1 - mean.p1 * mean.p1;
        var.p4 = squaredMean.p4 - mean.p4 * mean.p4;
        var.pp = squaredMean.pp - mean.pp * mean.pp;
    }

    public BreakProb getMean() {
        return mean;
    }

    public BreakProb getSquaredMean() {
        return squaredMean;
    }

    public BreakProb getVar() {
        return var;
    }

    public BreakProb calcDirichletProb() {
        double logSum = 0;
        if (var.p1 == 0 || var.p4 == 0) {
            return new BreakProb(mean.p1, mean.p4, mean.pp);
        }
        logSum += Math.log(mean.p1 * (1 - mean.p1) / var.p1 - 1);
        logSum += Math.log(mean.p4 * (1 - mean.p4) / var.p4 - 1);
        logSum /= 2;
        double sum = Math.exp(logSum);
        return new BreakProb(sum * mean.p1, sum * mean.p4, sum * mean.pp);
    }
}
