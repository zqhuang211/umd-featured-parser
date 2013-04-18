/*
 * StateTreeList.java
 *
 * Created on Nov 1, 2007, 11:46:41 PM
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.ling.Tree;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class ConstituentTreeList extends AbstractCollection<Tree<Constituent>> {

    private static final long serialVersionUID = 1L;
    private List<Tree<Constituent>> constituentTrees;
    private List<Tree<String>> stringTrees;
    private boolean lowMem = false;
    private Grammar grammar;

    static class ConstituentTreeIterator implements Iterator<Tree<Constituent>> {

        Iterator<Tree<String>> stringTreeIterator;
        Grammar grammar = null;

        public ConstituentTreeIterator(List<Tree<String>> stringTrees, Grammar grammar) {
            stringTreeIterator = stringTrees.iterator();
            this.grammar = grammar;
        }

        public boolean hasNext() {
            return stringTreeIterator.hasNext();
        }

        public Tree<Constituent> next() {
            Tree<String> stringTree = stringTreeIterator.next();
            return stringTreeToConstituentTree(stringTree, grammar);
        }

        public void remove() {
            stringTreeIterator.remove();
        }
    }

    public void setMemoryEfficient(boolean memoryEfficient) {
        this.lowMem = memoryEfficient;
    }

    public boolean isMemoryEfficient() {
        return lowMem;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return constituentTrees.removeAll(c);
    }

    public List<Tree<Constituent>> getTrees() {
        return constituentTrees;
    }

    @Override
    public Iterator<Tree<Constituent>> iterator() {
        if (lowMem) {
            return new ConstituentTreeIterator(stringTrees, grammar);
        } else {
            return constituentTrees.iterator();
        }
    }

    @Override
    public int size() {
        return constituentTrees.size();
    }

    public static void setParent(Tree<String> tree) {
        if (tree.isLeaf()) {
            return;
        }
        for (Tree<String> child : tree.getChildren()) {
            child.setParent(tree);
            setParent(child);
        }
    }

    public static void percolatePseudoPunc(Tree<String> tree, List<String> pseudoPuncs) {
        List<Tree<String>> terminals = tree.getTerminals();
        setParent(tree);
        for (int i = 0; i < pseudoPuncs.size(); i++) {
            String punc = pseudoPuncs.get(i);
            Tree<String> word = terminals.get(i);
            Tree<String> ancestor = word.getParent();
            Tree<String> validAncestor = ancestor;
            while (ancestor.getParent().getParent() != null) {
                List<Tree<String>> parentWords = ancestor.getParent().getTerminals();
                if (parentWords.get(parentWords.size() - 1) == word) {
                    ancestor = ancestor.getParent();
                    if (!ancestor.getLabel().startsWith("@")) {
                        validAncestor = ancestor;
                    }
                } else {
                    break;
                }
            }
            validAncestor.setExtraLabel(punc);
        }
    }

    public static void replacePosBreaks(Tree<String> tree, List<String> breaks) {
        List<Tree<String>> preterminals = tree.getPreTerminals();
        if (preterminals.size() != breaks.size()) {
            throw new RuntimeException("length mismatch...");
        }
        for (int i = 0; i < breaks.size(); i++) {
            String punc = preterminals.get(i).getExtraLabel();
            if (punc != null && (punc.equals("*4*") || punc.equals("*p*"))) {
                breaks.set(i, "*1*");
            }
        }
    }

    /**
     * Convert string-formated trees to constituent structure formatted trees.
     * If lowMem=true, only keep the string trees and the constituent trees are
     * constructed on the fly. Otherwise, the constituent trees are keeps and
     * the string tree structures are destroyed on the fly to reduce memory
     * load.
     *
     * @param stringTrees
     * @param grammar
     * @param lowMem
     */
    public ConstituentTreeList(List<Tree<String>> stringTrees, Grammar grammar,
            boolean lowMem) {
        this.lowMem = lowMem;
        this.grammar = grammar;
        if (lowMem) {
            this.stringTrees = stringTrees;
            for (Tree<String> tree : stringTrees) {
                stringTreeToConstituentTree(tree, grammar);
            }
        } else {
            this.constituentTrees = new ArrayList<Tree<Constituent>>();
            for (Tree<String> tree : stringTrees) {
                this.constituentTrees.add(stringTreeToConstituentTree(tree, grammar));
                tree.setChildren(null); // distroy string tree structure.
            }
        }
    }

    public static Tree<Constituent> stringTreeToConstituentTree(Tree<String> tree, Grammar grammar) {
        Tree<Constituent> result = stringTreeToConstituentTree(tree, 0, tree.getYield().size(), grammar);
        List<Constituent> words = result.getYield();
        for (int position = 0; position < words.size(); position++) {
            words.get(position).setFrom(position);
            words.get(position).setTo(position + 1);
        }
        return result;
    }

    public static Tree<Constituent> stringTreeToConstituentTree(Tree<String> tree, int from, int to,
            Grammar grammar) {
        if (tree.isLeaf()) {
            String word = tree.getLabel();
            Constituent constituent = new Constituent(-1, word, from, to);
            return new Tree<Constituent>(constituent);
        }
        String nodeName = tree.getLabel();
        if (nodeName.equals("ROOT")) {
            nodeName = "ROOT^g";
        }
        int node = grammar.getNode(nodeName);
        Constituent constituent = new Constituent(node);
        Tree<Constituent> newTree = new Tree<Constituent>(constituent);
        List<Tree<Constituent>> newChildren = new ArrayList<Tree<Constituent>>();
        for (Tree<String> child : tree.getChildren()) {
            short length = (short) child.getYield().size();
            Tree<Constituent> newChild = stringTreeToConstituentTree(child, from, from + length,
                    grammar);
            from += length;
            newChildren.add(newChild);
        }
        newTree.setChildren(newChildren);
        return newTree;
    }
}
