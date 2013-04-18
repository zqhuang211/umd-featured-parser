/*
 * Evaluates the precision and accuracy of the test_parse against the told_parse at different height levels
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
public class ParseEval {

    public static class Options {

        @Option(name = "-gold", required = true, usage = "gold parse tree")
        public String goldFile = null;
        @Option(name = "-test", required = true, usage = "test parse tree")
        public String testFile = null;
    }

    public static void main(String[] args) throws IOException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);

        BufferedReader goldReader = new BufferedReader(new InputStreamReader(new FileInputStream(opts.goldFile), Charset.forName("UTF-8")));
        BufferedReader testReader = new BufferedReader(new InputStreamReader(new FileInputStream(opts.testFile), Charset.forName("UTF-8")));

        String goldLine = "", testLine = "";

        int maxHeight = 10;
        List<HashSet<Bracket>> goldBracketsList = new ArrayList(maxHeight);
        for (int i = 0; i < maxHeight; i++) {
            goldBracketsList.add(new HashSet<Bracket>());
        }

        List<HashSet<Bracket>> testBracketsList = new ArrayList(maxHeight);
        for (int i = 0; i < maxHeight; i++) {
            testBracketsList.add(new HashSet<Bracket>());
        }

        double[] goldTotal = new double[maxHeight];
        double[] goldMatched = new double[maxHeight];

        double[] testTotal = new double[maxHeight];
        double[] testMatched = new double[maxHeight];

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
                    if (pos != -1)
                    label = label.substring(0, pos);
                    brackets.add(new Bracket(start, end,  label));
                    start = end;
                }
            }

            testLine = testReader.readLine();
            testLine = testLine.trim();
            Iterator<Tree<String>> testIterator = new Trees.PennTreeReader(testLine);
            Tree<String> testTree = testIterator.next();

            for (HashSet<Bracket> brackets : testBracketsList) {
                brackets.clear();
            }
            int testDepth = testTree.getDepth();
            int testHeight = Math.min(testDepth - 1, maxHeight); // ignore the root

            for (int i = 2; i < testHeight; i++) {
                int start = 0;
                HashSet<Bracket> brackets = testBracketsList.get(i);
                List<Tree<String>> subTrees = testTree.getAtHeight(i);
                for (Tree<String> subTree : subTrees) {
                    int end = start + subTree.getYield().size();
                    brackets.add(new Bracket(start, end, subTree.getLabel()));
                    start = end;
                }
            }

            for (int i = 2; i < Math.min(goldHeight, testHeight); i++) {
                HashSet<Bracket> goldBrackets = goldBracketsList.get(i);
                HashSet<Bracket> testBrackets = testBracketsList.get(i);

                goldTotal[i] += goldBrackets.size();
                for (Bracket bracket : goldBrackets) {
                    if (testBrackets.contains(bracket)) {
                        goldMatched[i]++;
                    }
                }

                testTotal[i] += testBrackets.size();
                for (Bracket bracket : testBrackets) {
                    if (goldBrackets.contains(bracket)) {
                        testMatched[i]++;
                    }
                }
            }
        }
        for (int i = 2; i < maxHeight; i++) {
            double recall =  goldMatched[i] / goldTotal[i];
            double precision = testMatched[i] / testTotal[i];
            double f1 = 1/ (0.5/recall + 0.5/precision);
            System.out.println("==================");
            System.out.println("height = " + i);
//            System.out.println("total gold = " + goldTotal[i]);
//            System.out.println("total matched = " + goldMatched[i]);
            System.out.println("labeled recall = " + recall);
//            System.out.println("------------------");
//            System.out.println("total tst = " + testTotal[i]);
//            System.out.println("total matched = " + testMatched[i]);
            System.out.println("labeled precision = " + precision);
            System.out.println("F1 = "+f1);
        }
    }
}