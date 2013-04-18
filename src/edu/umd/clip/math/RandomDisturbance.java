/*
 * Random.java
 *
 * Created on May 21, 2007, 7:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.math;

import java.util.Random;

/**
 *
 * @author zqhuang
 */
public class RandomDisturbance {

    private static Random random = new Random(0);
    private static double randomness = 0.01;
    private static int index = 0;
    
    public static void setRandSeed(long seed) {
        random.setSeed(seed);
    }
    
    public static void setRandomness(double randomness) {
        RandomDisturbance.randomness = randomness;
    }

    public static double generateRandomDisturbance(double prob) {
        return prob * randomness;
    }
    
    public static double generateRandomDisturbance() {
        double temp = randomness * (random.nextDouble() - 0.5);
//        System.err.println("rand = " + temp);
        return temp;
//        index++;
//        if (index > 10)
//            index = 0;
//        return randomness * (index++ - 5) * 0.01;
    }

    public static double generatePositiveRandomeDisturbance() {
        return randomness * (random.nextDouble());
    }
    
    public static double generateRandomDisturbance(int fakeSeed) {
        return randomness * (fakeSeed - 5) * 0.01;
    }
    
    public static double getRandomness() {
        return randomness;
    }
}
