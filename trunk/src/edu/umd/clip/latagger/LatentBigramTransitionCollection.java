/*
 * LatentBigramTransitionCollection.java
 *
 * Created on May 23, 2007, 2:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.latagger;

import edu.umd.clip.math.Operator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author Zhongqiang Huang
 */
public class LatentBigramTransitionCollection implements Iterable<LatentBigramTransitionItem> {
    private static final long serialVersionUID = 1L;
    private Collection<LatentBigramTransitionItem> collection;
    
    public LatentBigramTransitionCollection() {
        collection = null;
    }
    
    public LatentBigramTransitionCollection(Collection<LatentBigramTransitionItem> collection) {
        this.collection = collection;
    }
    
    public void setCollection(Collection<LatentBigramTransitionItem> collection) {
        this.collection = collection;
    }
    
    public double[] getNormalizationFactor() {
        double[] normalizationFactor = null;
        boolean firstItem = true;
        int prevTagStateNum = 0, currTagStateNum = 0;
        for (LatentBigramTransitionItem item : collection) {
            if (firstItem) {
                prevTagStateNum = item.getPrevTagStateNum();
                currTagStateNum = item.getCurrTagStateNum();
                normalizationFactor = new double[prevTagStateNum];
                Arrays.fill(normalizationFactor, 0.0);
                firstItem = false;
            } else {
                currTagStateNum = item.getCurrTagStateNum();
                if (prevTagStateNum != item.getPrevTagStateNum())
                    throw new RuntimeException("Error: prevTagStateNum does not match in a LatentBigramTranstionCollection");
            }
            
            for (int pts = 0; pts < prevTagStateNum; pts++)
                for (int cts = 0; cts < currTagStateNum; cts++)
                    normalizationFactor[pts] += item.getLatentScore(pts, cts);
        }
        return normalizationFactor;
    }
    
    public void normalize() {
        double[] normalizationFactor = getNormalizationFactor();
        for (LatentBigramTransitionItem item : collection)
            item.normalize(normalizationFactor);
    }
    
    public void applyOperator(Operator operator) {
        for (LatentBigramTransitionItem item : collection)
            item.applyOperator(operator);
    }
     
    public Iterator<LatentBigramTransitionItem> iterator() {
        return collection.iterator();
    }
}
