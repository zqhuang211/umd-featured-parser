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
public class TriMap<K3, K2, K1, V> extends HashMap<K3, BiMap<K2, K1, V>> {

    private static final long serialVersionUID = 1L;

    public boolean containsKey(K3 key3, K2 key2) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return false;
        }
        return biMap.containsKey(key2);
    }

    public boolean containsKey(K3 key3, K2 key2, K1 key1) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return false;
        }
        return biMap.containsKey(key2, key1);
    }

    @Override
    public boolean containsValue(Object value) {
        for (K3 key3 : keySet()) {
            if (containsValue(key3, value)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsValue(K3 key3, Object value) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return false;
        }
        return biMap.containsValue(value);
    }

    public boolean containsValue(K3 key3, K2 key2, V value) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return false;
        }
        return biMap.containsValue(key2, value);
    }

    public Set<Map.Entry<K2, HashMap<K1, V>>> entrySet(K3 key3) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return null;
        }
        return biMap.entrySet();
    }

    public Set<Map.Entry<K1, V>> entrySet(K3 key3, K2 key2) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return null;
        }
        return biMap.entrySet(key2);
    }

    public HashMap<K1, V> get(K3 key3, K2 key2) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return null;
        }
        return biMap.get(key2);
    }

    public V get(K3 key3, K2 key2, K1 key1) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return null;
        }
        return biMap.get(key2, key1);
    }

    public Set<K2> keySet(K3 key3) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return null;
        }
        return biMap.keySet();
    }

    public Set<K1> keySet(K3 key3, K2 key2) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return null;
        }
        return biMap.keySet(key2);
    }

    public V put(K3 key3, K2 key2, K1 key1, V value) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            biMap = new BiMap<K2, K1, V>();
            put(key3, biMap);
        }
        return biMap.put(key2, key1, value);
    }

    public void putAll(TriMap<? extends K3, ? extends K2, ? extends K1, ? extends V> m) {
        for (K3 key3 : m.keySet()) {
            BiMap<K2, K1, V> biMap = get(key3);
            if (biMap == null) {
                biMap = new BiMap<K2, K1, V>();
                put(key3, biMap);
            }
            biMap.putAll(m.get(key3));
        }
    }

    public V remove(K3 key3, K2 key2, K1 key1) {
        V oldValue = null;
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return oldValue;
        }
        oldValue = biMap.remove(key2, key1);
        if (biMap.isEmpty()) {
            remove(key3);
        }
        return oldValue;
    }

    public int size(K3 key3) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return 0;
        }
        return biMap.size();
    }

    public int size(K3 key3, K2 key2) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return 0;
        }
        return biMap.size(key2);
    }

    public Collection<HashMap<K1, V>> values(K3 key3) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return null;
        }
        return biMap.values();
    }

    public Collection<V> values(K3 key3, K2 key2) {
        BiMap<K2, K1, V> biMap = get(key3);
        if (biMap == null) {
            return null;
        }
        return biMap.values(key2);
    }
}
