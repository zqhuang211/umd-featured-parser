/*
 * LatentUnigramTransitionCollection.java
 * 
 * Created on May 24, 2007, 12:25:23 AM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.latagger;

import edu.umd.clip.math.Operator;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author zqhuang
 */
public class LatentUnigramTransitionCollection implements Iterable<LatentUnigramTransitionItem> {
    private static final long serialVersionUID = 1L;
    private Collection<LatentUnigramTransitionItem> collection;
    
    public LatentUnigramTransitionCollection() {
        collection = null;
    }
    
    public LatentUnigramTransitionCollection(Collection<LatentUnigramTransitionItem> collection) {
        this.collection = collection;
    }
    
    public void setCollection(Collection<LatentUnigramTransitionItem> collection) {
        this.collection = collection;
    }
    
    public void applyOperator(Operator operator) {
        for (LatentUnigramTransitionItem item : collection)
            item.applyOperator(operator);
    }   
    public double getNormalizationFactor() {
        double normalizationFactor = 0;
        int tagStateNum = 0;
        for (LatentUnigramTransitionItem item : collection) {
            tagStateNum = item.getTagStateNum();
            for (int ts = 0; ts < tagStateNum; ts++)
                normalizationFactor += item.getLatentScore(ts);
        }
        return normalizationFactor;
    }
    
    public void normalize() {
        double normalizationFactor = getNormalizationFactor();
        for (LatentUnigramTransitionItem item : collection)
            item.normalize(normalizationFactor);
    }
    
    public Iterator<LatentUnigramTransitionItem> iterator() {
        return collection.iterator();
    }
}

