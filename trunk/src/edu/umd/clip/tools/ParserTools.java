/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.tools;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.ling.Trees;
import edu.umd.clip.parser.Grammar;
import edu.umd.clip.parser.LexiconManager;
import edu.umd.clip.parser.TreeAnnotations;
import edu.umd.clip.util.Counter;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author zqhuang
 */
public class ParserTools {

    static BufferedReader inputData;
    static PrintWriter outputWriter;
    static PrintWriter errorWriter;

    public static class Options {

        @Option(name = "-input", required = false, usage = "File to parse (Default: stdin)")
        public String inputFile = null;
        @Option(name = "-output", required = false, usage = "Output parsed file (Default: stdout")
        public String outputFile = null;
        @Option(name = "-viterbi2maxrule", required = false, usage = "Removes latent variables and binarization on viterbi trees to retrieve maxrule parse (Default: false")
        public boolean viterbi2maxrule = false;
        @Option(name = "-getUnaryRules", required = false, usage = "Lists unary rules that are not ROOT production or preterminal generation rules (Default: false)")
        public boolean getUnaryRules = false;
        @Option(name = "-removeUnaryRules", required = false, usage = "Removes unary rules that are not ROOT production or preterminal generation rules (Default: false)")
        public boolean removeUnaryRules = false;
        @Option(name = "-getSentence", required = false, usage = "Returns the sentences of parse trees (Default: false)")
        public boolean getSentence = false;
        @Option(name = "-getZeroRuleNum", required = false, usage = "Print the number of zero rules in the grammar (Default: false)")
        public boolean getZeroRuleNum = false;
        @Option(name = "-getNumStates", required = false, usage = "Report the number of states per tag (Default: false) ")
        public boolean getNumStates = false;
        @Option(name = "-gr", required = false, usage = "Input grammar (Default: null)")
        public String grammarFile = null;
        @Option(name = "-check", required = false, usage = "Do Sanity Check of Trees (default: false)")
        public boolean sanityCheck = false;
        @Option(name = "-reportWords", required = false, usage = "Report top words for each syntatic category (default: false)")
        public int topn = 0;
        @Option(name = "-words", required = false, usage = "The set of unseen words ")
        public String wordFile = "";
        @Option(name = "-addPunc", required = false, usage = "Insert pseudo punctuation")
        public boolean addPunc = false;
        @Option(name = "-removePunc", required = false, usage = "Remove pseudo punctuation")
        public boolean removePunc = false;
        @Option(name = "-removeDupPre", required = false, usage = "Remove duplicated preterminals")
        public boolean removeDupPre = false;
        @Option(name = "-alignProsody", required = false, usage = "Return trees whose prosody marks align with brackets")
        public boolean alignProsody = false;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);

        String inputFile = opts.inputFile;
        String outputFile = opts.outputFile;
        boolean viterbi2maxrule = opts.viterbi2maxrule;
        boolean getUnaryRules = opts.getUnaryRules;
        boolean removeUnaryRules = opts.removeUnaryRules;
        boolean getSentence = opts.getSentence;

        OutputStreamWriter errorStream = new OutputStreamWriter(System.err, Charset.forName("UTF-8"));
        errorWriter = new PrintWriter(errorStream);

        InputStreamReader inStream = inputFile != null ? new InputStreamReader(
                new FileInputStream(inputFile), Charset.forName("UTF-8")) : new InputStreamReader(System.in, Charset.forName("UTF-8"));
        inputData = new BufferedReader(inStream); //FileReader(inData));

        OutputStreamWriter outStream = outputFile != null ? new OutputStreamWriter(
                new FileOutputStream(outputFile), Charset.forName("UTF-8")) : new OutputStreamWriter(System.out, Charset.forName("UTF-8"));
        outputWriter = new PrintWriter(outStream);

        if (opts.alignProsody) {
            alignProsody();
        }

        if (viterbi2maxrule) {
            convertViterbi2Maxrule();
        }

        if (getUnaryRules) {
            listUnaryRules();
        }

        if (removeUnaryRules) {
            deleteUnaryRules();
        }

        if (opts.addPunc) {
            insertPseudoPunc();
        }
        if (getSentence) {
            getSentences();
        }

        if (opts.sanityCheck) {
            treeCheck();
        }

        if (opts.removePunc) {
            removePunc();
        }

        if (opts.removeDupPre) {
            removeDumplicatePreterminals();
        }

        if (opts.topn > 0 && opts.grammarFile != null) {
            reportWords(opts.grammarFile, opts.topn, opts.wordFile);
        }

        if (opts.getNumStates) {
            reportNumStates(opts.grammarFile);
        }
    }

    private static Set<String> readWordSet(String wordFile) throws FileNotFoundException, IOException {
        if (wordFile == null) {
            return new HashSet<String>();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(wordFile)));
        String line = "";
        Set<String> wordSet = new HashSet<String>();
        while ((line = reader.readLine()) != null) {
            wordSet.add(line.trim());
        }
        return wordSet;
    }

    public static void removePunc(Tree<String> tree) {
        if (tree.isPreTerminal()) {
            return;
        }
        List<Tree<String>> children = tree.getChildren();
        if (tree.getLabel().equals("S") && children.size() == 2 &&
                (children.get(1).getLabel().equals("*4*") ||
                children.get(1).getLabel().equals("*p*") ||
                children.get(1).getLabel().equals("*<NA>*"))) {
            tree.setLabel(children.get(0).getLabel());
            tree.setChildren(children.get(0).getChildren());
        }
        List<Tree<String>> newChildren = new ArrayList<Tree<String>>();
        for (Tree<String> child : tree.getChildren()) {
            if (!child.getLabel().equals("*4*") &&
                    !child.getLabel().equals("*p*") &&
                    !child.getLabel().equals("*<NA>*")) {
                newChildren.add(child);
            }
        }
        tree.setChildren(newChildren);
        for (Tree<String> child : tree.getChildren()) {
            removePunc(child);
        }
    }

    public static void removePunc2(Tree<String> tree) {
        if (tree.isPreTerminal()) {
            return;
        }
        for (Tree<String> child : tree.getChildren()) {
            removePunc2(child);
        }
        List<Tree<String>> children = tree.getChildren();
        List<Tree<String>> newChildren = new ArrayList<Tree<String>>();
        for (Tree<String> child : children) {
            String label = child.getLabel();
            if (label.equals("PROSODY_S")) {
                newChildren.addAll(child.getChildren());
            } else if (label.endsWith("_P")) {
                Tree<String> preterminal = child.getChildren().get(child.getChildren().size() - 1);
                if (!label.startsWith(preterminal.getLabel())) {
                    throw new Error("mismatch...");
                }
                newChildren.add(preterminal);
            } else if (!label.equals("*4*") && !label.equals("*p*") && !label.equals("PROSODY")) {
                newChildren.add(child);
            }
        }
        tree.setChildren(newChildren);
    }

    public static void removeDuplicatePreterminal(Tree<String> tree) {
        List<Tree<String>> preterminals = tree.getPreTerminals();
        setParent(tree);
        for (Tree<String> preterminal : preterminals) {
            if (preterminal.getParent().getLabel().equals(preterminal.getLabel())) {
                preterminal.getParent().setChildren(preterminal.getChildren());
            }
        }
    }

    public static void reportWords(String grmmarFile, int top, String wordFile) throws FileNotFoundException, IOException {
        Grammar grammar = Grammar.load(grmmarFile);
        LexiconManager lexiconManager = grammar.getLexiconManager();
        lexiconManager.reportTopWords(top, readWordSet(wordFile));
    }

    public static void reportNumStates(String grammarFile) {
        Grammar grammar = Grammar.load(grammarFile);
        List<String> nodeList = grammar.getNodeList();
        int[] numStates = grammar.getNumStates();
        for (int ni = 0; ni < numStates.length; ni++) {
            System.out.println(nodeList.get(ni) + " has " + numStates[ni] + " states");
        }
    }

    public static void treeCheck() throws IOException {
        String line = null;
        int si = 0;
        while ((line = inputData.readLine()) != null) {
            si++;
            System.err.println(si);
            line = line.trim();
            if (line.equals("null")) {
                System.err.println(si + "-th line: " + line);
                continue;
            } else {
                Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
                Tree<String> tree = treeIterator.next();
            }
        }
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

    public static void insertPseudoPunc(Tree<String> tree) {
        List<Tree<String>> terminals = tree.getTerminals();
        List<String> pseudoPuncs = new ArrayList<String>();
        Pattern pattenr = Pattern.compile("^([^\\s\\_]+)__(\\S)([^\\s\\(\\)]+)$");
        for (Tree<String> terminal : terminals) {
            String word = terminal.getLabel();
            Matcher matcher = pattenr.matcher(word);
            if (!matcher.matches()) {
                throw new RuntimeException("unexpected word format: " + word);
            }
            terminal.setLabel(matcher.group(1));
            pseudoPuncs.add("*" + matcher.group(2) + "*");
        }
        setParent(tree);
        for (int i = 0; i < pseudoPuncs.size(); i++) {
            String punc = pseudoPuncs.get(i);
            if (punc.equals("*p*") || punc.equals("*4*")) {
                Tree<String> leftWord = terminals.get(i);
                Tree<String> rightWord = null;
                if (i < pseudoPuncs.size() - 1) {
                    rightWord = terminals.get(i + 1);
                }
                Tree<String> puncLeaf = new Tree<String>(punc, new ArrayList<Tree<String>>());
                Tree<String> puncTerminal = new Tree<String>("PROSODY", Collections.singletonList(puncLeaf));
                Tree<String> parent = leftWord.getParent().getParent();
                while (!parent.getTerminals().contains(rightWord) && parent.getParent().getParent() != null) {
                    parent = parent.getParent();
                }
                List<Tree<String>> newChildren = new ArrayList<Tree<String>>();
                for (Tree<String> child : parent.getChildren()) {
                    newChildren.add(child);
                    if (child.getTerminals().contains(leftWord)) {
                        newChildren.add(puncTerminal);
                    }
                }
                parent.setChildren(newChildren);
            }
        }
    }

    public static boolean breakAlignedWithBrackets(Tree<String> tree) {
        List<Tree<String>> terminals = tree.getTerminals();
        List<String> pseudoPuncs = new ArrayList<String>();
        Pattern pattenr = Pattern.compile("^(.+)__(\\S)([^\\s\\(\\)]+)__([^\\s\\(\\)]+)$");
        boolean hasProsody = false;
        for (Tree<String> terminal : terminals) {
            String word = terminal.getLabel();
            Matcher matcher = pattenr.matcher(word);
            if (!matcher.matches()) {
                throw new RuntimeException("unexpected word format: " + word);
            }
//            terminal.setLabel(matcher.group(1));
            if (!matcher.group(2).equals("1")) {
                pseudoPuncs.add("*" + matcher.group(2) + "*");
            } else {
                pseudoPuncs.add("");
            }
        }
        setParent(tree);
        for (int i = 0; i < pseudoPuncs.size(); i++) {
            String punc = pseudoPuncs.get(i);
            if (i == pseudoPuncs.size() - 1) {
                continue;
            }
            if (!punc.equals("")) {
                hasProsody = true;
                Tree<String> leftWord = terminals.get(i);
                Tree<String> rightWord = null;
                if (i < pseudoPuncs.size() - 1) {
                    rightWord = terminals.get(i + 1);
                }
                Tree<String> parent = leftWord.getParent().getParent();
                while (!parent.getTerminals().contains(rightWord) && parent.getParent().getParent() != null) {
                    parent = parent.getParent();
                }
                Tree<String> leftParent = null;
                Tree<String> rightParent = null;
                for (Tree<String> child : parent.getChildren()) {
                    if (child.getTerminals().contains(leftWord)) {
                        leftParent = child;
                    }
                    if (child.getTerminals().contains(rightWord)) {
                        rightParent = child;
                    }
                }
                if (leftParent.isPreTerminal()) {
                    return false;
                }
            }
        }
        return hasProsody;
    }

    public static void getSentences() throws IOException {
        String line = null;
        String sentence = null;
        int si = 0;
        while ((line = inputData.readLine()) != null) {
            si++;
            System.err.println(si);
            line =
                    line.trim();
            if (Pattern.matches("^\\(\\(\\)\\)$", line)) {
                continue;
            } else {
                Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
                Tree<String> tree = treeIterator.next();
                List<String> tags = tree.getPreTerminalYield();
                List<String> words = tree.getYield();
                sentence =
                        "";
                if (tags.size() != words.size()) {
                    throw new Error("length does not match in " + tree);
                }

                for (int i = 0; i <
                        words.size(); i++) {
                    String word = words.get(i);
                    String tag = tags.get(i);
                    if (!tag.equals("-NONE-")) {
                        sentence += word + " ";
                    }

                }
                outputWriter.println(sentence.trim());
                outputWriter.flush();
            }

        }
    }

    public static Tree<String> removeNoneNodes(Tree<String> tree) {
        if (tree.isPreTerminal()) {
            return tree;
        }

        List<Tree<String>> children = tree.getChildren();
        List<Tree<String>> newChildren = new ArrayList<Tree<String>>();
        for (Tree<String> child : children) {
            if (child.isPreTerminal() && child.getLabel().equals("-NONE-")) {
            } else {
                child = removeNoneNodes(child);
                newChildren.add(child);
            }

        }
        tree.setChildren(children);
        return tree;
    }

    public static void insertPseudoPunc() throws IOException {
        Trees.TreeTransformer<String> treeTransformer = new Trees.EmptyNodeStripper();
        String line = null;
        int i = 0;
        while ((line = inputData.readLine()) != null) {
            line = line.trim();
            i++;

            if (Pattern.matches("^\\(\\(\\)\\)$", line)) {
                continue;
            } else {
                Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
                Tree<String> tree = treeIterator.next();
                tree = treeTransformer.transformTree(tree);
                insertPseudoPunc(tree);

                outputWriter.print("(");
                for (Tree<String> child : tree.getChildren()) {
                    outputWriter.print(" " + child);
                }

                outputWriter.println(")");
                outputWriter.flush();
            }

        }
    }

    public static void removePunc() throws IOException {
        Trees.TreeTransformer<String> treeTransformer = new Trees.EmptyNodeStripper();
        String line = null;
        while ((line = inputData.readLine()) != null) {
            line = line.trim();
            if (Pattern.matches("^\\(\\(\\)\\)$", line)) {
                continue;
            } else {
                Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
                Tree<String> tree = treeIterator.next();
                tree = treeTransformer.transformTree(tree);
                removePunc2(tree);
                outputWriter.print("(");
                for (Tree<String> child : tree.getChildren()) {
                    outputWriter.print(" " + child);
                }

                outputWriter.println(")");
                outputWriter.flush();
            }

        }
    }

    public static void removeDumplicatePreterminals() throws IOException {
        Trees.TreeTransformer<String> treeTransformer = new Trees.EmptyNodeStripper();
        String line = null;
        while ((line = inputData.readLine()) != null) {
            line = line.trim();
            if (Pattern.matches("^\\(\\(\\)\\)$", line)) {
                continue;
            } else {
                Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
                Tree<String> tree = treeIterator.next();
                tree = treeTransformer.transformTree(tree);
                removeDuplicatePreterminal(tree);
                outputWriter.print("(");
                for (Tree<String> child : tree.getChildren()) {
                    outputWriter.print(" " + child);
                }

                outputWriter.println(")");
                outputWriter.flush();
            }

        }
    }

    public static void alignProsody() throws IOException {
        Trees.TreeTransformer<String> treeTransformer = new Trees.EmptyNodeStripper();
        String line = null;
        while ((line = inputData.readLine()) != null) {
            line = line.trim();
            if (Pattern.matches("^\\(\\(\\)\\)$", line)) {
                continue;
            } else {
                Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
                Tree<String> tree = treeIterator.next();
                tree = treeTransformer.transformTree(tree);
                if (!breakAlignedWithBrackets(tree)) {
                    outputWriter.println();
                    outputWriter.flush();
                    continue;
                }
                outputWriter.print("(");
                for (Tree<String> child : tree.getChildren()) {
                    outputWriter.print(" " + child);
                }

                outputWriter.println(")");
                outputWriter.flush();
            }

        }
    }

    public static void deleteUnaryRules() throws IOException {
        Trees.TreeTransformer<String> treeTransformer = new Trees.EmptyNodeStripper();
        String line = null;
        while ((line = inputData.readLine()) != null) {
            line = line.trim();
            if (Pattern.matches("^\\(\\(\\)\\)$", line)) {
                continue;
            } else {
                Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
                Tree<String> tree = treeIterator.next();
                tree =
                        treeTransformer.transformTree(tree);
                Tree<String> newTree = deleteUnaryRules(tree, true);
                outputWriter.print("(");
                for (Tree<String> child : newTree.getChildren()) {
                    outputWriter.print(" " + child);
                }

                outputWriter.println(")");
                outputWriter.flush();
            }

        }
    }

    public static void listUnaryRules() throws IOException {
        Counter<String> unaryCounter = new Counter<String>();
        String line = null;
        while ((line = inputData.readLine()) != null) {
            line = line.trim();
            if (Pattern.matches("^\\(\\(\\)\\)$", line)) {
                continue;
            } else {
                Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
                Tree<String> tree = treeIterator.next();
                for (Tree<String> child : tree.getChildren()) {
                    listUnaryRules(child, unaryCounter);
                }

            }
        }
        for (String rule : unaryCounter.keySet()) {
            outputWriter.println(rule + " : " + (int) unaryCounter.getCount(rule));
        }

        outputWriter.flush();
    }

    public static void listUnaryRules(Tree<String> tree, Counter<String> unaryCounter) {
        if (tree.isPreTerminal() || tree.isLeaf()) {
            return;
        }

        String parent = tree.getLabel();
        List<Tree<String>> children = tree.getChildren();
        if (children.size() == 1) {
            if (!children.get(0).isPreTerminal()) {
                String unaryRule = parent + " " + children.get(0).getLabel();
                unaryCounter.incrementCount(unaryRule, 1);
            } else {
                return;
            }

        }

        for (Tree<String> child : children) {
            listUnaryRules(child, unaryCounter);
        }

    }

    public static Tree<String> deleteUnaryRules(Tree<String> tree, boolean isRoot) {
        if (tree.isPreTerminal()) {
            return tree;
        }

        String parent = tree.getLabel();
        List<Tree<String>> children = tree.getChildren();
        if (children.size() == 1 && !isRoot) {
            Tree<String> child = children.get(0);
            if (child.isPreTerminal()) {
                return tree;
            } else {
                return deleteUnaryRules(child, false);
            }

        }
        List<Tree<String>> newChildren = new ArrayList<Tree<String>>();
        for (Tree<String> child : children) {
            newChildren.add(deleteUnaryRules(child, false));
        }

        return new Tree<String>(parent, newChildren);
    }

    public static void convertViterbi2Maxrule() throws FileNotFoundException, IOException {

        String line = null;
        while ((line = inputData.readLine()) != null) {
            line = line.trim();
            if (Pattern.matches("^\\(\\(\\)\\)$", line)) {
                outputWriter.println(line);
            } else {
                Iterator<Tree<String>> treeIterator = new Trees.PennTreeReader(line);
                Tree<String> viterbiTree = treeIterator.next();
                if (viterbiTree == null) {
                    errorWriter.println("The following input viterbi tree is malformed");
                    errorWriter.println(line);
                    outputWriter.println("(())");
                } else {
                    Tree<String> maxruleTree = TreeAnnotations.unAnnotateTree(viterbiTree);
                    if (!maxruleTree.getChildren().isEmpty()) {
                        outputWriter.print("(");
                        for (Tree<String> child : maxruleTree.getChildren()) {
                            outputWriter.print(" " + child);
                        }

                        outputWriter.println(")");
                    } else {
                        errorWriter.println("The following input viterbi tree becomes malformed after removing binarization and latent variables");
                        errorWriter.println(line);
                        outputWriter.println("(())");
                    }

                }
            }
            outputWriter.flush();
            errorWriter.flush();
        }
    }
}
