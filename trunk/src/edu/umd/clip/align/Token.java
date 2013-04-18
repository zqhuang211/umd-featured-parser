/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.align;

/**
 *
 * @author zqhuang
 */
public class Token {

    private String word;
    private String label;
    private Token alignedTo;

    public Token(String word) {
        this(word, null);
    }
    
    public Token(String word, String label) {
        this.word = word;
        this.label = label;
    }

    public Token getAlignedTo() {
        return alignedTo;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void setAlignedTo(Token alignedTo) {
        this.alignedTo = alignedTo;
    }

    public String getWord() {
        return word;
    }

    public String getLabel() {
        return label;
    }
}
