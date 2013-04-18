/*
 * UnaryOperator.java
 *
 * Created on May 16, 2007, 11:20 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.math;

/**
 *
 * @author zqhuang
 */
public abstract class UnaryOperator<T> implements Operator<T> {
    public T applyOperator(T arg) {
        throw new UnsupportedOperationException("This is a abstract class.");
    }

    public T applyOperator(T arg1, T arg2) {
        throw new UnsupportedOperationException();
    }   
}
