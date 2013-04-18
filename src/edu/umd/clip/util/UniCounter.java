/*
 * UniCounter.java
 *
 * Created on May 12, 2007, 12:27 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.util;

import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.math.Operator;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * UniCounter maintains counts of keys, with each operation for setting,
 * getting, and increasing counts of keys.
 *
 * @param K1 
 * @author Zhongiang Huang
 */
public final class UniCounter<K1> implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private Map<K1, Double> uniCounter;
    private double totalCount = 0;

    public void clear() {
        uniCounter.clear();
        totalCount = 0;
    }

    public double calcConditionalEntropy() {
        if (totalCount == 0) {
            return 0;
        }
        double entropy = 0;
        for (Map.Entry<K1, Double> entry : entrySet()) {
            double count = entry.getValue();
            if (count < 0) {
                throw new RuntimeException("count < 0");
            }
            if (count > 0) {
                double prob = count / totalCount;
                entropy += -prob * Math.log(prob) / ArrayMath.LOG2;
            }
        }
        if (Double.isInfinite(entropy) || Double.isNaN(entropy)) {
            entropy = 0;
        }
        return entropy;
    }

    public int getNonZeroCounts() {
        int nonZeroCounts = 0;
        for (Map.Entry<K1, Double> entry : entrySet()) {
            double count = entry.getValue();
            if (count < 0) {
                throw new RuntimeException("count < 0");
            }
            if (count > 0) {
                nonZeroCounts++;
            }
        }
        return nonZeroCounts;
    }

    @Override
    public UniCounter<K1> clone() {
        UniCounter<K1> newUniCounter = new UniCounter<K1>();
        for (K1 key : uniCounter.keySet()) {
            newUniCounter.setCount(key, uniCounter.get(key));
        }
        return newUniCounter;
    }

    /**
     * The elements in the counter.
     *
     * @return set of keys
     */
    public Set<K1> keySet() {
        return uniCounter.keySet();
    }

    /**
     * Apply operation on the counts of this counter.
     */
    @SuppressWarnings("unchecked")
    public void applyOperator(Operator operator) {
        for (K1 key : keySet()) {
            double newValue = (Double) operator.applyOperator(getCount(key));
            setCount(key, newValue);
        }
    }

    /**
     * The number of entries in the counter (not the total count -- use totalCount() instead).
     */
    public int size() {
        return uniCounter.size();
    }

    /**
     * True if there are no entries in the counter (false does not mean totalCount > 0)
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns whether the counter contains the given key.  Note that this is the
     * way to distinguish keys which are in the counter with count zero, and those
     * which are not in the counter (and will therefore return count zero from
     * getCount().
     *
     * @param key
     * @return whether the counter contains the key
     */
    public boolean containsKey(K1 key) {
        return uniCounter.containsKey(key);
    }

    /**
     * Finds the total of all counts in the counter.  This implementation iterates
     * through the entire counter every time this method is called.
     *
     * @return the counter's total
     */
    public double getCount() {
        if (totalCount == 0) {
            for (Double value : uniCounter.values()) {
                totalCount += value;
            }
        }
        return totalCount;
    }

    /**
     * Get the count of the element, or zero if the element is not in the
     * counter.
     *
     * @param key
     * @return the count of the element
     */
    public double getCount(K1 key) {
        Double value = uniCounter.get(key);
        if (value == null) {
            return 0;
        }
        return value;
    }

    /**
     * Destructively normalize this Counter in place.
     */
    public void normalize() {
        double oldTotalCount = totalCount;
        if (oldTotalCount == 0) {
            for (K1 key : keySet()) {
                setCount(key, 0);
            }
        } else {
            for (K1 key : keySet()) {
                double newValue = getCount(key) / oldTotalCount;
                setCount(key, newValue);
            }
        }
    }

    /**
     * Destrutively normalize this Counter in place using totalCount.
     *
     * @param totalCount
     */
    public void normalize(double overallCount) {
        for (K1 key : keySet()) {
            double newValue = getCount(key) / overallCount;
            setCount(key, newValue);
        }
    }

    /**
     * Set the count for the given key, clobbering any previous count.
     *
     * @param key
     * @param count
     */
    public synchronized void setCount(K1 key, double count) {
        totalCount -= getCount(key);
        uniCounter.put(key, count);
        totalCount += count;
    }

    /**
     * Increment a key's count by the given amount.
     *
     * @param key
     * @param increment
     */
    public synchronized void incrementCount(K1 key, double increment) {
        setCount(key, getCount(key) + increment);
    }

    /**
     * Increment each element in a given collection by a given amount.
     */
    public synchronized void incrementAll(Collection<? extends K1> collection, double count) {
        for (K1 key : collection) {
            incrementCount(key, count);
        }
    }

    public synchronized <T extends K1> void incrementAll(UniCounter<T> uniCounter) {
        for (T key : uniCounter.keySet()) {
            double count = uniCounter.getCount(key);
            incrementCount(key, count);
        }
    }

    /**
     * Returns a string representation with key/count pair separated by "\n".
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return toString("");
    }

    /**
     * Returns a string representation with each key/count pair headed by
     * the prefix and separated by "\n".
     *
     * @return prefixed string representaion
     */
    public String toString(String prefix) {
        StringBuilder sb1 = new StringBuilder();
        for (Map.Entry<K1, Double> entry : uniCounter.entrySet()) {
            StringBuilder sb2 = new StringBuilder(prefix);
            sb2.append(entry.getKey());
            sb2.append(" : ");
            sb2.append(entry.getValue());
            sb2.append("\n");
            sb1.append(sb2.toString());
        }
        return sb1.toString();
    }

    public Set<Map.Entry<K1, Double>> entrySet() {
        return uniCounter.entrySet();
    }

    public UniCounter() {
        totalCount = 0;
        uniCounter = new HashMap<K1, Double>();
    }

    public UniCounter(UniCounter<? extends K1> uniCounter) {
        this();
        incrementAll(uniCounter);
    }

    public UniCounter(Collection<? extends K1> collection) {
        this();
        incrementAll(collection, 1.0);
    }

    public static void main(String[] args) {
        UniCounter<String> counter = new UniCounter<String>();
        System.out.println(counter);
        counter.incrementCount("planets", 7);
        System.out.println(counter);
        counter.incrementCount("planets", 1);
        System.out.println(counter);
        counter.setCount("suns", 1);
        System.out.println(counter);
        counter.setCount("aliens", 0);
        System.out.println(counter);
        System.out.println(counter.toString());
        System.out.println("Total: " + counter.getCount());
    }
}

