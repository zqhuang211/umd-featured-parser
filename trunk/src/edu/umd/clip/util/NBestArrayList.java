/*
 * NBestList.java
 *
 * Created on May 3, 2007, 11:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * An array holds the n-best objects added to this list.
 * The objects kept in the array are sorted all the time. 
 * The n-th best object is removed out of the array when
 * another better object is added to the array.
 *
 * @author Zhongqiang Huang
 */

public class NBestArrayList extends ArrayList {
    protected int maxSize;
    protected Comparator comparator;
    
    /**
     * Initializes the array with maximum allowable objects set to maxSize 
     */
    public NBestArrayList(int maxSize, Comparator comparator) {
        this.maxSize = maxSize;
        this.comparator = comparator;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean add(Object element) {
        if (size() < maxSize) {
            super.add(element);
            Collections.sort(this, comparator);
        }
        else {
            if (comparator.compare(element, super.get(maxSize-1)) < 0) {
                remove(maxSize-1);
                super.add(element);
                Collections.sort(this, comparator);
            }
        }  
        return true;
    }
}
