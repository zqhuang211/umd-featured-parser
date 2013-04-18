/*
 * WordTagSequence.java
 *
 * Created on May 15, 2007, 11:00 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.latagger;

import java.util.ArrayList;

/**
 *
 * @author zqhuang
 */
public class WordTagSequence extends ArrayList<WordTagItem> {
    private static final long serialVersionUID = 1L;
    public boolean add(String word, Integer tag) {
        return super.add(new WordTagItem(word, tag));
    }
    private double weight = 1.0;

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}
