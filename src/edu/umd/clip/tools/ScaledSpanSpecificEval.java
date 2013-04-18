/*
 * Evaluates the precision and accuracy of two test_parses against the told_parse at different span level
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class ScaledSpanSpecificEval {

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

        int maxLength = 17;
        List<HashSet<Bracket>> goldBracketsList = new ArrayList(maxLength + 1);
        for (int i = 0; i < maxLength + 1; i++) {
            goldBracketsList.add(new HashSet<Bracket>());
        }

        List<HashSet<Bracket>> test1BracketsList = new ArrayList(maxLength + 1);
        for (int i = 0; i < maxLength + 1; i++) {
            test1BracketsList.add(new HashSet<Bracket>());
        }

        List<HashSet<Bracket>> test2BracketsList = new ArrayList(maxLength + 1);
        for (int i = 0; i < maxLength + 1; i++) {
            test2BracketsList.add(new HashSet<Bracket>());
        }

        int maxScale = 10;

        double[] gold1Total = new double[maxScale];
        double[] gold1Matched = new double[maxScale];

        double[] test1Total = new double[maxScale];
        double[] test1Matched = new double[maxScale];

        double[] gold2Total = new double[maxScale];
        double[] gold2Matched = new double[maxScale];

        double[] test2Total = new double[maxScale];
        double[] test2Matched = new double[maxScale];

        Set<Bracket> bracketSet = new HashSet<Bracket>();
        while ((goldLine = goldReader.readLine()) != null) {
            goldLine = goldLine.trim();
            Iterator<Tree<String>> goldIterator = new Trees.PennTreeReader(goldLine);
            Tree<String> goldTree = goldIterator.next();
            int goldLength = goldTree.getYield().size();

            bracketSet.clear();
            appendBracket(goldTree, bracketSet);
            for (HashSet<Bracket> brackets : goldBracketsList) {
                brackets.clear();
            }
            for (Bracket bracket : bracketSet) {
                int span = bracket.getEnd() - bracket.getStart();
                if (span > maxLength) {
                    continue;
                }
                goldBracketsList.get(span).add(bracket);
            }

            test1Line = test1Reader.readLine();
            test1Line = test1Line.trim();
            Iterator<Tree<String>> test1Iterator = new Trees.PennTreeReader(test1Line);
            Tree<String> test1Tree = test1Iterator.next();
            int test1Length = test1Tree.getYield().size();
            assert (goldLength == test1Length);

            bracketSet.clear();
            appendBracket(test1Tree, bracketSet);
            for (HashSet<Bracket> brackets : test1BracketsList) {
                brackets.clear();
            }
            for (Bracket bracket : bracketSet) {
                int span = bracket.getEnd() - bracket.getStart();
                if (span > maxLength) {
                    continue;
                }
                test1BracketsList.get(span).add(bracket);
            }

            test2Line = test2Reader.readLine();
            test2Line = test2Line.trim();
            Iterator<Tree<String>> test2Iterator = new Trees.PennTreeReader(test2Line);
            Tree<String> test2Tree = test2Iterator.next();
            int test2Length = test2Tree.getYield().size();
            assert (goldLength == test2Length);

            bracketSet.clear();
            appendBracket(test2Tree, bracketSet);
            for (HashSet<Bracket> brackets : test2BracketsList) {
                brackets.clear();
            }
            for (Bracket bracket : bracketSet) {
                int span = bracket.getEnd() - bracket.getStart();
                if (span > maxLength) {
                    continue;
                }
                test2BracketsList.get(span).add(bracket);
            }
            
              int limit = Math.min(goldLength, maxLength);

            for (int i = 1; i <= limit; i++) {
                HashSet<Bracket> goldBrackets = goldBracketsList.get(i);
                HashSet<Bracket> testBrackets = test1BracketsList.get(i);

                int scaledI = (int)((double) i / (limit + 1) * maxScale + 0.5) -1 ;


                gold1Total[scaledI] += goldBrackets.size();
                for (Bracket bracket : goldBrackets) {
                    if (testBrackets.contains(bracket)) {
                        gold1Matched[scaledI]++;
                    }
                }

                test1Total[scaledI] += testBrackets.size();
                for (Bracket bracket : testBrackets) {
                    if (goldBrackets.contains(bracket)) {
                        test1Matched[scaledI]++;
                    }
                }
            }

            for (int i = 1; i <= limit; i++) {
                HashSet<Bracket> goldBrackets = goldBracketsList.get(i);
                HashSet<Bracket> testBrackets = test2BracketsList.get(i);

                int scaledI = (int) ((double) i / (limit + 1) * maxScale + 0.5) -1;

                gold2Total[scaledI] += goldBrackets.size();
                for (Bracket bracket : goldBrackets) {
                    if (testBrackets.contains(bracket)) {
                        gold2Matched[scaledI]++;
                    }
                }

                test2Total[scaledI] += testBrackets.size();
                for (Bracket bracket : testBrackets) {
                    if (goldBrackets.contains(bracket)) {
                        test2Matched[scaledI]++;
                    }
                }
            }

        }
        System.out.println("span\tcount\trecall1\trecall2\td-recall\tprecision1\tprecision2\td-precision\tf1.1\tf1.2\td-f1");
        for (int span = 0; span < maxScale; span++) {
            double recall1 = gold1Matched[span] / gold1Total[span] * 100;
            double precision1 = test1Matched[span] / test1Total[span] * 100;
            double f1 = 1 / (0.5 / recall1 + 0.5 / precision1);

            double recall2 = gold2Matched[span] / gold2Total[span] * 100;
            double precision2 = test2Matched[span] / test2Total[span] * 100;
            double f2 = 1 / (0.5 / recall2 + 0.5 / precision2);

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
            System.out.format("%d\t%.0f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n", span, gold1Total[span], recall1, recall2, recall2 - recall1,
                    precision1, precision2, precision2 - precision1, f1, f2, f2 - f1);
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