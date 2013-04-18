/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.lacluster;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.util.BiCounter;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import edu.umd.clip.util.UniCounter;
import edu.umd.clip.jobs.JobManager;
import edu.umd.clip.math.MDI;
import edu.umd.clip.math.MDIClusterNotifier;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author zqhuang
 */
public class MDITrainer {

    private static final long serialVersionUID = 1L;

    public static class Options {

        private static final long serialVersionUID = 1L;
        @Option(name = "-freq", required = false, usage = "Count threshold of frequent words (default: 1000)")
        public int freq = 1000;
        @Option(name = "-height", required = false, usage = "Report at the specified height of clustering tree (default: 20)")
        public int height = 10;
        @Option(name = "-input", required = false, usage = "Input training text (default: stdin)")
        public String inputFile = null;
        @Option(name = "-jobs", required = false, usage = "Number of parallel jobs (default: 1)")
        public int jobs = 1;
    }

    public static void main(String[] args) throws FileNotFoundException, CloneNotSupportedException, IOException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);
        System.out.println(optParser.getPassedInOptions());

        UniCounter<String> unigramCounter = new UniCounter<String>();
        BiCounter<String, String> leftBigramCounter = new BiCounter<String, String>();
        BiCounter<String, String> rightBigramCounter = new BiCounter<String, String>();

        JobManager.initialize(opts.jobs);
        Thread thread = new Thread(JobManager.getInstance(), "Job Manager");
        thread.setDaemon(true);
        thread.start();


        InputStreamReader in = opts.inputFile != null ? new InputStreamReader(new FileInputStream(opts.inputFile), Charset.forName("UTF-8")) : new InputStreamReader(System.in, Charset.forName("UTF-8"));
        BufferedReader reader = new BufferedReader(in);
        String line = "";
        // tally counts
        while ((line = reader.readLine()) != null) {
            String[] words = line.trim().split("\\s+");
            for (int wi = 0; wi < words.length; wi++)
                words[wi] = words[wi].intern();
            for (int wi = 0; wi < words.length; wi++) {
                String prevWord = "**SOS_WORD***".intern();
                if (wi > 0) {
                    prevWord = words[wi - 1];
                }
                String nextWord = "***EOS_WORD***".intern();
                if (wi < words.length - 1) {
                    nextWord = words[wi + 1];
                }
                unigramCounter.incrementCount(words[wi], 1);
                leftBigramCounter.incrementCount(words[wi], prevWord, 1);
                rightBigramCounter.incrementCount(words[wi], nextWord, 1);
            }
        }

        List<Entry<String, Double>> wordList = new ArrayList<Entry<String, Double>>();
        wordList.addAll(unigramCounter.entrySet());
        Comparator<Entry<String, Double>> entryComparator = new Comparator<Entry<String, Double>>() {

            public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
                return Double.compare(o2.getValue(), o1.getValue());
            }
        };

        // sort by word counts
        Collections.sort(wordList, entryComparator);
        Collection<String> allWords = new HashSet<String>();
        Collection<String> rareWords = new HashSet<String>();

        // put all words in allWords collection, rare words in rareWords collection
        System.err.println("---------- the " + opts.freq + " most frequent words ----------");
        for (int i = 0; i < opts.freq; i++) {
            Entry<String, Double> entry = wordList.get(i);
            System.err.println(entry.getKey() + ": " + entry.getValue());
            allWords.add(entry.getKey());
        }

        for (int i = opts.freq; i < wordList.size(); i++) {
            Entry<String, Double> entry = wordList.get(i);
            allWords.add(entry.getKey());
            rareWords.add(entry.getKey());
        }

        MDI<String, String> algo = new MDI<String, String>(allWords, allWords);
        // normalize to obtain conditional probabilities and prepare for the MDI algorithm
        for (Entry<String, UniCounter<String>> biEntry : leftBigramCounter.entrySet()) {
            String currWord = biEntry.getKey();
            double currCount = unigramCounter.getCount(currWord);
            for (Entry<String, Double> uniEntry : biEntry.getValue().entrySet()) {
                String leftWord = uniEntry.getKey();
                double bigramCount = uniEntry.getValue();
                algo.setLeftProb(currWord, leftWord, bigramCount / currCount);
            }
        }

        for (Entry<String, UniCounter<String>> biEntry : rightBigramCounter.entrySet()) {
            String currWord = biEntry.getKey();
            double currCount = unigramCounter.getCount(currWord);
            for (Entry<String, Double> uniEntry : biEntry.getValue().entrySet()) {
                String rightWord = uniEntry.getKey();
                double bigramCount = uniEntry.getValue();
                algo.setRightProb(currWord, rightWord, bigramCount / currCount);
            }
        }

        final Map<Collection<String>, Tree<Collection<String>>> treeNodes =
                new HashMap<Collection<String>, Tree<Collection<String>>>();
        final MDIClusterNotifier<String> MDINotifier = new MDIClusterNotifier<String>() {

            public boolean notify(Collection<String> oldCluster, Collection<String> cluster1, Collection<String> cluster2) {
                Tree<Collection<String>> node = treeNodes.get(oldCluster);
                Tree<Collection<String>> subnode1 = new Tree<Collection<String>>(cluster1, new ArrayList<Tree<Collection<String>>>());
                Tree<Collection<String>> subnode2 = new Tree<Collection<String>>(cluster2, new ArrayList<Tree<Collection<String>>>());
                List<Tree<Collection<String>>> children = node.getChildren();
                children.add(subnode1);
                children.add(subnode2);
                treeNodes.put(cluster1, subnode1);
                treeNodes.put(cluster2, subnode2);
                return true;
            }
        };
        algo.setNotifier(MDINotifier);
        algo.normalizeDistributions();
        algo.partition(rareWords);

        Tree<Collection<String>> treeRoot = treeNodes.get(rareWords);
        List<Tree<Collection<String>>> treesAtHeight = treeRoot.getAtHeight(opts.height);
        for (Tree<Collection<String>> tree : treesAtHeight) {
            System.out.print("==>");
            for (String word : tree.getLabel()) {
                System.out.print(" " + word);
            }
            System.out.println();
        }
    }
}
