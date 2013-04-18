/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.tools;

import java.io.Serializable;

/**
 *
 * @author zqhuang
 */
public class BreakProb implements Serializable {

    double p1 = 0;
    double p4 = 0;
    double pp = 0;

    public BreakProb() {
    }

    public BreakProb(double p1, double p4, double pp) {
        this.p1 = p1;
        this.p4 = p4;
        this.pp = pp;
    }

    @Override
    public String toString() {
        return "1:" + p1 + "_4:" + p4 + "_p:" + pp;
    }

    public double calcLogDirichletProb(BreakProb counts) {
        double prob = 0;
        prob += (p1-1)*Math.log(counts.p1+0.001);
        prob += (p4-1)*Math.log(counts.p4+0.001);
        prob += (pp-1)*Math.log(counts.pp+0.001);
        prob += logGamma(p1+p4+pp);
        prob -= logGamma(p1);
        prob -= logGamma(p4);
        prob -= logGamma(pp);
        return prob;
    }

    static double logGamma(double x) {
        double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
        double ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1) + 24.01409822 / (x + 2) - 1.231739516 / (x + 3) + 0.00120858003 / (x + 4) - 0.00000536382 / (x + 5);
        return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
    }

    static double gamma(double x) {
        return Math.exp(logGamma(x));
    }
}
