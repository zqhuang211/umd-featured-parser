/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.clip.latagger;

/**
 *
 * @author zqhuang
 */
public class WordCount implements Comparable {
    String word;
    double count;

    public WordCount(String word, double count) {
        this.word = word;
        this.count = count;
    }

    public int compareTo(Object o) {
        WordCount other = (WordCount) o;
        return (int) Math.signum(other.count-count);
    }

    public String getWord() {
        return word;
    }

    public double getCount() {
        return count;
    }

    @Override
    public String toString() {
        return String.format("%s:%.4f", word, count);
    }
}
