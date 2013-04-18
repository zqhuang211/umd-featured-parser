/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.clip.parser;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 *
 * @author zqhuang
 */
public class ParseManager {

    private PriorityQueue<ParseJob> parseQueue;
    PrintWriter outputWriter;
    int nextSentId = 0;

    static class JobComparator implements Comparator<ParseJob> {

        public int compare(ParseJob job1, ParseJob job2) {
            return (int) Math.signum(job1.sentId - job2.sentId);
        }
    }

    public ParseManager(String outputFile) throws FileNotFoundException {
        parseQueue = new PriorityQueue<ParseJob>(100, new JobComparator());
        OutputStreamWriter outStream = outputFile != null ? new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8")) : new OutputStreamWriter(System.out, Charset.forName("UTF-8"));
        outputWriter = new PrintWriter(outStream);
    }

    public synchronized void finishedJob(int sentId, String parseTree) {
        if (sentId == nextSentId) {
            outputWriter.println(parseTree);
            nextSentId++;
            ParseJob job = parseQueue.peek();
            while (job != null && job.sentId == nextSentId) {
                outputWriter.println(job.parseTree);
                parseQueue.poll();
                job = parseQueue.peek();
                nextSentId++;
            }
            outputWriter.flush();
        } else {
            parseQueue.add(new ParseJob(sentId, parseTree));
        }
    }
}
