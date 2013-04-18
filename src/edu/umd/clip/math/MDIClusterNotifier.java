/**
 *
 */
package edu.umd.clip.math;

import java.util.*;

/**
 * @author Denis Filimonov <den@cs.umd.edu>
 *
 */
public interface MDIClusterNotifier<T> {

    /**
     * @param oldCluster
     * @param cluster1
     * @param cluster2
     * @return returns true if the algorithm should proceed splitting cluster1 and cluster2
     */
    boolean notify(Collection<T> oldCluster, Collection<T> cluster1, Collection<T> cluster2);
}
