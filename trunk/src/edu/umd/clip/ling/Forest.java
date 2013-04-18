/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.ling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author zqhuang
 */
public class Forest<L> {

    private static final long serialVersionUID = 1L;
    private L label;
    private List<List<Forest<L>>> childrenList;
    private int start;
    private int end;
    private boolean visited;
    private boolean pruned;

    public boolean isPruned() {
        return pruned;
    }

    public void setPruned(boolean pruned) {
        this.pruned = pruned;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public void setVisitedAll(boolean visited) {
        if (this.visited == visited) {
            return;
        }
        this.visited = visited;
        for (List<Forest<L>> children : childrenList) {
            for (Forest<L> child : children) {
                child.setVisitedAll(visited);
            }
        }
    }

    public boolean checkVisitedAll(boolean visited) {
        if (this.visited != visited) {
            return false;
        }
        for (List<Forest<L>> children : childrenList) {
            for (Forest<L> child : children) {
                if (!child.checkVisitedAll(visited)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setChildrenList(List<List<Forest<L>>> childrenList) {
        this.childrenList = childrenList;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setLabel(L label) {
        this.label = label;
    }

    public List<List<Forest<L>>> getChildrenList() {
        return childrenList;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public L getLabel() {
        return label;
    }

    public boolean isLeaf() {
        if (childrenList.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public Forest(L label, int start, int end, List<List<Forest<L>>> childrenList) {
        this.label = label;
        this.start = start;
        this.end = end;
        this.childrenList = childrenList;
    }

    public Forest(L label, int start, int end) {
        this(label, start, end, Collections.EMPTY_LIST);
    }

    public Forest(Tree<L> tree) {
        label = tree.getLabel();
        start = tree.getStart();
        end = tree.getEnd();
        if (tree.isLeaf()) {
            childrenList = Collections.EMPTY_LIST;
        } else {
            List<Forest<L>> children = new ArrayList<Forest<L>>();
            for (Tree<L> child : tree.getChildren()) {
                children.add(new Forest<L>(child));
            }
            childrenList = Collections.singletonList(children);
        }
    }

    @Override
    public String toString() {
        return label + "(" + start + "," + end + ")";
    }
}


