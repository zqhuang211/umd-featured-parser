/*
 * TagBankReader.java
 *
 * Created on May 15, 2007, 12:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.umd.clip.latagger;

import edu.umd.clip.parser.Converter;
import edu.umd.clip.util.Numberer;
import edu.umd.clip.util.Pair;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author zqhuang
 */
public class TagBankReader implements Iterator<WordTagSequence>{
    private static final long serialVersionUID = 1L;
    BufferedReader in;
    WordTagSequence nextSequence;
    
    /** Creates a new instance of TagBankReader */
    public TagBankReader(Reader in) {
        this.in = new BufferedReader(in);
        nextSequence = readSequence();
    }

    public boolean hasNext() {
        return (nextSequence != null);
    }

    public WordTagSequence next() {
        if (!hasNext()) throw new NoSuchElementException();
        WordTagSequence sequence;
        sequence = nextSequence;
        nextSequence = readSequence();
        return sequence;
    }

    public void remove() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }
    
    public WordTagSequence readSequence() {
        WordTagSequence sequence = new WordTagSequence();
        String line;
        try {
            line = in.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Error reading tagbank.");
        }
        if (line == null)
            return null;
        if (line.startsWith("***WHT***:")) {
            double weight = Double.valueOf(line.substring(10, line.indexOf(' ')));
            sequence.setWeight(weight);
            line = line.substring(line.indexOf(' ')+1);
        }
        List<String> items = Arrays.asList(line.split("\\s+"));
        for (String item : items) {
            Pair<String, String> wordTagPair = Converter.splitWordTagItem(item);
            String word = wordTagPair.getFirst();
            String tag = wordTagPair.getSecond();
            Integer tagState = Numberer.number("tags", tag);
            sequence.add(word, tagState);
        }
        return sequence;
    }
    
    public static void main(String[] argv) {
       Pattern itemSplitPattern = Pattern.compile("^(\\S+)/([^/\\s]+)$");
       String expression = "hello/NN";
       Matcher m = itemSplitPattern.matcher(expression);
       if (m.matches())
           System.out.println("matches.");
       else
           System.out.println("doesnot match.");   
    }
    
}
