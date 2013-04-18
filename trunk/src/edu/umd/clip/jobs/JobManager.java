/**
 * 
 */
package edu.umd.clip.jobs;

import java.util.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.*;

/**
 * @author Denis Filimonov <den@cs.umd.edu>
 *
 */
public class JobManager implements Runnable {

    private static final long serialVersionUID = 1L;
    private static JobManager instance;
    private HashSet<Worker> workers;
    private ThreadGroup workersThreadGroup;
    private final ReentrantLock workersLock;
    private PriorityQueue<Job> jobQueue;
    private ConcurrentLinkedQueue<Worker> freeWorkers;
    private Condition hasJobs;
    private Condition hasWorkers;

    public HashSet<Worker> getWorkers() {
        return workers;
    }

    private JobManager(int nrWorkers) {
        workersLock = new ReentrantLock();
        hasJobs = workersLock.newCondition();
        hasWorkers = workersLock.newCondition();

        workersThreadGroup = new ThreadGroup("workers group");
        jobQueue = new PriorityQueue<Job>(nrWorkers * 2, new JobComparator());
        workers = new HashSet<Worker>(nrWorkers);
        freeWorkers = new ConcurrentLinkedQueue<Worker>();
        for (int i = 0; i < nrWorkers; ++i) {
            Worker worker = new Worker(this, "Worker #" + Integer.toString(i));
            worker.setDaemon(true);
            workers.add(worker);
            freeWorkers.add(worker);
        }
    }

    static class JobComparator implements Comparator<Job> {

        public int compare(Job job1, Job job2) {
            return (int) Math.signum(job1.getPriority() - job2.getPriority());
        }
    }

    public static void initialize(int nrWorkers) {
        if (instance == null) {
            instance = new JobManager(nrWorkers);
        }
    }

    protected void finished(Worker worker) {
        Job job = worker.getCurrentJob();
        //System.out.println("Job "+job.getName()+" has finished.");
        job.getGroup().jobFinished(job);
        workersLock.lock();
        freeWorkers.add(worker);
        hasWorkers.signal();
        workersLock.unlock();
    }

    public void addJob(JobGroup group, Job job) {
        workersLock.lock();
        job.setGroup(group);
        group.addJob(job);
        jobQueue.add(job);
        hasJobs.signal();
        workersLock.unlock();
    }

    public void addJob(Job job) {
        if (job.getGroup() != null) {
            addJob(job.getGroup(), job);
            return;
        }
        workersLock.lock();
        if (workers.contains(Thread.currentThread())) {
            // if the job was submitted from another job add it to the same JobGroup
            Job parentJob = ((Worker) Thread.currentThread()).getCurrentJob();
            addJob(parentJob.getGroup(), job);
        } else {
            addJob(createJobGroup("anonymous"), job);
        }
        workersLock.unlock();
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        for (Iterator<Worker> it = workers.iterator(); it.hasNext();) {
            it.next().start();
        }
        while (!Thread.currentThread().isInterrupted()) {
            workersLock.lock();
            try {
                while (jobQueue.isEmpty()) {
                    hasJobs.await();
                }

                while (freeWorkers.isEmpty()) {
                    hasWorkers.await();
                }

                int nrJobsToRun = Math.min(jobQueue.size(), freeWorkers.size());

                if (false) {
                    ArrayList<Job> tmpQueue = new ArrayList<Job>(jobQueue.size());
                    /*
                    System.out.print("JobQueue: ");
                    
                    while(!jobQueue.isEmpty()) {
                    Job job = jobQueue.remove();
                    tmpQueue.add(job);
                    System.out.print(job.toString());
                    if (!jobQueue.isEmpty()) {
                    System.out.print(", ");
                    }
                    }
                    System.out.println();
                     */
                    for (Job job : tmpQueue) {
                        jobQueue.add(job);
                    }
                }

                for (int i = 0; i < nrJobsToRun; ++i) {
                    Job job = jobQueue.remove();
//                    if (jobQueue.size() % 100 == 0) {
//                        System.err.println(jobQueue.size() + " jobs remaining plus a few still running...");
//                        System.err.flush();
//                    }
                    Worker worker = freeWorkers.remove();
                    //System.out.println("Scheduling job "+job.toString());
                    worker.postJob(job);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                workersLock.unlock();
            }
        }
    }

    public JobGroup createJobGroup(String name) {
        return new JobGroup(name);
    }

    /**
     * @return the workersThreadGroup
     */
    protected ThreadGroup getWorkersThreadGroup() {
        return workersThreadGroup;
    }

    /**
     * @return the instance
     */
    public static JobManager getInstance() {
        return instance;
    }
}
