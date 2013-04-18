/**
 * 
 */
package edu.umd.clip.jobs;

/**
 * @author Denis Filimonov <den@cs.umd.edu>
 *
 */
public class Job implements Runnable {

    private JobGroup group;
    private String name;
    private Runnable runnable;
    private float priority = 10;

    public Job(Runnable runnable, String name) {
        this(runnable, null, name);
    }

    /**
     * @param runnable
     * @param group
     * @param name
     */
    public Job(Runnable runnable, JobGroup group, String name) {
        this.runnable = runnable;
        this.group = group;
        this.name = name;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        runnable.run();
    }

    /**
     * @return the group
     */
    public JobGroup getGroup() {
        return group;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public String toString() {
        return String.format("'%s'@'%s',pri=%f", name, group.toString(), priority);
    }

    /**
     * @param group the group to set
     */
    protected void setGroup(JobGroup group) {
        this.group = group;
    }

    public float getPriority() {
        return priority;
    }

    public void setPriority(float priority) {
        this.priority = priority;
    }
}
