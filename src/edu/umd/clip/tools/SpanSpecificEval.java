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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class SpanSpecificEval {

    public static class Options {

        @Option(name = "-gold", required = true, usage = "gold parse tree")
        public String goldFile = null;
        @Option(name = "-test1", required = true, usage = "test1 parse tree")
        public String test1File = null;
        @Option(name = "-test2", required = true, usage = "test2 parse tree")
        public String test2File = null;
    }

    public static void main(String[] args) throws IOException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);

        BufferedReader goldReader = new BufferedReader(new InputStreamReader(new FileInputStream(opts.goldFile), Charset.forName("UTF-8")));
        BufferedReader test1Reader = new BufferedReader(new InputStreamReader(new FileInputStream(opts.test1File), Charset.forName("UTF-8")));
        BufferedReader test2Reader = new BufferedReader(new InputStreamReader(new FileInputStream(opts.test2File), Charset.forName("UTF-8")));

        String goldLine = "", test1Line = "", test2Line = "";

        int maxLength = 50;
        List<UniCounter<Bracket>> goldBracketsList = new ArrayList(maxLength + 1);
        for (int i = 0; i < maxLength + 1; i++) {
            goldBracketsList.add(new UniCounter<Bracket>());
        }

        List<UniCounter<Bracket>> test1BracketsList = new ArrayList(maxLength + 1);
        for (int i = 0; i < maxLength + 1; i++) {
            test1BracketsList.add(new UniCounter<Bracket>());
        }

        List<UniCounter<Bracket>> test2BracketsList = new ArrayList(maxLength + 1);
        for (int i = 0; i < maxLength + 1; i++) {
            test2BracketsList.add(new UniCounter<Bracket>());
        }


        double[] gold1TotalS = new double[maxLength + 1];
        double[] gold1MatchedS = new double[maxLength + 1];

        double[] test1TotalS = new double[maxLength + 1];
        double[] test1MatchedS = new double[maxLength + 1];

        double[] gold2TotalS = new double[maxLength + 1];
        double[] gold2MatchedS = new double[maxLength + 1];

        double[] test2TotalS = new double[maxLength + 1];
        double[] test2MatchedS = new double[maxLength + 1];

        double gold1Total = 0;
        double gold1Matched = 0;
        double test1Total = 0;
        double test1Matched = 0;
        double gold2Total = 0;
        double gold2Matched = 0;
        double test2Total = 0;
        double test2Matched = 0;

        UniCounter<Bracket> goldBracketCount = new UniCounter<Bracket>();
        UniCounter<Bracket> test1BracketCount = new UniCounter<Bracket>();
        UniCounter<Bracket> test2BracketCount = new UniCounter<Bracket>();
        int sentId = 0;
        while ((goldLine = goldReader.readLine()) != null) {
            sentId++;
            goldLine = goldLine.trim();
            Iterator<Tree<String>> goldIterator = new Trees.PennTreeReader(goldLine);
            Tree<String> goldTree = goldIterator.next();
            int goldLength = goldTree.getYield().size();

            goldBracketCount.clear();
            appendBracket(goldTree, goldBracketCount, true);
            for (UniCounter<Bracket> brackets : goldBracketsList) {
                brackets.clear();
            }
            for (Entry<Bracket, Double> entry : goldBracketCount.entrySet()) {
                Bracket bracket = entry.getKey();
                int span = bracket.getEnd() - bracket.getStart();
                if (span > maxLength) {
                    continue;
                }
                goldBracketsList.get(span).incrementCount(bracket, entry.getValue());
            }

            test1Line = test1Reader.readLine();
            test1Line = test1Line.trim();
            Iterator<Tree<String>> test1Iterator = new Trees.PennTreeReader(test1Line);
            Tree<String> test1Tree = test1Iterator.next();
            int test1Length = test1Tree.getYield().size();
            assert (goldLength == test1Length);

            test1BracketCount.clear();
            appendBracket(test1Tree, test1BracketCount, true);
            for (UniCounter<Bracket> brackets : test1BracketsList) {
                brackets.clear();
            }
            for (Entry<Bracket, Double> entry : test1BracketCount.entrySet()) {
                Bracket bracket = entry.getKey();
                int span = bracket.getEnd() - bracket.getStart();
                if (span > maxLength) {
                    continue;
                }
                test1BracketsList.get(span).incrementCount(bracket, entry.getValue());
            }

            test2Line = test2Reader.readLine();
            test2Line = test2Line.trim();
            Iterator<Tree<String>> test2Iterator = new Trees.PennTreeReader(test2Line);
            Tree<String> test2Tree = test2Iterator.next();
            int test2Length = test2Tree.getYield().size();
            assert (goldLength == test2Length);

            test2BracketCount.clear();
            appendBracket(test2Tree, test2BracketCount, true);
            for (UniCounter<Bracket> brackets : test2BracketsList) {
                brackets.clear();
            }
            for (Entry<Bracket, Double> entry : test2BracketCount.entrySet()) {
                Bracket bracket = entry.getKey();
                int span = bracket.getEnd() - bracket.getStart();
                if (span > maxLength) {
                    continue;
                }
                test2BracketsList.get(span).incrementCount(bracket, entry.getValue());
            }

            int goldNum = 0;
            int match1Num = 0;
            int test1Num = 0;
            int match2Num = 0;
            int test2Num = 0;

            goldNum = (int) goldBracketCount.getCount();
            gold1Total += goldNum;
            for (Bracket bracket : goldBracketCount.keySet()) {
                if (test1BracketCount.containsKey(bracket)) {
                    int match = (int) Math.min(goldBracketCount.getCount(bracket), test1BracketCount.getCount(bracket));
                    gold1Matched += match;
                    match1Num += match;
                }
            }
            test1Num = (int) test1BracketCount.getCount();
            test1Total += test1Num;

            gold2Total += goldNum;
            for (Bracket bracket : goldBracketCount.keySet()) {
                if (test2BracketCount.containsKey(bracket)) {
                    int match = (int) Math.min(goldBracketCount.getCount(bracket), test2BracketCount.getCount(bracket));
                    gold2Matched += match;
                    match2Num += match;
                }
            }

            test2Num = (int) test2BracketCount.getCount();
            test2Total += test2Num;

//            System.out.format("%d:\t%d\t%d,\t%d\t%d\n", goldNum, test1Num, match1Num, test2Num, match2Num);
//            System.out.format("%d , %d , %d\n", match2Num, goldNum, test2Num);
            for (int span = 0; span <= Math.min(goldLength, maxLength); span++) {
                UniCounter<Bracket> goldBrackets = goldBracketsList.get(span);
                UniCounter<Bracket> testBrackets = test1BracketsList.get(span);

                gold1TotalS[span] += goldBrackets.getCount();
                for (Bracket bracket : goldBrackets.keySet()) {
                    if (testBrackets.containsKey(bracket)) {
                        gold1MatchedS[span] += Math.min(goldBrackets.getCount(bracket), testBrackets.getCount(bracket));
                    }
                }

                test1TotalS[span] += testBrackets.getCount();
                for (Bracket bracket : testBrackets.keySet()) {
                    if (goldBrackets.containsKey(bracket)) {
                        test1MatchedS[span] += Math.min(goldBrackets.getCount(bracket), testBrackets.getCount(bracket));
                    }
                }
                if (gold1MatchedS[span] != test1MatchedS[span]) {
                    throw new RuntimeException("length does not match...");
                }
            }


            for (int span = 0; span <= Math.min(goldLength, maxLength); span++) {
                UniCounter<Bracket> goldBrackets = goldBracketsList.get(span);
                UniCounter<Bracket> testBrackets = test2BracketsList.get(span);

                gold2TotalS[span] += goldBrackets.getCount();
                for (Bracket bracket : goldBrackets.keySet()) {
                    if (testBrackets.containsKey(bracket)) {
                        gold2MatchedS[span] += Math.min(goldBrackets.getCount(bracket), testBrackets.getCount(bracket));
                    }
                }

                test2TotalS[span] += testBrackets.getCount();
                for (Bracket bracket : testBrackets.keySet()) {
                    if (goldBrackets.containsKey(bracket)) {
                        test2MatchedS[span] += Math.min(goldBrackets.getCount(bracket), testBrackets.getCount(bracket));
                    }
                }
                if (gold2MatchedS[span] != test2MatchedS[span]) {
                    throw new RuntimeException("length does not match...");
                }
            }
        }
//        System.out.println("span\tcount\trecall1\trecall2\td-recall\tprecision1\tprecision2\td-precision\tf1.1\tf1.2\td-f1\tdp-error");
        double recall1 = gold1Matched / gold1Total * 100;
        double precision1 = gold1Matched / test1Total * 100;
        double f1 = 1 / (0.5 / recall1 + 0.5 / precision1);

        double recall2 = gold2Matched / gold2Total * 100;
        double precision2 = gold2Matched / test2Total * 100;
        double f2 = 1 / (0.5 / recall2 + 0.5 / precision2);

        double derror = ((100 - f1) - (100 - f2)) / (100 - f1) * 100;
//        System.out.format("all\t%.0f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n", gold1Total, recall1, recall2, recall2 - recall1,
//                precision1, precision2, precision2 - precision1, f1, f2, f2 - f1, derror);

        for (int span = 0; span <= maxLength; span++) {
            recall1 = gold1MatchedS[span] / gold1TotalS[span] * 100;
            precision1 = test1MatchedS[span] / test1TotalS[span] * 100;
            f1 = 1 / (0.5 / recall1 + 0.5 / precision1);

            recall2 = gold2MatchedS[span] / gold2TotalS[span] * 100;
            precision2 = test2MatchedS[span] / test2TotalS[span] * 100;
            f2 = 1 / (0.5 / recall2 + 0.5 / precision2);
            derror = ((100 - f1) - (100 - f2)) / (100 - f1) * 100;
//            System.out.println("==================");
//            System.out.println("span = " + i);
//            System.out.println("total = " + gold1Total[i]);
//            System.out.format("labeled recall1 = %.2f\n", recall1);
//            System.out.format("labeled recall2 = %.2f, -> %.2f\n", recall2, (recall2 - recall1));
//            System.out.format("labeled precision1 = %.2f\n", precision1);
//            System.out.format("labeled precision2 = %.2f, -> %.2f\n", precision2, (precision2 - precision1));
//            System.out.format("F11 = %.2f\n", f1);
//            System.out.format("F12 = %.2f, -> %.2f\n", f2, (f2 - f1));
//            System.out.format("%d\t%.2f\n", span, f2-f1);
//            System.out.format("%d\t%.0f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n", span, gold1TotalS[span], recall1, recall2, recall2 - recall1,
//                    precision1, precision2, precision2 - precision1, f1, f2, f2 - f1, derror);
            System.out.format("%d\t%.0f\t%.2f\t%.2f\t%.2f\t%.2f\n", span, gold1TotalS[span], f1, f2, f2-f1, derror);
//            System.out.format("%.0f\n", gold1TotalS[span]);
        }
    }

    private static void appendBracket(Tree<String> tree, UniCounter<Bracket> bracketCount, boolean noPunc) {
        int start = 0;
//        bracketSet.add(new Bracket(0, tree.getYield().size(), "ROOT"));
        for (Tree<String> child : tree.getChildren()) {
            int span = child.getYield().size();
            int end = start + span;
            appendBracket(child, start, end, bracketCount, noPunc);
            start = end;
        }
    }

    private static void appendBracket(Tree<String> tree, int start, int end, UniCounter<Bracket> bracketCount, boolean noPunc) {
        String label = tree.getLabel();
        int pos = label.indexOf('-');
        if (pos != -1) {
            label = label.substring(0, pos);
        }
        if (tree.isPreTerminal()) {
            bracketCount.incrementCount(new Bracket(start, start, label), 1);
            return;
        }

        if (noPunc) {
            List<String> preterminals = tree.getPreTerminalYield();
            int newstart = start, newend = end;
            if (preterminals.get(0).equals("PU")) {
                newstart = start + 1;
            }
            if (preterminals.get(preterminals.size() - 1).equals("PU")) {
                newend = end - 1;
            }
            if (newend > newstart) {
                bracketCount.incrementCount(new Bracket(newstart, newend, label), 1);
            }
        } else {
            bracketCount.incrementCount(new Bracket(start, end, label), 1);
        }
        for (Tree<String> child : tree.getChildren()) {
            int span = child.getYield().size();
            end = start + span;
            appendBracket(child, start, end, bracketCount, noPunc);
            start = end;
        }
    }
}