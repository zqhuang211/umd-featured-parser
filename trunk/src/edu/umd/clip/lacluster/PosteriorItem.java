/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.lacluster;

import edu.umd.clip.latagger.LatentTrainer;
import edu.umd.clip.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class PosteriorItem {

    private List<Pair<Integer, Double>> posteriorList;

    public PosteriorItem() {
        posteriorList = new ArrayList<Pair<Integer, Double>>();
    }

    public void addItem(int cluster, double prob) {
        posteriorList.add(new Pair<Integer, Double>(cluster, prob));
    }

    public List<Pair<Integer, Double>> getPosteriorList() {
        return posteriorList;
    }

    class PosteriorComparator implements Comparator<Pair<Integer, Double>> {

        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
            return Double.compare(o2.getSecond(), o1.getSecond());
        }
    }

    public PosteriorItem(double[] alphaScores, double alphaScale,
            double[] betaScores, double betaScale,
            double sentScore, double sentScale, int top) {
        calcPosterior(alphaScores, alphaScale, betaScores, betaScale, sentScore, sentScale, top);
    }

    public void calcPosterior(double[] alphaScores, double alphaScale,
            double[] betaScores, double betaScale,
            double sentScore, double sentScale, int top) {

        List<Pair<Integer, Double>> tmpPosteriorList = new ArrayList<Pair<Integer, Double>>();
        int clusterNum = alphaScores.length;
        double unigramScale = alphaScale + betaScale - sentScale;
        double unigramScaleFactor = Math.pow(LatentTrainer.SCALE, unigramScale);
        for (int ci = 0; ci < clusterNum; ci++) {
            double unigramScore = alphaScores[ci] * betaScores[ci] *
                    unigramScaleFactor / sentScore;
            if (unigramScore > 0) {
                tmpPosteriorList.add(new Pair<Integer, Double>(ci, unigramScore));
            }
        }
        Collections.sort(tmpPosteriorList, new PosteriorComparator());
        posteriorList = new ArrayList<Pair<Integer, Double>>();
        for (int i = 0; i < Math.min(top, tmpPosteriorList.size()); i++) {
            posteriorList.add(tmpPosteriorList.get(i));
        }
    }

    public void selectTopK(int top) {
        List<Pair<Integer, Double>> tmpPosteriorList = posteriorList;
        Collections.sort(tmpPosteriorList, new PosteriorComparator());
        posteriorList = new ArrayList<Pair<Integer, Double>>();
        for (int i = 0; i < Math.min(top, tmpPosteriorList.size()); i++) {
            posteriorList.add(tmpPosteriorList.get(i));
        }
    }
}
