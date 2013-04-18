/**
 * 
 */
package edu.umd.clip.jobs;

import java.util.concurrent.locks.*;
import java.util.logging.Logger;

/**
 * @author Denis Filimonov <den@cs.umd.edu>
 *
 */
public class Worker extends Thread {

    private static Logger logger =
            Logger.getLogger(Worker.class.getName());
    private JobManager manager;
    private boolean busy;
    private Job currentJob;
    private ReentrantLock lock;
    private Condition newJob;
    private String name;
    private Object reserved;

    public void setReserved(Object reserved) {
        this.reserved = reserved;
    }
    
    public Object getReserved() {
        return reserved;
    }
    

    /**
     * @param manager
     */
    protected Worker(JobManager manager, String name) {
        super(manager.getWorkersThreadGroup(), name);
        this.name = name;
        this.manager = manager;
        this.busy = false;
        this.lock = new ReentrantLock();
        newJob = lock.newCondition();
    }
    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */

    @Override
    public void run() {
        while (!isInterrupted()) {
            lock.lock();
            try {
                while (!busy) {
                    newJob.await();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
//            logger.info(name + " is running job "+currentJob.getName());
            currentJob.run();
//            logger.info(name + " finished job "+currentJob.getName());
            busy = false;
            manager.finished(this);
        }
    }

    protected void postJob(Job job) {
        lock.lock();
        assert (busy == false);
        currentJob = job;
        busy = true;
        newJob.signal();
        lock.unlock();
    }

    /**
     * @return the busy
     */
    protected boolean isBusy() {
        return busy;
    }

    /**
     * @return the currentJob
     */
    protected Job getCurrentJob() {
        return currentJob;
    }
}
