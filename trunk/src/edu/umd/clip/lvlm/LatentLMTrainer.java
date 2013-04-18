/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.lvlm;

import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import edu.umd.clip.jobs.JobManager;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class LatentLMTrainer {

    public static class Options {

        @Option(name = "-train", required = false, usage = "Input training file (Default: stdin)")
        public String trainingFile = null;
        @Option(name = "-lm", required = true, usage = "Output language model (Required)")
        public String modelFile;
        @Option(name = "-splitRate", required = false, usage = "Splitting rate (Default: 0.5)")
        public double splitRate = 0.5;
        @Option(name = "-minEM", required = false, usage = "Minimum number of EM iteration after each splitting (Default: 20)")
        public int minEM = 20;
        @Option(name = "-maxEM", required = false, usage = "Maximum number of EM iteration after each splitting (Default: 50)")
        public int maxEM = 50;
        @Option(name = "-minInc", required = false, usage = "EM stop if less then this minimum percentage of likelihood reduction is achieved (Default: 0.0001)")
        public double minInc = 0.0001;
        @Option(name = "-splitNum", required = false, usage = "Total number of splitting (Default: 6)")
        public int splitNum = 6;
        @Option(name = "-unk", required = false, usage = "Words occur no more than the specified counts are converted to UNK (Default: 5)")
        public int unk = 5;
        @Option(name = "-jobs", required = false, usage = "Number of parallel jobs (Default: 1)")
        public int jobs = 1;
    }

    public static void main(String[] args) throws Exception {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);
        System.out.println("Calling with " + optParser.getPassedInOptions());

        JobManager.initialize(opts.jobs);
        Thread thread = new Thread(JobManager.getInstance(), "Job Manager");
        thread.setDaemon(true);
        thread.start();

        LatentLM latentLM = new LatentLM();
        latentLM.setSplittingRate(opts.splitRate);

        List<List<Token>> trainingData = latentLM.loadTrainingData(opts.trainingFile, opts.unk);

        latentLM.doInitializationStep(trainingData);
        String modelFile = opts.modelFile + "-" + 0 + ".lm";
        System.out.format("\nI am saving the latent language model to: %s\n", modelFile);
        latentLM.save(modelFile);

        for (int splitIt = 1; splitIt <= opts.splitNum; splitIt++) {
            latentLM.doSplitting();
            double lastll = Double.NEGATIVE_INFINITY;
            boolean lastEM = false;
            System.out.println();
            for (int emIt = 1; emIt <= opts.maxEM; emIt++) {
                double ll = latentLM.doEMStep(trainingData, (lastEM || emIt == opts.maxEM) && splitIt != opts.splitNum);
                System.out.format("(%d,%d)-th EM: %.6f\n", splitIt, emIt, ll);
                System.out.flush();
                if (lastEM) {
                    break;
                }
                if (emIt >= opts.minEM && Math.abs((ll - lastll) / ll) < opts.minInc) {
                    lastEM = true;
                }
                lastll = ll;
            }
            modelFile = opts.modelFile + "-" + splitIt + ".lm";
            System.out.format("\nI am saving the latent language model to: %s\n", modelFile);
            latentLM.save(modelFile);
        }
    }
}
