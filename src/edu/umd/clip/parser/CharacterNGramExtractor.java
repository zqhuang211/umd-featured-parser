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
public class CharacterNGramExtractor implements PredicateExtractor, Serializable {
    private static final long serialVersionUID = 1L;

    private static String affixType = "A";
    private static int maxAffixLen = 4;

    public List<String> extract(String word) {
        List<String> predList = new ArrayList<String>();
        int len = word.length();
        int affixLen = Math.min(len, maxAffixLen);
        for (int i = 1; i <= affixLen; i++) {
            predList.add((affixType + i + "=" + word.substring(0, i)).intern());
        }
        return predList;
    }
}
