/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.lacluster;

import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import edu.umd.clip.util.Pair;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class ClusterReporter {

    private static final long serialVersionUID = 1L;

    public static class Options {

        @Option(name = "-cluster", required = true, usage = "Input cluster")
        public String cluster = null;
        @Option(name = "-text", required = false, usage = "Input text")
        public String text = null;
        @Option(name = "-top", required = false, usage = "Top clusters")
        public int top = 10;
    }

    public static void main(String[] args) throws FileNotFoundException, CloneNotSupportedException, IOException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);
        System.out.println(optParser.getPassedInOptions());

        LatentCluster latentCluster = new LatentCluster();
        latentCluster.load(opts.cluster);
        latentCluster.setInitUnseen(true);
        if (opts.text == null) {
            latentCluster.printTopWords(opts.top);
        } else {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                    new FileInputStream(opts.text), Charset.forName("UTF-8")));
            String line = "";
            while ((line = reader.readLine()) != null) {
                List<AlphaBetaItem> alphaBetaList = Corpus.convertString2AlphaBeta(line);
                List<PosteriorItem> postList = latentCluster.calcPosterior(alphaBetaList, opts.top);
                if (postList == null)
                    continue;
                int sentSize = alphaBetaList.size();
                System.out.println("--------------------------------------");
                for (int i = 1; i < sentSize - 1; i++) {
                    System.out.print(alphaBetaList.get(i).getWord() + ":");
                    List<Pair<Integer, Double>> postItem = postList.get(i).getPosteriorList();
                    for (int ci = 0; ci < postItem.size(); ci++) {
                        System.out.printf(" %d_%.3f", postItem.get(ci).getFirst(), postItem.get(ci).getSecond());
                    }
                    System.out.println();
                }
                System.out.println();
                System.out.flush();
            }
        }
    }
}
