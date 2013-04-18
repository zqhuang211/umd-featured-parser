/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author zqhuang
 */
public class BiSet<K1, E> extends HashMap<K1, HashSet<E>> implements Cloneable {

    private static final long serialVersionUID = 1L;

    @Override
    public BiSet<K1, E> clone() {
        BiSet<K1, E> newBiSet = new BiSet<K1, E>();
        for (Map.Entry<K1, HashSet<E>> entry : entrySet()) {
            HashSet<E> newSet = new HashSet<E>();
            for (E e : entry.getValue())
                newSet.add(e);
            newBiSet.put(entry.getKey(), newSet);
        }
        return newBiSet;
    }

    public synchronized boolean add(K1 key1, E element) {
        HashSet<E> uniSet = get(key1);
        if (uniSet == null) {
            uniSet = new HashSet<E>();
            put(key1, uniSet);
        }
        return uniSet.add(element);
    }

    public boolean contains(K1 key1, E element) {
        HashSet<E> uniSet = get(key1);
        if (uniSet == null) {
            return false;
        }
        return uniSet.contains(element);
    }

    public boolean isEmpty(K1 key1) {
        HashSet<E> uniSet = get(key1);
        if (uniSet == null) {
            return true;
        }
        return uniSet.isEmpty();
    }

    public boolean remove(K1 key1, E element) {
        HashSet<E> uniSet = get(key1);
        if (uniSet == null) {
            return false;
        }
        return uniSet.remove(element);
    }

    public int size(K1 key1) {
        HashSet<E> uniSet = get(key1);
        if (uniSet == null) {
            return 0;
        }
        return uniSet.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (K1 key : keySet()) {
            sb.append(key + "->" + get(key) + "\n");
        }
        return sb.toString();
    }
}
