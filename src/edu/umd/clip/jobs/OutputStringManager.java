/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.jobs;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 *
 * @author zqhuang
 */
public class OutputStringManager {

    private PriorityQueue<OutputJob> outputQueue;
    StringBuilder sb;
    int nextId = 0;
    boolean first = true;

    @Override
    public String toString() {
        return sb.toString();
    }

    static class JobComparator implements Comparator<OutputJob> {

        public int compare(OutputJob job1, OutputJob job2) {
            return (int) Math.signum(job1.id - job2.id);
        }
    }

    public OutputStringManager() {
        outputQueue = new PriorityQueue<OutputJob>(100, new JobComparator());
        sb = new StringBuilder();
        first = true;
    }

    public synchronized void finishedJob(int sentId, String str) {
        if (sentId == nextId) {
            if (first) {
                sb.append(str);
                first = false;
            } else {
                sb.append("\n" + str);
            }
            nextId++;
            OutputJob job = outputQueue.peek();
            while (job != null && job.id == nextId) {
                if (first) {
                    sb.append(job.str);
                    first = false;
                } else {
                    sb.append("\n" + job.str);
                }
                outputQueue.poll();
                job = outputQueue.peek();
                nextId++;
            }
        } else {
            outputQueue.add(new OutputJob(sentId, str));
        }
    }
}
