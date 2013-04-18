/*
 * LatentLatentTaggerData.java
 *
 * Created on May 24, 2007, 9:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.latagger;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.util.Numberer;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author Zhongqiang Huang
 */
public class LatentTaggerData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    LatentEmission latentEmission;
    LatentTransition latentTransition;
    Map<String, Numberer> numberer;
    int[] latentStateNumber;
    Tree<Integer>[] splitTrees;

    public LatentTaggerData() {
    }
    
    public LatentTaggerData(LatentEmission latentEmission, 
            LatentTransition latentTransition, 
            Map<String, Numberer> numberer, 
            int[] latentStateNumber,
            Tree<Integer>[] splitTrees) {
        this.latentEmission = latentEmission;
        this.latentTransition = latentTransition;
        this.numberer = numberer;
        this.latentStateNumber = latentStateNumber;
        this.splitTrees = splitTrees;
    }

    public LatentEmission getLatentEmission() {
        return latentEmission;
    }

    public LatentTransition getLatentTransition() {
        return latentTransition;
    }

    public int[] getLatentStateNum() {
        return latentStateNumber;
    }

    public Map<String, Numberer> getNumberer() {
        return numberer;
    }

    public boolean save(String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName); // Save to file
            GZIPOutputStream gzos = new GZIPOutputStream(fos); // Compressed
            ObjectOutputStream out = new ObjectOutputStream(gzos); // Save objects
            out.writeObject(this); // Write the mix of grammars
            out.flush(); // Always flush the output.
            out.close(); // And close the stream.
        } catch (IOException e) {
            System.out.println("IOException: "+e);
            return false;
        }
        return true;
    }
    
    public static LatentTaggerData load(String fileName) {
        LatentTaggerData pData = null;
        try {
            FileInputStream fis = new FileInputStream(fileName); // Load from file
            GZIPInputStream gzis = new GZIPInputStream(fis); // Compressed
            ObjectInputStream in = new ObjectInputStream(gzis); // Load objects
            pData = (LatentTaggerData)in.readObject(); // Read the mix of grammars
            in.close(); // And close the stream.
        } catch (IOException e) {
            System.out.println("IOException\n"+e);
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found!");
            return null;
        }
        return pData;
    }

    public Tree<Integer>[] getSplitTrees() {
        return splitTrees;
    }
    
    
}
