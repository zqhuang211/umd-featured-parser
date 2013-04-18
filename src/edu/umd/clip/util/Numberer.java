package edu.umd.clip.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Gives unique integer serial numbers to a family of objects, identified
 * by a name space.  A Numberer is like a collection of {@link Index}es,
 * and for
 * many purposes it is more straightforward to use an Index, but
 * Numberer can be useful precisely because it maintains a global name
 * space for numbered object families, and provides facilities for mapping
 * across numberings within that space.  At any rate, it's widely used in
 * some existing packages.
 *
 * @author Dan Klein
 */
public class Numberer implements Serializable {

    private static Map numbererMap = new HashMap();

    public static Map getNumberers() {
        return numbererMap;
    }

    /** You need to call this after deserializing Numberer objects to
     *  restore the global namespace, since static objects aren't serialized.
     */
    public static void setNumberers(Map numbs) {
        numbererMap = numbs;
    }

    public static boolean save(String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName); // Save to file
//            GZIPOutputStream gzos = new GZIPOutputStream(fos); // Compressed
            ObjectOutputStream out = new ObjectOutputStream(fos); // Save objects
            out.writeObject(numbererMap); // Write the mix of grammars
            out.flush(); // Always flush the output.
            out.close(); // And close the stream.
        } catch (IOException e) {
            System.out.println("IOException: " + e);
            return false;
        }
        return true;
    }

    public static Numberer getGlobalNumberer(String type) {
        Numberer n = (Numberer) numbererMap.get(type);
        if (n == null) {
            n = new Numberer();
            numbererMap.put(type, n);
        }
        return n;
    }

    /** Get a number for an object in namespace type.
     *  This looks up the Numberer for <code>type</code> in the global
     *  namespace map (creating it if none previously existed), and then
     *  returns the appropriate number for the key.
     */
    public static int number(String type, Object o) {
        return getGlobalNumberer(type).number(o);
    }

    public static Object object(String type, int n) {
        return getGlobalNumberer(type).object(n);
    }

    /**
     * For an Object <i>o</i> that occurs in Numberers of type
     * <i>sourceType</i> and <i>targetType</i>, translates the serial
     * number <i>n</i> of <i>o</i> in the <i>sourceType</i> Numberer to
     * the serial number in the <i>targetType</i> Numberer.
     */
    public static int translate(String sourceType, String targetType, int n) {
        return getGlobalNumberer(targetType).number(getGlobalNumberer(sourceType).object(n));
    }
    private int total;
    private Map intToObject;
    private Map objectToInt;
    private boolean locked = false;

    public static void lockAll() {
        for (Object type : numbererMap.keySet()) {
            getGlobalNumberer((String) type).lock();
        }
    }

    public int total() {
        return total;
    }

    public void lock() {
        locked = true;
    }

    public boolean hasSeen(Object o) {
        return objectToInt.keySet().contains(o);
    }

    public Set objects() {
        return objectToInt.keySet();
    }

    public Set<Integer> numbers() {
        return intToObject.keySet();
    }

    public int number(Object o) {
        Integer i = (Integer) objectToInt.get(o);
        if (i == null) {
            if (locked) {
                throw new NoSuchElementException("no object: " + o);
            }
            i = total;
            objectToInt.put(o, i);
            intToObject.put(i, o);
            total++;
        }
        return i.intValue();
    }

    public Object object(int n) {
        return intToObject.get(n);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < total; i++) {
            sb.append(i);
            sb.append("->");
            sb.append(object(i));
            if (i < total - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public Numberer() {
        total = 0;
        intToObject = new HashMap();
        objectToInt = new HashMap();
    }
    private static final long serialVersionUID = 1L;
}
