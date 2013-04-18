/*
 * BiCounter.java
 *
 * Created on May 12, 2007, 12:27 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.util;

import edu.umd.clip.math.Operator;
import java.io.Serializable;
import java.util.HashMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class BiCounter<K2, K1> implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private Map<K2, UniCounter<K1>> biCounter;
    private double totalCount = 0;

    public double calcConditionalEntropy() {
        if (totalCount == 0) {
            return 0;
        }
        double entropy = 0;
        for (Map.Entry<K2, UniCounter<K1>> biEntry : entrySet()) {
            UniCounter<K1> uniCounter = biEntry.getValue();
            double count = uniCounter.getCount();
            if (count < 0) {
                throw new RuntimeException("count < 0");
            }
            if (count > 0) {
                entropy += count / totalCount * uniCounter.calcConditionalEntropy();
//                        (uniCounter.calcConditionalEntropy() +
//                        Math.log(totalCount / count) / ArrayMath.LOG2);
            }
        }
        if (Double.isInfinite(entropy) || Double.isNaN(entropy)) {
            entropy = 0;
        }
        return entropy;
    }

    @Override
    public BiCounter<K2, K1> clone() {
        BiCounter<K2, K1> newBiCounter = new BiCounter<K2, K1>();
        for (K2 key : biCounter.keySet()) {
            newBiCounter.setCounter(key, biCounter.get(key).clone());
        }
        return newBiCounter;
    }

    /**
     * Ensure that the UniCounter with key is in the biCounter.
     *
     * @param key the key for the UniCounter
     * @return the UniCounter
     */
    public void setCounter(K2 key, UniCounter<K1> uniCounter) {
        totalCount -= getCount(key);
        biCounter.put(key, uniCounter);
        totalCount += uniCounter.getCount();
    }

    public UniCounter<K1> ensureCounter(K2 key) {
        UniCounter<K1> uniCounter = biCounter.get(key);
        if (uniCounter == null) {
            uniCounter = new UniCounter<K1>();
            biCounter.put(key, uniCounter);
        }
        return uniCounter;
    }

    /**
     * Apply operation on the counts of this counter.
     */
    public void applyOperator(Operator operator) {
        totalCount = 0;
        for (Entry<K2, UniCounter<K1>> entry : entrySet()) {
            UniCounter<K1> uniCounter = entry.getValue();
            uniCounter.applyOperator(operator);
            totalCount += uniCounter.getCount();
        }
    }

    /**
     * Returns the keys that have been inserted into this BiCounter.
     *
     * @return 
     */
    public Set<K2> keySet() {
        return biCounter.keySet();
    }

    /**
     * Sets the count for a particular (key, value) pair.
     */
    public void setCount(K2 key2, K1 key1, double count) {
        UniCounter<K1> uniCounter = ensureCounter(key2);
        totalCount -= getCount(key2, key1);
        uniCounter.setCount(key1, count);
        totalCount += count;
    }

    /**
     * Increments the count for a particular (key, value) pair.
     */
    public void incrementCount(K2 key2, K1 key1, double count) {
        UniCounter<K1> uniCounter = ensureCounter(key2);
        uniCounter.incrementCount(key1, count);
        totalCount += count;
    }

    /**
     * Gets the count of the given (key, value) entry, or zero if that entry is
     * not present.  Does not create any objects.
     */
    public double getCount(K2 key2, K1 key1) {
        UniCounter<K1> uniCounter = biCounter.get(key2);
        if (uniCounter == null) {
            return 0.0;
        }
        return uniCounter.getCount(key1);
    }

    /**
     * Gets the sub-counter for the given key.  If there is none, a counter is
     * created for that key, and installed in the BiCounter.  You can, for
     * example, add to the returned empty counter directly (though you shouldn't).
     * This is so whether the key is present or not, modifying the returned
     * counter has the same effect (but don't do it).
     */
    public UniCounter<K1> getCounter(K2 key) {
        return biCounter.get(key);
    }

    /**
     * Gets the total count of the given key, or zero if that key is not
     * present. Does not create any objects.
     */
    public double getCount(K2 key) {
        UniCounter<K1> uniCounter = biCounter.get(key);
        if (uniCounter == null) {
            return 0.0;
        }
        return uniCounter.getCount();
    }

    /**
     * Returns the total of all counts in sub-counters.  This implementation is
     * linear; it recalculates the total each time.
     */
    public double getCount() {
        if (totalCount == 0) {
            for (UniCounter<K1> uniCounter : biCounter.values()) {
                totalCount += uniCounter.getCount();
            }
        }
        return totalCount;
    }

    public void normalize() {
        double newTotalCount = 0;
        for (Entry<K2, UniCounter<K1>> entry : entrySet()) {
            UniCounter<K1> uniCounter = entry.getValue();
            uniCounter.normalize();
            newTotalCount += uniCounter.getCount();
        }
        totalCount = newTotalCount;
    }

    public void normalize(double overallCount) {
        totalCount = 0;
        for (Entry<K2, UniCounter<K1>> entry : entrySet()) {
            UniCounter<K1> uniCounter = entry.getValue();
            uniCounter.normalize(overallCount);
            totalCount += uniCounter.getCount();
        }
    }

    /**
     * Returns a string representation with each key1/key2/count separated
     * by "\n"
     */
    @Override
    public String toString() {
        return toString("");
    }

    /**
     * Returns a string representation with each key1/key2/count headed by
     * prefix and separated by "\n";
     */
    public String toString(String prefix) {
        StringBuilder sb1 = new StringBuilder();
        for (Map.Entry<K2, UniCounter<K1>> entry : biCounter.entrySet()) {
            StringBuilder sb2 = new StringBuilder(prefix);
            sb2.append(entry.getKey());
            sb2.append("->");
            sb1.append(entry.getValue().toString(sb2.toString()));
        }
        return sb1.toString();
    }

    public Set<Map.Entry<K2, UniCounter<K1>>> entrySet() {
        return biCounter.entrySet();
    }

    /**
     * Creates a BiCounter Instance
     */
    public BiCounter() {
        totalCount = 0;
        biCounter = new HashMap<K2, UniCounter<K1>>();
    }

    public static void main(String[] args) {
        BiCounter<String, String> bigramCounterMap = new BiCounter<String, String>();
        bigramCounterMap.incrementCount("people", "run", 1);
        bigramCounterMap.incrementCount("cats", "growl", 2);
        bigramCounterMap.incrementCount("cats", "scamper", 3);
        System.out.println(bigramCounterMap);
        System.out.println("Entries for cats: " + bigramCounterMap.getCounter("cats"));
        System.out.println("Entries for dogs: " + bigramCounterMap.getCounter("dogs"));
        System.out.println("Count of cats scamper: " + bigramCounterMap.getCount("cats", "scamper"));
        System.out.println("Count of snakes slither: " + bigramCounterMap.getCount("snakes", "slither"));
        System.out.println("Total count: " + bigramCounterMap.getCount());
        System.out.println(bigramCounterMap);
    }

    public boolean containsKey(K2 key2, K1 key1) {
        UniCounter<K1> uniCounter = biCounter.get(key2);
        if (uniCounter == null) {
            return false;
        }
        return uniCounter.containsKey(key1);
    }

    public boolean containsKey(K2 key2) {
        UniCounter<K1> uniCounter = biCounter.get(key2);
        if (uniCounter == null) {
            return false;
        }
        return true;
    }
}
