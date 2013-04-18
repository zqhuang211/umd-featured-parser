/*
 * Corpus.java
 *
 * Created on May 15, 2007, 4:37 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.util.Numberer;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author zqhuang
 */
public class Corpus {

    private Charset charset;
    private Collection<WordTagSequence> stateCorpus;

    public Corpus() {
        this.charset = Charset.forName("UTF-8");
    }

    public Corpus(Charset charset) {
        this.charset = charset;
    }

    public Collection<WordTagSequence> getStateCorpus() {
        return stateCorpus;
    }

    /**
     * Loads the training corpus from the POS-tagged file, which contains one 
     * POS-tagged sentence per line
     */
    public void loadTrainingCorpus(String trainingFile) throws FileNotFoundException {
       
    }
}


