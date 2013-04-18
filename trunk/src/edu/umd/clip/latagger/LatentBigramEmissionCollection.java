/*
 * LatentBigramEmissionCollection.java
 *
 * Created on May 23, 2007, 2:21 PM
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
public class LatentBigramEmissionCollection implements Iterable<LatentBigramEmissionItem> {
    private static final long serialVersionUID = 1L;
    private Collection<LatentBigramEmissionItem> collection;
    
    public LatentBigramEmissionCollection() {
        collection = null;
    }
    
    public LatentBigramEmissionCollection(Collection<LatentBigramEmissionItem> collection) {
        this.collection = collection;
    }
    
    public void setCollection(Collection<LatentBigramEmissionItem> collection) {
        this.collection = collection;
    }
    
    public double[] getNormalizationFactor() {
        double[] normalizationFactor = null;
        boolean firstItem = true;
        int tagStateNum = 0;
        for (LatentBigramEmissionItem item : collection) {
            if (firstItem) {
                tagStateNum = item.getTagStateNum();
                normalizationFactor = new double[tagStateNum];
                Arrays.fill(normalizationFactor, 0.0);
                firstItem = false;
            } else {
                if (tagStateNum != item.getTagStateNum())
                    throw new RuntimeException("Error: tagStateNum does not match in a LatentBigramEmissionCollection");
            }
            
            for (int ts = 0; ts < tagStateNum; ts++)
                normalizationFactor[ts] += item.getLatentScore(ts);
        }
        return normalizationFactor;
    }
    
    public void normalize() {
        double[] normalizationFactor = getNormalizationFactor();
        for (LatentBigramEmissionItem item : collection)
            item.normalize(normalizationFactor);
    }
    
    public void increaseAll(int tagState, double amount) {
        for (LatentBigramEmissionItem item : collection)
            item.increase(tagState, amount);
    }
    
    public void applyOperator(Operator operator) {
        for (LatentBigramEmissionItem item : collection)
            item.applyOperator(operator);
    }
    
    public Iterator<LatentBigramEmissionItem> iterator() {
        return collection.iterator();
    }
}