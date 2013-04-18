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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author zqhuang
 */
public class CalcNodeBreakDiriChletProb {

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

        Map<String, BreakProbHolder> nodeProbHolder = new HashMap<String, BreakProbHolder>();
        String line = "";
        while ((line = trainReader.readLine()) != null) {
            Tree<String> tree = TreeReader.string2TreeNoBinarize(line.toString());
            tallyNodeBreakProb(tree, nodeProbHolder);
        }
        Map<String, BreakProb> nodeProbs = new HashMap<String, BreakProb>();
        for (Entry<String, BreakProbHolder> entry : nodeProbHolder.entrySet()) {
            String node = entry.getKey();
            BreakProbHolder holder = entry.getValue();
            holder.normalize();
            nodeProbs.put(node, holder.calcDirichletProb());
        }
        for (Entry<String, BreakProb> entry : nodeProbs.entrySet()) {
            String node = entry.getKey();
            BreakProb prob = entry.getValue();
            System.out.println(node + " -> 1: " + prob.p1);
            System.out.println(node + " -> 4: " + prob.p4);
            System.out.println(node + " -> p: " + prob.pp);
        }
        FileOutputStream fos = new FileOutputStream(opts.out); // save to file
        GZIPOutputStream gzos = new GZIPOutputStream(fos); // compressed
        ObjectOutputStream out = new ObjectOutputStream(gzos);
        out.writeObject(nodeProbs);
        out.flush();
        out.close();
    }

    private static void tallyNodeBreakProb(Tree<String> tree, Map<String, BreakProbHolder> nodeProbHolder) {
        List<Tree<String>> terminals = tree.getTerminals();
        Map<Tree<String>, BreakProb> treeBreakMap = new HashMap<Tree<String>, BreakProb>();
        Pattern pattenr = Pattern.compile("^.+__\\S_[^\\s\\(\\)]+:1_([^\\s\\(\\)]+):4_([^\\s\\(\\)]+):p_([^\\s\\(\\)]+)__[^\\s\\(\\)]+$");
        for (Tree<String> terminal : terminals) {
            if (terminal == terminals.get(terminals.size() - 1)) {
                continue;
            }
            String word = terminal.getLabel();
            Matcher matcher = pattenr.matcher(word);
            if (!matcher.matches()) {
                throw new RuntimeException("unexpected word format: " + word);
            }
            double p1 = Double.valueOf(matcher.group(1));
            double p4 = Double.valueOf(matcher.group(2));
            double pp = Double.valueOf(matcher.group(3));
            BreakProb breakProb = new BreakProb(p1, p4, pp);
            treeBreakMap.put(terminal, breakProb);
        }
        for (Tree<String> child : tree.getChildren()) {
            tallyNodeBreakProb(child, treeBreakMap, nodeProbHolder);
        }
    }

    private static void tallyNodeBreakProb(Tree<String> tree, Map<Tree<String>, BreakProb> treeBreakMap,
            Map<String, BreakProbHolder> nodeProbHolder) {
        if (tree.isPreTerminal()) {
            return;
        }
        List<Tree<String>> terminals = tree.getTerminals();
        Tree<String> lastWord = terminals.get(terminals.size() - 1);
        BreakProb breakProb = treeBreakMap.get(lastWord);
        if (breakProb != null) {
            String label = tree.getLabel();
            BreakProbHolder holder = nodeProbHolder.get(label);
            if (holder == null) {
                holder = new BreakProbHolder();
                nodeProbHolder.put(label, holder);
            }
            holder.addProb(breakProb);
        }
        for (Tree<String> child : tree.getChildren()) {
            tallyNodeBreakProb(child, treeBreakMap, nodeProbHolder);
        }
    }
}
