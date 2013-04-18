package edu.umd.clip.util;

import java.util.Arrays;

public class ArrayUtil {

    public static int[] clone(int[] a) {
        if (a == null) {
            return null;
        } else {
            return a.clone();
        }
    }

    public static double[] clone(double[] a) {
        if (a == null) {
            return null;
        } else {
            return a.clone();
        }
    }

    public static int[][] clone(int[][] a) {
        if (a == null) {
            return null;
        }
        int[][] res = new int[a.length][];
        for (int i = 0; i < a.length; i++) {
            res[i] = clone(a[i]);
        }
        return res;
    }

    public static double[][] clone(double[][] a) {
        if (a == null) {
            return null;
        }
        double[][] res = new double[a.length][];
        for (int i = 0; i < a.length; i++) {
            res[i] = clone(a[i]);
        }
        return res;
    }

    public static double[][][] clone(double[][][] a) {
        if (a == null) {
            return null;
        }
        double[][][] res = new double[a.length][][];
        for (int i = 0; i < a.length; i++) {
            res[i] = clone(a[i]);
        }
        return res;
    }

    public static double[][][][] clone(double[][][][] a) {
        if (a == null) {
            return null;
        }
        double[][][][] res = new double[a.length][][][];
        for (int i = 0; i < a.length; i++) {
            res[i] = clone(a[i]);
        }
        return res;
    }

    public static void fill(float[][] a, float val) {
        for (int i = 0; i < a.length; i++) {
            Arrays.fill(a[i], val);
        }
    }

    public static void fill(float[][][] a, float val) {
        for (int i = 0; i < a.length; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(int[][] a, int val) {
        for (int i = 0; i < a.length; i++) {
            Arrays.fill(a[i], val);
        }
    }

    public static void fill(int[][][] a, int val) {
        for (int i = 0; i < a.length; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(double[][] a, double val) {
        for (int i = 0; i < a.length; i++) {
            Arrays.fill(a[i], val);
        }
    }

    public static void fill(double[][][] a, double val) {
        for (int i = 0; i < a.length; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(double[][][] a, int until, double val) {
        for (int i = 0; i < until; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(int[][][] a, int until, int val) {
        for (int i = 0; i < until; i++) {
            fill(a[i], val);
        }
    }

    public static String toString(float[][] a) {
        String s = "[";
        for (int i = 0; i < a.length; i++) {
            s = s.concat(Arrays.toString(a[i]) + ", ");
        }
        return s + "]";
    }

    public static String toString(float[][][] a) {
        String s = "[";
        for (int i = 0; i < a.length; i++) {
            s = s.concat(toString(a[i]) + ", ");
        }
        return s + "]";
    }

    public static String toString(double[][] a) {
        String s = "[";
        for (int i = 0; i < a.length; i++) {
            s = s.concat(Arrays.toString(a[i]) + ", ");
        }
        return s + "]";
    }

    public static String toString(double[][][] a) {
        String s = "[";
        for (int i = 0; i < a.length; i++) {
            s = s.concat(toString(a[i]) + ", ");
        }
        return s + "]";
    }

    public static String toString(boolean[][] a) {
        String s = "[";
        for (int i = 0; i < a.length; i++) {
            s = s.concat(Arrays.toString(a[i]) + ", ");
        }
        return s + "]";
    }

    public static void multiplyInPlace(double[][][] array, double d) {
        for (int i = 0; i < array.length; i++) {
            multiplyInPlace(array[i], d);
        }
    }

    public static void multiplyInPlace(double[][] array, double d) {
        for (int i = 0; i < array.length; i++) {
            multiplyInPlace(array[i], d);
        }
    }

    public static void multiplyInPlace(double[] array, double d) {
        if (array == null) {
            return;
        }
        for (int i = 0; i < array.length; i++) {
            array[i] *= d;
        }
    }
}
