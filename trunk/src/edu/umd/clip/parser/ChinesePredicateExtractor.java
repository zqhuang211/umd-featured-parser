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
public class ChinesePredicateExtractor implements PredicateExtractor, Serializable {

    private static final long serialVersionUID = 1L;
    private static String prefixType = "P";
    private static String suffixType = "S";
    private static String digitType = "has_digit";
    private static String uppercaseType = "has_uc";
    private static String hyphenType = "has_hyphen";
    private static String letterType = "has_letter";

    @Override
    public List<String> extract(String word) {
        List<String> predicateList = new ArrayList<String>();
        List<String> charList = wordToCharList(word);

        int len = charList.size();
        boolean containsDigit = false;
        boolean containsUppercase = false;
        boolean containsHyphen = false;
        boolean containsLetter = false;
        String prefix = "";
        String suffix = "";
        for (int i = 0; i < len; i++) {
            String affix = charList.get(i);
            prefix += affix;
            suffix = charList.get(len - 1 - i) + suffix;
            if (i == 0) {
                predicateList.add(prefixType + i + "=" + prefix.intern());
                predicateList.add(suffixType + i + "=" + suffix.intern());
            }
            predicateList.add("has_" + affix);
            char[] charArray = word.toCharArray();
            if (charArray.length == 1) {
                char character = charArray[0];
                if (!containsDigit && Character.isDigit(character)) {
                    containsDigit = true;
                    predicateList.add(digitType.intern());
                }
                if (!containsUppercase && Character.isUpperCase(character)) {
                    containsUppercase = true;
                    predicateList.add(uppercaseType.intern());
                }
                if (!containsHyphen && character == '-') {
                    containsHyphen = true;
                    predicateList.add(hyphenType.intern());
                }
                if (!containsLetter && Character.isLetter(character)) {
                    containsLetter = true;
                    predicateList.add(letterType.intern());
                }
            }
        }
        return predicateList;
    }

    public List<String> wordToCharList(String word) {
        List<String> charList = new ArrayList<String>();
        char[] charArray = word.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            int codePoint = Character.codePointAt(charArray, i);
            String unicodeChar;
            if (codePoint <= '\uFFFF') {
                unicodeChar = new String(charArray, i, 1);
            } else {
                unicodeChar = new String(charArray, i, 2);
                i++;
            }
            charList.add(unicodeChar);
        }
        return charList;
    }
}
