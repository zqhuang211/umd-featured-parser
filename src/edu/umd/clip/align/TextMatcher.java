package edu.umd.clip.align;

import java.util.*;

/**
 * This class determines the optimal alignment given two lists of tokens (e.g., strings, usually words).
 * The default cost of a deletion, insertion, and mismatch is 1.
 * This cost can be changed by passing a different valuator.
 * 
 * It is up to the caller to tokenize a text appropriately. For example, removing punctuation, spaces, and putting remaining words into a list.
 * The algorithm below uses the "equals()" method to test for equality of 2 tokens. 
 * 
 * The memory requirement for the alignment algorithm is NxM, where N and M are the 
 * number of tokens in the reference and hypothesis lists. 
 */
public class TextMatcher {

    public static interface Valuator {

        double matchPercent(String ref, String hyp);
    }
    public static final Valuator DEFAULT_VAL = new Valuator() {

        public double matchPercent(String ref, String hyp) {
            int l1 = ref.length();
            int l2 = hyp.length();
            int minl = Math.min(l1, l2);
            int lm = 0;
            for (int i = 0; i < minl; i++) {
                if (ref.charAt(i) == hyp.charAt(i)) {
                    lm++;
                } else {
                    break;
                }
            }
            lm /= 2.0;
            int rm = 0;
            for (int i = 0; i < minl; i++) {
                if (ref.charAt(l1 - 1 - i) == hyp.charAt(l2 - 1 - i)) {
                    rm++;
                } else {
                    break;
                }
            }
            return (Math.max(lm, rm) /(double) Math.max(l1, l2));
//            double s1 = 0;
//            if (!hyp.equals("") && hyp.contains(ref)) {
//                s1 = ref.length() / (double) hyp.length();
//            }
//            double s2 = 0;
//            if (!ref.equals("") && ref.contains(hyp)) {
//                s2 = hyp.length() / (double) ref.length();
//            }
//            if (hyp.endsWith(ref))
//                return ref.length() / (double) hyp.length();
//                return 0;
//            return Math.max(s1, s2);
        }
    };

    /**
     * Helper class that computes precision, recall, and "word error rate".
     */
    public static class Result {

        public final int matches;
        public final int mismatches;
        public final int insertions;
        public final int deletions;

        public Result(String result) {
            int nM = 0, nMis = 0, nI = 0, nD = 0;

            for (int i = 0; i < result.length(); ++i) {
                switch (result.charAt(i)) {
                    case '=':
                        ++nM;
                        break;
                    case '*':
                        ++nMis;
                        break;
                    case 'i':
                        ++nI;
                        break;
                    case 'd':
                        ++nD;
                        break;
                    default:
                        throw new Error("Bug!");
                }
            }

            matches = nM;
            mismatches = nMis;
            insertions = nI;
            deletions = nD;
        }

        public float precision() {
            int nGuesses = matches + mismatches + insertions;
            return (float) (matches / (double) nGuesses);
        }

        public float recall() {
            int nTotal = matches + mismatches + deletions;
            return (float) (matches / (double) nTotal);
        }

        public float wer() {
            int nTotal = matches + mismatches + deletions;
            int nErrors = mismatches + insertions + deletions;
            return nErrors / (float) nTotal;
        }
    }

    /**
     * Aligns the given 2 lists of tokens. The first is called "reference", the second "hypothesis".
     * The penalty of a mismatch, of a deletion, and of an insertion is given by the valuator parameter.
     * This function finds the alignment that minimizes the total penalty.
     *
     * It returns a string of codes (characters), where each character represents one token, and:
     *    "="		means 	it's a match
     *    "*"  	means	it's a mismatch
     *    "i"		means	an extra word has been inserted in the hypothesis
     *    "d"		means 	a reference word has been deleted in the hypothesis
     */
    public static String match(List<String> reference, List<String> hypothesis) {
        double[][] matchCounts = new double[hypothesis.size() + 1][reference.size() + 1];

        //errorCounts[0][0] is always 0, the starting point.

        for (int h = 0; h <= hypothesis.size(); ++h) {
            String hw = (h == 0) ? null : hypothesis.get(h - 1);
            for (int r = 0; r <= reference.size(); ++r) {
                String rw = (r == 0) ? null : reference.get(r - 1);
                if (h == 0) {
                    //still haven't moved hypothesis
                    if (r == 0) {
                        matchCounts[0][0] = 0; //oh well
                    } else {
                        matchCounts[0][r] = matchCounts[0][r - 1]; //  + val.deletionCost(rw); //deletion
                    }
                } else {
                    if (r == 0) {
                        matchCounts[h][0] = matchCounts[h - 1][0]; // + val.insertionCost(hw); //insertion
                    } else {
                        double fromR = matchCounts[h][r - 1]; // + val.deletionCost(rw); //deletion
                        double fromH = matchCounts[h - 1][r]; // + val.insertionCost(hw); //insertion
                        double fromBoth = matchCounts[h - 1][r - 1]; //match or...
//                        if (reference.get(r - 1).equals(hypothesis.get(h - 1))) {
//                            fromBoth += 1; // val.mismatchCost(rw, hw); //actually mismatch
//                        }
                        fromBoth += DEFAULT_VAL.matchPercent(rw, hw);
                        matchCounts[h][r] = Math.max(fromBoth, Math.max(fromR, fromH));
                    }
                }
            }
        }

        //now counts[LASTH][LASTR] is equal to the number of errors when following the best path
        int r = reference.size();
        int h = hypothesis.size();

        StringBuffer result = new StringBuffer();
        while (h > 0 || r > 0) {
            //where do we come from: from left, from up, or from up-left?
            if (h == 0) {
                result.append('d');
                --r;
            } else if (r == 0) {
                result.append('i');
                --h;
            } else {
                double match = DEFAULT_VAL.matchPercent(reference.get(r - 1), hypothesis.get(h - 1));
                if (doubleEqual(matchCounts[h][r], matchCounts[h - 1][r])) {
                    result.append('i');
                    --h;
                } else if (doubleEqual(matchCounts[h][r], matchCounts[h][r - 1])) {
                    result.append('d');
                    --r;
                } else if (doubleEqual(matchCounts[h][r], match + matchCounts[h - 1][r - 1])) {
                    result.append(doubleEqual(match, 1.0) ? '=' : '*');
                    --r;
                    --h;
                } else {
                    throw new Error("Bug!");
                }
            }
        }

        return result.reverse().toString();
    }

    public static String matchToken(List<Token> reference, List<Token> hypothesis) {
        double[][] matchCounts = new double[hypothesis.size() + 1][reference.size() + 1];

        //errorCounts[0][0] is always 0, the starting point.

        for (int h = 0; h <= hypothesis.size(); ++h) {
            String hw = (h == 0) ? null : hypothesis.get(h - 1).getWord();
            for (int r = 0; r <= reference.size(); ++r) {
                String rw = (r == 0) ? null : reference.get(r - 1).getWord();
                if (h == 0) {
                    //still haven't moved hypothesis
                    if (r == 0) {
                        matchCounts[0][0] = 0; //oh well
                    } else {
                        matchCounts[0][r] = matchCounts[0][r - 1]; //  + val.deletionCost(rw); //deletion
                    }
                } else {
                    if (r == 0) {
                        matchCounts[h][0] = matchCounts[h - 1][0]; // + val.insertionCost(hw); //insertion
                    } else {
                        double fromR = matchCounts[h][r - 1]; // + val.deletionCost(rw); //deletion
                        double fromH = matchCounts[h - 1][r]; // + val.insertionCost(hw); //insertion
                        double fromBoth = matchCounts[h - 1][r - 1]; //match or...
//                        if (reference.get(r - 1).equals(hypothesis.get(h - 1))) {
//                            fromBoth += 1; // val.mismatchCost(rw, hw); //actually mismatch
//                        }
                        fromBoth += DEFAULT_VAL.matchPercent(rw, hw);
                        matchCounts[h][r] = Math.max(fromBoth, Math.max(fromR, fromH));
                    }
                }
            }
        }

        //now counts[LASTH][LASTR] is equal to the number of errors when following the best path
        int r = reference.size();
        int h = hypothesis.size();

        StringBuffer result = new StringBuffer();
        while (h > 0 || r > 0) {
            //where do we come from: from left, from up, or from up-left?
            if (h == 0) {
                result.append('d');
                --r;
            } else if (r == 0) {
                result.append('i');
                --h;
            } else {
                double match = DEFAULT_VAL.matchPercent(reference.get(r - 1).getWord(), hypothesis.get(h - 1).getWord());
                if (doubleEqual(matchCounts[h][r], matchCounts[h - 1][r])) {
                    result.append('i');
                    --h;
                } else if (doubleEqual(matchCounts[h][r], matchCounts[h][r - 1])) {
                    result.append('d');
                    --r;
                } else if (doubleEqual(matchCounts[h][r], match + matchCounts[h - 1][r - 1])) {
                    result.append(doubleEqual(match, 1.0) ? '=' : '*');
                    --r;
                    --h;
                } else {
                    throw new Error("Bug!");
                }
            }
        }

        return result.reverse().toString();
    }

    private static boolean doubleEqual(double s1, double s2) {
        if (Math.abs(s1 - s2) < 0.000001) {
            return true;
        } else {
            return false;
        }
    }

    public static void alignTokenList(List<Token> ref, List<Token> hyp, String result) {
        int r = 0, h = 0;
        Token ref_token = null;
        Token hyp_token = null;
        for (int i = 0; i < result.length(); ++i) {
            switch (result.charAt(i)) {
                case '=':
                    ref_token = ref.get(r++);
                    hyp_token = hyp.get(h++);
                    hyp_token.setAlignedTo(ref_token);
                    ref_token.setAlignedTo(hyp_token);
                    break;
                case '*':
                    ref_token = ref.get(r++);
                    hyp_token = hyp.get(h++);
                    hyp_token.setAlignedTo(null);
                    ref_token.setAlignedTo(null);
                    break;
                case 'i':
                    hyp_token = hyp.get(h++);
                    hyp_token.setAlignedTo(null);
                    break;
                case 'd':
                    ref_token = ref.get(r++);
                    ref_token.setAlignedTo(null);
                    break;
                default:
                    throw new Error("Bug!");
            }
        }
    }

    /**
     * Given a reference, a hypothesis, and their alignment result, this function will print
     * the reference and hypothesis in parallel in three columns:
     *    ref_word  hyp_word hyp_start hyp_end
     * for deletion and insertions, the corresponding word and/or time is replaced with ""
     */
    public static String getPrettyVerticalPrint(List<String> ref, List<Token> hyp, String result) {
        Result res = new Result(result);
        System.out.println("WER = " + (100 * res.wer()) + "%");
        StringBuilder sb = new StringBuilder();

        int maxLen = 0;
        for (String word : ref) {
            if (word.length() > maxLen) {
                maxLen = word.length();
            }
        }
        for (Token token : hyp) {
            if (token.getWord().length() > maxLen) {
                maxLen = token.getWord().length();
            }
        }

        int r = 0, h = 0;
        for (int i = 0; i < result.length(); ++i) {
            switch (result.charAt(i)) {
                case '=':
                    String ref_word = ref.get(r++);
                    Token hyp_tw = hyp.get(h++);
                    sb.append(fill(ref_word, maxLen) + "\t" +
                            fill(hyp_tw.getWord(), maxLen) + "\n");
                    break;
                case '*':
                    ref_word = ref.get(r++);
                    hyp_tw = hyp.get(h++);
                    sb.append(fill(ref_word, maxLen) + "\t" +
                            fill(hyp_tw.getWord(), maxLen) + "\n");
                    break;
                case 'i':
                    hyp_tw = hyp.get(h++);
                    sb.append(fill("", maxLen) + "\t" +
                            fill(hyp_tw.getWord(), maxLen) + "\n");
                    break;
                case 'd':
                    ref_word = ref.get(r++);
                    sb.append(fill(ref_word, maxLen) + "\t" +
                            fill("", maxLen) + "\t" +
                            fill("", maxLen) + "\t" +
                            fill("", maxLen) + "\n");
                    break;
                default:
                    throw new Error("Bug!");
            }
        }
        return sb.toString();
    }

    /**
     * Given a reference, a hypothesis, and their alignment result, this function will print
     * the reference and hypothesis in parallel in three columns:
     *    ref_word  hyp_word hyp_start hyp_end
     * for deletion and insertions, the corresponding word and/or time is replaced with ""
     */
    public static String getPrettyVerticalTokenPrint(List<Token> ref, List<Token> hyp, String result) {
        Result res = new Result(result);
//        System.out.println("WER = " + (100 * res.wer()) + "%");
        StringBuilder sb = new StringBuilder();
//        StringBuffer hB = new StringBuffer();
//        StringBuffer rB = new StringBuffer();
        int maxLen = 0;
        for (Token token : ref) {
            if (token.getWord().length() > maxLen) {
                maxLen = token.getWord().length();
            }
        }
        for (Token token : hyp) {
            if (token.getWord().length() > maxLen) {
                maxLen = token.getWord().length();
            }
        }

        int r = 0, h = 0;
        for (int i = 0; i < result.length(); ++i) {
            switch (result.charAt(i)) {
                case '=':
                    Token ref_token = ref.get(r++);
                    Token hyp_token = hyp.get(h++);
                    sb.append(fill(ref_token.getWord(), maxLen) + "\t" +
                            fill(hyp_token.getWord(), maxLen) + "\n");
                    break;
                case '*':
                    ref_token = ref.get(r++);
                    hyp_token = hyp.get(h++);
                    sb.append(fill(ref_token.getWord(), maxLen) + "\t" +
                            fill(hyp_token.getWord(), maxLen) + "\n");
                    break;
                case 'i':
                    hyp_token = hyp.get(h++);
                    sb.append(fill("", maxLen) + "\t" +
                            fill(hyp_token.getWord(), maxLen) + "\n");
                    break;
                case 'd':
                    ref_token = ref.get(r++);
                    sb.append(fill(ref_token.getWord(), maxLen) + "\t" +
                            fill("", maxLen) + "\n");
                    break;
                default:
                    throw new Error("Bug!");
            }
        }
        return sb.toString();
    }

    /**
     * Given a reference, a hypothesis, and their alignment result, this function will
     * print 2 lines of text. The top line is the reference, and the bottom line is the hypothesis.
     * The purpose is to see easily how the 2 texts match (with a fixed width font).
     * Insertion/deletions are replaced with "****".
     */
    public static void prettyPrint(List<String> ref, List<String> hyp, String result) {
        Result res = new Result(result);
        System.out.println("WER = " + (100 * res.wer()) + "%");

        StringBuffer hB = new StringBuffer();
        StringBuffer rB = new StringBuffer();
        int r = 0, h = 0;
        for (int i = 0; i < result.length(); ++i) {
            switch (result.charAt(i)) {
                case '=':
                    hB.append(hyp.get(h++));
                    rB.append(ref.get(r++));
                    break;
                case '*':
                    hB.append(hyp.get(h));
                    rB.append(ref.get(r));
                    fill(rB, ref.get(r++), hB, hyp.get(h++));
                    break;
                case 'i':
                    hB.append(hyp.get(h));
                    fill(rB, "", hB, hyp.get(h++));
                    break;
                case 'd':
                    rB.append(ref.get(r));
                    fill(rB, ref.get(r++), hB, "");
                    break;
                default:
                    throw new Error("Bug!");
            }
            rB.append(' ');
            hB.append(' ');
        }
        System.out.println(hB.toString());
        System.out.println(rB.toString());
    }

    private static void fill(StringBuffer rB, String r, StringBuffer hB, String h) {
        int d = h.length() - r.length();
        if (d > 0) {
            while (--d >= 0) {
                rB.append(' ');
            }
        } else {
            while (++d <= 0) {
                hB.append(' ');
            }
        }
    }

    private static String fill(String word, int maxLen) {
        for (int i = maxLen - word.length(); i > 0; i--) {
            word = word + " ";
        }
        return word;
    }
}







