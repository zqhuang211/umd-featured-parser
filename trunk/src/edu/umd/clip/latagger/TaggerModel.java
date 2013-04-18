/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.ling.Tree;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class TaggerModel implements Serializable {

    private static final long serialVersionUID = 1L;
    LatentEmission emission;
    LatentTransition transition;
    int numTags;
    int[] numStates;
    int[][] fineToCoarseMapping;

    public TaggerModel(LatentEmission emission, LatentTransition transition) {
        this.emission = emission;
        this.transition = transition;
    }

    public void setEmission(LatentEmission emission) {
        this.emission = emission;
    }

    public void setTransition(LatentTransition transition) {
        this.transition = transition;
    }

    public LatentTransition getTransition() {
        return transition;
    }

    public LatentEmission getEmission() {
        return emission;
    }

    public void setup(Tree<Integer>[] splitTrees, int depth) {
        numTags = splitTrees.length;
        numStates = new int[numTags];
        fineToCoarseMapping = new int[numTags][];

        for (int ti = 0; ti < numTags; ti++) {
            List<Tree<Integer>> splitTreeList = splitTrees[ti].getAtDepth(depth);
            numStates[ti] = splitTreeList.size();
            fineToCoarseMapping[ti] = new int[splitTreeList.size()];
            if (depth > 0) {
                List<Tree<Integer>> coarserSplitTreeList = splitTrees[ti].getAtDepth(depth - 1);
                for (int csi = 0; csi < coarserSplitTreeList.size(); csi++) {
                    for (Tree<Integer> child : coarserSplitTreeList.get(csi).getChildren()) {
                        fineToCoarseMapping[ti][child.getLabel()] = csi;
                    }
                }
            }
        }
    }

    public int[] getNumStates() {
        return numStates;
    }

    public int getNumTags() {
        return numTags;
    }

    public int[][] getFineToCoarseMapping() {
        return fineToCoarseMapping;
    }
}
