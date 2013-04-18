/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.ling.PTBLineLexer;
import edu.umd.clip.ling.Tree;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import edu.umd.clip.jobs.Job;
import edu.umd.clip.jobs.JobGroup;
import edu.umd.clip.jobs.JobManager;
import edu.umd.clip.jobs.Worker;
import edu.umd.clip.util.GlobalLogger;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author zqhuang
 */
public class SingleParser {

    private static final long serialVersionUID = 1L;

    public static class Options {

        private static final long serialVersionUID = 1L;
        @Option(name = "-gr", required = true, usage = "Grammar file\n")
        public String grFileName;
        @Option(name = "-input", required = false, usage = "File to parse (Default: STDIN)")
        public String inputFile;
        @Option(name = "-output", required = false, usage = "Output parsed file (Default: STDOUT)")
        public String outputFile;
        @Option(name = "-tokenize", required = false, usage = "Use the Stanford PTBLex Tokenizer for English (Default: false)")
        public boolean tokenize = false;
        @Option(name = "-jobs", usage = "Number of concurrent jobs (Default: 1)")
        public int jobs = 1;
        @Option(name = "-score", usage = "Output parsing score (Default: false)")
        public boolean score = false;
        @Option(name = "-ll", usage = "Output log-likelihood score of the entire sentence (Default: false)")
        public boolean ll = false;
        @Option(name = "-maxParsingMin", usage = "The maximum allowable minutes to parse a sentence. (Default: POS_INFINITY")
        public double maxParsingMin = 0;
        @Option(name = "-constrainedViterbi", usage = "Use viterbi decoding on the max-rule-product tree (Default: false)")
        public boolean constrainedViterbi = false;
        @Option(name = "-nolatent", usage = "Remove latent annotations from the viterbi derivation. (Default: false)")
        public boolean nolatent = false;
        @Option(name = "-viterbi", usage = "Use viterbi decoding (Default: false)")
        public boolean viterbi = false;
        @Option(name = "-nbest", usage = "Indicate nbest parsing (Default: 0)")
        public int nbest = 0;
        @Option(name = "-oov", usage = "OOV handling method, choose from simple or heuristic (Default: heuristic)")
        public LexiconManager.OOVHandler oovHandler = LexiconManager.OOVHandler.heuristic;
        @Option(name = "-logLevel", required = false, usage = "Logging level: FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE (Default: INFO)")
        public String logLevel = "INFO";
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);

        GlobalLogger.init();
        GlobalLogger.setLevel(Level.parse(opts.logLevel));
        GlobalLogger.log(Level.INFO, String.format("Calling with " + optParser.getPassedInOptions()));

        JobManager.initialize(opts.jobs);
        Thread thread = new Thread(JobManager.getInstance(), "Job Manager");
        thread.setDaemon(true);
        thread.start();
        JobManager jobManager = JobManager.getInstance();

//        FeatureRichOOVLexicon.setLexRegWeight(opts.lexRegWeight);
        String grFileName = opts.grFileName;
        final Grammar finalGrammar = Grammar.load(grFileName);
        List<Grammar> grammarList = finalGrammar.getParsingGrammarList();
        for (Grammar grammar : grammarList) {
            grammar.getLexiconManager().setOovHandler(opts.oovHandler);
        }

        PTBLineLexer tokenizer = null;
        if (opts.tokenize) {
            tokenizer = new PTBLineLexer();
        }
        InputStreamReader inputStreamReader = opts.inputFile != null ? new InputStreamReader(new FileInputStream(opts.inputFile), Charset.forName("UTF-8")) : new InputStreamReader(System.in, Charset.forName("UTF-8"));
        BufferedReader inputReader = new BufferedReader(inputStreamReader);

        for (Worker worker : jobManager.getWorkers()) {
            ParseDecoder parser = new ParseDecoder(grammarList);
            parser.setMaxParsingTime(opts.maxParsingMin);
            parser.setNbestSize(opts.nbest);
            worker.setReserved(parser);
        }

        final boolean outputScore = opts.score;
        final boolean outputLL = opts.ll;
        final boolean viterbiOnMaxRule = opts.constrainedViterbi;
        final boolean nolatent = opts.nolatent;
        final boolean viterbi = opts.viterbi;
        final int nbest = opts.nbest;

        final ParseManager parseManager = new ParseManager(opts.outputFile);
        Date startTime = new Date();
        try {
            JobGroup grp = jobManager.createJobGroup("parsing");
            String line = "";
            int sentNum = 0;
            while ((line = inputReader.readLine()) != null) {
                final int sentId = sentNum++;
                line = line.trim();
                if (line.equals("")) {
                    parseManager.finishedJob(sentId, "");
                    continue;
                }
                final String finalLine = line;
                List<String> sentence = null;
                if (opts.tokenize) {
                    sentence = new ArrayList<String>();
                    for (String word : tokenizer.tokenizeLine(line)) {
                        sentence.addAll(Arrays.asList(word.trim().split(" +")));
                    }
                } else {
                    sentence = Arrays.asList(line.trim().split(" +"));
                }
                final List<String> finalSentence = sentence;
                Job job = new Job(new Runnable() {
                    public void run() {
                        Worker worker = (Worker) Thread.currentThread();
                        ParseDecoder parser = (ParseDecoder) worker.getReserved();
                        if (nbest == 0) {
                            Tree<String> parsedTree = null;
                            String parsedLine = null;
                            if (viterbi) {
                                parsedTree = getViterbiParse(parser, finalSentence);
                            } else {
                                parsedTree = getBestParse(parser, finalSentence);
                            }

                            if (viterbiOnMaxRule) {
                                Tree<String> viterbiTree = getViterbiOnMaxRule(parser, parsedTree, finalSentence);
                                parsedLine = printParse(parser, viterbiTree, outputLL, outputScore, true, nolatent, -1);
                                if (parsedLine.equals("( ())")) {
                                    System.err.println("Failed on viterbiOnMaxRule parsing the " + sentId + "-th sentence: " + finalLine);
                                }
                            } else {
                                parsedLine = printParse(parser, parsedTree, outputLL, outputScore, viterbi, nolatent, -1);
                                if (parsedLine.equals("( ())")) {
                                    System.err.println("Failed on maxrule parsing the " + sentId + "-th sentence: " + finalLine);
                                }
                            }
                            parseManager.finishedJob(sentId, parsedLine);
                        } else {
                            List<Tree<String>> parsedTrees = null;
                            String parsedLines = "";
                            if (viterbi) {
                                throw new RuntimeException("nNbest viterbi parsing is not supported yet...");
                            } else {
                                parsedTrees = getNBestParse(parser, finalSentence);
                            }
                            if (viterbiOnMaxRule) {
                                List<Tree<String>> viterbiTrees = getViterbiOnMaxRule(parser, parsedTrees, finalSentence);
                                parsedLines = printParse(parser, viterbiTrees, outputLL, outputScore, true, nolatent);
                                if (parsedLines.startsWith("====================\n0")) {
                                    System.err.println("Failed on viterbiOnMaxRule nbest parsing the " + sentId + "-th sentence: " + finalLine);
                                }
                            } else {
                                parsedLines = printParse(parser, parsedTrees, outputLL, outputScore, viterbi, nolatent);
                                if (parsedLines.startsWith("====================\n0")) {
                                    System.err.println("Failed on maxrule nbest parsing the " + sentId + "-th sentence: " + finalLine);
                                }
                            }
                            parseManager.finishedJob(sentId, parsedLines);
                        }
                    }
                },
                        String.valueOf(sentId) + "-th tree");
                job.setPriority(sentId);
                jobManager.addJob(grp, job);
            }
            grp.join();
        } catch (Exception e) {
            System.err.println(e);
        }

        Date endTime = new Date();
        double dur = (endTime.getTime() - startTime.getTime()) / 1000;
        System.err.println("parsing takes: " + dur + " s");
    }

    private static Tree<String> getViterbiParse(ParseDecoder parser, List<String> sentence) {
        Tree<String> parsedTree = parser.getBestViterbiParse(sentence);
        if (parsedTree == null && !parser.hasExceededTimeLimit()) {
            parser.useSmallThreasholds();
            parsedTree = parser.getBestViterbiParse(sentence);
            if (parsedTree == null && !parser.hasExceededTimeLimit()) {
                parser.useVerySmallThreaholds();
                parsedTree = parser.getBestViterbiParse(sentence);
            }
            parser.useLargeThreasholds();
        }
        return parsedTree;
    }

    private static Tree<String> getBestParse(ParseDecoder parser, List<String> sentence) {
        Tree<String> parsedTree = parser.getBestParse(sentence);
        if (parsedTree == null && !parser.hasExceededTimeLimit()) {
            parser.useSmallThreasholds();
            parsedTree = parser.getBestParse(sentence);
            if (parsedTree == null && !parser.hasExceededTimeLimit()) {
                parser.useVerySmallThreaholds();
                parsedTree = parser.getBestParse(sentence);
            }

            parser.useLargeThreasholds();
        }
        return parsedTree;
    }

    private static List<Tree<String>> getNBestParse(ParseDecoder parser, List<String> sentence) {
        List<Tree<String>> parsedTrees = parser.getNBestParse(sentence);
        if (parsedTrees == null && !parser.hasExceededTimeLimit()) {
            parser.useSmallThreasholds();
            parsedTrees = parser.getNBestParse(sentence);
            if (parsedTrees == null && !parser.hasExceededTimeLimit()) {
                parser.useVerySmallThreaholds();
                parsedTrees = parser.getNBestParse(sentence);
            }
            parser.useLargeThreasholds();
        }
        return parsedTrees;
    }

    private static List<Tree<String>> getViterbiOnMaxRule(ParseDecoder parser, List<Tree<String>> parsedTrees, List<String> sentence) {
        if (parsedTrees == null) {
            return null;
        }
        List<Tree<String>> viterbiTrees = new ArrayList<Tree<String>>();
        for (Tree<String> parsedTree : parsedTrees) {
            viterbiTrees.add(getViterbiOnMaxRule(parser, parsedTree, sentence));
        }
        return viterbiTrees;
    }

    private static Tree<String> getViterbiOnMaxRule(ParseDecoder parser, Tree<String> parsedTree, List<String> sentence) {
        Tree<String> viterbiTree = null;
        if (parsedTree != null) {
            viterbiTree = parser.getBestViterbiTree(parsedTree);
        }

        if (viterbiTree == null) {
            viterbiTree = parser.getBestViterbiParse(sentence);
            if (viterbiTree == null && !parser.hasExceededTimeLimit()) {
                parser.useSmallThreasholds();
                viterbiTree = parser.getBestViterbiParse(sentence);
                if (viterbiTree == null && !parser.hasExceededTimeLimit()) {
                    parser.useVerySmallThreaholds();
                    viterbiTree = parser.getBestViterbiParse(sentence);
                }

                parser.useLargeThreasholds();
            }
        }
        return viterbiTree;
    }

    private static String printParse(ParseDecoder parser, Tree<String> parsedTree,
            boolean outputLL, boolean outputScore, boolean viterbi,
            boolean nolatent, int ibest) {
        StringBuilder sb = new StringBuilder();

        if (parsedTree == null) {
            sb.append("( ())");
        } else {
            if (viterbi) {
                if (outputScore) {
                    sb.append(String.format("score=%.5f : ", parser.getViterbiScore()));
                }
            } else {
                if (outputLL) {
                    sb.append(String.format("ll=%.5f : ", parser.getLL()));
                }
                if (outputScore) {
                    if (ibest < 0) {
                        sb.append(String.format("score=%.5f : ", parser.getMaxScore()));
                    } else {
                        sb.append(String.format("score=%.5f : ", parser.getMaxScore(ibest)));
                    }
                }
            }

            if (!viterbi || (viterbi && nolatent)) {
                parsedTree = TreeAnnotations.unAnnotateTree(parsedTree);
            }
            sb.append("(");
            for (Tree<String> child : parsedTree.getChildren()) {
                sb.append(" ").append(child);
            }

            sb.append(")");
        }
        return sb.toString();
    }

    private static String printParse(ParseDecoder parser, List<Tree<String>> parsedTrees,
            boolean outputLL, boolean outputScore, boolean viterbi,
            boolean nolatent) {
        StringBuilder sb = new StringBuilder();
        sb.append("====================\n");
        if (parsedTrees == null) {
            sb.append(0);
        } else {
            sb.append(parsedTrees.size());
            for (Tree<String> parsedTree : parsedTrees) {
                sb.append("\n").append(printParse(parser, parsedTree, outputLL, outputScore, viterbi, nolatent, parsedTrees.indexOf(parsedTree)));
            }
        }
        return sb.toString();
    }
}
