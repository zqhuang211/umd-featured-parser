/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.math.RandomDisturbance;
import edu.umd.clip.util.GlobalLogger;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import edu.umd.clip.jobs.JobManager;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

/**
 *
 * @author zqhuang
 */
public class GrammarTrainer {

    private static final long serialVersionUID = 1L;
    public static final double SCALE = Math.exp(100);
    // Note: e^709 is the largest double java can handle.
    public static boolean VERBOSE = false;
    public static Random RANDOM = new Random(0);

    public static class Options {

        @Option(name = "-in", required = false, usage = "Initial grammar (Default: null)")
        public String initGrammar = null;
        @Option(name = "-out", required = true, usage = "Output grammar prefix")
        public String outFileName;
        @Option(name = "-train", required = true, usage = "List of training files")
        public String trainList = null;
        @Option(name = "-numSplits", usage = "The number of split&merge iterations (Default: 6)")
        public int numSplits = 6;
        @Option(name = "-mergingRate", usage = "Merging percentage (Default: 0.5)")
        public double mergingRate = 0.5;
        @Option(name = "-splitIter", usage = "Number of EM iterations after splitting (Default: 50, set to 200 if -featured)")
        public int splitIterations = 50;
        @Option(name = "-mergeIter", usage = "Number of EM iterations after merging (Default: 20)")
        public int mergeIterations = 20;
        @Option(name = "-smoothIter", usage = "Number of EM iterations with smoothing (Default: 20)")
        public int smoothIterations = 10;
        @Option(name = "-initLBFGSIter", usage = "Number of LBFGS iterations after state splitting and merging (Default: 20)")
        public int initLBFGSIter = 20;
        @Option(name = "-LBFGSIter", usage = "Number of LBFGS iterations between EM iterations (Default: 20)")
        public int LBFGSIter = 20;
        @Option(name = "-OOVLBFGSIter", usage = "Number of LBFGS iterations for the featured OOV model (Default: 100)")
        public int OOVLBFGSIter = 100;
        @Option(name = "-b", usage = "LEFT/RIGHT Binarization (Default: RIGHT)")
        public Binarization binarization = Binarization.RIGHT;
        @Option(name = "-lang", usage = "Langage, choose from english, chinese, arabic, or others (Default: english)")
        public Grammar.Language lang = Grammar.Language.english;
        @Option(name = "-hor", usage = "Horizontal Markovization (Default: 0)")
        public int horizontalMarkovization = 0;
        @Option(name = "-ver", usage = "Vertical Markovization (Default: 1)")
        public int verticalMarkovization = 1;
        @Option(name = "-rare", usage = "Rare word threshold (Default: 12)")
        public double rareThreshold = 12;
        @Option(name = "-lessMem", usage = "Use the (slower) memory efficient mode (Default: false)")
        public boolean lessMem = false;
        @Option(name = "-jobs", usage = "number of concurrent jobs (Default: 1)")
        public int jobs = 1;
        @Option(name = "-seed", usage = "Seed for random number generator (Default: 0)")
        public int seed = 0;
        @Option(name = "-manualTrees", usage = "The number of manually labeled trees (Default: Integer.MAX_VALUE)")
        public int manualTrees = Integer.MAX_VALUE;
        @Option(name = "-autoWeight", usage = "The weight of the automatically labeled trees (Default: 1)")
        public double autoWeight = 1;
        @Option(name = "-featured", usage = "Use the feature-rich lexicon (Default: false)")
        public boolean useFeatureLexicon = false;
        @Option(name = "-lexReg", usage = "Regularization weight of lexical rule features (Default: 1)")
        public double lexWeight = 1;
        @Option(name = "-synReg", usage = "Regularization weight of phrasal rule probabilities (Default: 0)")
        public double synWeight = 0;
        @Option(name = "-wordPred", required = false, usage = "Word predicates for the latent lexical model (Default: null)")
        public String wordPredFile = null;
        @Option(name = "-oovPred", required = false, usage = "Word predicates for the OOV model (Default: null)")
        public String oovPredicate = null;
        @Option(name = "-oov", required = false, usage = "The rare word threshold for the FeaturedOOVLexicon (Default: 12)")
        public double oovThreshold = 12;
        @Option(name = "-oovLexReg", usage = "Regularization weight of lexical rule features for the FeaturedOOVLexicon (Default: 1)")
        public double oovLexWeight = 1;
        @Option(name = "-logLevel", required = false, usage = "Logging level: FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE (Default: INFO)")
        public String logLevel = "INFO";
    }

    public static void main(String[] args) throws Exception {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);
        GlobalLogger.init();
        GlobalLogger.setLevel(Level.parse(opts.logLevel));
        
        GlobalLogger.log(Level.INFO, String.format("Calling with " + optParser.getPassedInOptions()));

        RandomDisturbance.setRandSeed(opts.seed);
        int numSplits = opts.numSplits;
        String trainList = opts.trainList;

        Binarization binarization = opts.binarization;
        String outFileName = opts.outFileName;

        GlobalLogger.log(Level.INFO, String.format("Grammars will be saved using prefix: %s", outFileName));

        JobManager.initialize(opts.jobs);
        Thread thread = new Thread(JobManager.getInstance(), "Job Manager");
        thread.setDaemon(true);
        thread.start();

        Corpus corpus = new Corpus(trainList);
        List<Tree<String>> trainStringTrees = Corpus.binarizeAndFilterTrees(corpus.getTrainTrees(), opts.verticalMarkovization, opts.horizontalMarkovization, binarization, VERBOSE);
        corpus = null;
        if (opts.mergingRate > 0) {
            GlobalLogger.log(Level.INFO, String.format("This percentage of recent splits will be merged at each SM round: %d%%", (int) (opts.mergingRate * 100)));
        }

        int initSplit = 1;
        Grammar grammar = null;
        if (opts.initGrammar == null) {
            grammar = new Grammar(opts.lang, opts.useFeatureLexicon);
        } else {
            grammar = Grammar.load(opts.initGrammar);
            initSplit = grammar.getNumSplits() + 1;
        }

        grammar.setManualTrees(opts.manualTrees);
        grammar.setAutoWeight(opts.autoWeight);
        grammar.setMergingRate(opts.mergingRate);
        grammar.setRareWordThreshold(opts.rareThreshold); //TODO: keep all the setters together.

        FeaturedLexiconManager featuredLexicon = null;
        if (opts.useFeatureLexicon) { //TODO: revisit this, clean up
            featuredLexicon = (FeaturedLexiconManager) grammar.getLexiconManager();
            featuredLexicon.setRegWeights(opts.lexWeight, opts.synWeight);
            featuredLexicon.loadWordPredicates(opts.wordPredFile);
            featuredLexicon.setOptimizationMode(FeaturedLexiconManager.OptimizationMode.indirect);

            FeaturedOOVLexiconManager oovLexicon = featuredLexicon.getOOVLexicon();
            oovLexicon.setLexRegWeight(opts.oovLexWeight);
            oovLexicon.setRareWordThreshold(opts.oovThreshold);
            oovLexicon.loadWordPredicates(opts.oovPredicate);
            oovLexicon.setIterNum(opts.OOVLBFGSIter);
        }

        ConstituentTreeList trainConstituentTrees = new ConstituentTreeList(trainStringTrees, grammar, opts.lessMem);
        trainStringTrees = null; // free memory
        System.gc();

        if (opts.initGrammar == null) {
            grammar.doInitStep(trainConstituentTrees);
            if (opts.useFeatureLexicon) {
                featuredLexicon.setupOOVLexicon();
                featuredLexicon.clear();
            }
            String grFile = outFileName + "-" + 0 + ".gr";
            GlobalLogger.log(Level.INFO, String.format("Saving 0-th grammar: %s", grFile));
            //TODO: check this
//            grammar.clearFeatureRichLexicon();
            grammar.save(grFile);
        }
        int maxIter = 0;

        for (int splitIndex = initSplit; splitIndex <= numSplits; splitIndex++) {
            for (int typeIndex = 0; typeIndex < 3; typeIndex++) {
                switch (typeIndex % 3) {
                    case 0: // splitting
                        GlobalLogger.log(Level.INFO, String.format("Beginning splitting step: %d", splitIndex));
                        grammar.setSmoothingMode(false);
                        grammar.doSplittingStep();
                        maxIter = opts.splitIterations;
                        break;
                    case 1: // merging
                        GlobalLogger.log(Level.INFO, String.format("Beginning merging step: %d", splitIndex));
                        grammar.setSmoothingMode(false);
                        grammar.doMergingStep(trainConstituentTrees);
                        maxIter = opts.mergeIterations;

                        break;
                    case 2: // smoothing
                        GlobalLogger.log(Level.INFO, String.format("Beginning smoothing step: %d", splitIndex));
                        grammar.setSmoothingMode(true);
                        maxIter = opts.smoothIterations;
                        break;
                }
                GlobalLogger.log(Level.INFO, String.format("Number of EM iterations: %d", maxIter));

                // Direct optimization only follows after certain rounds of 
                // indirect optimization.
                // TODO: double check this for the feature rich model
//                grammar.setDirectOptimization(false);
                for (int iter = 0; iter < maxIter; iter++) {
                    /**
                     * Only do direct optimization after maxIter/5 rounds of
                     * indirect optimization for the splitting and merging
                     * steps. Indirect optimization is performed first because
                     * it reduces training likelihood faster then direct
                     * optimization.
                     */
                    //TODO: double check this for the feature rich model
                    if (opts.useFeatureLexicon) {
                        if (iter == 0 && typeIndex != 2) {
                            featuredLexicon.setIterNum(opts.initLBFGSIter);
                        }
                        if (iter == maxIter / 5 && typeIndex != 2) {
                            featuredLexicon.setOptimizationMode(FeaturedLexiconManager.OptimizationMode.direct);
                            featuredLexicon.initOptimizationParams();
                        }
                    }
//                   
                    /**
                     * The phrasal rule probabilities are always smoothed at the
                     * smoothing setup. For the lexical rule probabilities, they
                     * are regularized during LBFGS optimization for the feature
                     * rich model and are explicitly smoothed when retrieved for
                     * the regular model.
                     */
                    // TODO: check this for the feature rich model
//                    if (typeIndex == 2) {
//                        grammar.getRuleManager().smoothRuleProbs();
//                    }
                    double logLikelihood = grammar.doEMStep(trainConstituentTrees);
                    GlobalLogger.log(Level.INFO, String.format("%d-th EM: %.2f", new Object[]{iter, logLikelihood}));
                }
                if (opts.useFeatureLexicon) {
                    featuredLexicon.setOptimizationMode(FeaturedLexiconManager.OptimizationMode.indirect);
                    featuredLexicon.initOptimizationParams();
                    featuredLexicon.setIterNum(opts.LBFGSIter);
                }

                /**
                 * This is called because the next step always starts with
                 * indirect optimization.
                 */
                //TODO: Double check this for the feature rich model
//                grammar.initIndirectOptimization();
            }
//            System.out.printf("Beginning coarsening step: %d", splitIndex);
//            grammar.setupSplitTree(trainConstituentTrees);
            if (opts.useFeatureLexicon) {
                featuredLexicon.clear();
            }
            String grFile = outFileName + "-" + splitIndex + ".gr";
            GlobalLogger.log(Level.INFO, String.format("Saving %d-th grammar: %s", splitIndex, grFile));
//            Pair<Long, Long> ruleNum = grammar.getRuleManager().getRuleNum();]
            grammar.save(grFile);
        }
        GlobalLogger.log(Level.INFO, String.format("Training completed"));
    }
}
