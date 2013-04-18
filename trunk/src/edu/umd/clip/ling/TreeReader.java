/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.ling;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.ling.Trees;
import edu.umd.clip.parser.Binarization;
import edu.umd.clip.parser.ConstituentTreeList;
import edu.umd.clip.parser.Grammar;
import edu.umd.clip.parser.Constituent;
import edu.umd.clip.parser.TreeAnnotations;
import java.util.Iterator;

/**
 *
 * @author zqhuang
 */
public class TreeReader {

    public static Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();

    public static Tree<String> string2Tree(String line) {
        Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
        Tree<String> stringTree = treeIterator.next();
        if (stringTree != null) {
            Tree<String> normalizedTree = treeTransformer.transformTree(stringTree);
            Tree<String> binarizedTree = TreeAnnotations.processTree(normalizedTree, 1, 0, Binarization.RIGHT, false);
            return binarizedTree;
        } else {
            return null;
        }
    }

    public static Tree<String> string2TreeNoBinarize(String line) {
        Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
        Tree<String> stringTree = treeIterator.next();
        if (stringTree != null) {
            Tree<String> normalizedTree = treeTransformer.transformTree(stringTree);
            return normalizedTree;
        } else {
            return null;
        }
    }

    public static Tree<Constituent> stringTree2NodeConstituentTree(Tree<String> stringTree, Grammar grammar) {
        return ConstituentTreeList.stringTreeToConstituentTree(stringTree, grammar);
    }

    public static Tree<Constituent> string2NodeConstituentTree(String line, Grammar grammar) {
        return stringTree2NodeConstituentTree(string2Tree(line), grammar);
    }

    public static Tree<String> string2TreeLeftBinarize(String line) {
        Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
        Tree<String> stringTree = treeIterator.next();
        if (stringTree != null) {
            Tree<String> normalizedTree = treeTransformer.transformTree(stringTree);
            Tree<String> binarizedTree = TreeAnnotations.processTree(normalizedTree, 1, 0, Binarization.LEFT, false);
            return binarizedTree;
        } else {
            return null;
        }
    }
}
