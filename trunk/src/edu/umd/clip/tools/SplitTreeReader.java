/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.tools;

import edu.umd.clip.ling.Tree;
import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author zqhuang
 */
public class SplitTreeReader {

    public static class Options {

        @Option(name = "-input", required = true, usage = "the serialized split tree list")
        public String input = null;
    }

    public static void main(String[] args) {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);

        Map<String, Tree<Integer>> splitTreeMap = SplitTreeReader.read(opts.input);
        for (Entry<String, Tree<Integer>> entry : splitTreeMap.entrySet()) {
            System.out.println(entry.getKey() + " --> "+entry.getValue());
        }
    }

    public static Map<String, Tree<Integer>> read(String fileName) {
        Map<String, Tree<Integer>> splitTreeMap = null;
        try {
            FileInputStream fis = new FileInputStream(fileName); // load from file
            GZIPInputStream gzis = new GZIPInputStream(fis); // compressed
            ObjectInputStream in = new ObjectInputStream(gzis); // load objects

            splitTreeMap = (Map<String, Tree<Integer>>) in.readObject(); // read the grammars

            in.close(); // and close the stream.

        } catch (IOException e) {
            System.out.println("IOException\n" + e);
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found!");
            return null;
        }
        return splitTreeMap;
    }
}
