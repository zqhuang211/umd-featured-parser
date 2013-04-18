/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.cluster;

import java.util.List;

/**
 *
 * @author zqhuang
 */
public class Feature {

    private String label;
    private List<ClusterFeature> cfList;

    public Feature(String id, List<ClusterFeature> cfList) {
        this.label = id;
        this.cfList = cfList;
    }

    public String getLabel() {
        return label;
    }

    public List<ClusterFeature> getCfList() {
        return cfList;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Feature other = (Feature) obj;
        if ((this.label == null) ? (other.label != null) : !this.label.equals(other.label)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + (this.label != null ? this.label.hashCode() : 0);
        return hash;
    }
}
