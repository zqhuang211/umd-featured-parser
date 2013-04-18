/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.clip.parser;

import java.util.List;

/**
 *
 * @author zqhuang
 */
public interface PredicateExtractor {
    public List<String> extract(String word);
}