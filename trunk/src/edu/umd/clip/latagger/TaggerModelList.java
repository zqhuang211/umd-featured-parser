/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.ling.Tree;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author zqhuang
 */
public class TaggerModelList extends ArrayList<TaggerModel> implements Serializable {

    private static final long serialVersionUID = 1L;

    public TaggerModelList(LatentEmission finestEmission, LatentTransition finestTransition, Tree<Integer>[] splitTrees) {
        LatentEmission finerEmission = finestEmission;
        LatentTransition finerTransition = finestTransition;

        int numTags = splitTrees.length;
        int numSplits = 0;
        for (int i = 0; i < numTags; i++) {
            Tree<Integer> splitTree = splitTrees[i];
            int depth = splitTree.getDepth();
            if (depth > 1) {
                if (numSplits == 0) {
                    numSplits = depth - 1;
                }
                if (numSplits != depth - 1) {
                    throw new RuntimeException("depth doesnot match...");
                }
            }
        }

        add(new TaggerModel(finerEmission, finerTransition));

        for (int i = numSplits; i >= 1; i--) {
            LatentEmission coarserEmission = finerEmission.getCoarserEmission(splitTrees, i);
            LatentTransition coarserTransition = finerTransition.getCoarserTransition(splitTrees, i);

            finerEmission = coarserEmission;
            finerTransition = coarserTransition;
            add(0, new TaggerModel(finerEmission, finerTransition));
        }

        for (int i = 0; i <= numSplits; i++) {
            get(i).setup(splitTrees, i);
        }
    }
}
