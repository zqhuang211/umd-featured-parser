/*
 * Evaluates the precision and accuracy of two test_parses against the told_parse at different span level
 */
package edu.umd.clip.tools;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.ling.Trees;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import edu.umd.clip.util.UniCounter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author zqhuang
 */
public class NodeSpecificEval {

    public static class Options {

        @Option(name = "-gold", required = true, usage = "gold parse tree")
        public String goldFile = null;
        @Option(name = "-test", required = true, usage = "test parse tree")
        public String testFile = null;
        @Option(name = "-pos", required = false, usage = "also report pos (default: false)")
        public boolean reportPOS = false;
        @Option(name = "-fun", required = false, usage = "average parsing accuracy across funtion tags (default: false)")
        public boolean fun = false;
        @Option(name = "-nt", required = false, usage = "average parsing accuracy across nonterminal tags (default: false)")
        public boolean nt = false;
        @Option(name = "-ADVP_PRT", required = false, usage = "merge ADVP and PRT")
        public boolean merge_ap = false;
        @Option(name = "-noEdited", required = false, usage = "remove edited constituents")
        public boolean noEdited = false;
    }

    public static void main(String[] args) throws IOException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);

        BufferedReader goldReader = new BufferedReader(new InputStreamReader(new FileInputStream(opts.goldFile), Charset.forName("UTF-8")));
        BufferedReader testReader = new BufferedReader(new InputStreamReader(new FileInputStream(opts.testFile), Charset.forName("UTF-8")));

        String goldLine = "", testLine = "";


        UniCounter<Bracket> goldBracketCount = new UniCounter<Bracket>();
        UniCounter<Bracket> testBracketCount = new UniCounter<Bracket>();

        double goldNum = 0, testNum = 0;
        double matchNum = 0;
        UniCounter<String> goldNodeNum = new UniCounter<String>();
        UniCounter<String> testNodeNum = new UniCounter<String>();
        UniCounter<String> matchNodeNum = new UniCounter<String>();

        System.out.print("\tC\tR\tP\tF\n");

        int sentId = 0;
        while ((goldLine = goldReader.readLine()) != null) {
            sentId++;
            double thisGold = 0;
            double thisTest = 0;
            double thisMatch = 0;
            goldLine = goldLine.trim();
            Iterator<Tree<String>> goldIterator = new Trees.PennTreeReader(goldLine);
            Tree<String> goldTree = goldIterator.next();
            if (opts.merge_ap) {
                mergeADVPPRT(goldTree);
            }
            if (opts.noEdited) {
                removeEdited(goldTree);
            }
            removeEq(goldTree);

            setIndex(goldTree, opts.noEdited);
            goldBracketCount.clear();
            appendBracket(goldTree, goldBracketCount, true, opts.reportPOS);
            if (opts.noEdited) {
                goldBracketCount = mergeEdit(goldBracketCount);
            }

            testLine = testReader.readLine();
            testLine = testLine.trim();
            Iterator<Tree<String>> test1Iterator = new Trees.PennTreeReader(testLine);
            Tree<String> test1Tree = test1Iterator.next();
            if (opts.merge_ap) {
                mergeADVPPRT(test1Tree);
            }
            if (opts.noEdited) {
                removeEdited(test1Tree);
            }
            setIndex(goldTree, test1Tree);
            for (Tree<String> preterminal : test1Tree.getPreTerminals()) {
                preterminal.setDuration(1);
            }
            setEditDuration(test1Tree);
            testBracketCount.clear();
            appendBracket(test1Tree, testBracketCount, true, opts.reportPOS);
            if (opts.noEdited) {
                testBracketCount = mergeEdit(testBracketCount);
            }

            goldNum += (int) goldBracketCount.getCount();
            testNum += (int) testBracketCount.getCount();

            thisGold = goldBracketCount.getCount();
            thisTest = testBracketCount.getCount();

//            System.err.println(goldBracketCount.getCount());

            for (Entry<Bracket, Double> entry : goldBracketCount.entrySet()) {
                Bracket bracket = entry.getKey();
                Double count = entry.getValue();
                if (opts.fun) {
                    goldNodeNum.incrementCount(removeFun(bracket.getLabel()), count);
                } else if (opts.nt) {
                    goldNodeNum.incrementCount(removeNT(bracket.getLabel()), count);
                } else {
                    goldNodeNum.incrementCount(bracket.getLabel(), count);
                }
            }

            for (Entry<Bracket, Double> entry : testBracketCount.entrySet()) {
                Bracket bracket = entry.getKey();
                Double count = entry.getValue();
                if (opts.fun) {
                    testNodeNum.incrementCount(removeFun(bracket.getLabel()), count);
                } else if (opts.nt) {
                    testNodeNum.incrementCount(removeNT(bracket.getLabel()), count);
                } else {
                    testNodeNum.incrementCount(bracket.getLabel(), count);
                }
            }

            for (Bracket bracket : goldBracketCount.keySet()) {
                if (testBracketCount.containsKey(bracket)) {
                    int match = (int) Math.min(goldBracketCount.getCount(bracket), testBracketCount.getCount(bracket));
                    thisMatch += match;
                    matchNum += match;
                    if (opts.fun) {
                        matchNodeNum.incrementCount(removeFun(bracket.getLabel()), match);
                    } else if (opts.nt) {
                        matchNodeNum.incrementCount(removeNT(bracket.getLabel()), match);
                    } else {
                        matchNodeNum.incrementCount(bracket.getLabel(), match);
                    }
                }
            }
            double thisRecall = thisMatch / thisGold * 100;
            double thisPrecision = thisMatch / thisTest * 100;
            double thisF1 = 1 / (0.5 / thisRecall + 0.5 / thisPrecision);
            System.out.format("%d:\t%.0f(%.0f, %.0f)\t%.2f\t%.2f\t%.2f\n", sentId, thisGold, thisTest, thisMatch, thisRecall, thisPrecision, thisF1);
        }

        System.out.println("============= total =============");
        double recall = matchNum / goldNum * 100;
        double precision = matchNum / testNum * 100;
        double f1 = 1 / (0.5 / recall + 0.5 / precision);
        System.out.format("Total:\t%.0f(%.0f, %.0f)\t%.2f\t%.2f\t%.2f\n", goldNum, testNum, matchNum, recall, precision, f1);
        for (Entry<String, Double> goldEntry : goldNodeNum.entrySet()) {
            String node = goldEntry.getKey();
            double nGoldNum = goldEntry.getValue();
            double nTestNum = testNodeNum.getCount(node);
            double nMatchNum = matchNodeNum.getCount(node);

            double nrecall = nMatchNum / nGoldNum * 100;
            double nprecision = nMatchNum / nTestNum * 100;
            double nf1 = 1 / (0.5 / nrecall + 0.5 / nprecision);
            System.out.format("%s:\t%.0f(%.0f, %.2f)\t%.2f\t%.2f\t%.2f\n", node, nGoldNum, nTestNum, nMatchNum, nrecall, nprecision, nf1);
        }
    }

    private static void removeEdited2(Tree<String> tree) {
        if (tree.isPreTerminal()) {
            return;
        }
        String label = tree.getLabel();
        int pos = label.indexOf('-');
        if (pos != -1) {
            label = label.substring(0, pos);
        }

        if (label.equals("EDITED")) {
            return;
        }

        List<Tree<String>> children = tree.getChildren();
        while (true) {
            int editPos = -1;
            for (int i = 0; i < children.size(); i++) {
                Tree<String> child = children.get(i);
                label = child.getLabel();
                pos = label.indexOf('-');
                if (pos != -1) {
                    label = label.substring(0, pos);
                }
                if (label.equals("EDITED")) {
                    editPos = i;
                    break;
                }
            }
            if (editPos == -1) {
                break;
            }
            children.remove(editPos);
            String prevLabel = "";
            String nextLabel = "";
            if (editPos > 0) {
                if (!children.get(editPos - 1).isPreTerminal()) {
                    prevLabel = children.get(editPos - 1).getLabel();
                    pos = prevLabel.indexOf('-');
                    if (pos != -1) {
                        prevLabel = prevLabel.substring(0, pos);
                    }
                }
            }
            if (editPos < children.size()) {
                if (!children.get(editPos).isPreTerminal()) {
                    nextLabel = children.get(editPos).getLabel();
                    pos = nextLabel.indexOf('-');
                    if (pos != -1) {
                        nextLabel = nextLabel.substring(0, pos);
                    }
                }
            }
//            if (!prevLabel.equals("") && prevLabel.equals(nextLabel)) {
//                List<Tree<String>> newChildren = new ArrayList<Tree<String>>();
//                Tree<String> newChild = new Tree<String>(children.get(editPos - 1).getLabel());
//                newChild.setChildren(newChildren);
//                newChildren.addAll(children.get(editPos - 1).getChildren());
//                newChildren.addAll(children.get(editPos).getChildren());
//                children.add(editPos, newChild);
//                children.remove(editPos + 1);
//                children.remove(editPos - 1);
//            }
        }
        for (Tree<String> child : tree.getChildren()) {
            removeEdited(child);
        }
    }

    private static void removeEdited(Tree<String> tree) {
        if (tree.isPreTerminal()) {
            return;
        }
        String label = tree.getLabel();
        int pos = label.indexOf('-');
        if (pos != -1) {
            label = label.substring(0, pos);
        }

        if (label.equals("EDITED")) {
            List<Tree<String>> editChildren = new ArrayList<Tree<String>>();
            editChildren.addAll(tree.getPreTerminals());
            tree.setChildren(editChildren);
        } else {
            for (Tree<String> child : tree.getChildren()) {
                removeEdited(child);
            }
        }

//        List<Tree<String>> children = tree.getChildren();
//        for (int i = 0; i < children.size(); i++) {
//            Tree<String> child = children.get(i);
//            label = child.getLabel();
//            pos = label.indexOf('-');
//            if (pos != -1) {
//                label = label.substring(0, pos);
//            }
//            if (label.equals("EDITED")) {
//            }
//        }

//        List<Tree<String>> newChildren = new ArrayList<Tree<String>>();
//        newChildren.add(children.get(0));
//        boolean prevEdit = false;
//        String prevLabel = children.get(0).getLabel();
//        pos = prevLabel.indexOf('-');
//        if (pos != -1) {
//            prevLabel = prevLabel.substring(0, pos);
//        }
//        if (prevLabel.equals("EDITED")) {
//            prevEdit = true;
//        }
//
//        for (int i = 1; i < children.size(); i++) {
//            boolean currEdit = false;
//            String currLabel = children.get(i).getLabel();
//            pos = currLabel.indexOf('-');
//            if (pos != -1) {
//                currLabel = currLabel.substring(0, pos);
//            }
//            if (currLabel.equals("EDITED")) {
//                currEdit = true;
//            }
//            if (prevEdit && currEdit) {
//                List<Tree<String>> newPrevChildren = new ArrayList<Tree<String>>();
//                Tree<String> prevChild = newChildren.get(newChildren.size() - 1);
//                newPrevChildren.addAll(prevChild.getChildren());
//                newPrevChildren.addAll(children.get(i).getChildren());
//                prevChild.setChildren(newPrevChildren);
//            } else {
//                newChildren.add(children.get(i));
//            }
//            prevEdit = currEdit;
//        }

    }

    private static void mergeADVPPRT(Tree<String> tree) {
        if (tree.isPreTerminal()) {
            return;
        }

        String label = tree.getLabel();
        String extra = "";
        int pos = label.indexOf('~');
        if (pos != -1) {
            extra = label.substring(pos + 1);
            label =
                    label.substring(0, pos);
        }

        pos = label.indexOf('-');
        if (pos != -1) {
            label = label.substring(0, pos);
        }

        if (!extra.equals("")) {
            label = label + "~" + extra;
        }
        if (label.equals("ADVP") || label.equals("PRT")) {
            tree.setLabel("ADVP_PRT");
        }

        for (Tree<String> child : tree.getChildren()) {
            mergeADVPPRT(child);
        }

    }

    private static void removeEq(Tree<String> tree) {
        if (tree.isPreTerminal()) {
            return;
        }

        String label = tree.getLabel();
        String extra = "";
        int pos = label.indexOf('~');
        if (pos != -1) {
            extra = label.substring(pos + 1);
            label = label.substring(0, pos);
        }

        pos = label.indexOf('-');
        if (pos != -1) {
            label = label.substring(0, pos);
        }

        pos = label.indexOf('=');
        if (pos != -1) {
            label = label.substring(0, pos);
        }

        if (!extra.equals("")) {
            label = label + "~" + extra;
        }
        tree.setLabel(label);

        for (Tree<String> child : tree.getChildren()) {
            removeEq(child);
        }
    }

    private static String removeFun(String label) {
        int pos = label.indexOf('~');
        if (pos != -1) {
            return label.substring(0, pos);
        } else {
            return label;
        }

    }

    private static String removeNT(String label) {
        int pos = label.indexOf('~');
        if (pos != -1) {
            return label.substring(pos);
        } else {
            return label;
        }

    }

    private static void appendBracket(Tree<String> tree, UniCounter<Bracket> bracketCount, boolean noPunc, boolean reportPOS) {
        int start = 0;
//        boolean allEdit = true;
//        for (Tree<String> preterminal : tree.getPreTerminals()) {
//            if (preterminal.getDuration() != 0) {
//                allEdit = false;
//                break;
//            }
//        }
//        if (allEdit)
//            return;
//        bracketCount.incrementCount(new Bracket(0, tree.getYield().size(), "ROOT"), 1);
        for (Tree<String> child : tree.getChildren()) {
            int span = child.getYield().size();
            int end = start + span;
            appendBracket(child, start, end, bracketCount, noPunc, reportPOS);
            start = end;
        }

    }

    private static UniCounter<Bracket> mergeEdit2(UniCounter<Bracket> bracketCounter) {
        UniCounter<Bracket> newBracketCounter = new UniCounter<Bracket>();
        List<Bracket> editList = new ArrayList<Bracket>();
        for (Entry<Bracket, Double> entry : bracketCounter.entrySet()) {
            Bracket bracket = entry.getKey();
            double count = entry.getValue();
            if (bracket.getLabel().equals("EDITED")) {
                newBracketCounter.incrementCount(bracket, 1);
            } else {
                newBracketCounter.incrementCount(bracket, count);
            }
        }
        return newBracketCounter;
    }

    private static UniCounter<Bracket> mergeEdit(UniCounter<Bracket> bracketCounter) {
        UniCounter<Bracket> newBracketCounter = new UniCounter<Bracket>();
        List<Bracket> editList = new ArrayList<Bracket>();
        for (Entry<Bracket, Double> entry : bracketCounter.entrySet()) {
            Bracket bracket = entry.getKey();
            double count = entry.getValue();
            if (bracket.getLabel().equals("EDITED")) {
                if (count != 1) {
                    throw new RuntimeException("multiple edit...");
                }
                editList.add(bracket);
            } else {
                newBracketCounter.incrementCount(bracket, count);
            }
        }
        while (!editList.isEmpty()) {
            boolean find = false;
            Bracket edit1 = editList.get(0);
            for (int i = 1; i < editList.size(); i++) {
                Bracket edit2 = editList.get(i);
                if (edit1.start == edit2.end && edit2.start < edit2.end) {
                    edit2.end = edit1.end;
                    edit2.label += "_" + edit1.label;
                    find = true;
                }
                if (edit1.end == edit2.start && edit1.end > edit1.start) {
                    edit2.start = edit1.start;
                    edit2.label += "_" + edit1.label;
                    find = true;
                }
                if (find) {
                    break;
                }
            }
            editList.remove(edit1);
            if (!find) {
                newBracketCounter.incrementCount(edit1, 1);
            }
        }
        return newBracketCounter;
    }

    private static void appendBracket(Tree<String> tree, int start, int end, UniCounter<Bracket> bracketCount, boolean noPunc, boolean reportPOS) {
        if (tree.isPreTerminal()) {
            if (reportPOS) {
                bracketCount.incrementCount(new Bracket(start, start, tree.getLabel()), 1);
            }
            return;
        }

        if (tree.isLeaf()) {
            return;
        }

        String label = tree.getLabel();
        String extra = "";
        int pos = label.indexOf('~');
        if (pos != -1) {
            extra = label.substring(pos + 1);
            label = label.substring(0, pos);
        }

        pos = label.indexOf('-');
        if (pos != -1) {
            label = label.substring(0, pos);
        }

        if (!extra.equals("")) {
            label = label + "~" + extra;
        }
        List<Tree<String>> preterminals = tree.getPreTerminals();
//
//
        if (noPunc) {
            int newstart = start, newend = end;
            int i = 0;
            while (i < preterminals.size() && isPunc(preterminals.get(i).getLabel())) {
                newstart++;
                i++;
            }
            i = preterminals.size() - 1;
            while (i >= 0 && isPunc(preterminals.get(i).getLabel())) {
                newend--;
                i--;
            }

//            if (newend > newstart) {
//                bracketCount.incrementCount(new Bracket(preterminals.get(0).getEnd(), preterminals.get(preterminals.size() - 1).getEnd() + 1, label), 1);
            bracketCount.incrementCount(new Bracket(newstart, newend, label), 1);
//            }
        } else {
            bracketCount.incrementCount(new Bracket(start, end, label), 1);
        }
//        bracketCount.incrementCount(new Bracket(preterminals.get(0).getStart(), preterminals.get(preterminals.size() - 1).getEnd(), label), 1);
        for (Tree<String> child : tree.getChildren()) {
            int span = child.getYield().size();
            end = start + span;
            appendBracket(child, start, end, bracketCount, noPunc, reportPOS);
            start = end;
        }
    }

    private static boolean isPunc(String label) {
        if (label.equals("PU")) {
            return true;
        }
        if (label.equals(",")) {
            return true;
        }
        if (label.equals(":")) {
            return true;
        }
        if (label.equals("-")) {
            return true;
        }
        if (label.equals("``")) {
            return true;
        }
        if (label.equals("''")) {
            return true;
        }
        if (label.equals(".")) {
            return true;
        }
        return false;
    }

    private static void setIndex(Tree<String> goldTree, Tree<String> testTree) {
        List<Tree<String>> goldPreterminals = goldTree.getPreTerminals();
        List<Tree<String>> testPreterminals = testTree.getPreTerminals();
        if (goldPreterminals.size() != testPreterminals.size()) {
            throw new RuntimeException("length mismatch...");
        }
        for (int i = 0; i < goldPreterminals.size(); i++) {
            testPreterminals.get(i).setStart(goldPreterminals.get(i).getStart());
            testPreterminals.get(i).setEnd(goldPreterminals.get(i).getEnd());
        }
    }

    private static void setIndex(Tree<String> tree, boolean noEdit) {
        int loc = 0;
        List<Tree<String>> preterminals = tree.getPreTerminals();
        for (Tree<String> preterminal : preterminals) {
            preterminal.setStart(loc);
            loc++;
            preterminal.setEnd(loc);
            preterminal.setDuration(1);
        }
        if (noEdit) {
            setEditDuration(tree);

//            loc = 0;
//            for (Tree<String> preterminal : preterminals) {
//                if (preterminal.getDuration() != 0) {
//                    preterminal.setStart(loc);
//                    loc += preterminal.getDuration();
//                    preterminal.setEnd(loc);
//                }
//            }
//            for (int i = 0; i < preterminals.size(); i++) {
//                if (preterminals.get(i).getDuration() == 0) {
//                    if (i > 0) {
//                        preterminals.get(i).setStart(preterminals.get(i - 1).getEnd());
//                    }
//                    if (i < preterminals.size() - 1) {
//                        preterminals.get(i).setEnd(preterminals.get(i + 1).getStart());
//                    }
//                }
//            }
        }
    }

    private static void setEditDuration(Tree<String> tree) {
        if (tree.isPreTerminal()) {
            return;
        }
        String label = tree.getLabel();
        int pos = label.indexOf('~');
        if (pos != -1) {
            label = label.substring(0, pos);
        }
        pos = label.indexOf('-');
        if (pos != -1) {
            label = label.substring(0, pos);
        }
        if (label.equals("EDITED")) {
            for (Tree<String> preterminal : tree.getPreTerminals()) {
                preterminal.setDuration(0);
            }
        } else {
            for (Tree<String> child : tree.getChildren()) {
                setEditDuration(child);
            }
        }
    }
}
