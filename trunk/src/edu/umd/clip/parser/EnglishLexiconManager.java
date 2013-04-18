/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.util.UniCounter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map.Entry;

/**
 *
 * @author zqhuang
 */
public class EnglishLexiconManager extends LexiconManager implements Serializable {

    private static final long serialVersionUID = 1L;
    protected UniCounter<String>[] tagSigCounts;
    protected UniCounter<String> sigCounter;
    protected String lastWord;
    protected String lastSignature;

    @Override
    public void setupArray() {
        super.setupArray();

        sigCounter = new UniCounter<String>();
        for (Entry<String, Double> entry : wordCountsMap.entrySet()) {
            String word = entry.getKey();
            double count = entry.getValue();
            String signature = getCachedSignature(word);
            sigCounter.incrementCount(signature, count);
        }

        tagSigCounts = new UniCounter[numNodes];
        for (int ni = 0; ni < numNodes; ni++) {
            if (tagWordCounts[ni] == null) {
                continue;
            }
            tagSigCounts[ni] = new UniCounter<String>();
            for (Entry<String, Double> entry : tagWordCounts[ni].entrySet()) {
                String word = entry.getKey();
                double count = entry.getValue();
                if (wordCountsMap.getCount(word) < rareWordThreshold) {
                    String signature = getCachedSignature(word);
                    tagSigCounts[ni].incrementCount(signature, count);
                }
            }
        }
    }

    @Override
    public void checkTrainedWithPunc() {
        if (!trainedWithPuncChecked) {
            trainedWithPuncChecked = true;
            trainedWithPunc = false;
            for (String node : nodeMap.keySet()) {
                if (ClosedPosSets.isPunc((String) node)) {
                    trainedWithPunc = true;
                    break;
                }
            }
        }
    }

    @Override
    public double[] getProbs(int tag, String word, boolean viterbi) {
        double pb_W_T = 0;
        double[] resultArray = new double[numStates[tag]];
        Arrays.fill(resultArray, 0.0);
        double c_W = wordCountsMap.getCount(word);
        boolean seen = c_W != 0;

        checkTrainedWithPunc();
        boolean isPuncWord = ClosedPosSets.isPunc(word);
        boolean isPuncTag = false;
        String tagName = nodeList.get(tag);
        if (trainedWithPunc) {
            isPuncTag = ClosedPosSets.isPunc(tagName);
        }
        if (trainedWithPunc && !seen && isPuncWord != isPuncTag) {
            if (viterbi) {
                Arrays.fill(resultArray, Double.NEGATIVE_INFINITY);
            }
            return resultArray;
        }

        double[] twCounts = null;
        String sig = getCachedSignature(word);
        boolean sigSeen = sigCounter.getCount(sig) > 0;
        double sigProb = 0;
        if (seen) {
            if (latentTagWordCounts[tag] != null
                    && latentTagWordCounts[tag].containsKey(word)) {
                twCounts = latentTagWordCounts[tag].get(word).getArray();
            } else {
                twCounts = new double[numStates[tag]];
            }
        } else if (oovHandler == OOVHandler.heuristic && sigSeen & !isPuncWord) {
            sigProb = getSigProb(sig, tag);
        }

        for (int tsi = 0; tsi < numStates[tag]; tsi++) {
            if (seen) {
                double c_T = nodeCounts[tag][tsi];
                if (c_T == 0) {
                    continue;
                }
                double c_TW = twCounts[tsi];
                double c_Tunseen = unseenLatentTagCounts[tag][tsi];
                double p_T_U = (totalUnseenTokens == 0) ? 1 : c_Tunseen / totalUnseenTokens;
                double pb_T_W = 0;
                if (c_W > unknownSmoothingThreshold) {
                    pb_T_W = (c_TW + 0.0001 * p_T_U) / (c_W + 0.0001);
                } else {
                    pb_T_W = (c_TW + wordSmoothingParam * p_T_U) / (c_W + wordSmoothingParam);
                }
                if (pb_T_W == 0) {
                    continue;
                }
                pb_W_T = pb_T_W * c_W / c_T;
            } else {
                if (oovHandler == OOVHandler.heuristic & sigProb > 0) {
                    pb_W_T = sigProb;
                } else {
                    double c_Tseen = nodeCounts[tag][tsi];
                    if (trainedWithPunc && isPuncTag && isPuncWord) {
                        double c_surfaceT = tagCounts[tag];
                        pb_W_T = (1.0 / totalTokens + ClosedPosSets.overlapRate(word, tagName)) / numStates[tag] / totalUnseenTokens / c_surfaceT;
                    } else {
                        double c_Tunseen = unseenLatentTagCounts[tag][tsi];
                        double p_T_U = (totalUnseenTokens == 0) ? 1 : c_Tunseen / totalUnseenTokens;
                        pb_W_T = p_T_U / c_Tseen;
                    }
                }
            }
            resultArray[tsi] = pb_W_T;
        }

        resultArray = smooth(resultArray, tag, smoothingMatrix[tag]);
        if (viterbi) {
            for (int si = 0; si < resultArray.length; si++) {
                resultArray[si] = Math.log(resultArray[si]);
            }
        }
        return resultArray;
    }

    public double[] smooth(double[] scores, int tag, double[][] smoothingMatrix) {
        int ns = scores.length;
        double newScores[] = new double[ns];
        for (int i = 0; i < ns; i++) {
            for (int j = 0; j < ns; j++) {
                newScores[i] += smoothingMatrix[i][j] * scores[j];
            }
        }
        return newScores;
    }

    private double getSigProb(String sig, int tag) {
        double c_T = tagCounts[tag];
        double c_S = sigCounter.getCount(sig);
        double pb_C_T = 0;
        if (c_T == 0) {
            return 0;
        }

        double c_TS = tagSigCounts[tag].getCount(sig);

        if (c_TS == 0) {
            return 0;
        }
        double c_Tunseen = unseenTagCounts[tag];

        double p_T_U = (totalUnseenTokens == 0) ? 1 : c_Tunseen / totalUnseenTokens;
        double pb_T_S = 0;
        if (c_S > unknownSmoothingThreshold) {
            pb_T_S = c_TS / c_S;
        } else {
            pb_T_S = (c_TS + affixSmoothingParam * p_T_U) / (c_S + affixSmoothingParam);
        }

        if (pb_T_S == 0) {
            return 0;
        }

        pb_C_T = pb_T_S * c_S / c_T;
        return pb_C_T;
    }

    protected String getCachedSignature(String word) {
        if (word.equals(lastWord)) {
            return lastSignature;
        } else {
            String sig = getSignature(word);
            lastSignature = sig;
            return sig;
        }
    }

    protected String getSignature(String word) {
        //    int unknownLevel = Options.get().useUnknownWordSignatures;
        StringBuilder sb = new StringBuilder("UNK");
        int wlen = word.length();
        int numCaps = 0;
        boolean hasDigit = false;
        boolean hasDash = false;
        boolean hasLower = false;
        for (int i = 0; i < wlen; i++) {
            char ch = word.charAt(i);
            if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (ch == '-') {
                hasDash = true;
            } else if (Character.isLetter(ch)) {
                if (Character.isLowerCase(ch)) {
                    hasLower = true;
                } else if (Character.isTitleCase(ch)) {
                    hasLower = true;
                    numCaps++;

                } else {
                    numCaps++;
                }

            }
        }
        char ch0 = word.charAt(0);
        String lowered = word.toLowerCase();
        if (Character.isUpperCase(ch0) || Character.isTitleCase(ch0)) {
            sb.append("-CAPS");

        } else if (!Character.isLetter(ch0) && numCaps > 0) {
            sb.append("-CAPS");
        } else if (hasLower) { // (Character.isLowerCase(ch0)) {
            sb.append("-LC");
        }

        if (hasDigit) {
            sb.append("-NUM");
        }

        if (hasDash) {
            sb.append("-DASH");
        }

        if (lowered.endsWith("s") && wlen >= 3) {
            char ch2 = lowered.charAt(wlen - 2);
            if (ch2 != 's' && ch2 != 'i' && ch2 != 'u') {
                sb.append("-s");
            }

        } else if (word.length() >= 5 && !hasDash && !(hasDigit && numCaps > 0)) {
            if (lowered.endsWith("ed")) {
                sb.append("-ed");
            } else if (lowered.endsWith("ing")) {
                sb.append("-ing");
            } else if (lowered.endsWith("ion")) {
                sb.append("-ion");
            } else if (lowered.endsWith("er")) {
                sb.append("-er");
            } else if (lowered.endsWith("est")) {
                sb.append("-est");
            } else if (lowered.endsWith("ly")) {
                sb.append("-ly");
            } else if (lowered.endsWith("ity")) {
                sb.append("-ity");
            } else if (lowered.endsWith("y")) {
                sb.append("-y");
            } else if (lowered.endsWith("al")) {
                sb.append("-al");
            }

        }
        return sb.toString().intern();
    }
}
