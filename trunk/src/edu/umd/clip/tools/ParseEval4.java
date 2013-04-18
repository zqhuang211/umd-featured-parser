/*
 * Evaluates the precision and accuracy of two test_parses against the told_parse at different span level
 * Only evaluate the brackets spaning the same range
 */
package edu.umd.clip.tools;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.ling.Trees;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
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
public class ParseEval4 {

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
        List<HashMap<Integer, Bracket>> goldBracketsList = new ArrayList(maxLength + 1);
        for (int i = 0; i < maxLength + 1; i++) {
            goldBracketsList.add(new HashMap<Integer, Bracket>());
        }

        List<HashMap<Integer, Bracket>> test1BracketsList = new ArrayList(maxLength + 1);
        for (int i = 0; i < maxLength + 1; i++) {
            test1BracketsList.add(new HashMap<Integer, Bracket>());
        }

        List<HashMap<Integer, Bracket>> test2BracketsList = new ArrayList(maxLength + 1);
        for (int i = 0; i < maxLength + 1; i++) {
            test2BracketsList.add(new HashMap<Integer, Bracket>());
        }


        double[] gold1Total = new double[maxLength + 1];
        double[] gold1Matched = new double[maxLength + 1];

        double[] test1Total = new double[maxLength + 1];
        double[] test1Matched = new double[maxLength + 1];

        double[] gold2Total = new double[maxLength + 1];
        double[] gold2Matched = new double[maxLength + 1];

        double[] test2Total = new double[maxLength + 1];
        double[] test2Matched = new double[maxLength + 1];

        Set<Bracket> bracketSet = new HashSet<Bracket>();
        while ((goldLine = goldReader.readLine()) != null) {
            goldLine = goldLine.trim();
            Iterator<Tree<String>> goldIterator = new Trees.PennTreeReader(goldLine);
            Tree<String> goldTree = goldIterator.next();
            int goldLength = goldTree.getYield().size();

            bracketSet.clear();
            appendBracket(goldTree, bracketSet);
            for (HashMap<Integer, Bracket> brackets : goldBracketsList) {
                brackets.clear();
            }
            for (Bracket bracket : bracketSet) {
                int span = bracket.getEnd() - bracket.getStart();
                if (span > maxLength) {
                    continue;
                }
                goldBracketsList.get(span).put(bracket.getSpanKey(), bracket);
            }

            test1Line = test1Reader.readLine();
            test1Line = test1Line.trim();
            Iterator<Tree<String>> test1Iterator = new Trees.PennTreeReader(test1Line);
            Tree<String> test1Tree = test1Iterator.next();
            int test1Length = test1Tree.getYield().size();
            assert (goldLength == test1Length);

            bracketSet.clear();
            appendBracket(test1Tree, bracketSet);
            for (HashMap<Integer, Bracket> brackets : test1BracketsList) {
                brackets.clear();
            }
            for (Bracket bracket : bracketSet) {
                int span = bracket.getEnd() - bracket.getStart();
                if (span > maxLength) {
                    continue;
                }
                test1BracketsList.get(span).put(bracket.getSpanKey(), bracket);
            }

            test2Line = test2Reader.readLine();
            test2Line = test2Line.trim();
            Iterator<Tree<String>> test2Iterator = new Trees.PennTreeReader(test2Line);
            Tree<String> test2Tree = test2Iterator.next();
            int test2Length = test2Tree.getYield().size();
            assert (goldLength == test2Length);

            bracketSet.clear();
            appendBracket(test2Tree, bracketSet);
            for (HashMap<Integer, Bracket> brackets : test2BracketsList) {
                brackets.clear();
            }
            for (Bracket bracket : bracketSet) {
                int span = bracket.getEnd() - bracket.getStart();
                if (span > maxLength) {
                    continue;
                }
                test2BracketsList.get(span).put(bracket.getSpanKey(), bracket);
            }

            for (int span = 1; span <= Math.min(goldLength, maxLength); span++) {
                HashMap<Integer, Bracket> goldBrackets = goldBracketsList.get(span);
                HashMap<Integer, Bracket> testBrackets = test1BracketsList.get(span);

                for (Entry<Integer, Bracket> entry : goldBrackets.entrySet()) {
                    int spanKey = entry.getKey();
                    Bracket goldBracket = entry.getValue();
                    if (testBrackets.containsKey(spanKey)) {
                        gold1Total[span]++;
                        Bracket testBracket = testBrackets.get(spanKey);
                        if (goldBracket.equals(testBracket)) {
                            gold1Matched[span]++;
                        }
                    }
                }

                for (Entry<Integer, Bracket> entry : testBrackets.entrySet()) {
                    int spanKey = entry.getKey();
                    Bracket testBracket = entry.getValue();
                    if (goldBrackets.containsKey(spanKey)) {
                        test1Total[span]++;
                        Bracket goldBracket = goldBrackets.get(spanKey);
                        if (goldBracket.equals(testBracket)) {
                            test1Matched[span]++;
                        }
                    }
                }

            }

            for (int span = 1; span <= Math.min(goldLength, maxLength); span++) {
                HashMap<Integer, Bracket> goldBrackets = goldBracketsList.get(span);
                HashMap<Integer, Bracket> testBrackets = test2BracketsList.get(span);

                for (Entry<Integer, Bracket> entry : goldBrackets.entrySet()) {
                    int spanKey = entry.getKey();
                    Bracket goldBracket = entry.getValue();
                    if (testBrackets.containsKey(spanKey)) {
                        gold2Total[span]++;
                        Bracket testBracket = testBrackets.get(spanKey);
                        assert (goldBracket.getStart() == testBracket.getStart());
                        assert (goldBracket.getEnd() == testBracket.getEnd());
                        if (goldBracket.equals(testBracket)) {
                            gold2Matched[span]++;
                        }
                    }
                }

                for (Entry<Integer, Bracket> entry : testBrackets.entrySet()) {
                    int spanKey = entry.getKey();
                    Bracket testBracket = entry.getValue();
                    if (goldBrackets.containsKey(spanKey)) {
                        test2Total[span]++;
                        Bracket goldBracket = goldBrackets.get(spanKey);
                        assert (goldBracket.getStart() == testBracket.getStart());
                        assert (goldBracket.getEnd() == testBracket.getEnd());
                        if (goldBracket.equals(testBracket)) {
                            test2Matched[span]++;
                        }
                    }
                }
            }
        }
        for (int span = 1; span <= maxLength; span++) {
            double recall1 = gold1Matched[span] / gold1Total[span] * 100;
            double precision1 = test1Matched[span] / test1Total[span] * 100;
            double f1 = 1 / (0.5 / recall1 + 0.5 / precision1);

            double recall2 = gold2Matched[span] / gold2Total[span] * 100;
            double precision2 = test2Matched[span] / test2Total[span] * 100;
            double f2 = 1 / (0.5 / recall2 + 0.5 / precision2);

            System.out.println("==================");
            System.out.println("span = " + span);
            System.out.println("total = " + gold1Total[span]);
            System.out.format("labeled recall1 = %.2f\n", recall1);
            System.out.format("labeled recall2 = %.2f, -> %.2f\n", recall2, (recall2 - recall1));
            System.out.format("labeled precision1 = %.2f\n", precision1);
            System.out.format("labeled precision2 = %.2f, -> %.2f\n", precision2, (precision2 - precision1));
            System.out.format("F11 = %.2f\n", f1);
            System.out.format("F12 = %.2f, -> %.2f\n", f2, (f2 - f1));
//            System.out.format("%d\t%.2f\n", span, f2 - f1);
        }
    }

    private static void appendBracket(Tree<String> tree, Set<Bracket> bracketSet) {
        int start = 0;
        for (Tree<String> child : tree.getChildren()) {
            int span = child.getYield().size();
            int end = start + span;
            appendBracket(child, start, end, bracketSet);
            start = end;
        }
    }

    private static void appendBracket(Tree<String> tree, int start, int end, Set<Bracket> bracketSet) {
        if (tree.isLeaf()) {
            return;
        }

        String label = tree.getLabel();
        int pos = label.lastIndexOf('-');
        if (pos != -1) {
            label = label.substring(0, pos);
        }

        bracketSet.add(new Bracket(start, end, label));
        for (Tree<String> child : tree.getChildren()) {
            int span = child.getYield().size();
            end =
                    start + span;
            appendBracket(child, start, end, bracketSet);
            start =
                    end;
        }
    }
}