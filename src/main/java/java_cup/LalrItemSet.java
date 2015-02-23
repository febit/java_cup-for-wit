package java_cup;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * This class represents a set of LALR items. For purposes of building these
 * sets, items are considered unique only if they have unique cores (i.e.,
 * ignoring differences in their lookahead sets). This class provides fairly
 * conventional set oriented operations (union, sub/super-set tests, etc.), as
 * well as an LALR "closure" operation (see computeClosure()).
 */
public class LalrItemSet {

    protected final HashMap<LalrItem, LalrItem> _all;

    public LalrItemSet() {
        this._all = new HashMap<LalrItem, LalrItem>(11);
    }

    /**
     * Constructor for cloning from another set.
     *
     * @param other indicates set we should copy from.
     */
    public LalrItemSet(LalrItemSet other) {
        _all = new HashMap<LalrItem, LalrItem>(other._all);
    }

    public Iterable<LalrItem> all() {
        return _all.values();
    }

    /**
     * Return the item in the set matching a particular item (or null if not
     * found)
     *
     * @param itm the item we are looking for.
     */
    public LalrItem find(LalrItem itm) {
        return _all.get(itm);
    }

    /**
     * Add a singleton item, merging lookahead sets if the item is already part
     * of the set. returns the element of the set that was added or merged into.
     *
     * @param itm the item being added.
     */
    public LalrItem add(LalrItem itm) {
        LalrItem other = _all.get(itm);

        /* if so, merge this lookahead into the original and leave it */
        if (other != null) {
            (other.lookahead).add(itm.lookahead);
            return other;
        } else {
            hashcode = 0;

            _all.put(itm, itm);
            return itm;
        }
    }

    /**
     * Compute the closure of the set using the LALR closure rules. Basically
     * for every item of the form:
     * <pre>
     *    [L ::= a *N alpha, l]
     * </pre> (where N is a a non terminal and alpha is a string of symbols)
     * make sure there are also items of the form:
     * <pre>
     *    [N ::= *beta, first(alpha l)]
     * </pre> corresponding to each Production of N. Items with identical cores
     * but differing lookahead sets are merged by creating a new item with the
     * same core and the union of the lookahead sets (the LA in LALR stands for
     * "lookahead merged" and this is where the merger is). This routine assumes
     * that nullability and first sets have been computed for iterator
     * productions before it is called.
     */
    public void computeClosure() {
        hashcode = 0;
        LinkedList<LalrItem> items = new LinkedList<LalrItem>(this._all.values());
        while (!items.isEmpty()) {
            LalrItem item = items.removeFirst();
            NonTerminal nt = item.dotBefore();
            if (nt != null) {
                /* create the lookahead set based on first after dot */
                TerminalSet newLookahead = item.calcLookahead(item.lookahead);

                /* are we going to need to propagate our lookahead to new item */
                boolean needProp = item.lookahead_visible();

                /* create items for each Production of that non term */
                for (Production prod : nt.productions) {

                    /* create new item with dot at start and that lookahead */
                    LalrItem newItem = new LalrItem(prod, new TerminalSet(newLookahead));
                    LalrItem added = add(newItem);
                    /* if propagation is needed link to that item */
                    if (needProp) {
                        item.addPropagate(added);
                    }

                    /* was this was a new item*/
                    if (added == newItem) {
                        /* that may need further closure, consider it also */
                        items.add(newItem);
                    }
                }
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LalrItemSet)) {
            return false;
        }
        LalrItemSet set = (LalrItemSet) other;
        if (_all.size() != set._all.size()) {
            return false;
        }
        return this._all.keySet().containsAll(set._all.keySet());
    }

    private int hashcode = 0;

    @Override
    public int hashCode() {
        if (hashcode == 0) {
            int result = 0;
            for (LalrItem item : all()) {
                result ^= item.hashCode();
            }
            hashcode = result == 0 ? -1 : result;
        }
        return hashcode;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("{\n");
        for (LalrItem item : all()) {
            result.append(' ').append(item).append("\n");
        }
        result.append("}");
        return result.toString();
    }

}
