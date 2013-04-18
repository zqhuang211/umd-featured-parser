/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.clip.tools;

/**
 *
 * @author zqhuang
 */
public class Bracket {
    int start;
    int end;
    String label;
    int count;

    public String getLabel() {
        return label;
    }

    public Bracket(int start, int end, String label) {
        this.start = start;
        this.end = end;
        this.label = label;
        count = 1;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
    
    @Override
    public int hashCode() {
        return label.hashCode() << 16 + start << 8 + end;
    }

    public int getSpanKey() {
        return start << 8 + end;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Bracket other = (Bracket) obj;
        if (this.start != other.start) {
            return false;
        }
        if (this.end != other.end) {
            return false;
        }
        if (this.label == null || !this.label.equals(other.label)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return label+"("+start+","+end+")";
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void increaseCount() {
        count++;
    }

    public void increaseCount(int add) {
        count += add;
    }
}
