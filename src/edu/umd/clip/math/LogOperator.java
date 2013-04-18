/*
 * LogOperator.java
 *
 * Created on May 17, 2007, 11:40 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.math;

/**
 *
 * @author Zhongqiang Huang
 */
public class LogOperator extends UnaryOperator<Double> {
    @Override
    public Double applyOperator(Double arg) {
        return Math.log(arg);
    }
}
