/**
 * 
 */
package edu.umd.clip.jobs;

import java.util.concurrent.locks.*;
import java.util.*;

/**
 * @author Denis Filimonov <den@cs.umd.edu>
 *
 */
public class JobGroup {

    private String name;
    private ReentrantLock lock;
    private Condition done;
    private HashSet<Job> jobs;

    /**
     * @param name
     */
    protected JobGroup(String name) {
        this.name = name;
        lock = new ReentrantLock();
        done = lock.newCondition();
        jobs = new HashSet<Job>();
    }

    protected void addJob(Job job) {
        lock.lock();
        jobs.add(job);
        //System.out.println("GRP "+name+": Added job '"+job.getName()+"'");
        lock.unlock();
    }

    protected void jobFinished(Job job) {
        lock.lock();
        jobs.remove(job);
        //System.out.println("GRP "+name+": Removed job '"+job.getName()+"'");
        if (jobs.isEmpty()) {
            done.signalAll();
        }
        lock.unlock();
    }

    public void join() {
        lock.lock();
        try {
            while (!jobs.isEmpty()) {
                done.await();
            }
        //System.out.println("Group '"+name+"' is empty.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
