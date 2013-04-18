/*
 * AlphaBetaSequence.java
 *
 * Created on May 22, 2007, 10:49 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.latagger;

import java.util.ArrayList;

/**
 *
 * @author Zhongqiang Huang
 */
public class AlphaBetaSequence extends ArrayList<AlphaBetaItem> {
    private static final long serialVersionUID = 1L;
    private double weight = 1.0;

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}
