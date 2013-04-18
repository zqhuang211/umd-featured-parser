/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.latagger;

import edu.umd.clip.parser.Converter;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author zqhuang
 */
public class ClosedPosSets {

    private static Set<String> puncLetters;
    private static Set<String> puncSet;
    private static Set<String> closedChinesePosSet;

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
        boolean rslt = true;
        for (String letter : Converter.stringToCharacters(word)) {
            if (!puncLetters.contains(letter) && !ClosedPosSets.isPuncCode(letter)) {
                rslt = false;
                break;
            }
        }
        return rslt;
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
}
