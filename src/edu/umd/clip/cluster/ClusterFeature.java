/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.clip.cluster;

/**
 *
 * @author zqhuang
 */
public class ClusterFeature {
    private Cluster word;
    private Feature feature;
    private double value;

    public ClusterFeature(Cluster word, Feature feature, double value) {
        this.word = word;
        this.feature = feature;
        this.value = value;
    }

    public Feature getFeature() {
        return feature;
    }

    public double getValue() {
        return value;
    }

    public Cluster getWord() {
        return word;
    }
}
