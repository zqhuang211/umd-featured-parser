/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class WordIdentityExtractor implements PredicateExtractor, Serializable {
    private static final long serialVersionUID = 1L;

    private static String type = "WID";

    @Override
    public List<String> extract(String word) {
         String predicate = type + "=" + word;
        return Arrays.asList(predicate.intern());
    }
}
