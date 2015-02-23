package java_cup;

import java.util.BitSet;

/**
 * A set of terminals.
 */
public class TerminalSet {

    protected BitSet _elements;

    public TerminalSet() {
        _elements = new BitSet(Terminal.size());
    }

    public TerminalSet(TerminalSet other) {
        _elements = (BitSet) other._elements.clone();
    }

    /**
     * Determine if the set is empty.
     */
    public boolean empty() {
        return _elements.isEmpty();
    }

    /**
     * Given its id determine if the set contains a particular Terminal.
     *
     * @param indx the id of the Terminal in question.
     */
    public boolean contains(int indx) {
        return _elements.get(indx);
    }

    /**
     * Determine if this set is an (improper) subset of another.
     *
     * @param other the set we are testing against.
     */
    public boolean isSubOf(TerminalSet other) {

        /* make a copy of the other set */
        BitSet copy_other = (BitSet) other._elements.clone();

        /* and or in */
        copy_other.or(_elements);

        /* if it hasn't changed, we were a subset */
        return copy_other.equals(other._elements);
    }

    /**
     * Add a single Terminal to the set.
     *
     * @param sym the Terminal being added.
     * @return true if this changes the set.
     */
    public boolean add(Terminal sym) {
        boolean result = _elements.get(sym.id);
        if (!result) {
            _elements.set(sym.id);
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
        BitSet copy = (BitSet) _elements.clone();

        /* or in the other set */
        _elements.or(other._elements);

        /* changed if we are not the same as the copy */
        return !_elements.equals(copy);
    }

    /**
     * Determine if this set intersects another.
     *
     * @param other the other set in question.
     */
    public boolean intersects(TerminalSet other) {
        BitSet copy = (BitSet) other._elements.clone();

        /* xor out our values */
        copy.xor(this._elements);

        /* see if its different */
        return !copy.equals(other._elements);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TerminalSet)) {
            return false;
        }
        return _elements.equals(((TerminalSet) other)._elements);
    }

    @Override
    public int hashCode() {
        return this._elements.hashCode();
    }

    @Override
    public String toString() {
        String result;
        boolean comma_flag;
        result = "{";
        comma_flag = false;
        for (int t = 0; t < Terminal.size(); t++) {
            if (_elements.get(t)) {
                if (comma_flag) {
                    result += ", ";
                } else {
                    comma_flag = true;
                }
                result += (Terminal.find(t).name);
            }
        }
        result += "}";

        return result;
    }
}
