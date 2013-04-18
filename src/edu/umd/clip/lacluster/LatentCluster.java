/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.lacluster;

import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.math.RandomDisturbance;
import edu.umd.clip.latagger.LatentTrainer;
import edu.umd.clip.util.Pair;
import edu.umd.clip.util.UniCounter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author zqhuang
 */
public class LatentCluster implements Serializable {

    private static final long serialVersionUID = 1L;
    private int clusterNum = 0;
    private double[] unseenClusterProbs;
    private UniCounter<String> wordCounts;
    private double[][] transitionCounts;
    private double[][] transitionProbs;
    private UniCounter<String>[] emissionCounts;
    private UniCounter<String>[] emissionProbs;
    private Collection<List<AlphaBetaItem>> alphaBetaCorpus;
    private boolean initUnseen;

    public void setInitUnseen(boolean initUnseen) {
        this.initUnseen = initUnseen;
    }

    public boolean isInitUnseen() {
        return initUnseen;
    }

    public void split() {
        int newClusterNum = 2 * clusterNum - 2;
        double[][] newTransitionProbs = new double[newClusterNum][newClusterNum];
        UniCounter<String>[] newEmissionProbs = new UniCounter[newClusterNum];
        for (int prevClusterIndex = 0; prevClusterIndex < clusterNum; prevClusterIndex++) {
            int prevSplitFactor = 2;
            if (prevClusterIndex == 0 || prevClusterIndex == clusterNum - 1) {
                prevSplitFactor = 1;
            }
            for (int currClusterIndex = 0; currClusterIndex < clusterNum; currClusterIndex++) {
                int currSplitFactor = 2;
                if (currClusterIndex == 0 || currClusterIndex == clusterNum - 1) {
                    currSplitFactor = 1;
                }
                double oldScore = transitionProbs[prevClusterIndex][currClusterIndex];
                for (int prevSplitIndex = 0; prevSplitIndex < prevSplitFactor; prevSplitIndex++) {
                    double randomnessComponent = 0;
                    if (currSplitFactor > 1) {
                        randomnessComponent = oldScore / currSplitFactor * RandomDisturbance.generateRandomDisturbance();
                    }
                    for (int currSplitIndex = 0; currSplitIndex < currSplitFactor; currSplitIndex++) {
                        if (currSplitIndex == 1) {
                            randomnessComponent *= -1;
                        }
                        int newPrevClusterIndex = (prevClusterIndex - 1) * prevSplitFactor + 1 + prevSplitIndex;
                        if (prevClusterIndex == 0) {
                            newPrevClusterIndex = 0;
                        } else if (prevClusterIndex == clusterNum - 1) {
                            newPrevClusterIndex = newClusterNum - 1;
                        }

                        int newCurrClusterIndex = (currClusterIndex - 1) * currSplitFactor + 1 + currSplitIndex;
                        if (currClusterIndex == 0) {
                            newCurrClusterIndex = 0;
                        } else if (currClusterIndex == clusterNum - 1) {
                            newCurrClusterIndex = newClusterNum - 1;
                        }
                        newTransitionProbs[newPrevClusterIndex][newCurrClusterIndex] =
                                transitionProbs[prevClusterIndex][currClusterIndex] / currSplitFactor + randomnessComponent;
                    }
                }
            }
        }
        for (int clusterIndex = 0; clusterIndex < clusterNum; clusterIndex++) {
            int splitFactor = 2;
            if (clusterIndex == 0 || clusterIndex == clusterNum - 1) {
                splitFactor = 1;
            }
            for (int splitIndex = 0; splitIndex < splitFactor; splitIndex++) {
                int newClusterIndex = (clusterIndex - 1) * splitFactor + 1 + splitIndex;
                if (clusterIndex == 0) {
                    newClusterIndex = 0;
                } else if (clusterIndex == clusterNum - 1) {
                    newClusterIndex = newClusterNum - 1;
                }
                newEmissionProbs[newClusterIndex] = new UniCounter<String>();
                for (Entry<String, Double> entry : emissionProbs[clusterIndex].entrySet()) {
                    newEmissionProbs[newClusterIndex].incrementCount(entry.getKey(), entry.getValue());
                }
            }
        }
        clusterNum = newClusterNum;
        transitionProbs = newTransitionProbs;
        emissionProbs = newEmissionProbs;
    }

    private void resetCounts() {
        transitionCounts = new double[clusterNum][clusterNum];
        emissionCounts = new UniCounter[clusterNum];
        for (int ci = 0; ci < clusterNum; ci++) {
            emissionCounts[ci] = new UniCounter<String>();
        }
    }

    public double doEM() {
        resetCounts();
        double ll = 0;
        for (List<AlphaBetaItem> sentence : alphaBetaCorpus) {
            resetAlphaBeta(sentence);
            ll += doExpectionStep(sentence);
            clearAlphaBeta(sentence);
        }
        doMaximizationStep();
        return ll;
    }

    private void resetAlphaBeta(List<AlphaBetaItem> sentence) {
        for (AlphaBetaItem item : sentence) {
            item.reset(clusterNum);
        }
    }

    private void clearAlphaBeta(List<AlphaBetaItem> sentence) {
        for (AlphaBetaItem item : sentence) {
            item.clear();
        }
    }

    public void setCorpus(Collection<List<AlphaBetaItem>> corpus) {
        alphaBetaCorpus = corpus;
    }

    public static Collection<List<AlphaBetaItem>> convertString2AlphaBeta(Collection<List<String>> stringCorpus) {
        Collection<List<AlphaBetaItem>> alphaBetaCorpus = new ArrayList<List<AlphaBetaItem>>();
        for (List<String> sentence : stringCorpus) {
            List<AlphaBetaItem> alphaBetaSentence = new ArrayList<AlphaBetaItem>();
            for (String word : sentence) {
                alphaBetaSentence.add(new AlphaBetaItem(word));
            }
            alphaBetaCorpus.add(alphaBetaSentence);
        }
        return alphaBetaCorpus;
    }

    public void doInitialization(Collection<List<AlphaBetaItem>> corpus) {
        clusterNum = 1;
        wordCounts = new UniCounter<String>();
        Map<String, Integer> tagMap = new HashMap<String, Integer>();
        for (List<AlphaBetaItem> sentence : corpus) {
            for (int i = 1; i < sentence.size() - 1; i++) {
                String tag = sentence.get(i).getTag();
                Integer tagi = tagMap.get(tag);
                wordCounts.incrementCount(sentence.get(i).getWord(), 1);
                if (tagi == null) {
                    tagi = clusterNum++;
                    tagMap.put(tag, tagi);
                }
            }
        }
        clusterNum++;
        tagMap.put("SOS", 0);
        tagMap.put("EOS", clusterNum - 1);

        double[] unseenCounts = new double[clusterNum];
        double[] totalCounts = new double[clusterNum];
        double total = 0;
        transitionCounts = new double[clusterNum][clusterNum];
        emissionCounts = new UniCounter[clusterNum];
        for (int ci = 0; ci < clusterNum; ci++) {
            emissionCounts[ci] = new UniCounter<String>();
        }
        for (List<AlphaBetaItem> sentence : corpus) {
            int wordNum = sentence.size();
            int prevCluster = 0;
            for (int wi = 0; wi < wordNum; wi++) {
                AlphaBetaItem item = sentence.get(wi);
                int currCluster = tagMap.get(item.getTag());
                String word = item.getWord();
                emissionCounts[currCluster].incrementCount(word, 1);
                totalCounts[currCluster]++;
                if (wordCounts.getCount(word) == 1) {
                    unseenCounts[currCluster]++;
                }
                if (wi != 0) {
                    transitionCounts[prevCluster][currCluster]++;
                }
                prevCluster = currCluster;
            }
        }
        unseenClusterProbs = new double[clusterNum];
        for (int ci = 0; ci < clusterNum; ci++)
            total += totalCounts[ci];
        for (int ci = 0; ci < clusterNum; ci++) {
            unseenClusterProbs[ci] = (unseenCounts[ci] / totalCounts[ci]) / total;
        }
        doMaximizationStep();
    }

    private void doForward(List<AlphaBetaItem> sentence) {
        int sentSize = sentence.size();
        double[] prevAlphaScores = sentence.get(0).getAlphaScores();
        prevAlphaScores[0] = 1;
        int prevScale = 0;
        for (int wi = 1; wi < sentSize; wi++) {
            AlphaBetaItem currItem = sentence.get(wi);
            String currWord = currItem.getWord();
            double[] currAlphaScores = currItem.getAlphaScores();
            for (int cci = 0; cci < clusterNum; cci++) {
                for (int pci = 0; pci < clusterNum; pci++) {
                    currAlphaScores[cci] += prevAlphaScores[pci] * transitionProbs[pci][cci];
                }
                double emission = emissionProbs[cci].getCount(currWord);
                if (initUnseen && emission == 0) {
                    emission = unseenClusterProbs[cci];
                }
                currAlphaScores[cci] *= emission;
            }
            int currScale = ArrayMath.scaleScores(currAlphaScores, prevScale);
            currItem.setAlphaScale(currScale);
            prevAlphaScores = currAlphaScores;
            prevScale = currScale;
        }
    }

    private void doBackward(List<AlphaBetaItem> sentence) {
        int sentSize = sentence.size();
        AlphaBetaItem nextItem = sentence.get(sentSize - 1);
        double[] nextBetaScores = nextItem.getBetaScores();
        nextBetaScores[clusterNum - 1] = 1;
        int nextScale = 0;
        String nextWord = nextItem.getWord();
        for (int wi = sentSize - 2; wi >= 0; wi--) {
            AlphaBetaItem currItem = sentence.get(wi);
            double[] currBetaScores = currItem.getBetaScores();
            for (int nci = 0; nci < clusterNum; nci++) {
                double emission = emissionProbs[nci].getCount(nextWord);
                if (initUnseen && emission == 0) {
                    emission = unseenClusterProbs[nci];
                }
                if (emission == 0) {
                    continue;
                }
                for (int cci = 0; cci < clusterNum; cci++) {
                    currBetaScores[cci] += nextBetaScores[nci] * transitionProbs[cci][nci] * emission;
                }
            }
            int currScale = ArrayMath.scaleScores(currBetaScores, nextScale);
            currItem.setBetaScale(currScale);
            nextWord = currItem.getWord();
            nextBetaScores = currBetaScores;
            nextScale = currScale;
        }
    }

    public void selectTopK(int top) {
        Map<String, PosteriorItem> wordPosterior = new HashMap<String, PosteriorItem>();
        for (int ci = 0; ci < clusterNum; ci++) {
            for (Entry<String, Double> entry : emissionCounts[ci].entrySet()) {
                String word = entry.getKey();
                double posterior = entry.getValue();
                PosteriorItem postItem = wordPosterior.get(word);
                if (postItem == null) {
                    postItem = new PosteriorItem();
                    wordPosterior.put(word, postItem);
                }
                postItem.addItem(ci, posterior);
            }
        }

        emissionCounts = new UniCounter[clusterNum];
        for (Entry<String, PosteriorItem> entry : wordPosterior.entrySet()) {
            String word = entry.getKey();
            PosteriorItem postItem = entry.getValue();
            postItem.selectTopK(top);
            for (Pair<Integer, Double> pair : postItem.getPosteriorList()) {
                int ci = pair.getFirst();
                double posterior = pair.getSecond();
                if (emissionCounts[ci] == null) {
                    emissionCounts[ci] = new UniCounter<String>();
                }
                emissionCounts[ci].incrementCount(word, posterior);
            }
        }

        double[] clusterCounts = new double[clusterNum];
        for (int ci = 0; ci < clusterNum; ci++) {
            for (Entry<String, Double> entry : emissionCounts[ci].entrySet()) {
                clusterCounts[ci] += entry.getValue();
            }
        }

        emissionProbs = new UniCounter[clusterNum];
        for (int ci = 0; ci < clusterNum; ci++) {
            emissionProbs[ci] = new UniCounter<String>();
            for (Entry<String, Double> entry : emissionCounts[ci].entrySet()) {
                emissionProbs[ci].incrementCount(entry.getKey(), entry.getValue() / clusterCounts[ci]);
            }
        }
    }

    private void doMaximizationStep() {
        double[] clusterCounts = new double[clusterNum];
        for (int ci = 0; ci < clusterNum; ci++) {
            for (Entry<String, Double> entry : emissionCounts[ci].entrySet()) {
                clusterCounts[ci] += entry.getValue();
            }
        }

        transitionProbs = new double[clusterNum][clusterNum];
        for (int pci = 0; pci < clusterNum; pci++) {
            if (clusterCounts[pci] == 0) {
                ArrayMath.fill(transitionProbs[pci], 0);
            } else {
                for (int cci = 0; cci < clusterNum; cci++) {
                    transitionProbs[pci][cci] = transitionCounts[pci][cci] / clusterCounts[pci];
                }
            }
        }

        emissionProbs = new UniCounter[clusterNum];
        for (int ci = 0; ci < clusterNum; ci++) {
            emissionProbs[ci] = new UniCounter<String>();
            for (Entry<String, Double> entry : emissionCounts[ci].entrySet()) {
                emissionProbs[ci].incrementCount(entry.getKey(), entry.getValue() / clusterCounts[ci]);
            }
        }
    }

    private double doExpectionStep(List<AlphaBetaItem> sentence) {
        doForward(sentence);
        doBackward(sentence);

        AlphaBetaItem lastItem = sentence.get(sentence.size() - 1);
        double[] lastAlphaScores = lastItem.getAlphaScores();
        double sentScore = lastAlphaScores[clusterNum - 1];
        double sentScale = lastItem.getAlphaScale();

        double ll = (Math.log(sentScore) + Math.log(LatentTrainer.SCALE) * sentScale);
//        System.out.println(ll);
        if (Double.isNaN(ll) || Double.isInfinite(ll)) {
            System.out.println(sentence.toString());
            ll = 0;
        }

        int sentSize = sentence.size();
        AlphaBetaItem prevItem = sentence.get(0);
        emissionCounts[0].incrementCount(prevItem.getWord(), 1);
        for (int wi = 1; wi < sentSize; wi++) {
            AlphaBetaItem currItem = sentence.get(wi);
            String currWord = currItem.getWord();
            double[] currAlphaScores = currItem.getAlphaScores();
            double[] currBetaScores = currItem.getBetaScores();
            double unigramScale = currItem.getAlphaScale() + currItem.getBetaScale() - sentScale;
            double unigramScaleFactor = Math.pow(LatentTrainer.SCALE, unigramScale);
            for (int ci = 0; ci < clusterNum; ci++) {
                double unigramScore = currAlphaScores[ci] * currBetaScores[ci] *
                        unigramScaleFactor / sentScore;
                if (unigramScore > 0) {
                    emissionCounts[ci].incrementCount(currWord, unigramScore);
                }
            }

            double[] prevAlphaScores = prevItem.getAlphaScores();
            double bigramScale = prevItem.getAlphaScale() + currItem.getBetaScale() - sentScale;
            double bigramScaleFactor = Math.pow(LatentTrainer.SCALE, bigramScale);
            for (int cci = 0; cci < clusterNum; cci++) {
                double emission = emissionProbs[cci].getCount(currWord);
                if (initUnseen && emission == 0) {
                    emission = unseenClusterProbs[cci];
                }
                if (emission == 0) {
                    continue;
                }
                for (int pci = 0; pci < clusterNum; pci++) {
                    double bigramScore = prevAlphaScores[pci] * transitionProbs[pci][cci] *
                            emission * currBetaScores[cci] * bigramScaleFactor / sentScore;
                    if (bigramScore > 0) {
                        transitionCounts[pci][cci] += bigramScore;
                    }
                }
            }
            prevItem = currItem;
        }
        return ll;
    }

    public List<PosteriorItem> calcPosterior(List<AlphaBetaItem> sentence, int top) {
        resetAlphaBeta(sentence);
        doForward(sentence);
        doBackward(sentence);

        AlphaBetaItem lastItem = sentence.get(sentence.size() - 1);
        double[] lastAlphaScores = lastItem.getAlphaScores();
        double sentScore = lastAlphaScores[clusterNum - 1];
        double sentScale = lastItem.getAlphaScale();

        if (sentScore == 0)
            return null;

        int sentSize = sentence.size();
        List<PosteriorItem> postList = new ArrayList<PosteriorItem>();
        for (int wi = 0; wi < sentSize; wi++) {
            AlphaBetaItem item = sentence.get(wi);
            postList.add(new PosteriorItem(item.getAlphaScores(), item.getAlphaScale(),
                    item.getBetaScores(), item.getBetaScale(),
                    sentScore, sentScale, top));
        }
        return postList;
    }

    public boolean save(String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName); // Save to file
            GZIPOutputStream gzos = new GZIPOutputStream(fos); // Compressed
            ObjectOutputStream out = new ObjectOutputStream(gzos); // Save objects
            out.writeInt(clusterNum);
            out.writeObject(unseenClusterProbs);
            out.writeObject(transitionProbs);
            out.writeObject(emissionProbs);
            out.flush(); // Always flush the output.
            out.close(); // And close the stream.
        } catch (IOException e) {
            System.out.println("IOException: " + e);
            return false;
        }
        return true;
    }

    public boolean load(String fileName) {
        try {
            FileInputStream fis = new FileInputStream(fileName); // Load from file
            GZIPInputStream gzis = new GZIPInputStream(fis); // Compressed
            ObjectInputStream in = new ObjectInputStream(gzis); // Load objects
            clusterNum = in.readInt();
            unseenClusterProbs = (double[]) in.readObject();
            transitionProbs = (double[][]) in.readObject();
            emissionProbs = (UniCounter<String>[]) in.readObject();
            in.close(); // And close the stream.
        } catch (IOException e) {
            System.out.println("IOException\n" + e);
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found!");
            return false;
        }
        return true;
    }

    public void printTopWords(int top) {
        Comparator<Entry<String, Double>> comparator = new Comparator<Entry<String, Double>>() {

            public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
                return Double.compare(o2.getValue(), o1.getValue());
            }
        };
        for (int ci = 0; ci < clusterNum; ci++) {
            List<Entry<String, Double>> entryList = new ArrayList<Entry<String, Double>>();
            for (Entry<String, Double> entry : emissionProbs[ci].entrySet()) {
                entryList.add(entry);
            }
            Collections.sort(entryList, comparator);
            System.out.print(ci + ":");
            for (int i = 0; i < Math.min(top, entryList.size()); i++) {
                Entry<String, Double> entry = entryList.get(i);
                System.out.printf(" %s(%.6f)", entry.getKey(), entry.getValue());
            }
            System.out.println("\n");
        }
    }
}
