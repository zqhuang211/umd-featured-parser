/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class EnglishMorphExtractor implements PredicateExtractor, Serializable {

    private static final long serialVersionUID = 1L;
    private static String featType = "M";

    public List<String> extract(String word) {
        List<String> featList = new ArrayList<String>();
        int wlen = word.length();
        int numCaps = 0;
        boolean hasDigit = false;
        boolean hasDash = false;
        boolean hasLower = false;
        for (int i = 0; i < wlen; i++) {
            char ch = word.charAt(i);
            if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (ch == '-') {
                hasDash = true;
            } else if (Character.isLetter(ch)) {
                if (Character.isLowerCase(ch)) {
                    hasLower = true;
                } else if (Character.isTitleCase(ch)) {
                    hasLower = true;
                    numCaps++;

                } else {
                    numCaps++;
                }
            }
        }
        char ch0 = word.charAt(0);
        if (numCaps > 0) {
            featList.add(featType + "-CAPS");
        }

        if (hasDigit) {
            featList.add(featType + "-NUM");
        }

        if (hasDash) {
            featList.add(featType + "-DASH");
        }

        String lowered = word.toLowerCase();
        if (lowered.endsWith("s") && wlen >= 3) {
            char ch2 = lowered.charAt(wlen - 2);
            if (ch2 != 's' && ch2 != 'i' && ch2 != 'u') {
                featList.add(featType + "-s");
            }
        } else if (word.length() >= 5 && !hasDash && !(hasDigit && numCaps > 0)) {
            if (lowered.endsWith("ed")) {
                featList.add(featType + "-ed");
            } else if (lowered.endsWith("ing")) {
                featList.add(featType + "-ing");
            } else if (lowered.endsWith("ion")) {
                featList.add(featType + "-ion");
            } else if (lowered.endsWith("er")) {
                featList.add(featType + "-er");
            } else if (lowered.endsWith("est")) {
                featList.add(featType + "-est");
            } else if (lowered.endsWith("ly")) {
                featList.add(featType + "-ly");
            } else if (lowered.endsWith("ity")) {
                featList.add(featType + "-ity");
            } else if (lowered.endsWith("y")) {
                featList.add(featType + "-y");
            } else if (lowered.endsWith("al")) {
                featList.add(featType + "-al");
            }
        }
        return featList;
    }
}
