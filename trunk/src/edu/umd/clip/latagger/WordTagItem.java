/*
 * WordTagItem.java
 *
 * Created on May 15, 2007, 10:51 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.latagger;

import java.util.List;

/**
 *
 * @author zqhuang
 */
public class WordTagItem {
    private static final long serialVersionUID = 1L;
    private String word;
    private Integer tag;
    
    public WordTagItem(String word, Integer tag) {
        this.word = word.intern();
        this.tag = tag;
    }

    /** Creates a new instance of WordTagItem */
    public WordTagItem(String word, Integer tag, List<String> charList) {
        this.word = word;
        this.tag = tag;
    }

    public String getWord() {
        return word;
    }

    public Integer getTag() {
        return tag;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void setTag(Integer tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(word);
        sb.append("/");
        sb.append(tag);
        return sb.toString();
    }
}
