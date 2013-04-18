/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.util.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class ClosedPosSets {

    private static Set<String> puncLetters;
    private static Set<String> puncSet;
    private static Set<String> closedChinesePosSet;
    private static Set<String> englishPuncTagSet;
    private static Set<String> openEnglishPosSet;

    private static synchronized void checkEnglishPuncTagSet(Set<String> puncTags) {
        if (englishPuncTagSet == null) {
            englishPuncTagSet = new HashSet<String>();
            for (String punc : puncTags) {
                if (isPunc(punc)) {
                    englishPuncTagSet.add(punc);
                }
            }
        }
    }

    private static synchronized void checkEnglishOpenPosSet() {
        if (openEnglishPosSet == null) {
            openEnglishPosSet = new HashSet<String>();
            openEnglishPosSet.add("RB");
            openEnglishPosSet.add("JJ");
            openEnglishPosSet.add("VB");
            openEnglishPosSet.add("VBP");
            openEnglishPosSet.add("VBN");
            openEnglishPosSet.add("NN");
            openEnglishPosSet.add("CD");
            openEnglishPosSet.add("RBS");
            openEnglishPosSet.add("RBR");
            openEnglishPosSet.add("JJR");
            openEnglishPosSet.add("VBZ");
            openEnglishPosSet.add("JJS");
            openEnglishPosSet.add("VBD");
            openEnglishPosSet.add("NNS");
            openEnglishPosSet.add("NNP");
            openEnglishPosSet.add("VBG");
            openEnglishPosSet.add("NNPS");
        }
    }

    private static synchronized void checkChineseClosedPosSet() {
        if (closedChinesePosSet == null) {
            closedChinesePosSet = new HashSet<String>();
            closedChinesePosSet.add("AS");
            closedChinesePosSet.add("BA");
            closedChinesePosSet.add("CC");
            closedChinesePosSet.add("CS");
            closedChinesePosSet.add("DEC");
            closedChinesePosSet.add("DEG");
            closedChinesePosSet.add("DER");
            closedChinesePosSet.add("DEV");
            closedChinesePosSet.add("DT");
            closedChinesePosSet.add("ETC");
            closedChinesePosSet.add("IJ");
            closedChinesePosSet.add("LB");
            closedChinesePosSet.add("LC");
            closedChinesePosSet.add("P");
            closedChinesePosSet.add("PN");
            closedChinesePosSet.add("PU");
            closedChinesePosSet.add("SB");
            closedChinesePosSet.add("SP");
            closedChinesePosSet.add("VC");
            closedChinesePosSet.add("VE");
        }
    }

    private synchronized static void checkPuncSet() {
        if (puncSet == null) {
            puncSet = new HashSet<String>();
            puncSet.add("-LRB-");
            puncSet.add("-RRB-");
            puncSet.add("-LCB-");
            puncSet.add("-RCB-");
            puncSet.add("-LSB-");
            puncSet.add("-RSB-");
        }
    }

    private synchronized static void checkPuncLetters() {
        if (puncLetters == null) {
            puncLetters = new HashSet();
            puncLetters.add("/");
            puncLetters.add("︰");
            puncLetters.add("～");
            puncLetters.add("─");
            puncLetters.add("﹖");
            puncLetters.add(",");
            puncLetters.add("｛");
            puncLetters.add("━");
            puncLetters.add("﹗");
            puncLetters.add("\"");
            puncLetters.add("﹚");
            puncLetters.add("″");
            puncLetters.add("„");
            puncLetters.add("｡");
            puncLetters.add("（");
            puncLetters.add("–");
            puncLetters.add("﹏");
            puncLetters.add("﹙");
            puncLetters.add("〉");
            puncLetters.add("＞");
            puncLetters.add("〗");
            puncLetters.add("|");
            puncLetters.add("．");
            puncLetters.add("‐");
            puncLetters.add("^");
            puncLetters.add("∶");
            puncLetters.add("・");
            puncLetters.add("＝");
            puncLetters.add("＋");
            puncLetters.add("\\");
            puncLetters.add("？");
            puncLetters.add("’");
            puncLetters.add("~");
            puncLetters.add("-");
            puncLetters.add("／");
            puncLetters.add("；");
            puncLetters.add("!");
            puncLetters.add("＿");
            puncLetters.add("―");
            puncLetters.add("。");
            puncLetters.add(" ");
            puncLetters.add("{");
            puncLetters.add("｀");
            puncLetters.add("＊");
            puncLetters.add("%");
            puncLetters.add("‟");
            puncLetters.add("_");
            puncLetters.add("+");
            puncLetters.add("『");
            puncLetters.add("』");
            puncLetters.add("‚");
            puncLetters.add("‑");
            puncLetters.add("」");
            puncLetters.add("【");
            puncLetters.add("〃");
            puncLetters.add("°");
            puncLetters.add("'");
            puncLetters.add("}");
            puncLetters.add("！");
            puncLetters.add("〕");
            puncLetters.add("=");
            puncLetters.add("—");
            puncLetters.add("－");
            puncLetters.add("｜");
            puncLetters.add("）");
            puncLetters.add("：");
            puncLetters.add("⊙");
            puncLetters.add("<");
            puncLetters.add("＾");
            puncLetters.add("¨");
            puncLetters.add("％");
            puncLetters.add(">");
            puncLetters.add("＼");
            puncLetters.add("﹐");
            puncLetters.add("…");
            puncLetters.add("?");
            puncLetters.add("&");
            puncLetters.add("‛");
            puncLetters.add("ˉ");
            puncLetters.add("〖");
            puncLetters.add("•");
            puncLetters.add("`");
            puncLetters.add("·");
            puncLetters.add("〈");
            puncLetters.add("》");
            puncLetters.add(":");
            puncLetters.add("‒");
            puncLetters.add("*");
            puncLetters.add("＆");
            puncLetters.add("﹕");
            puncLetters.add("〔");
            puncLetters.add("、");
            puncLetters.add("＂");
            puncLetters.add("＇");
            puncLetters.add("●");
            puncLetters.add("﹔");
            puncLetters.add(".");
            puncLetters.add("】");
            puncLetters.add("”");
            puncLetters.add("「");
            puncLetters.add(";");
            puncLetters.add("“");
            puncLetters.add("﹒");
            puncLetters.add("‘");
            puncLetters.add("｝");
            puncLetters.add("､");
            puncLetters.add("﹁");
            puncLetters.add("］");
            puncLetters.add("′");
            puncLetters.add("＜");
            puncLetters.add("《");
            puncLetters.add("﹂");
            puncLetters.add("［");
            puncLetters.add("，");
            puncLetters.add("￣");
        }
    }

    public static boolean isPunc(String word) {
        checkPuncLetters();
        checkPuncSet();
        if (puncSet.contains(word)) {
            return true;
        }
        boolean result = true;
        for (String letter : StringUtils.wordToChars(word)) {
            if (!puncLetters.contains(letter) && !ClosedPosSets.isPuncCode(letter)) {
                result = false;
                break;
            }
        }
        return result;
    }

    private static boolean isPuncCode(String pos) {
        char[] charArray = pos.toCharArray();
        if (charArray.length != 1) {
            return false;
        }
        int codePoint = Character.codePointAt(charArray, 0);
        if ((codePoint >= '\u2000' && codePoint <= '\u206F') ||
                (codePoint >= '\u2E00' && codePoint <= '\u2E7F')) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isClosedChinesePosTag(String pos) {
        checkChineseClosedPosSet();
        return closedChinesePosSet.contains(pos);
    }

    public static boolean isOpenEnglishPosTag(String pos) {
        checkEnglishOpenPosSet();
        return openEnglishPosSet.contains(pos);
    }

    public static void main(String[] args) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")));
        String line = "";
        Set<String> puncs = new HashSet<String>();
        while ((line = reader.readLine()) != null) {
            for (String word : Arrays.asList(line.trim().split(" +"))) {
                if (isPunc(word)) {
                    puncs.add(word);
                }
            }
        }
        for (String punc : puncs) {
            System.out.println(punc);
        }
    }

    public static double overlapRate(String punc1, String punc2) {
        List<String> punc1Chars = StringUtils.wordToChars(punc1);
        List<String> punc2Chars = StringUtils.wordToChars(punc2);
        double overlap = 0;
        for (String punc1Char : punc1Chars) {
            if (punc2Chars.contains(punc1Char)) {
                overlap++;
            }
        }
        for (String punc2Char : punc2Chars) {
            if (punc1Chars.contains(punc2Char)) {
                overlap++;
            }
        }
        double l1 = punc1Chars.size();
        double l2 = punc2Chars.size();
        return Math.min(l1, l2) / Math.max(l1, l2) * overlap / (l1 + l2);
    }

    public static Set<String> getEnglishPuncTagsWithOverlap(String punc, Set<String> posTags) {
        Set<String> mostLikelyPuncTags = new HashSet<String>();
        checkEnglishPuncTagSet(posTags);
        for (String puncTag : englishPuncTagSet) {
            double score = overlapRate(punc, puncTag);
            if (score > 0) {
                mostLikelyPuncTags.add(puncTag);
            }
        }
        return mostLikelyPuncTags;
    }
}
