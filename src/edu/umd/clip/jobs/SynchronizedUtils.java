/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.clip.jobs;

import java.util.Collection;

/**
 *
 * @author zqhuang
 */
public class SynchronizedUtils {
    public static synchronized void addToCollection(Collection collection, Object item) {
        collection.add(item);
    }
}
