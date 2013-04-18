/*
 * Operator.java
 *
 * Created on May 16, 2007, 11:17 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.math;

/**
 *
 * @author zqhuang
 */
public interface Operator<T> {
    public T applyOperator(T arg);
    public T applyOperator(T arg1, T arg2);
}
