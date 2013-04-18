/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.tools;

import edu.umd.clip.ling.TreeReader;
import edu.umd.clip.ling.Tree;
import edu.umd.clip.util.BiCounter;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author zqhuang
 */
public class BreakBasedNBestSelector {

    public static class Options {

        @Option(name = "-nbest", required = true, usage = "n-best file")
        public String nbestFile = null;
        @Option(name = "-prosody", required = true, usage = "prosody file")
        public String prosodyFile = null;
        @Option(name = "-prob", required = true, usage = "probability file")
        public String prob = null;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);
//        System.err.println("Calling with " + optParser.getPassedInOptions());

        InputStreamReader nbestStream = new InputStreamReader(
                new FileInputStream(opts.nbestFile), Charset.forName("UTF-8"));
        BufferedReader nbestReader = new BufferedReader(nbestStream);

        InputStreamReader prosodyStream = new InputStreamReader(
                new FileInputStream(opts.prosodyFile), Charset.forName("UTF-8"));
        BufferedReader prosodyReader = new BufferedReader(prosodyStream);

        FileInputStream fis = new FileInputStream(opts.prob); // load from file
        GZIPInputStream gzis = new GZIPInputStream(fis); // compressed
        ObjectInputStream in = new ObjectInputStream(gzis); // load objects
        BiCounter<String, String> nodeBreakProb = (BiCounter<String, String>) in.readObject();
//        Map<String, BreakProb> nodeBreakProb = (Map<String, BreakProb>) in.readObject();

        String prosodyLine = "";
        String nbestLine = "";
        int ti = 0;
        while ((prosodyLine = prosodyReader.readLine()) != null) {
            ti++;
            Tree<String> prosodyTree = TreeReader.string2TreeNoBinarize(prosodyLine.trim());
            List<BreakProb> breakProbList = getBreakProbList(prosodyTree);
            nbestLine = nbestReader.readLine();
            if (!nbestLine.trim().contains("================")) {
                throw new Error("unexpected format: " + nbestLine);
            }
            System.err.println(nbestLine + " " + ti);
            nbestLine = nbestReader.readLine();
            int nbestSize = Integer.valueOf(nbestLine.trim());
            System.err.println(nbestLine);
//            String firstTree = null;
//            String bestTree = null;
//            int besti = 0;
            double bestScore = Double.NEGATIVE_INFINITY;
            String bestParse = "";
            for (int i = 0; i < nbestSize; i++) {
                nbestLine = nbestReader.readLine().trim();
                String parserLLString = nbestLine.substring(0, nbestLine.indexOf(':'));
                double parserLL = 0;
                if (parserLLString.equals("INF")) {
                    parserLL = Double.NEGATIVE_INFINITY;
                } else {
                    parserLL = Double.valueOf(parserLLString);
                }
                nbestLine = nbestLine.substring(nbestLine.indexOf(':') + 2);
                if (i == 0) {
                    bestParse = nbestLine;
                }
                Tree<String> currTree = TreeReader.string2TreeNoBinarize(nbestLine);
                double score = getBreakProbScore(currTree, breakProbList, nodeBreakProb);
                score += parserLL;
                if (score > bestScore) {
                    bestScore = score;
                    bestParse = nbestLine;
                }
                System.err.println(score + ":\t" + nbestLine);
            }
            System.err.flush();
            System.out.println(bestParse);
            System.out.flush();
        }
    }

    private static double calcBracketNum(Tree<String> tree) {
        if (tree.isPreTerminal()) {
            return 0;
        }
        double count = 0;
        for (Tree<String> child : tree.getChildren()) {
            count += calcBracketNum(child);
        }
        return count + 1;
    }

    private static List<BreakProb> getBreakProbList(Tree<String> tree) {
        List<Tree<String>> terminals = tree.getTerminals();
        List<BreakProb> breakProbList = new ArrayList<BreakProb>();
        Pattern pattenr = Pattern.compile("^.+__\\S_[^\\s\\(\\)]+:1_([^\\s\\(\\)]+):4_([^\\s\\(\\)]+):p_([^\\s\\(\\)]+)__[^\\s\\(\\)]+$");
        for (Tree<String> terminal : terminals) {
            String word = terminal.getLabel();
            Matcher matcher = pattenr.matcher(word);
            if (!matcher.matches()) {
                throw new RuntimeException("unexpected word format: " + word);
            }
            double p1 = Double.valueOf(matcher.group(1));
            double p4 = Double.valueOf(matcher.group(2));
            double pp = Double.valueOf(matcher.group(3));
            BreakProb breakProb = new BreakProb(p1, p4, pp);
            breakProbList.add(breakProb);
        }
        return breakProbList;
    }

    private static double getBreakProbScore(Tree<String> tree,
            List<BreakProb> breakProbList,
            BiCounter<String, String> nodeBreakProb) {
        List<Tree<String>> terminals = tree.getTerminals();
        if (terminals.size() != breakProbList.size()) {
            throw new Error("length mismatch...");
        }
        setParent(tree);
        Map<Tree<String>, BreakProb> breakProbMap = new HashMap<Tree<String>, BreakProb>();
        double score = 0;
        for (int i = 0; i < terminals.size(); i++) {
//            breakProbMap.put(terminals.get(i), breakProbList.get(i));
            Tree<String> terminal = terminals.get(i);
            BreakProb breakProb = breakProbList.get(i);
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
//            if (nodeBreakProb.containsKey(label, "1")) {
//                score += breakProb.p1 * nodeBreakProb.getCount(label, "1");
//            } else {
//                score += breakProb.p1 * Math.log(0.01);
//            }
//            if (nodeBreakProb.containsKey(label, "4")) {
//                score += breakProb.p4 * nodeBreakProb.getCount(label, "4");
//            } else {
//                score += breakProb.p4 * Math.log(0.01);
//            }
//            if (nodeBreakProb.containsKey(label, "p")) {
//                score += breakProb.pp * nodeBreakProb.getCount(label, "p");
//            } else {
//                score += breakProb.pp * Math.log(0.01);
//            }
            double max = Math.max(breakProb.p1, Math.max(breakProb.p4, breakProb.pp));
            String breakType = "";
            if (breakProb.p1 == max) {
                breakType = "1";
            } else if (breakProb.p4 == max) {
                breakType = "4";
            } else {
                breakType = "p";
            }
            if (!nodeBreakProb.containsKey(label, breakType)) {
                score += Math.log(0.01);
            } else {
                score += nodeBreakProb.getCount(label, breakType);
            }
        }
        return score;
//        double score = 0;
//        for (Tree<String> child : tree.getChildren()) {
//            score += getBreakProbScore(child, breakProbMap, nodeBreakProb);
//        }
//        return score;
    }

    private static double getBreakProbScore(Tree<String> tree,
            Map<Tree<String>, BreakProb> breakProbMap,
            BiCounter<String, String> nodeBreakProb) {
        if (tree.isPreTerminal()) {
            return 0;
        }
        double score = 0;
        for (Tree<String> child : tree.getChildren()) {
            score += getBreakProbScore(child, breakProbMap, nodeBreakProb);
        }
        List<Tree<String>> terminals = tree.getTerminals();
        Tree<String> lastWord = terminals.get(terminals.size() - 1);
        BreakProb breakProb = breakProbMap.get(lastWord);
        if (breakProb != null) {
            String label = tree.getLabel();
            double max = Math.max(breakProb.p1, Math.max(breakProb.p4, breakProb.pp));
            String thisBreak = "";
            if (breakProb.p1 == max) {
                thisBreak = "1";
            } else if (breakProb.p4 == max) {
                thisBreak = "4";
            } else {
                thisBreak = "p";
            }
            if (nodeBreakProb.containsKey(label, thisBreak)) {
                score += nodeBreakProb.getCount(label, thisBreak);
            } else {
                score += Math.log(0.001);
            }
//            score += nodeBreakProb.get(label).calcLogDirichletProb(breakProb);
//            score += breakProb.p1 * nodeBreakProb.getCount(label, "1") +
//                    breakProb.p4 * nodeBreakProb.getCount(label, "4") +
//                    breakProb.pp * nodeBreakProb.getCount(label, "p");
        }
        return score;
    }

    private static List<Boolean> getBreakPos(Tree<String> tree) {
        List<Tree<String>> terminals = tree.getTerminals();
        List<Boolean> breakPos = new ArrayList<Boolean>();
        Pattern pattenr = Pattern.compile("^(.+)__(\\S)([^\\s\\(\\)]+)__([^\\s\\(\\)]+)$");
        for (Tree<String> terminal : terminals) {
            String word = terminal.getLabel();
            Matcher matcher = pattenr.matcher(word);
            if (!matcher.matches()) {
                throw new RuntimeException("unexpected word format: " + word);
            }
            terminal.setLabel(matcher.group(1));
            if (!matcher.group(2).equals("1")) {
                breakPos.add(true);
            } else {
                breakPos.add(false);
            }
        }
        return breakPos;
    }

    private static void setParent(Tree<String> tree) {
        if (tree.isLeaf()) {
            return;
        }
        for (Tree<String> child : tree.getChildren()) {
            child.setParent(tree);
            setParent(child);
        }
    }

    private static boolean checkBreakBracketingCompatibility(List<Boolean> breakPos, Tree<String> tree) {
        setParent(tree);
        List<Tree<String>> terminals = tree.getTerminals();
        for (int i = 0; i < breakPos.size(); i++) {
            if (i == breakPos.size() - 1) {
                continue;
            }
            if (!breakPos.get(i)) {
                continue;
            }
            Tree<String> leftWord = terminals.get(i);
            Tree<String> rightWord = terminals.get(i + 1);

            Tree<String> parent = leftWord.getParent().getParent();
            while (!parent.getTerminals().contains(rightWord) && parent.getParent().getParent() != null) {
                parent = parent.getParent();
            }
            Tree<String> leftParent = null;
            for (Tree<String> child : parent.getChildren()) {
                if (child.getTerminals().contains(leftWord)) {
                    leftParent = child;
                }
            }
            if (leftParent.isPreTerminal()) {
                return false;
            }
        }
        return true;
    }
}
