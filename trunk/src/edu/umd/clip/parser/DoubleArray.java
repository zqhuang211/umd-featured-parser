/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author zqhuang
 */
public class DoubleArray implements Serializable {

    private static final long serialVersionUID = 1L;
    private double[] array;

    public DoubleArray() {
        array = null;
    }

    public DoubleArray(double[] array) {
        this.array = array;
    }

    public void clearArray() {
        Arrays.fill(array, 0.0);
    }

    public void setArray(double[] array) {
        this.array = array;
    }

    public double[] getArray() {
        return array;
    }

    public synchronized void add(double[] other) {
        if (array == null) {
            array = new double[other.length];
        } else {
            assert array.length == other.length;
        }
        for (int i = 0; i < array.length; i++) {
            array[i] += other[i];
        }
    }

    public synchronized void add(double[] other, double weight) {
        if (array == null) {
            array = new double[other.length];
        } else {
            assert array.length == other.length;
        }
        for (int i = 0; i < array.length; i++) {
            array[i] += other[i] * weight;
        }
    }

    public synchronized void add(double[] other, double[] weights) {
        assert array.length == weights.length;
        if (array == null) {
            array = new double[other.length];
        } else {
            assert array.length == other.length;
        }
        for (int i = 0; i < array.length; i++) {
            array[i] += other[i] * weights[i];
        }
    }

    @Override
    public String toString() {
        if (array == null) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < array.length; i++) {
                sb.append(array[i]).append(" ");
            }
            sb.setLength(sb.length() - 1);
            sb.append(']');
            return sb.toString();
        }
    }
}
