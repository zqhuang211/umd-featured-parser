/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.clip.lvlm;

import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import edu.umd.clip.jobs.JobManager;

/**
 *
 * @author zqhuang
 */
public class LatentLMTester {
  public static class Options {

        @Option(name = "-lm", required = true, usage = "Input language model (Required)")
        public String modelFile;
        @Option(name = "-test", required = false, usage = "Test file (Default: stdin)")
        public String testFile;
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
        
        LatentLM latentLM = LatentLM.load(opts.modelFile);
        latentLM.calcPerplexity(opts.testFile);
    }
}