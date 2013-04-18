/*
 * LatentTagStates.java
 *
 * Created on May 22, 2007, 12:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.util.Numberer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author zqhuang
 */
public class LatentTagStates {

    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(LatentTagStates.class.getName());
    private static int[] latentStateNum = null;
    private static boolean[][] latentStateMergeSignal = null; // merge if true
    private static Set<Integer> notSplitTagSet = new HashSet<Integer>();
    private static Tree<Integer>[] splitTrees = null;

    public static void printInfo() {
        int totalStates = 0;
        Numberer tagNumberer = Numberer.getGlobalNumberer("tags");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < latentStateNum.length; i++) {
            String tag = (String) tagNumberer.object(i);
            sb.append(tag + ":" + latentStateNum[i] + ", ");
            totalStates += latentStateNum[i];
        }
        System.out.println(sb);
        System.out.println("Total number of latent states: " + totalStates);
    }

    public static void setSplitTrees(Tree<Integer>[] splitTrees) {
        LatentTagStates.splitTrees = splitTrees;
    }
    //    private static Set<Integer> splitableTagSet = new HashSet<Integer>();

    public static void initLatentTagStates() {
        Numberer tagNumberer = Numberer.getGlobalNumberer("tags");
        int tagNumbers = tagNumberer.total();
        for (Integer tagIndex : tagNumberer.numbers()) {
            if (tagIndex >= tagNumbers) {
                throw new RuntimeException("Error: the indexing in tagNumberer is invalid.");
            }
        }
        latentStateNum = new int[tagNumbers];
        Arrays.fill(latentStateNum, 1);
        notSplitTagSet.add(tagNumberer.number("SOS"));
        notSplitTagSet.add(tagNumberer.number("EOS"));
        latentStateMergeSignal = new boolean[tagNumbers][];
        splitTrees = new Tree[tagNumbers];
        for (int i = 0; i < tagNumbers; i++) {
            if (!notSplitTagSet.contains(i)) {
                latentStateMergeSignal[i] = new boolean[1];
            } else {
                latentStateMergeSignal[i] = null;
            }
            splitTrees[i] = new Tree<Integer>(0);
        }
    }

    public static void initRestart(int stage) {
        Numberer tagNumberer = Numberer.getGlobalNumberer("tags");
        int numTags = tagNumberer.total();
        notSplitTagSet.add(tagNumberer.number("SOS"));
        notSplitTagSet.add(tagNumberer.number("EOS"));
        latentStateMergeSignal = new boolean[numTags][];
        for (int tag = 0; tag < numTags; tag++) {
            if (notSplitTagSet.contains(tag)) {
                latentStateMergeSignal[tag] = null;
            } else {
                if (stage == 1 && latentStateNum[tag] % 2 != 0) {
                    throw new RuntimeException("Incorrect split...");
                }
                if (stage == 1) {
                    latentStateMergeSignal[tag] = new boolean[latentStateNum[tag] / 2];
                } else {
                    latentStateMergeSignal[tag] = new boolean[latentStateNum[tag]];
                }
                ArrayMath.fill(latentStateMergeSignal[tag], false);
            }
        }
    }

    public static boolean[][] getLatentStateMergeSignal() {
        return latentStateMergeSignal;
    }

    public static boolean[] getLatentStateMergeSignal(int tag) {
        return latentStateMergeSignal[tag];
    }

    public static void setLatentStateMergeSignal(int tag, int tagState, boolean signal) {
        latentStateMergeSignal[tag][tagState] = signal;
    }

    public static boolean getLatentStateMergeSignal(int tag, int tagState) {
        return latentStateMergeSignal[tag][tagState];
    }

    public static Tree<Integer>[] getSplitTrees() {
        return splitTrees;
    }

    public static void setLatentStateNum(int[] stateNumber) {
        latentStateNum = stateNumber;
    }

    public static void mergeStates() {
        logger.info("Before merging I have " + ArrayMath.sum(latentStateNum) + "sub-states");
        Numberer tagNumberer = Numberer.getGlobalNumberer("tags");
        for (int i = 0; i < latentStateMergeSignal.length; i++) {
            if (latentStateMergeSignal[i] == null) {
                List<Tree<Integer>> children = new ArrayList<Tree<Integer>>();
                children.add(new Tree<Integer>(0));
                List<Tree<Integer>> terminals = splitTrees[i].getTerminals();
                if (terminals.size() != 1) {
                    throw new RuntimeException("more than 1 state in non-split tag");
                }
                terminals.get(0).setChildren(children);
                continue;
            }
            int newTagStateNum = 0;
            List<Tree<Integer>> terminals = splitTrees[i].getTerminals();
            int oldTagStateNum = latentStateNum[i];
            if (oldTagStateNum != terminals.size() * 2 || oldTagStateNum != latentStateMergeSignal[i].length * 2) {
                throw new RuntimeException("Length does not match");
            }
            for (int j = 0; j < latentStateMergeSignal[i].length; j++) {
                List<Tree<Integer>> children = new ArrayList<Tree<Integer>>();
                if (latentStateMergeSignal[i][j]) {
                    Tree<Integer> tree1 = new Tree<Integer>(newTagStateNum);
                    children.add(tree1);
                    newTagStateNum += 1;
                } else {
                    Tree<Integer> tree1 = new Tree<Integer>(newTagStateNum);
                    children.add(tree1);
                    newTagStateNum += 1;
                    Tree<Integer> tree2 = new Tree<Integer>(newTagStateNum);
                    children.add(tree2);
                    newTagStateNum += 1;
                }
                terminals.get(j).setChildren(children);
            }
            latentStateMergeSignal[i] = new boolean[newTagStateNum];
            ArrayMath.fill(latentStateMergeSignal[i], false);
            logger.info(tagNumberer.object(i) + " had " + latentStateNum[i] + " sub-states, and now has " + newTagStateNum);
            latentStateNum[i] = newTagStateNum;
        }
        logger.info("After merging I have " + ArrayMath.sum(latentStateNum) + "sub-states");
    }

    //    public static Set<Integer> getNotSplitTagSet() {
    //        return notSplitTagSet;
    //    }
    /**
     *
     * @param tagIndex
     * @return
     */
    public static boolean isNotSplitableTag(int tagIndex) {
        //        return !splitableTagSet.contains(tagIndex);
        return notSplitTagSet.contains(tagIndex);
    }

    public static void splitStates() {
        for (int i = 0; i < latentStateNum.length; i++) {
            latentStateNum[i] *= 2;
        }

        for (Integer i : notSplitTagSet) {
            latentStateNum[i] = 1;
        }
    }

    public static int[] getLatentStateNum() {
        return latentStateNum;
    }

    /**
     *
     * @param tagIndex
     * @return
     */
    public static int getLatentStateNum(Integer tagIndex) {
        return latentStateNum[tagIndex];
    }
}
