package edu.umd.clip.math;

import edu.umd.clip.util.MutableDouble;
import java.util.*;
import edu.umd.clip.jobs.*;

public class MDI<S, T> {

    private HashMap<S, HashMap<T, MutableDouble>> leftProbs;
    private HashMap<S, HashMap<T, MutableDouble>> rightProbs;
    private Collection<S> totalVocab;
    private Collection<T> otherVocab;
    private MDIClusterNotifier<S> notifier;
    private static final int MAX_ITERATIONS = 15;
    private static final double SORTA_INFINITY = 10;
    private JobGroup jobGroup;

    public MDI(Collection<S> vocab, Collection<T> vocab2) {
        leftProbs = new HashMap<S, HashMap<T, MutableDouble>>();
        rightProbs = new HashMap<S, HashMap<T, MutableDouble>>();
        totalVocab = vocab;
        otherVocab = vocab2;
    }

    public void setLeftProb(S word, T left, double prob) {
        setProb(leftProbs, word, left, prob);
    }

    public void setRightProb(S word, T right, double prob) {
        setProb(rightProbs, word, right, prob);
    }

    private void setProb(HashMap<S, HashMap<T, MutableDouble>> hash, S word, T other, double prob) {
        if (!totalVocab.contains(word)) {
            System.out.printf("unknown word %s\n", word);
        }
//        if (!otherVocab.contains(other)) {
//            System.out.printf("unknown other word %s\n", other);
//        }
        HashMap<T, MutableDouble> p = hash.get(word);
        if (p == null) {
            p = new HashMap<T, MutableDouble>();
            p.put(other, new MutableDouble(prob));
            hash.put(word, p);
        } else {
            MutableDouble d = p.get(other);
            if (d == null) {
                p.put(other, new MutableDouble(prob));
            } else {
                d.set(prob);
            }
        }
    }

    private static class Dimension<T> {

        public boolean right;
        public T word;

        /**
         * @param right
         * @param word
         */
        public Dimension(boolean right, T word) {
            super();
            this.right = right;
            this.word = word;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Dimension) {
                Dimension<T> d = (Dimension<T>) other;
                return d.right == right && d.word.equals(word);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int code = word.hashCode();
            if (right) {
                code ^= 1;
            }
            return code;
        }
    }

    private double getProb(HashMap<S, HashMap<T, MutableDouble>> hash, S word, T other) {
        try {
            return hash.get(word).get(other).doubleValue();
        } catch (NullPointerException e) {
            return 0.0;
        }
    }

    private double getLeftProb(S word, T left) {
        return getProb(leftProbs, word, left);
    }

    private double getRightProb(S word, T right) {
        return getProb(rightProbs, word, right);
    }

    public void normalizeDistributions() {
        for (S word : totalVocab) {
            if (leftProbs.get(word) == null) {
                leftProbs.put(word, new HashMap<T, MutableDouble>(otherVocab.size()));
            }
            if (rightProbs.get(word) == null) {
                rightProbs.put(word, new HashMap<T, MutableDouble>(otherVocab.size()));
            }
        }
        for (HashMap<T, MutableDouble> entry : leftProbs.values()) {
            double count = 0.0;
            for (MutableDouble d : entry.values()) {
                count += d.doubleValue();
            }
            if (count == 0) {
                MutableDouble uniform = new MutableDouble(1.0 / otherVocab.size());
                for (T word : otherVocab) {
                    entry.put(word, uniform);
                }
            } else {
                for (MutableDouble d : entry.values()) {
                    d.set(d.doubleValue() / count);
                }
            }
        }

        for (HashMap<T, MutableDouble> entry : rightProbs.values()) {
            double count = 0.0;
            for (MutableDouble d : entry.values()) {
                count += d.doubleValue();
            }
            if (count == 0) {
                MutableDouble uniform = new MutableDouble(1.0 / otherVocab.size());
                for (T word : otherVocab) {
                    entry.put(word, uniform);
                }
            } else {
                for (MutableDouble d : entry.values()) {
                    d.set(d.doubleValue() / count);
                }
            }
        }

    }

    private HashMap<Dimension<T>, MutableDouble> getCenter(Collection<S> vocab) {
        /*
        for(S word : vocab) {
        if (!checkWord(word)) {
        System.out.printf("checkWord('%s') failed\n", word);
        }
        }
         */
        HashMap<Dimension<T>, MutableDouble> center = new HashMap<Dimension<T>, MutableDouble>(vocab.size() * 2);
        for (T other : otherVocab) {
            double lProb = 0.0;
            double rProb = 0.0;
            for (S word : vocab) {
                lProb += getLeftProb(word, other);
                rProb += getRightProb(word, other);
            }
            lProb /= vocab.size();
            rProb /= vocab.size();
            center.put(new Dimension<T>(false, other), new MutableDouble(lProb));
            center.put(new Dimension<T>(true, other), new MutableDouble(rProb));
        }
        checkPoint(center);
        return center;
    }

    private double distance(S word1, S word2) {
        double d = 0.0;
        // KL-divergence
        for (T word : otherVocab) {
            double leftProb1 = getLeftProb(word1, word);
            double rightProb1 = getRightProb(word1, word);
            double leftProb2 = getLeftProb(word2, word);
            double rightProb2 = getRightProb(word2, word);

            if (leftProb1 > 0.0) {
                if (leftProb2 > 0) {
                    d += leftProb1 * ProbMath.log2(leftProb1 / leftProb2);
                } else {
                    d += leftProb1 * SORTA_INFINITY;
                }
            }

            if (rightProb1 > 0.0) {
                if (rightProb2 > 0) {
                    d += rightProb1 * ProbMath.log2(rightProb1 / rightProb2);
                } else {
                    d += rightProb1 * SORTA_INFINITY;
                }
            }
        }
        return d;
    }

    private double distanceToCenter(S word, HashMap<Dimension<T>, MutableDouble> center) {
        double d = 0.0;
        for (Map.Entry<Dimension<T>, MutableDouble> point : center.entrySet()) {
            double prob = (point.getKey().right)
                    ? getRightProb(word, point.getKey().word)
                    : getLeftProb(word, point.getKey().word);
            double pointProb = point.getValue().doubleValue();
            if (prob > 0.0) {
                if (pointProb > 0) {
                    d += prob * ProbMath.log2(prob / pointProb);
                } else {
                    d += prob * SORTA_INFINITY;
                }
            }
        }
        checkPoint(center);
        if (d < -0.1) {
            System.out.printf("negative distance (%s): %g\n", word, d);
            assert (checkWord(word));
            assert (checkPoint(center));
        }
        return d > 0 ? d : 0;
    }

    private boolean checkWord(S word) {
        double prob = 0;
        for (MutableDouble p : leftProbs.get(word).values()) {
            if (p.doubleValue() < 0) {
                return false;
            }
            prob += p.doubleValue();
        }
        if (Math.abs(prob - 1) > 0.01) {
            System.out.printf("checkWord(%s) : leftProb = %g\n", word, prob);
            return false;
        }
        prob = 0;
        for (MutableDouble p : rightProbs.get(word).values()) {
            if (p.doubleValue() < 0) {
                return false;
            }
            prob += p.doubleValue();
        }
        if (Math.abs(prob - 1) > 0.01) {
            System.out.printf("checkWord(%s) : rightProb = %g\n", word, prob);
            return false;
        }
        return true;
    }

    private boolean checkPoint(HashMap<Dimension<T>, MutableDouble> point) {
        double leftProb = 0;
        double rightProb = 0;

        for (Map.Entry<Dimension<T>, MutableDouble> entry : point.entrySet()) {
            if (entry.getValue().doubleValue() < 0) {
                return false;
            }
            if (entry.getKey().right) {
                rightProb += entry.getValue().doubleValue();
            } else {
                leftProb += entry.getValue().doubleValue();
            }
        }
        boolean result = Math.abs(rightProb - 1) < 0.01 && Math.abs(leftProb - 1) < 0.01;
        if (!result) {
            System.out.printf("rightProb=%g, leftProb=%g\n", rightProb, leftProb);
        }
        return result;
    }

    private HashMap<Dimension<T>, MutableDouble> wordToVector(S word) {
        HashMap<Dimension<T>, MutableDouble> vector = new HashMap<Dimension<T>, MutableDouble>(otherVocab.size());
        for (T w : otherVocab) {
            vector.put(new Dimension<T>(false, w), new MutableDouble(getLeftProb(word, w)));
            vector.put(new Dimension<T>(true, w), new MutableDouble(getRightProb(word, w)));
        }
        return vector;
    }

    private void partition(final Collection<S> vocab, final HashMap<Dimension<T>, MutableDouble> center) {
        Runnable runnable = new Runnable() {

            public void run() {
                partitionImpl(vocab, center);
            }
        };
        Job job = new Job(runnable, "MDI");
        JobManager manager = JobManager.getInstance();
        manager.addJob(jobGroup, job);
    }

    private void partitionImpl(Collection<S> vocab, HashMap<Dimension<T>, MutableDouble> center) {
        // find two points closest to the center
        double firstVal = Double.POSITIVE_INFINITY;
        double secondVal = Double.POSITIVE_INFINITY;
        S firstWord = null;
        S secondWord = null;

        LinkedList<S> cluster1 = new LinkedList<S>();
        LinkedList<S> cluster2 = new LinkedList<S>();
        HashMap<Dimension<T>, MutableDouble> newCenter1 = null;
        HashMap<Dimension<T>, MutableDouble> newCenter2 = null;

        if (vocab.size() > 2) {
            for (S word : vocab) {
                double dist = distanceToCenter(word, center);
                if (dist < secondVal) {
                    if (dist < firstVal) {
                        secondVal = firstVal;
                        secondWord = firstWord;
                        firstVal = dist;
                        firstWord = word;
                    } else {
                        secondVal = dist;
                        secondWord = word;
                    }
                }
            }
            if (distance(firstWord, secondWord) <= 0.0) {
                System.out.printf("zero distance between '%s' and '%s'\n", firstWord, secondWord);
                // pick an arbitrary second word
                for (S word : vocab) {
                    if (word.equals(firstWord) || word.equals(secondWord)) {
                        continue;
                    }
                    if (distance(firstWord, word) > 0.0) {
                        secondWord = word;
                        break;
                    }
                }
                System.out.printf("selected '%s' and '%s'\n", firstWord, secondWord);
            }

            HashMap<Dimension<T>, MutableDouble> center1 = wordToVector(firstWord);
            HashMap<Dimension<T>, MutableDouble> center2 = wordToVector(secondWord);
            for (S word : vocab) {
                double dist1 = distanceToCenter(word, center1);
                double dist2 = distanceToCenter(word, center2);
                if (dist1 < dist2) {
                    cluster1.add(word);
                } else {
                    cluster2.add(word);
                }
            }

            int changed = 1;
            int iteration = 0;
            while (++iteration <= MAX_ITERATIONS && changed > 0) {
                System.out.printf("Iteration %d/%d\n", iteration, MAX_ITERATIONS);
                changed = 0;
                newCenter1 = getCenter(cluster1);
                newCenter2 = getCenter(cluster2);

                LinkedList<S> tmpCluster1 = new LinkedList<S>();
                LinkedList<S> tmpCluster2 = new LinkedList<S>();

                for (ListIterator<S> it = cluster1.listIterator(); it.hasNext();) {
                    S word = it.next();
                    double dist1 = distanceToCenter(word, newCenter1);
                    double dist2 = distanceToCenter(word, newCenter2);
                    if (dist1 <= dist2) {
                        tmpCluster1.add(word);
                    } else {
                        tmpCluster2.add(word);
                        changed++;
                        System.out.printf("changing %s : %g > %g\n", word, dist1, dist2);
                    }
                }

                for (ListIterator<S> it = cluster2.listIterator(); it.hasNext();) {
                    S word = it.next();
                    double dist1 = distanceToCenter(word, newCenter1);
                    double dist2 = distanceToCenter(word, newCenter2);
                    if (dist2 <= dist1) {
                        tmpCluster2.add(word);
                    } else {
                        tmpCluster1.add(word);
                        changed++;
                        System.out.printf("changing %s : %g < %g\n", word, dist1, dist2);
                    }
                }
                cluster1 = tmpCluster1;
                cluster2 = tmpCluster2;
                System.out.printf("changed: %d/%d\n", changed, vocab.size());
                if (cluster1.size() == 0 || cluster2.size() == 0) {
                    LinkedList<S> cl = cluster1.size() == 0 ? cluster2 : cluster1;
                    System.out.printf("Cannot split %s, split them randomly\n", cl.toString());
                    tmpCluster1 = new LinkedList<S>();
                    tmpCluster2 = new LinkedList<S>();
                    boolean b = true;
                    for (S word : cl) {
                        if (b) {
                            tmpCluster1.add(word);
                        } else {
                            tmpCluster2.add(word);
                        }
                        b = !b;
                    }
                    cluster1 = tmpCluster1;
                    cluster2 = tmpCluster2;
                }
            }
        } else if (vocab.size() == 2) {
            Iterator<S> it = vocab.iterator();
            S word1 = it.next();
            S word2 = it.next();
            cluster1.add(word1);
            cluster2.add(word2);
            newCenter1 = wordToVector(word1);
            newCenter2 = wordToVector(word2);
        } else {
            return;
        }

        if (notifyNewCluster(vocab, cluster1, cluster2)) {
            partition(cluster1, newCenter1);
            partition(cluster2, newCenter2);
        }

    }

    private boolean notifyNewCluster(Collection<S> vocab, Collection<S> cluster1, Collection<S> cluster2) {
        if (notifier != null) {
            return notifier.notify(vocab, cluster1, cluster2);
        } else {
            return false;
        }
    }

    /**
     * @param notifier the notifier to set
     */
    public void setNotifier(MDIClusterNotifier<S> notifier) {
        this.notifier = notifier;
    }

    /**
     * @return the totalVocab
     */
    public Collection<S> getVocab() {
        return totalVocab;
    }

    public void partition(Collection<S> vocab) {
        for (S word : vocab) {
            if (!checkWord(word)) {
                System.out.printf("checkWord(%s) failed\n", word);
            }
        }
        jobGroup = JobManager.getInstance().createJobGroup("MDI");
        partition(vocab, getCenter(vocab));
        jobGroup.join();
    }
}
