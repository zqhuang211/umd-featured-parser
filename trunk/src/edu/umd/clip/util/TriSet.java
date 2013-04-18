/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.util;

import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author zqhuang
 */
public class TriSet<K2, K1, E> extends HashMap<K2, BiSet<K1, E>> {

    private static final long serialVersionUID = 1L;

    public HashSet<E> get(K2 key2, K1 key1) {
        BiSet<K1, E> biSet = get(key2);
        if (biSet == null) {
            return null;
        }
        return biSet.get(key1);
    }

    public boolean add(K2 key2, K1 key1, E element) {
        BiSet<K1, E> biSet = get(key2);
        if (biSet == null) {
            biSet = new BiSet<K1, E>();
            put(key2, biSet);
        }
        return biSet.add(key1, element);
    }

    public boolean contains(K2 key2, K1 key1, E element) {
        BiSet<K1, E> biSet = get(key2);
        if (biSet == null) {
            return false;
        }
        return biSet.contains(key1, element);
    }

    public boolean isEmpty(K2 key2) {
        BiSet<K1, E> biSet = get(key2);
        if (biSet == null) {
            return true;
        }
        return biSet.isEmpty();
    }

    public boolean isEmpty(K2 key2, K1 key1) {
        BiSet<K1, E> biSet = get(key2);
        if (biSet == null) {
            return true;
        }
        return biSet.isEmpty(key1);
    }

    public boolean remove(K2 key2, K1 key1, E element) {
        BiSet<K1, E> biSet = get(key2);
        if (biSet == null) {
            return false;
        }
        return biSet.remove(key1, element);
    }

    public int size(K2 key2, K1 key1) {
        BiSet<K1, E> biSet = get(key2);
        if (biSet == null) {
            return 0;
        }
        return biSet.size(key1);
    }
}
