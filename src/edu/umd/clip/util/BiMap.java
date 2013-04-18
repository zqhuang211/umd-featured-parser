/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class BiMap<K2, K1, V> extends HashMap<K2, HashMap<K1, V>> {
    private static final long serialVersionUID = 1L;
    public boolean containsKey(K2 key2, K1 key1) {
        HashMap<K1, V> uniMap = get(key2);
        if (uniMap == null) {
            return false;
        }
        return uniMap.containsKey(key1);
    }

    @Override
    public boolean containsValue(Object value) {
        for (K2 key2 : keySet()) {
            if (containsValue(key2, value)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsValue(K2 key2, Object value) {
        HashMap<K1, V> uniMap = get(key2);
        if (uniMap == null) {
            return false;
        }
        return uniMap.containsKey(value);
    }

    public Set<Map.Entry<K1, V>> entrySet(K2 key2) {
        HashMap<K1, V> uniMap = get(key2);
        if (uniMap == null) {
            return null;
        }
        return uniMap.entrySet();
    }

    public V get(K2 key2, K1 key1) {
        HashMap<K1, V> uniMap = get(key2);
        if (uniMap == null) {
            return null;
        }
        return uniMap.get(key1);
    }

    public Set<K1> keySet(K2 key2) {
        HashMap<K1, V> uniMap = get(key2);
        if (uniMap == null) {
            return null;
        }
        return uniMap.keySet();
    }

    public V put(K2 key2, K1 key1, V value) {
        HashMap<K1, V> uniMap = get(key2);

        if (uniMap == null) {
            uniMap = new HashMap<K1, V>();
            put(key2, uniMap);
        }
        return uniMap.put(key1, value);
    }

    public void putAll(BiMap<? extends K2, ? extends K1, ? extends V> m) {
        for (K2 key2 : m.keySet()) {
            HashMap<K1, V> uniMap = get(key2);
            if (uniMap == null) {
                uniMap = new HashMap<K1, V>();
                put(key2, uniMap);
            }
            uniMap.putAll(m.get(key2));
        }
    }

    public V remove(K2 key2, K1 key1) {
        V oldValue = null;
        HashMap<K1, V> uniMap = get(key2);
        if (uniMap == null) {
            return oldValue;
        }
        oldValue = uniMap.remove(key1);
        if (uniMap.isEmpty()) {
            remove(key2);
        }
        return oldValue;
    }

    public int size(K2 key2) {
        HashMap<K1, V> uniMap = get(key2);
        if (uniMap == null) {
            return 0;
        }
        return uniMap.size();
    }

    public Collection<V> values(K2 key2) {
        HashMap<K1, V> uniMap = get(key2);
        if (uniMap == null) {
            return null;
        }
        return uniMap.values();
    }
}
