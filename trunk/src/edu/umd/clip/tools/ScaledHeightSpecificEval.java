/*
 * Evaluates the precision and accuracy of two test_parses against the told_parse at different depth level.
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

/**
 *
 * @author zqhuang
 */
public class ScaledHeightSpecificEval {

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

        int maxHeight = 30;
        List<HashSet<Bracket>> goldBracketsList = new ArrayList(maxHeight);
        for (int i = 0; i < maxHeight; i++) {
            goldBracketsList.add(new HashSet<Bracket>());
        }

        List<HashSet<Bracket>> test1Brackets = new ArrayList(maxHeight);
        for (int i = 0; i < maxHeight; i++) {
            test1Brackets.add(new HashSet<Bracket>());
        }

        List<HashSet<Bracket>> test2Brackets = new ArrayList(maxHeight);
        for (int i = 0; i < maxHeight; i++) {
            test2Brackets.add(new HashSet<Bracket>());
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

        while ((goldLine = goldReader.readLine()) != null) {
            goldLine = goldLine.trim();
            Iterator<Tree<String>> goldIterator = new Trees.PennTreeReader(goldLine);
            Tree<String> goldTree = goldIterator.next();

            for (HashSet<Bracket> brackets : goldBracketsList) {
                brackets.clear();
            }
            int goldDepth = goldTree.getDepth();
            int goldHeight = Math.min(goldDepth - 1, maxHeight); // ignore the root

            for (int i = 2; i < goldHeight; i++) {
                HashSet<Bracket> brackets = goldBracketsList.get(i);
                int start = 0;
                List<Tree<String>> subTrees = goldTree.getAtHeight(i);
                for (Tree<String> subTree : subTrees) {
                    int end = start + subTree.getYield().size();
                    String label = subTree.getLabel();
                    int pos = label.lastIndexOf('-');
                    if (pos != -1) {
                        label = label.substring(0, pos);
                    }
                    brackets.add(new Bracket(start, end, label));
                    start = end;
                }
            }

            test1Line = test1Reader.readLine();
            test1Line = test1Line.trim();
            Iterator<Tree<String>> test1Iterator = new Trees.PennTreeReader(test1Line);
            Tree<String> test1Tree = test1Iterator.next();

            for (HashSet<Bracket> brackets : test1Brackets) {
                brackets.clear();
            }
            int test1Depth = test1Tree.getDepth();
            int test1Height = Math.min(test1Depth - 1, maxHeight); // ignore the root

            for (int i = 2; i < test1Height; i++) {
                int start = 0;
                HashSet<Bracket> brackets = test1Brackets.get(i);
                List<Tree<String>> subTrees = test1Tree.getAtHeight(i);
                for (Tree<String> subTree : subTrees) {
                    int end = start + subTree.getYield().size();
                    brackets.add(new Bracket(start, end, subTree.getLabel()));
                    start = end;
                }
            }

            test2Line = test2Reader.readLine();
            test2Line = test2Line.trim();
            Iterator<Tree<String>> test2Iterator = new Trees.PennTreeReader(test2Line);
            Tree<String> test2Tree = test2Iterator.next();

            for (HashSet<Bracket> brackets : test2Brackets) {
                brackets.clear();
            }
            int test2Depth = test2Tree.getDepth();
            int test2Height = Math.min(test2Depth - 1, maxHeight); // ignore the root

            for (int i = 2; i < test2Height; i++) {
                int start = 0;
                HashSet<Bracket> brackets = test2Brackets.get(i);
                List<Tree<String>> subTrees = test2Tree.getAtHeight(i);
                for (Tree<String> subTree : subTrees) {
                    int end = start + subTree.getYield().size();
                    brackets.add(new Bracket(start, end, subTree.getLabel()));
                    start = end;
                }
            }

            int limit = Math.min(goldHeight, test1Height);

            for (int i = 2; i < limit; i++) {
                HashSet<Bracket> goldBrackets = goldBracketsList.get(i);
                HashSet<Bracket> testBrackets = test1Brackets.get(i);

                int scaledI = (int) ((double) (i - 1) / (limit - 2) * maxScale + 0.5) - 1;


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

            for (int i = 2; i < limit; i++) {
                HashSet<Bracket> goldBrackets = goldBracketsList.get(i);
                HashSet<Bracket> testBrackets = test2Brackets.get(i);

                int scaledI = (int) ((double) (i - 1) / (limit - 2) * maxScale + 0.5) - 1;//int scaledI = (int) ((double) i / (limit + 1) * maxScale + 0.5) - 1;

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
        System.out.println("height\tcount\trecall1\trecall2\td-recall\tprecision1\tprecision2\td-precision\tf1.1\tf1.2\td-f1");
        for (int i = 0; i < maxScale; i++) {
            double recall1 = gold1Matched[i] / gold1Total[i] * 100;
            double precision1 = test1Matched[i] / test1Total[i] * 100;
            double f1 = 1 / (0.5 / recall1 + 0.5 / precision1);

            double recall2 = gold2Matched[i] / gold2Total[i] * 100;
            double precision2 = test2Matched[i] / test2Total[i] * 100;
            double f2 = 1 / (0.5 / recall2 + 0.5 / precision2);

//            System.out.println("==================");
//            System.out.println("height = " + i);
//            System.out.format("labeled recall1 = %.2f\n", recall1);
//            System.out.format("labeled recall2 = %.2f, -> %.2f\n", recall2, (recall2-recall1));
//            System.out.format("labeled precision1 = %.2f\n", precision1);
//            System.out.format("labeled precision2 = %.2f, -> %.2f\n", precision2, (precision2-precision1));
//            System.out.format("F11 = %.2f\n", f1);
//            System.out.format("F12 = %.2f, -> %.2f\n", f2, (f2-f1));
            System.out.format("%d\t%.0f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n", i, gold1Total[i], recall1, recall2, recall2 - recall1,
                    precision1, precision2, precision2 - precision1, f1, f2, f2 - f1);
        }
    }
}