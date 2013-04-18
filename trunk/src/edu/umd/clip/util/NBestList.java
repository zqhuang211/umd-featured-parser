/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.util;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author zqhuang
 */
public class NBestList<T extends Comparable<T>> extends ArrayList<T> {
    protected int maxSize;

    /**
     * Initializes the array with maximum allowable objects set to maxSize
     */
    public NBestList(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(T element) {
        if (size() < maxSize) {
            super.add(element);
            Collections.sort(this);
        } else {
            if (element.compareTo(super.get(maxSize-1)) < 0) {
                remove(maxSize-1);
                super.add(element);
                Collections.sort(this);
            }
        }
        return true;
    }
}
