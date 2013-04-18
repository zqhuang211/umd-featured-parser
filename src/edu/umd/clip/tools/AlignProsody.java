/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.tools;

import edu.umd.clip.util.Option;
import edu.umd.clip.util.OptionParser;
import edu.umd.clip.align.TextMatcher;
import edu.umd.clip.align.Token;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author zqhuang
 */
public class AlignProsody {

    private static final long serialVersionUID = 1L;

    public static class Options {

        private static final long serialVersionUID = 1L;
        @Option(name = "-src", required = false, usage = "The src words to align")
        public String src = null;
        @Option(name = "-trg1", required = true, usage = "The target words to align")
        public String trg1 = null;
        @Option(name = "-trg2", required = true, usage = "The target words to align")
        public String trg2 = null;
        @Option(name = "-align", required = true, usage = "The alignment")
        public String align = null;
        @Option(name = "-srclab", required = true, usage = "The src lab mapped from the target")
        public String srclab = null;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        OptionParser optParser = new OptionParser(Options.class);
        Options opts = (Options) optParser.parse(args, true);
        System.err.println("Calling with " + optParser.getPassedInOptions());

        BufferedReader srcReader = new BufferedReader(new InputStreamReader(new FileInputStream(opts.src)));
        BufferedReader trg1Reader = null;
        BufferedReader trg2Reader = null;
        try {
            trg1Reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(opts.trg1)));
        } catch (FileNotFoundException ex) {
            System.err.println("File not found: " + opts.trg1);
        }
        try {
            trg2Reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(opts.trg2)));
        } catch (FileNotFoundException ex) {
            System.err.println("File not found: " + opts.trg1);
        }

        List<Token> srcList = new ArrayList<Token>();
        List<Token> trg1List = new ArrayList<Token>();
        List<Token> trg2List = new ArrayList<Token>();
        String result1 = null;
        String result2 = null;
        double wer1 = Double.POSITIVE_INFINITY;
        double wer2 = Double.POSITIVE_INFINITY;

//      read words in the parse trees
        String line = "";
        while ((line = srcReader.readLine()) != null) {
            srcList.add(new Token(line.trim()));
        }

        List<Token> srcModList = new ArrayList<Token>();
        for (Token token : srcList) {
            Token modToken = new Token(mapString(token.getWord()));
            token.setAlignedTo(modToken);
            srcModList.add(modToken);
        }

        Pattern pattern2 = Pattern.compile("^(.*)(\'.+)$");
        Pattern pattern1 = Pattern.compile("^(.*)(n\'t)$");
        if (trg1Reader != null) {
            while ((line = trg1Reader.readLine()) != null) {
                String[] words = line.trim().split("\\s+");
                if (words[1].endsWith(".")) {
                    words[1] = words[1].substring(0, words[1].length() - 1);
                }
                Matcher matcher1 = pattern1.matcher(words[1]);

                if (matcher1.matches()) {
                    trg1List.add(new Token(matcher1.group(1), "1_1:1_1:4_0:p_0"));
                    trg1List.add(new Token(matcher1.group(2), words[4]));
                } else {
                    Matcher matcher2 = pattern2.matcher(words[1]);
                    if (matcher2.matches()) {
                        trg1List.add(new Token(matcher2.group(1), "1_1:1_1:4_0:p_0"));
                        trg1List.add(new Token(matcher2.group(2), words[4]));
                    } else {
                        trg1List.add(new Token(mapString(words[1]), words[4]));
                    }
                }
            }
            result1 = TextMatcher.matchToken(srcModList, trg1List);
            TextMatcher.Result tmp = new TextMatcher.Result(result1);
            wer1 = tmp.wer();
            System.out.println(wer1 + "   alignment error with " + opts.trg1);
            if (wer1 < 0.5) {
                TextMatcher.alignTokenList(srcModList, trg1List, result1);
                PrintWriter alignWriter = new PrintWriter(opts.align);
                alignWriter.println(TextMatcher.getPrettyVerticalTokenPrint(srcModList, trg1List, result1));
                alignWriter.flush();

                PrintWriter outWriter = new PrintWriter(opts.srclab);
                for (Token token : srcList) {
                    Token modToken = token.getAlignedTo();
                    Token alignedToken = modToken.getAlignedTo();
                    if (alignedToken == null) {
                        outWriter.println(token.getWord());
                    } else {
                        outWriter.println(token.getWord() + "\t" + alignedToken.getLabel());
                    }
                }
                outWriter.flush();
                System.exit(0);
            }
        }

        if (trg2Reader != null) {
            while ((line = trg2Reader.readLine()) != null) {
                String[] words = line.trim().split("\\s+");
                if (words[1].endsWith(".")) {
                    words[1] = words[1].substring(0, words[1].length() - 1);
                }
                Matcher matcher1 = pattern1.matcher(words[1]);
                if (matcher1.matches()) {
                    trg2List.add(new Token(matcher1.group(1), "1_1:1_1:4_0:p_0"));
                    trg2List.add(new Token(matcher1.group(2), words[4]));
                } else {
                    Matcher matcher2 = pattern2.matcher(words[1]);
                    if (matcher2.matches()) {
                        trg2List.add(new Token(matcher2.group(1), "1_1:1_1:4_0:p_0"));
                        trg2List.add(new Token(matcher2.group(2), words[4]));
                    } else {
                        trg2List.add(new Token(mapString(words[1]), words[4]));
                    }
                }
            }
            result2 = TextMatcher.matchToken(srcModList, trg2List);
            TextMatcher.Result tmp = new TextMatcher.Result(result2);
            wer2 = tmp.wer();
            System.out.println(wer2 + "   alignment error with " + opts.trg2);
            if (wer2 < 0.5) {
                TextMatcher.alignTokenList(srcModList, trg2List, result2);
                PrintWriter alignWriter = new PrintWriter(opts.align);
                alignWriter.println(TextMatcher.getPrettyVerticalTokenPrint(srcModList, trg2List, result2));
                alignWriter.flush();

                PrintWriter outWriter = new PrintWriter(opts.srclab);
                for (Token token : srcList) {
                    Token modToken = token.getAlignedTo();
                    Token alignedToken = modToken.getAlignedTo();
                    if (alignedToken == null) {
                        outWriter.println(token.getWord());
                    } else {
                        outWriter.println(token.getWord() + "\t" + alignedToken.getLabel());
                    }
                }
                outWriter.flush();
                System.exit(0);
            }
        }
        throw new RuntimeException("I cannot find an alignment with less than 50% alignment error");
    }

    public static String mapString(String input) {
        String output = input;
        if (input.equals("er")) {
            output = "uh";
        } else if (input.equals("eh")) {
            output = "uh";
        } else if (input.equals("mhm")) {
            output = "uhhuh";
        } else if (input.equals("hm")) {
            output = "huh";
        } else if (input.equals("um")) {
            output = "uh";
        } else if (input.equals("uh-huh")) {
            output = "uhhuh";
        }
        return output;
    }
}
