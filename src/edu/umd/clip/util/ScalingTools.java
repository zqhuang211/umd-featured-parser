/**
 *
 */
package edu.umd.clip.util;

import java.util.Arrays;
import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.math.SloppyMath;

/**
 * @author petrov
 *
 */
public class ScalingTools {

    // SCALING
    public static final int LOGSCALE = 100;
    public static final double SCALE = Math.exp(LOGSCALE);
    // Note: e^709 is the largest double java can handle.
    
    public static double calcScaleFactor(double logScale) {
        return calcScaleFactor(logScale, SCALE);
    }
    
    public static int getLogScale(int logScale) {
        return LOGSCALE * logScale;
    }
    
    public static double calcScaleFactor2(double logScale) {
        return logScale * 100;
    }
    
    public static double calcScaleFactor(double logScale, double scale) {
        if (logScale == Integer.MIN_VALUE) {
            return 0.0; //System.out.println("give me a break!");
        }
        if (logScale == 0.0) {
            return 1.0;
        }
        if (logScale == 1.0) {
            return scale;
        }
        if (logScale == 2.0) {
            return scale * scale;
        }
        if (logScale == 3.0) {
            return scale * scale * scale;
        }
        if (logScale == -1.0) {
            return 1.0 / scale;
        }
        if (logScale == -2.0) {
            return 1.0 / scale / scale;
        }
        if (logScale == -3.0) {
            return 1.0 / scale / scale / scale;
        }
        return Math.pow(scale, logScale);
    }
    
    public static double scaleToScale(double score, int oriScale, int newScale) {
        return score * calcScaleFactor(oriScale - newScale);
    }
    
    public static int scaleArray(double[] scores, int previousScale) {
        double[] oldScores = new double[scores.length];
        System.arraycopy(scores, 0, oldScores, 0, scores.length);
        
        if (previousScale == Integer.MIN_VALUE) {
            return previousScale; //System.out.println("give me a break!");
        }
//  	if (true) return previousScale;
        int logScale = 0;
        double scale = 1.0;
        double max = ArrayMath.max(scores);
        if (max == Double.POSITIVE_INFINITY) {
//	  	System.out.println("Infinity");
            return 0;
        }
        if (max == 0) {
            System.out.println("Scaling zeros is problematic...");
            return previousScale;
        }
        while (max > SCALE) {
            max /= SCALE;
            scale *= SCALE;
            logScale += 1;
        }
        while (max > 0.0 && max < 1.0 / SCALE) {
            max *= SCALE;
            scale /= SCALE;
            logScale -= 1;
        }
        if (logScale != 0) {
            for (int i = 0; i < scores.length; i++) {
                scores[i] /= scale;
            }
        }
//	  if (SloppyMath.isDangerous(ArrayMath.max(scores))){
//	  	System.out.println("Undeflow when scaling scores!");
//	  	}
        return previousScale + logScale;
    }
    
    public static void scaleArrayToScale(double[] scores, int previousScale, int newScale) {
        int scaleDiff = previousScale - newScale;
        if (scaleDiff == 0) {
            return; // nothing to do
        }
        double max = ArrayMath.max(scores);
        if (SloppyMath.isDangerous(max)) {
            return;
        }
        double scale = calcScaleFactor(scaleDiff);
        
        if (Math.abs(scaleDiff) >= 8 * LOGSCALE) {
            // under-/overflow...
            Arrays.fill(scores, 0.0);
            return;
        }
        
        for (int i = 0; i < scores.length; i++) {
            scores[i] *= scale;
        }
//	  if (SloppyMath.isDangerous(ArrayMath.max(scores))){
//	  	System.out.println("Undeflow when scaling scores!");
//	  	}
    }
}
