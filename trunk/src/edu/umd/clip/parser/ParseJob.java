/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.clip.parser;

/**
 *
 * @author zqhuang
 */
public class ParseJob{

    public int sentId;
    public String parseTree;

    public ParseJob(int sentId, String parseTree) {
        this.sentId = sentId;
        this.parseTree = parseTree;
    }
}
