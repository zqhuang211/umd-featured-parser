/**
 *
 */
package edu.umd.clip.math;

/**
 * @author Denis Filimonov <den@cs.umd.edu>
 *
 */
public final class ProbMath {

	public final static double LOG2 = Math.log(2.0);
	public final static double INV_LOG2 = 1.0 / LOG2;

	public final static double log2(double x) {
		return INV_LOG2 * Math.log(x);
	}

	public final static boolean approxEqual(double d1, double d2) {
		return approxEqual(d1, d2, 0.001);
	}

	public final static boolean approxEqual(double d1, double d2, double precision) {
		return Math.abs(d1 - d2) < (Math.abs(d1) + Math.abs(d2)) * 0.5 * precision;
	}
}
