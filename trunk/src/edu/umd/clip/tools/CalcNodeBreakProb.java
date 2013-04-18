/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.tools;

import edu.umd.clip.ling.TreeReader;
import edu.umd.clip.ling.Tree;
import edu.umd.clip.math.LogOperator;
import edu.umd.clip.util.BiCounter;
import java.io.FileNotFoundException;
import java.io.IOException;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author zqhuang
 */
public class CalcNodeBreakProb {

    public static class Options {

        @Option(name = "-train", required = true, usage = "training file")
        public String train = null;
        @Option(name = "-out", required = true, usage = "output parameter file")
        public String out = null;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);
        System.err.println("Calling with " + optParser.getPassedInOptions());

        InputStreamReader trainStream = new InputStreamReader(
                new FileInputStream(opts.train), Charset.forName("UTF-8"));
        BufferedReader trainReader = new BufferedReader(trainStream);

        BiCounter<String, String> nodeBreakCounter = new BiCounter<String, String>();
        String line = "";
        while ((line = trainReader.readLine()) != null) {
            Tree<String> tree = TreeReader.string2TreeNoBinarize(line.toString());
            tallyNodeBreakProb(tree, nodeBreakCounter);
        }
        nodeBreakCounter.normalize();
        System.out.println(nodeBreakCounter.toString());
        nodeBreakCounter.applyOperator(new LogOperator());
        FileOutputStream fos = new FileOutputStream(opts.out); // save to file
        GZIPOutputStream gzos = new GZIPOutputStream(fos); // compressed
        ObjectOutputStream out = new ObjectOutputStream(gzos);
        out.writeObject(nodeBreakCounter);
        out.flush();
        out.close();
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

    private static void tallyNodeBreakProb(Tree<String> tree, BiCounter<String, String> nodeBreakCounter) {
        List<Tree<String>> terminals = tree.getTerminals();
        Map<Tree<String>, BreakProb> treeBreakMap = new HashMap<Tree<String>, BreakProb>();
        Pattern pattern = Pattern.compile("^.+__\\S_[^\\s\\(\\)]+:1_([^\\s\\(\\)]+):4_([^\\s\\(\\)]+):p_([^\\s\\(\\)]+)__[^\\s\\(\\)]+$");
        setParent(tree);
        for (Tree<String> terminal : terminals) {
            if (terminal == terminals.get(terminals.size() - 1)) {
                continue;
            }
            String word = terminal.getLabel();
            Matcher matcher = pattern.matcher(word);
            if (!matcher.matches()) {
                throw new RuntimeException("unexpected word format: " + word);
            }
            double p1 = Double.valueOf(matcher.group(1));
            double p4 = Double.valueOf(matcher.group(2));
            double pp = Double.valueOf(matcher.group(3));
            BreakProb breakProb = new BreakProb(p1, p4, pp);
            treeBreakMap.put(terminal, breakProb);

            Tree<String> anscent = terminal.getParent();
            while (true) {
                List<Tree<String>> parentTerminals = anscent.getParent().getTerminals();
                if (parentTerminals.get(parentTerminals.size() - 1) == terminal && anscent.getParent().getParent() != null) {
                    anscent = anscent.getParent();
                } else {
                    break;
                }
            }
            String label = anscent.getLabel();
//            nodeBreakCounter.incrementCount(label, "1", breakProb.p1);
//            nodeBreakCounter.incrementCount(label, "4", breakProb.p4);
//            nodeBreakCounter.incrementCount(label, "p", breakProb.pp);


            double max = Math.max(breakProb.p1, Math.max(breakProb.p4, breakProb.pp));
            if (breakProb.p1 == max) {
                nodeBreakCounter.incrementCount(label, "1", 1);
            } else if (breakProb.p4 == max) {
                nodeBreakCounter.incrementCount(label, "4", 1);
            } else {
                nodeBreakCounter.incrementCount(label, "p", 1);
            }
        }
//        for (Tree<String> child : tree.getChildren()) {
//            tallyNodeBreakProb(child, treeBreakMap, nodeBreakCounter);
//        }
    }

    private static void tallyNodeBreakProb(Tree<String> tree, Map<Tree<String>, BreakProb> treeBreakMap,
            BiCounter<String, String> nodeBreakCounter) {
        if (tree.isPreTerminal()) {
            return;
        }
        List<Tree<String>> terminals = tree.getTerminals();
        Tree<String> lastWord = terminals.get(terminals.size() - 1);
        BreakProb breakProb = treeBreakMap.get(lastWord);
        if (breakProb != null) {
            String label = tree.getLabel();
            double max = Math.max(breakProb.p1, Math.max(breakProb.p4, breakProb.pp));
            if (breakProb.p1 == max) {
                nodeBreakCounter.incrementCount(label, "1", 1);
            } else if (breakProb.p4 == max) {
                nodeBreakCounter.incrementCount(label, "4", 1);
            } else {
                nodeBreakCounter.incrementCount(label, "p", 1);
            }
//            nodeBreakCounter.incrementCount(label, "4", breakProb.p4);
//            nodeBreakCounter.incrementCount(label, "p", breakProb.pp);
        }
        for (Tree<String> child : tree.getChildren()) {
            tallyNodeBreakProb(child, treeBreakMap, nodeBreakCounter);
        }
    }
}
