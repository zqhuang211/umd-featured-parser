/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.ling.PTBLineLexer;
import edu.umd.clip.jobs.Job;
import edu.umd.clip.jobs.JobGroup;
import edu.umd.clip.jobs.JobManager;
import edu.umd.clip.jobs.Worker;
import edu.umd.clip.ling.Tree;
import edu.umd.clip.util.GlobalLogger;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 *
 * @author zqhuang
 */
public class ProductParser {

    private static final long serialVersionUID = 1L;

    public static class Options {

        private static final long serialVersionUID = 1L;
        @Option(name = "-gl", required = true, usage = "GrammarList\n")
        public String grFileList;
        @Option(name = "-input", required = false, usage = "File to parse (Default: STDIN)")
        public String inputFile;
        @Option(name = "-output", required = false, usage = "Output parsed file (Default: STDOUT)")
        public String outputFile;
        @Option(name = "-jobs", usage = "Number of concurrent jobs (Default: 1)")
        public int jobs = 1;
        @Option(name = "-tokenize", required = false, usage = "Use the Stanford PTBLex Tokenizer (Default: false)")
        public boolean tokenize = false;
        @Option(name = "-disablePruning", required = false, usage = "Do not use incremental pruning (Default: false)")
        public boolean disablePruning = false;
        @Option(name = "-maxParsingMin", usage = "The maximum allowable minutes to parse a sentence. (Default: POS_INFINITY")
        public double maxParsingMin = 0;
        @Option(name = "-minPostProb", usage = "Pruning away rules with log posteriory probability lower than this threshold (Default: -15)")
        public double minPostProb = -15;
        @Option(name = "-oov", usage = "OOV handling method, choose from simple or heuristic (Default: heuristic)")
        public LexiconManager.OOVHandler oovHandler = LexiconManager.OOVHandler.heuristic;
        @Option(name = "-logLevel", required = false, usage = "Logging level: FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE (Default: INFO)")
        public String logLevel = "INFO";
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, Exception {
        OptionParser optParser = new OptionParser(Options.class);
        final Options opts = (Options) optParser.parse(args, true);

        GlobalLogger.init();
        GlobalLogger.setLevel(Level.parse(opts.logLevel));
        GlobalLogger.log(Level.INFO, String.format("Calling with " + optParser.getPassedInOptions()));

        PTBLineLexer tokenizer = null;
        if (opts.tokenize) {
            tokenizer = new PTBLineLexer();
        }

        JobManager.initialize(opts.jobs);
        Thread thread = new Thread(JobManager.getInstance(), "Job Manager");
        thread.setDaemon(true);
        thread.start();
        JobManager jobManager = JobManager.getInstance();
        InputStreamReader grStream = opts.grFileList != null ? new InputStreamReader(new FileInputStream(opts.grFileList), Charset.forName("UTF-8")) : new InputStreamReader(System.in, Charset.forName("UTF-8"));
        BufferedReader grReader = new BufferedReader(grStream);
        final ConcurrentLinkedQueue<List<Grammar>> grammarLists = new ConcurrentLinkedQueue<List<Grammar>>();
        String grFileName = "";
        Date startTime = new Date();

        int gi = 0;

        try {
            JobGroup grp = jobManager.createJobGroup("parsing");
            while ((grFileName = grReader.readLine()) != null) {
                final String fgrFileName = grFileName;
                final int fgi = gi++;
                Job job = new Job(
                        new Runnable() {
                    @Override
                    public void run() {
                        Grammar finalGrammar = Grammar.load(fgrFileName.trim());
                        List<Grammar> grammarList = finalGrammar.getParsingGrammarList();
                        if (grammarList == null) {
                            GlobalLogger.log(Level.SEVERE, String.format("Failed to load grammar from file: %s", fgrFileName));
                            System.exit(1);
                        }
                        for (Grammar grammar : grammarList) {
                            grammar.getLexiconManager().setOovHandler(opts.oovHandler);
                        }
                        grammarLists.add(grammarList);
                    }
                },
                        String.valueOf(fgi) + "-th grammar");
                job.setPriority(fgi);
                jobManager.addJob(grp, job);
            }
            grp.join();
        } catch (Exception e) {
            System.err.println(e);
            System.exit(-1);
        }


        Date endTime = new Date();
        double dur = (endTime.getTime() - startTime.getTime()) / 1000;
        GlobalLogger.log(Level.INFO, String.format("loading grammar takes: %.2f s", dur));

        for (Worker worker : jobManager.getWorkers()) {
            List<ParseDecoder> decoderList = new ArrayList<ParseDecoder>();
            for (List<Grammar> grammarList : grammarLists) {
                ParseDecoder decoder = new ParseDecoder(grammarList);
                decoder.setMaxParsingTime(opts.maxParsingMin);
                decoderList.add(decoder);
            }
            MultiParseDecoder multiDecoder = new MultiParseDecoder(decoderList);
            multiDecoder.setMinPostProb(opts.minPostProb);
            multiDecoder.setIncrementalPruning(!opts.disablePruning);
            worker.setReserved(multiDecoder);
        }
        InputStreamReader sentStream = opts.inputFile != null ? new InputStreamReader(new FileInputStream(opts.inputFile), Charset.forName("UTF-8")) : new InputStreamReader(System.in, Charset.forName("UTF-8"));
        BufferedReader sentReader = new BufferedReader(sentStream);
        startTime = new Date();
        final ParseManager parseManager = new ParseManager(opts.outputFile);
        try {
            JobGroup grp = jobManager.createJobGroup("parsing");
            String line = "";
            int sentNum = 0;
            while ((line = sentReader.readLine()) != null) {
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
                Job job = new Job(
                        new Runnable() {
                    public void run() {
                        try {
                            Worker worker = (Worker) Thread.currentThread();
                            MultiParseDecoder multiDecoder = (MultiParseDecoder) worker.getReserved();
                            Tree<String> parsedTree = null;
                            parsedTree = multiDecoder.getBestParse(finalSentence);
                            StringBuilder sb = new StringBuilder();
                            if (parsedTree == null) {
                                sb.append("( ())");
                            } else {
                                parsedTree = TreeAnnotations.unAnnotateTree(parsedTree);
                                sb.append("(");
                                for (Tree<String> child : parsedTree.getChildren()) {
                                    sb.append(" ").append(child);
                                }
                                sb.append(")");
                            }
                            parseManager.finishedJob(sentId, sb.toString());
                        } catch (Exception e) {
                            System.err.println(e);
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
            System.exit(-1);
        }
        endTime = new Date();
        dur = (endTime.getTime() - startTime.getTime()) / 1000;
        GlobalLogger.log(Level.INFO, String.format("parsing takes: %.2f s", dur));
    }
}
