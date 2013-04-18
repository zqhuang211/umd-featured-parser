/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.clip.cluster;

import java.util.List;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class Cluster implements Comparable<Cluster> {
    private int id;
    private Set<Cluster> subClusters;
    private List<ClusterFeature> wfList;

    public void setSubClusters(Set<Cluster> subClusters) {
        this.subClusters = subClusters;
    }

    public Cluster(int id, List<ClusterFeature> wfList) {
        this.id = id;
        this.wfList = wfList;
    }

    public int getId() {
        return id;
    }

    public List<ClusterFeature> getWfList() {
        return wfList;
    }

    public int compareTo(Cluster o) {
      return (int) Math.signum(id-o.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Cluster other = (Cluster) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + this.id;
        return hash;
    }
}
