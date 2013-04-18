/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.ling.PennTreebankReader;
import edu.umd.clip.ling.Tree;
import edu.umd.clip.ling.Trees;
import edu.umd.clip.util.GlobalLogger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author zqhuang
 */
public class Corpus {

    private static final long serialVersionUID = 1L;

    public static enum LANGUAGE {

        ENGLISH, CHINESE
    }
    public static LANGUAGE language;
    private List<Tree<String>> trainTrees = new ArrayList<Tree<String>>();

    public Corpus(String trainList) throws Exception {
        loadTrees(trainList);
        for (Tree<String> tree : trainTrees) {
            tree.getYield().size();
        }
    }

    private void loadTrees(String trainList) throws Exception {
        GlobalLogger.log(Level.INFO, String.format("Loading training trees from training file list: %s", trainList));
        trainTrees.addAll(readTrees(trainList, Charset.forName("UTF-8")));
        int nTokens = 0;
        for (Tree<String> tree : trainTrees) {
            nTokens += tree.getYield().size();
        }
        GlobalLogger.log(Level.INFO, String.format("Number of training trees: %d", trainTrees.size()));
        GlobalLogger.log(Level.INFO, String.format("Number of training tokens: %d", nTokens));
    }

    public static List<Tree<String>> readTrees(String listFile, Charset charset) throws Exception {
        Collection<Tree<String>> trees = PennTreebankReader.readTrees(charset, listFile);
        // normalize trees
        Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
        List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
        int fileId = 0;
        for (Tree<String> tree : trees) {
            fileId++;
            if (tree.getChildren().size() != 1) {
                System.err.println("Warning: the root node in the " + fileId + "-th tree in list " + listFile + " has " + tree.getChildren().size() + " children, it is now removed: " + tree);
                continue;
            }
            Tree<String> normalizedTree = treeTransformer.transformTree(tree);
            normalizedTreeList.add(normalizedTree);
        }
        if (normalizedTreeList.isEmpty()) {
            throw new Exception("failed to load any trees in the file list " + listFile);
        }

        return normalizedTreeList;
    }

    public static List<Tree<String>> binarizeAndFilterTrees(List<Tree<String>> trees, int verticalAnnotations,
            int horizontalAnnotations,
            Binarization binarization, boolean VERBOSE) {
        List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();
        GlobalLogger.log(Level.INFO, String.format("Binarizing and annotating trees..."));

        if (VERBOSE) {
            GlobalLogger.log(Level.INFO, String.format("annotation levels: vertical=" + verticalAnnotations + " horizontal=" + horizontalAnnotations));
        }

        int i = 0;
        for (Tree<String> tree : trees) {
            i++;
            Tree<String> processedTree = null;
            processedTree = TreeAnnotations.processTree(tree, verticalAnnotations, horizontalAnnotations, binarization, false);
            binarizedTrees.add(processedTree);
            tree.setChildren(null);
        }
        return binarizedTrees;
    }

    public List<Tree<String>> getTrainTrees() {
        return trainTrees;
    }
}
