/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.tools;

import edu.umd.clip.parser.Grammar;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import edu.umd.clip.util.Pair;

/**
 *
 * @author zqhuang
 */
public class ComputeRuleNum {

    public static class Options {

        @Option(name = "-model", required = true, usage = "model file")
        public String modelFile = null;
    }

    public static void main(String[] args) {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);

        String modelFile = opts.modelFile;
        Grammar grammar = Grammar.load(modelFile);

        Pair<Long, Long> ruleNum = grammar.getRuleManager().getRuleNum();
        double totalRules = ruleNum.getFirst();
        double zeroRules = ruleNum.getSecond();
        double nonzeroRules = totalRules - zeroRules;
        System.out.format("total rules: %.0f, zero rules: %.0f, nonzero rules: %.0f, percentage: %.2f%%\n",
                totalRules, zeroRules, nonzeroRules, zeroRules / totalRules*100);
    }
}
