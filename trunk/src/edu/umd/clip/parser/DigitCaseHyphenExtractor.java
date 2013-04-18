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
public class DigitCaseHyphenExtractor implements PredicateExtractor, Serializable {

    private static final long serialVersionUID = 1L;
    private static String digitType = "hasDigit";
    private static String initcapType = "initCap";
    private static String allcapType = "allCap";
    private static String hyphenType = "hasHyphen";

    @Override
    public List<String> extract(String word) {
        boolean containsDigit = false;
        boolean containsHyphen = false;
        boolean allCap = true;
        List<String> predictList = new ArrayList<String>();
        int len = word.length();
        if (Character.isUpperCase(word.charAt(0))) {
            predictList.add(initcapType.intern());
        }
        for (int i = 0; i < len; i++) {
            char character = word.charAt(i);
            if (!containsDigit && Character.isDigit(character)) {
                containsDigit = true;
                predictList.add(digitType.intern());
            }
            if (!containsHyphen && character == '-') {
                containsHyphen = true;
                predictList.add(hyphenType.intern());
            }
            if (allCap && !Character.isUpperCase(character)) {
                allCap = false;
            }
        }
        if (allCap) {
            predictList.add(allcapType.intern());
        }
        return predictList;
    }
}
