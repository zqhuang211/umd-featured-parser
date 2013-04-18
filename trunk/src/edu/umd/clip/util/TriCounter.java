/*
 * TriCounter.java
 *
 * Created on May 12, 2007, 12:28 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.util;

import edu.umd.clip.math.Operator;
import java.io.Serializable;
import java.util.HashMap;

import java.util.Map;
import java.util.Set;


/**
 *
 * @param K3 
 * @param K2 
 * @param K1 
 * @author zqhuang
 */
public class TriCounter<K3,K2,K1> implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    Map<K3, BiCounter<K2,K1>> triCounter;
    
    @Override
    public TriCounter<K3,K2,K1> clone() {
        TriCounter<K3,K2,K1> newTriCounter = new TriCounter<K3,K2,K1>();
        for (K3 key : triCounter.keySet()) {
            newTriCounter.setCounter(key, triCounter.get(key).clone());
        }
        return newTriCounter;
    }
    
    public void setCounter(K3 key, BiCounter<K2,K1> biCounter) {
        triCounter.put(key, biCounter);
    }
    
    /**
     * Ensure that the BiCounter with key is in the triCounter.
     *
     * @param key the key for the UniCounter
     * @return the UniCounter
     */
    protected BiCounter<K2,K1> ensureCounter(K3 key) {
        BiCounter<K2,K1> biCounter = triCounter.get(key);
        if (biCounter == null) {
            biCounter = new BiCounter<K2,K1>();
            triCounter.put(key, biCounter);
        }
        return biCounter;
    }
    
    /**
     * Apply operation on the counts of this counter.
     * @param operator 
     */
    public void applyOperator(Operator operator) {
        for (K3 key : keySet())
            getCounter(key).applyOperator(operator);
    }
    
    /**
     * Returns the keys that have been inserted into this TriCounter.
     *
     * @return 
     */
    public Set<K3> keySet() {
        return triCounter.keySet();
    }
    
    /**
     * Sets the count for a particular (key, value) pair.
     * @param key3 
     * @param key2 
     * @param key1 
     * @param count 
     */
    public void setCount(K3 key3, K2 key2, K1 key1, double count) {
        BiCounter<K2,K1> biCounter = ensureCounter(key3);
        biCounter.setCount(key2, key1, count);
    }
    
    /**
     * Increments the count for a particular (key, value) pair.
     * @param key3 
     * @param key2 
     * @param key1 
     * @param count 
     */
    public void incrementCount(K3 key3, K2 key2, K1 key1, double count) {
        BiCounter<K2,K1> biCounter = ensureCounter(key3);
        biCounter.incrementCount(key2, key1, count);
    }
    
    
    /**
     * Gets the sub-counter for the given key.  If there is none, a counter is
     * created for that key, and installed in the TriCounter.  You can, for
     * example, add to the returned empty counter directly (though you shouldn't).
     * This is so whether the key is present or not, modifying the returned
     * counter has the same effect (but don't do it).
     * @param key 
     * @return 
     */
    public BiCounter<K2,K1> getCounter(K3 key) {
        return triCounter.get(key);
    }
    
    public UniCounter<K1> getCounter(K3 key3, K2 key2) {
        BiCounter<K2,K1> biCounter = getCounter(key3);
        if (biCounter == null)
            return null;
        else
            return biCounter.getCounter(key2);
    }
    
    /**
     * Returns the total of all counts in sub-counters.  This implementation is
     * linear; it recalculates the total each time.
     * @return 
     */
    public double getCount() {
        double total = 0.0;
        for (Map.Entry<K3, BiCounter<K2,K1>> entry : triCounter.entrySet()) {
            BiCounter<K2,K1> counter = entry.getValue();
            total += counter.getCount();
        }
        return total;
    }
    
    public Set<Map.Entry<K3, BiCounter<K2,K1>>> entrySet() {
        return triCounter.entrySet();
    }
    
    /**
     * Gets the total count of the given key, or zero if that key is
     * not present.  Does not create any objects.
     * @param key 
     * @return 
     */
    public double getCount(K3 key) {
        BiCounter<K2,K1> biCounter = triCounter.get(key);
        if (biCounter == null)
            return 0.0;
        return biCounter.getCount();
    }
    
    /**
     * Gets the total counts of the given key3/key2 pair, or zero if
     * the pair is not present. Does not create any objects.
     * @param key3 
     * @param key2 
     * @return 
     */
    public double getCount(K3 key3, K2 key2) {
        BiCounter<K2,K1> biCounter = triCounter.get(key3);
        if (biCounter == null)
            return 0.0;
        return biCounter.getCount(key2);
    }
    
    public boolean containsKey(K3 key3, K2 key2, K1 key1) {
        BiCounter<K2, K1> biCounter = triCounter.get(key3);
        if (biCounter == null)
            return false;
        return biCounter.containsKey(key2, key1);
    }
    
    public boolean containsKey(K3 key3, K2 key2) {
        BiCounter<K2, K1> biCounter = triCounter.get(key3);
        if (biCounter == null)
            return false;
        return biCounter.containsKey(key2);
    }
    
    public boolean containsKey(K3 key3) {
        if (triCounter.get(key3) == null)
            return false;
        else
            return true;
    }
    
    /**
     * Gets the count of the given (key3, key2, key1) entry, or zero if that entry is
     * not present.  Does not create any objects.
     * @param key3 
     * @param key2 
     * @param key1 
     * @return 
     */
    public double getCount(K3 key3, K2 key2, K1 key1) {
        BiCounter<K2,K1> biCounter = triCounter.get(key3);
        if (biCounter == null)
            return 0.0;
        return biCounter.getCount(key2, key1);
    }
    
    public void normalize() {
        for (K3 key : keySet())
            getCounter(key).normalize();
    }
    
    public void normalize(double totalCount) {
        for (K3 key : keySet()) {
            getCounter(key).normalize(totalCount);
        }
    }
    /**
     * Returns a string representation with each (key3, key2, key1, count) separated
     * by "\n"
     * @return 
     */
    @Override
    public String toString() {
        return toString("");
    }
    
    /**
     * Returns a string representation with each (key3, key2, key1, count) headed by
     * prefix and separated by "\n";
     * @param prefix 
     * @return 
     */
    public String toString(String prefix) {
        StringBuilder sb1 = new StringBuilder();
        for (Map.Entry<K3, BiCounter<K2,K1>> entry : triCounter.entrySet()) {
            StringBuilder sb2 = new StringBuilder(prefix);
            sb2.append(entry.getKey());
            sb2.append("->");
            sb1.append(entry.getValue().toString(sb2.toString()));
        }
        return sb1.toString();
    }
    
    /**
     * Creates a TriCounter Instance
     */
    public TriCounter() {
        triCounter = new HashMap<K3, BiCounter<K2,K1>>();
    }
    
    
    public static void main(String[] args) {
        TriCounter<String, String, String> trigramCounter = new TriCounter<String, String, String>();
        trigramCounter.incrementCount("NV", "people", "run", 1);
        trigramCounter.incrementCount("VV", "cats", "growl", 2);
        trigramCounter.incrementCount("VP", "cats", "scamper", 3);
        System.out.println(trigramCounter);
        System.out.println("Entries for cats: "+trigramCounter.getCounter("VV"));
        System.out.println("Entries for dogs: "+trigramCounter.getCounter("VP"));
        System.out.println("Count of cats scamper: "+trigramCounter.getCount("VV", "cats", "scamper"));
        System.out.println("Count of snakes slither: "+trigramCounter.getCount("snakes", "slither"));
        System.out.println(trigramCounter);
    }
}
