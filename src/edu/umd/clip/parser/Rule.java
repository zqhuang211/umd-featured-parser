package edu.umd.clip.parser;

import java.io.Serializable;

public class Rule implements Serializable {

    private static final long serialVersionUID = 1L;

    protected int parent = -1;
    

    public int getParent() {
        return parent;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    public boolean isUnary() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Rule other = (Rule) obj;
        if (this.parent != other.parent) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.parent;
        return hash;
    }
}
