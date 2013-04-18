/*
 * Converter.java
 *
 * Created on May 15, 2007, 6:14 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.util.Pair;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class Converter {

    private static final long serialVersionUID = 1L;

    /** Creates a new instance of Converter */
    public Converter() {
    }

 
    public static List<String> stringToCharacters(String str) {
        List<String> unicodeCharList = new ArrayList<String>();
        char[] charArray = str.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            int codePoint = Character.codePointAt(charArray, i);
            //            if (!Character.isLetter(codePoint))
            //                throw new RuntimeException("Error decoding input with Unicode.");
            if (codePoint <= '\uFFFF') {
                unicodeCharList.add((new String(charArray, i, 1)).intern());
            } else {
                unicodeCharList.add((new String(charArray, i, 2)).intern());
                i++;
            }
        }
        return unicodeCharList;
    }

    public static Pair splitWordTagItem(String wordTagItem) {
        int i;
        for (i = wordTagItem.length() - 2; i > 0; i--) {
            if (wordTagItem.charAt(i) == '/') {
                break;
            }
        }
        if (i <= 0) {
            throw new RuntimeException("Error: not a word/tag pair: " + wordTagItem);
        }

        return new Pair<String, String>(wordTagItem.substring(0, i), wordTagItem.substring(i + 1));
    }

    public static void main(String[] args) {
        String a = "???abc";
        String b = "abcd";
        String c = "()?acd1";

    }
}
