/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.jobs;

/**
 *
 * @author zqhuang
 */
public class SynchronizedCounter {
    private double count = 0;
    public synchronized void add(double c) {
        count += c;
    }

    public double getCount() {
        return count;
    }
}
