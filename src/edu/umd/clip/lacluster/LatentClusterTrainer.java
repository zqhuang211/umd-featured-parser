/*
 * LatentTaggerTrainer.java
 *
 * Created on May 23, 2007, 1:43 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.lacluster;

import edu.umd.clip.math.RandomDisturbance;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Zhongqiang Huang
 */
public class LatentClusterTrainer {

    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(LatentClusterTrainer.class.getName());

    public static class Options {

        @Option(name = "-seed", required = false, usage = "Seed of the random number generator (default: 0)")
        public int seed = 0;
        @Option(name = "-train", required = true, usage = "Input training file (required)")
        public String trainFileName;
        @Option(name = "-unlabeled", required = true, usage ="Input unlabeled training data")
        public String unlabeled;
        @Option(name = "-model", required = true, usage = "Output model file prefix (required)")
        public String modelPrefix;
        @Option(name = "-splitNum", required = false, usage = "The number of state splits (default: 10)")
        public int splitNum = 10;
        @Option(name = "-iterNum", required = false, usage = "The number of iterations after each state spliting (default: 20)")
        public int iterNum = 20;
    }

    public static void main(String[] args) throws FileNotFoundException, CloneNotSupportedException, IOException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);
        System.out.println(optParser.getPassedInOptions());

        RandomDisturbance.setRandSeed(opts.seed);
        String trainFile = opts.trainFileName;
        String modelPrefix = opts.modelPrefix;
        int splitNum = opts.splitNum;
        int iterNum = opts.iterNum;

        logger.info("loading the training corpus...");
        Collection<List<AlphaBetaItem>> labeledCorpus = Corpus.loadTrainingData(trainFile);
        logger.info("initialization...");
        LatentCluster latentCluster = new LatentCluster();
        latentCluster.doInitialization(labeledCorpus);
        latentCluster.save(modelPrefix+"-0.cluster");
        Collection<List<AlphaBetaItem>> unlabeledCorpus = Corpus.convertString2AlphaBeta(Corpus.loadStringTrainingData(opts.unlabeled));
        latentCluster.setInitUnseen(true);
        latentCluster.setCorpus(unlabeledCorpus);
        latentCluster.doEM();
        latentCluster.setInitUnseen(false);
        for (int splitIndex = 1; splitIndex <= splitNum; splitIndex++) {
//            latentCluster.split();
            for (int iterIndex = 0; iterIndex < iterNum; iterIndex++) {
                double ll = latentCluster.doEM();
                System.gc();
                System.out.println(splitIndex + "-th split, " + iterIndex + "-th iteration: " + ll);
//                if (iterIndex == iterNum/2)
//                    latentCluster.selectTopK(4);
            }
            latentCluster.save(modelPrefix+"-"+splitIndex+".cluster");
        }
    }
}
