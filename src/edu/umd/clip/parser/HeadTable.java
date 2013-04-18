package edu.umd.clip.parser;

import edu.umd.clip.ling.Tree;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeadTable {

    public static class HeadPecolationRule {

        final static boolean left = true;
        final static boolean right = false;
        boolean searchDirection;
        List<String> candidateList;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            if (searchDirection == left) {
                sb.append("l");
            } else {
                sb.append("r");
            }
            for (String candidate : candidateList) {
                sb.append(" ").append(candidate);
            }
            sb.append(")");
            return sb.toString();
        }

        public HeadPecolationRule(String rule) {
            this(Arrays.asList(rule.split("\\s+")));
        }

        public HeadPecolationRule(List<String> rule) {
            if (rule == null) {
                throw new RuntimeException(
                        "Error: I cannot work with empty head percolation rules.");
            }
            if (rule.get(0).equals("l")) {
                searchDirection = left;
            } else if (rule.get(0).equals("r")) {
                searchDirection = right;
            } else {
                throw new RuntimeException(
                        "Error: I cannot recognize the head search direction: " + rule.get(0));
            }
            candidateList = rule.subList(1, rule.size());
        }

        public Tree<String> lookupHead(List<Tree<String>> nodes) {
            String nodeName = "";
            int cutPosition = -1;
            if (searchDirection == left) {
                if (candidateList.size() == 0) {
                    return nodes.get(0);
                } else {
                    for (int i = 0; i < candidateList.size(); i++) {
                        String candidate = candidateList.get(i);
                        for (int j = 0; j < nodes.size(); j++) {
                            nodeName = nodes.get(j).getLabel();
                            cutPosition = nodeName.indexOf('^');
                            if (cutPosition != -1) {
                                nodeName = nodeName.substring(0, cutPosition);
                            }
                            if (candidate.equals(nodeName)) {
                                return nodes.get(j);
                            }
                        }
                    }
                    return null;
                }
            } else {
                if (candidateList.isEmpty()) {
                    return nodes.get(nodes.size() - 1);
                } else {
                    for (int i = 0; i < candidateList.size(); i++) {
                        String candidate = candidateList.get(i);
                        for (int j = nodes.size() - 1; j >= 0; j--) {
                            nodeName = nodes.get(j).getLabel();
                            cutPosition = nodeName.indexOf('^');
                            if (cutPosition != -1) {
                                nodeName = nodeName.substring(0, cutPosition);
                            }
                            if (candidate.equals(nodeName)) {
                                return nodes.get(j);
                            }
                        }
                    }
                    return null;
                }
            }
        }
    }
    private static HashMap<String, List<HeadPecolationRule>> headPercolationTable = null;

    public static void loadHeadTable(String headTableFile) throws IOException {
        headPercolationTable = new HashMap<String, List<HeadPecolationRule>>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(headTableFile), Charset.forName("UTF-8")));
        String line = "";
        while ((line = reader.readLine()) != null) {
            parseHeadPercolationRules(line);
        }
        if (!headPercolationTable.containsKey("*default*")) {
            throw new RuntimeException("Error: I did not find the default percolation rule.");
        }
    }

    private static void parseHeadPercolationRules(String line) {
        Pattern topP = Pattern.compile("^(\\S*)\\s+(.*)$");
        Pattern ruleP = Pattern.compile("^\\s*\\(([^()]+)\\)(.*)$");
        Matcher topM = topP.matcher(line);
        if (!topM.matches()) {
            throw new RuntimeException("Error: I cannot recognize the head rule entry: " + line);
        }
        String parent = topM.group(1);
        String allRules = topM.group(2);
        List<HeadPecolationRule> headPercolationRules = null;
        if (headPercolationTable.containsKey(parent)) {
            headPercolationRules = headPercolationTable.get(parent);
        } else {
            headPercolationRules = new ArrayList<HeadPecolationRule>();
            headPercolationTable.put(parent, headPercolationRules);
        }
        while (true) {
            Matcher ruleM = ruleP.matcher(allRules);
            if (!ruleM.matches()) {
                break;
            }
            headPercolationRules.add(new HeadPecolationRule(ruleM.group(1)));
            allRules = ruleM.group(2);
        }
    }

    public static String getHeadWord(String nodeName, List<Tree<String>> headList) {
        List<HeadPecolationRule> headPercolationRules = null;
        int cutPosition = nodeName.indexOf('^');
        if (cutPosition != -1) {
            nodeName = nodeName.substring(0, cutPosition);
        }
        if (headPercolationTable.containsKey(nodeName)) {
            headPercolationRules = headPercolationTable.get(nodeName);
        }
        if (headPercolationRules == null)
            headPercolationRules = new ArrayList<HeadPecolationRule>();
        headPercolationRules.addAll(headPercolationTable.get("*default*"));
        for (HeadPecolationRule rule : headPercolationRules) {
            Tree<String> head = rule.lookupHead(headList);
            if (head != null) {
                return head.getExtraLabel();
            }
        }
        throw new RuntimeException("Error: I cannot find the head percolation rule for: " + nodeName + "->" + headList);
    }
}
