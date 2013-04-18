/*
 * LatentTaggerTrainer.java
 *
 * Created on May 23, 2007, 1:43 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.math.RandomDisturbance;
import edu.umd.clip.util.Numberer;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import edu.umd.clip.jobs.JobManager;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

/**
 *
 * @author Zhongqiang Huang
 */
public class LatentTaggerTrainer {

    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(LatentTaggerTrainer.class.getName());

    public static class Options {

        @Option(name = "-seed", required = false, usage = "Seed of the random number generator (default: 0)")
        public int seed = 0;
        @Option(name = "-train", required = true, usage = "Input training file (required)")
        public String trainFileName;
        @Option(name = "-old", required = false, usage = "Old model file")
        public String oldModel;
        @Option(name = "-model", required = true, usage = "Output model prefix (required)")
        public String modelPrefixName;
        @Option(name = "-lex", required = true, usage = "Output lexicon file (required)")
        public String lexFile;
        @Option(name = "-splitNum", required = false, usage = "The number of state splits (default: 6)")
        public int splitNum = 6;
        @Option(name = "-maxSplitIter", required = false, usage = "The maximum number of iterations after each state spliting (default: 50)")
        public int maxSplitIter = 50;
        @Option(name = "-minSplitIter", required = false, usage = "The minimum number of iterations after each state spliting (default: 50)")
        public int minSplitIter = 50;
        @Option(name = "-maxMergeIter", required = false, usage = "The maximum number of iterations after each state merging (default: 20)")
        public int maxMergeIter = 20;
        @Option(name = "-minMergeIter", required = false, usage = "The minimum number of iterations after each state merging (default: 20)")
        public int minMergeIter = 20;
        @Option(name = "-maxSmoothIter", required = false, usage = "The maximum number of iterations for the smoothing step (default: 10)")
        public int maxSmoothIter = 10;
        @Option(name = "-minSmoothIter", required = false, usage = "The minimum number of iterations for the smoothing step (default: 10)")
        public int minSmoothIter = 10;
        @Option(name = "-mergeRate", required = false, usage = "Set the percentage of substates to be merged (default: 0.5)")
        public double mergeRate = 0.5;
        @Option(name = "-startSplit", required = false, usage = "Split number in oldModel (default: 0)")
        public int startSplit = 0;
        @Option(name = "-unk", required = false, usage = "Unknown word threshold (default: 5)")
        public int unk = 5;
        @Option(name = "-jobs", required = false, usage = "Number of concurrent jobs (default: 1)")
        public int jobs = 1;
        @Option(name = "-tagSmooth", required = false, usage = "The transition smoothing parameter (default: 0.1)")
        public double tagSmoothingParam = 0.1;
        @Option(name = "-lexSmooth", required = false, usage = "The lexicon smoothing parameter (default: 0.1)")
        public double lexSmoothingParam = 0.1;
        @Option(name = "-stage", required = false, usage = "The start stage, 0 for splitting, 1 for merging, 2 for smoothing (default: 0)")
        public int stage = 0;
        @Option(name = "-forceSmooth", required = false, usage = "Force to use new smoothing parameters if restarting from an old model (default: false)")
        public boolean forceSmooth = false;
        @Option(name = "-addNewData", required = false, usage = "Add new data for training, need to deal with unknown words, now only use the labeled data to update the lexicon model.")
        public boolean addNewData = false;
    }

    public static void main(String[] args) throws FileNotFoundException, CloneNotSupportedException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);
        System.out.println(optParser.getPassedInOptions());

        RandomDisturbance.setRandSeed(opts.seed);
        String trainFile = opts.trainFileName;
        String modelPrefix = opts.modelPrefixName;
        String lexFile = opts.lexFile;
        String oldModel = opts.oldModel;
        int splitNum = opts.splitNum;
        int startSplit = opts.startSplit;
        if (opts.stage == 0) {
            startSplit++;
        }

        JobManager.initialize(opts.jobs);
        Thread thread = new Thread(JobManager.getInstance(), "Job Manager");
        thread.setDaemon(true);
        thread.start();

        LatentEmission latentEmission = null;
        LatentTransition latentTransition = null;
        String outputModel = null;
        LatentTaggerData latentTaggerData = null;

        LatentTrainer latentTrainer = null;
        if (oldModel != null) {
            logger.info("I am now loading the old model from " + oldModel);
            latentTaggerData = LatentTaggerData.load(oldModel);
            Numberer.setNumberers(latentTaggerData.getNumberer());
            LatentTagStates.setLatentStateNum(latentTaggerData.getLatentStateNum());
            LatentTagStates.setSplitTrees(latentTaggerData.getSplitTrees());
            LatentTagStates.initRestart(opts.stage);
            latentEmission = latentTaggerData.getLatentEmission();
            latentTransition = latentTaggerData.getLatentTransition();
            if (opts.forceSmooth) {
                latentEmission.setLatentSmoothingParam(opts.lexSmoothingParam);
                latentTransition.setLatentSmoothingParam(opts.tagSmoothingParam);
            }
            latentTrainer = new LatentTrainer(latentEmission, latentTransition, opts.mergeRate);
            latentTrainer.loadTrainingData(trainFile, opts.unk);
        } else {
            latentEmission = new LatentEmission();
            latentTransition = new LatentTransition();
            latentTrainer = new LatentTrainer(latentEmission, latentTransition, opts.mergeRate);
            latentTrainer.loadTrainingData(opts.trainFileName, opts.unk);
            logger.info("I am now tallying the statistics on the training set...");
            latentEmission.setLatentSmoothingParam(opts.lexSmoothingParam);
            latentTransition.setLatentSmoothingParam(opts.tagSmoothingParam);
            latentEmission.tallyCounts(latentTrainer.getTrainingCorpus());
            latentEmission.initializeLatent();
            latentTransition.tallyCounts(latentTrainer.getTrainingCorpus());
            latentTransition.initializeLatent();
            logger.info("I am now saving the map file to disk...");
            latentEmission.saveLexicon(lexFile);
            logger.info("I am now saving the standard bigram tagger model to disk...");
            outputModel = modelPrefix + "-0.tagger";
            latentTaggerData = new LatentTaggerData(latentEmission, latentTransition,
                    Numberer.getNumberers(), LatentTagStates.getLatentStateNum(),
                    LatentTagStates.getSplitTrees());
            latentTaggerData.save(outputModel);
        }

        double trainLogLikelihood = 0;

        int maxStep = 3;
        int minIterNum = 0;
        int maxIterNum = 0;
        String stepName = "";
        boolean first = true;
        for (int splitIndex = startSplit; splitIndex <= splitNum; splitIndex++) {
            int startStage = 0;
            if (first) {
                startStage = opts.stage;
                first = false;
            }
            for (int stepIndex = startStage; stepIndex < maxStep; stepIndex++) {
                switch (stepIndex) {
                    case 0:
                        latentTrainer.splitStates();
                        latentTrainer.setSmoothingFlag(false);
                        minIterNum = opts.minSplitIter;
                        maxIterNum = opts.maxSplitIter;
                        stepName = "splitting";
                        break;
                    case 1:
                        latentTrainer.mergeStates();
                        latentTrainer.setSmoothingFlag(false);
                        minIterNum = opts.minMergeIter;
                        maxIterNum = opts.maxMergeIter;
                        stepName = "merging";
                        break;
                    case 2:
                        latentTrainer.setSmoothingFlag(true);
                        minIterNum = opts.minSmoothIter;
                        maxIterNum = opts.maxSmoothIter;
                        stepName = "smoothing";
                        break;
                }
                int iterIndex = 0;
                do {
                    iterIndex++;
                    trainLogLikelihood = latentTrainer.doExpectationStep();
                    logger.info("Training Info(" + splitIndex + "," + stepName + "," + iterIndex + "): the previous training log likelihood score is: " + trainLogLikelihood);
                    latentTrainer.doMaximizationStep();
                } while (iterIndex < maxIterNum);
                outputModel = modelPrefix + "-" + splitIndex + "-" + stepName + ".tagger";
                logger.info("Info: I am saving " + outputModel);
                latentTaggerData.save(outputModel);
            }
        }
        System.gc();
    }
}
