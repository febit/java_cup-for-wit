package java_cup;

import java.util.BitSet;

/**
 * A set of terminals.
 */
public class TerminalSet {

    protected BitSet datas;

    public TerminalSet() {
        datas = new BitSet(Terminal.size());
    }

    public TerminalSet(TerminalSet other) {
        datas = (BitSet) other.datas.clone();
    }

    /**
     * Determine if the set is empty.
     */
    public boolean empty() {
        return datas.isEmpty();
    }

    /**
     * Given its id determine if the set contains a particular Terminal.
     *
     * @param indx the id of the Terminal in question.
     */
    public boolean contains(int indx) {
        return datas.get(indx);
    }

    /**
     * Determine if this set is an (improper) subset of another.
     *
     * @param other the set we are testing against.
     */
    public boolean isSubOf(TerminalSet other) {

        /* make a copy of the other set */
        BitSet copy_other = (BitSet) other.datas.clone();

        /* and or in */
        copy_other.or(datas);

        /* if it hasn't changed, we were a subset */
        return copy_other.equals(other.datas);
    }

    /**
     * Add a single Terminal to the set.
     *
     * @param sym the Terminal being added.
     * @return true if this changes the set.
     */
    public boolean add(Terminal sym) {
        boolean result = datas.get(sym.id);
        if (!result) {
            datas.set(sym.id);
        }
        return result;
    }

    /**
     * Add (union) in a complete set.
     *
     * @param other the set being added.
     * @return true if this changes the set.
     */
    public boolean add(TerminalSet other) {
        /* make a copy */
        BitSet copy = (BitSet) datas.clone();

        /* or in the other set */
        datas.or(other.datas);

        /* changed if we are not the same as the copy */
        return !datas.equals(copy);
    }

    /**
     * Determine if this set intersects another.
     *
     * @param other the other set in question.
     */
    public boolean intersects(TerminalSet other) {
        return datas.intersects(other.datas);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TerminalSet)) {
            return false;
        }
        return datas.equals(((TerminalSet) other).datas);
    }

    @Override
    public int hashCode() {
        return this.datas.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('{');
        boolean comma_flag = false;
        for (int t = 0; t < Terminal.size(); t++) {
            if (datas.get(t)) {
                if (comma_flag) {
                    result.append(',');
                } else {
                    comma_flag = true;
                }
                result.append(Terminal.get(t).name);
            }
        }
        return result.append('}').toString();
    }
}
