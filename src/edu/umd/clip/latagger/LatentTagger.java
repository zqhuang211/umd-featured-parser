/*
 * LatentTagger.java
 *
 * Created on May 15, 2007, 11:44 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.util.Numberer;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import edu.umd.clip.jobs.Job;
import edu.umd.clip.jobs.JobGroup;
import edu.umd.clip.jobs.JobManager;
import edu.umd.clip.jobs.OutputManager;
import edu.umd.clip.jobs.SynchronizedCounter;
import edu.umd.clip.jobs.Worker;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class LatentTagger {

    private static final long serialVersionUID = 1L;

    public static class Options {

        @Option(name = "-model", required = true, usage = "Input model file (Required)\n")
        public String modelFileName;
        @Option(name = "-input", required = false, usage = "Input test file (default: stdin)\n")
        public String testFileName;
        @Option(name = "-output", required = false, usage = "Output tagged file (default: stdout)\n")
        public String outputFileName;
        @Option(name = "-decoder", required = false, usage = "Choose a specific decoder. 0:viterbi, 1:variationalTrigram (default: 1)\n")
        public int decoder = 1;
        @Option(name = "-jm", required = false, usage = "Specify JM smothing parameter. (default: use the trained parameter)")
        public double jmSmoothingParam = 0;
        @Option(name = "-nbest", required = false, usage = "Set nbest size. (default: 0)")
        public int nbest = 0;
        @Option(name = "-perp", required = false, usage = "Calculate perplexity. (default: false)")
        public boolean perp = false;
        @Option(name = "-score", required = false, usage = "Output tagging score. (default: 0)")
        public boolean score = false;
        @Option(name = "-jobs", required = false, usage = "Number of parallel jobs. (default: 1")
        public int jobs = 1;
    }

    public static void main(String[] args) throws FileNotFoundException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);
        System.err.println(optParser.getPassedInOptions());

        String modelFile = opts.modelFileName;
        String testFile = opts.testFileName;
        String outputFile = opts.outputFileName;
        double jmSmoothingParam = opts.jmSmoothingParam;

        LatentTaggerData latentTaggerData = LatentTaggerData.load(modelFile);
        Numberer.setNumberers(latentTaggerData.getNumberer());
        LatentTagStates.setLatentStateNum(latentTaggerData.getLatentStateNum());
        LatentTagStates.setSplitTrees(latentTaggerData.getSplitTrees());
        LatentEmission latentEmission = latentTaggerData.getLatentEmission();
        LatentTransition latentTransition = latentTaggerData.getLatentTransition();
        if (jmSmoothingParam != 0) {
            latentTransition.setJMSmoothingParam(jmSmoothingParam);
        }
        TaggerModelList modelList = new TaggerModelList(latentEmission, latentTransition, LatentTagStates.getSplitTrees());
        final OutputManager outputManager = new OutputManager(opts.outputFileName);

        JobManager.initialize(opts.jobs);
        Thread thread = new Thread(JobManager.getInstance(), "Job Manager");
        thread.setDaemon(true);
        thread.start();
        JobManager jobManager = JobManager.getInstance();
        for (Worker worker : jobManager.getWorkers()) {
            CoarseToFineDecoder decoder = new CoarseToFineDecoder(modelList, opts.decoder);
            if (opts.nbest > 0) {
                decoder.setNbestSize(opts.nbest);
            }
            worker.setReserved(decoder);
        }
        JobGroup grp = jobManager.createJobGroup("tagging");

        try {
            BufferedReader inputReader = new BufferedReader(testFile != null ? new InputStreamReader(new FileInputStream(testFile), Charset.forName("UTF-8")) : new InputStreamReader(System.in, Charset.forName("UTF-8")));
            BufferedWriter outputWriter = new BufferedWriter(outputFile != null ? new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8")) : new OutputStreamWriter(System.out, Charset.forName("UTF-8")));

            final boolean perp = opts.perp;
            final int nbest = opts.nbest;
            final boolean score = opts.score;

            String inLine = "";
            int sentid = 0;

            final SynchronizedCounter totalLogLikelihoodCounter = new SynchronizedCounter();
            final SynchronizedCounter totalWordsCounter = new SynchronizedCounter();

            while ((inLine = inputReader.readLine()) != null) {
                final int finalSentId = sentid++;
                final List<String> sentence = Arrays.asList(inLine.trim().split("\\s+"));
                Job job = new Job(
                        new Runnable() {

                            public void run() {
                                Worker worker = (Worker) Thread.currentThread();
                                CoarseToFineDecoder decoder = (CoarseToFineDecoder) worker.getReserved();
                                if (perp) {
                                    double logLikelihood = decoder.getLogLikelihood(sentence);
                                    if (Double.isInfinite(logLikelihood)) {
                                        System.err.println("Failed to compute log likelihood score for the following sentence:\n " + sentence);
                                    } else {
                                        totalLogLikelihoodCounter.add(logLikelihood);
                                        totalWordsCounter.add(sentence.size());
                                    }
                                } else if (nbest > 0) {
                                    List<List<String>> nbestTaggedSentences = decoder.getNBestTags(sentence);
                                    int nbestSize = 0;
                                    if (nbestTaggedSentences != null) {
                                        nbestSize = nbestTaggedSentences.size();
                                    }
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("======================== " + nbestSize + "\n");

                                    for (int i = 0; i < nbestSize; i++) {
                                        if (score) {
                                            sb.append(decoder.getIBestMaxScore(i) + " ");

                                        }
                                        for (String item : nbestTaggedSentences.get(i)) {
                                            sb.append(item + " ");
                                        }
                                        sb.append("\n");
                                    }
                                    outputManager.finishedJob(finalSentId, sb.toString());
                                } else {
                                    List<String> taggedSentence = decoder.getBestTags(sentence);
                                    StringBuilder sb = new StringBuilder();
                                    if (taggedSentence == null) {
                                        System.err.println("failed on the " + finalSentId + "-th sentence: " + sentence);
                                        sb.append("###failed###");
                                    } else {
                                        if (score) {
                                            sb.append(decoder.getMaxScore() + " ");
                                        }
                                        for (String item : taggedSentence) {
                                            sb.append(item + " ");
                                        }
                                    }
                                    sb.append("\n");
                                    outputManager.finishedJob(finalSentId, sb.toString());
                                }
                            }
                        },
                        String.valueOf(finalSentId) + "-th sent");
                job.setPriority(finalSentId);
                jobManager.addJob(grp, job);
            }
            grp.join();
            outputWriter.flush();

            if (opts.perp) {
                double aveLogLikelihood = totalLogLikelihoodCounter.getCount() / totalWordsCounter.getCount();
                double perplexity = Math.pow(2, -aveLogLikelihood / Math.log(2));
                System.out.format("Total Number of Words: %.0f\n", totalWordsCounter.getCount());
                System.out.format("Total Log-Likelihood Score: %.6f\n", totalLogLikelihoodCounter.getCount());
                System.out.format("Average Log-Likelihood Score: %.6f\n", aveLogLikelihood);
                System.out.format("Perplexity: %.6f\n", perplexity);
                System.out.flush();
            }

            outputWriter.flush();

            inputReader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }
    
}
