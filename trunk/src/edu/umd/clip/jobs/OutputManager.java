/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.jobs;

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
public class OutputManager {

    private PriorityQueue<OutputJob> outputQueue;
    PrintWriter outputWriter;
    int nextId = 0;

    static class JobComparator implements Comparator<OutputJob> {

        public int compare(OutputJob job1, OutputJob job2) {
            return (int) Math.signum(job1.id - job2.id);
        }
    }

    public OutputManager(String outputFile) throws FileNotFoundException {
        outputQueue = new PriorityQueue<OutputJob>(100, new JobComparator());
        OutputStreamWriter outStream = outputFile != null ? new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8")) : new OutputStreamWriter(System.out, Charset.forName("UTF-8"));
        outputWriter = new PrintWriter(outStream);
    }

    public synchronized void finishedJob(int sentId, String str) {
        if (sentId == nextId) {
            outputWriter.println(str);
            nextId++;
            OutputJob job = outputQueue.peek();
            while (job != null && job.id == nextId) {
                outputWriter.println(job.str);
                outputQueue.poll();
                job = outputQueue.peek();
                nextId++;
            }
            outputWriter.flush();
        } else {
            outputQueue.add(new OutputJob(sentId, str));
        }
    }
}
