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
public class PrefixSuffixExtractor implements PredicateExtractor, Serializable {
    private static final long serialVersionUID = 1L;

    private static String prefixType = "P";
    private static String suffixType = "S";
    private static int maxPrefixLen = 3;
    private static int maxSuffixLen = 3;

    @Override
    public List<String> extract(String word) {
        List<String> prefixSuffixList = new ArrayList<String>();
        int len = word.length();
        int prefixLen = Math.min(len, maxPrefixLen);
        for (int i = 1; i <= prefixLen; i++) {
            prefixSuffixList.add(prefixType + i + "=" + word.substring(0, i).intern());
        }
        int suffixLen = Math.min(len, maxSuffixLen);
        for (int i = 1; i <= suffixLen; i++) {
            prefixSuffixList.add(suffixType + i + "=" + word.substring(len - i).intern());
        }
        return prefixSuffixList;
    }
}
