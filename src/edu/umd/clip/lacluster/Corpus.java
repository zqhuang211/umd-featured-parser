/*
 * Corpus.java
 *
 * Created on May 15, 2007, 4:37 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.lacluster;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class Corpus {

    private static final long serialVersionUID = 1L;

    public static Collection<List<AlphaBetaItem>> loadTrainingData(String file) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
        Collection<List<AlphaBetaItem>> corpus = new ArrayList<List<AlphaBetaItem>>();
        String line = "";
        while ((line = reader.readLine()) != null) {
            List<AlphaBetaItem> alphaBetaList = new ArrayList<AlphaBetaItem>();
            alphaBetaList.add(new AlphaBetaItem("***SOS_WORD***".intern(), "SOS".intern()));
            for (String item : Arrays.asList(line.trim().split("\\s+"))) {
                int loc = item.indexOf("/");
                alphaBetaList.add(new AlphaBetaItem(item.substring(0, loc).intern(),
                        item.substring(loc + 1, item.length()).intern()));
            }
            alphaBetaList.add(new AlphaBetaItem("***EOS_WORD***".intern(), "EOS".intern()));
            corpus.add(alphaBetaList);
        }
        return corpus;
    }

    public static Collection<List<String>> loadStringTrainingData(String file) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
        Collection<List<String>> corpus = new ArrayList<List<String>>();
        String line = "";
        while ((line = reader.readLine()) != null) {
            List<String> words = new ArrayList<String>();
            words.add("***SOS_WORD***".intern());
            for (String word : Arrays.asList(line.trim().split("\\s+"))) {
                words.add(word.intern());
            }
            words.add("***EOS_WORD***".intern());
            corpus.add(words);
        }
        return corpus;
    }

    public static List<AlphaBetaItem> convertString2AlphaBeta(String line) {
        List<AlphaBetaItem> alphaBetaSent = new ArrayList<AlphaBetaItem>();
        alphaBetaSent.add(new AlphaBetaItem("***SOS_WORD***"));
        for (String word : Arrays.asList(line.trim().split("\\s+"))) {
            alphaBetaSent.add(new AlphaBetaItem(word.intern()));
        }
        alphaBetaSent.add(new AlphaBetaItem("***EOS_WORD***"));
        return alphaBetaSent;
    }

        public static Collection<List<AlphaBetaItem>> convertString2AlphaBeta(Collection<List<String>> stringCorpus) {
        Collection<List<AlphaBetaItem>> alphaBetaCorpus = new ArrayList<List<AlphaBetaItem>>();
        for (List<String> sentence : stringCorpus) {
            List<AlphaBetaItem> alphaBetaSentence = new ArrayList<AlphaBetaItem>();
            for (String word : sentence) {
                alphaBetaSentence.add(new AlphaBetaItem(word));
            }
            alphaBetaCorpus.add(alphaBetaSentence);
        }
        return alphaBetaCorpus;
    }
}


