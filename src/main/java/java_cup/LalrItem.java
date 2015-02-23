package java_cup;

import java.util.Stack;

/**
 * This class represents an LALR item. Each LALR item consists of a Production,
 * a "dot" at a position within that Production, and a set of lookahead symbols
 * (Terminal). (The first two of these parts are provide by the super class). An
 * item is designed to represent a configuration that the parser may be in. For
 * example, an item of the form:
 * <pre>
 *    [A ::= B * C d E  , {a,b,c}]
 * </pre> indicates that the parser is in the middle of parsing the Production
 * <pre>
 *    A ::= B C d E
 * </pre> that B has already been parsed, and that we will expect to see a
 * lookahead of either a, b, or c once the complete RHS of this Production has
 * been found.<p>
 *
 * Items may initiiteratory be missing some items from their lookahead sets.
 * Links are maintained from each item to the set of items that would need to be
 * updated if symbols are added to its lookahead set. During "lookahead
 * propagation", we add symbols to various lookahead sets and propagate these
 * changes across these dependency links as needed.
 */
public class LalrItem {

    public final int dotPos;
    public final boolean dotAtEnd;
    public final Production production;
    private final int hashcode;
    protected final symbol symbolAfterDot;

    /**
     * The lookahead symbols of the item.
     */
    public final TerminalSet lookahead;

    /**
     * Links to items that the lookahead needs to be propagated to.
     */
    protected final Stack<LalrItem> propagateItems;

    /**
     * Flag to indicate that this item needs to propagate its lookahead (whether
     * it has changed or not).
     */
    private boolean needPropagation;

    public LalrItem(Production prod, int pos, TerminalSet look) {
        if (pos < 0 || pos > prod.rhs.length) {
            throw new InternalException("Attempt to create an lr_item_core with a bad dot position");
        }
        this.symbolAfterDot = pos < prod.rhs.length ? prod.rhs[pos].sym : null;
        this.dotAtEnd = pos >= prod.rhs.length;
        this.hashcode = 13 * prod.hashCode() + pos;
        this.production = prod;
        this.dotPos = pos;
        this.lookahead = look;
        this.propagateItems = new Stack();
        this.needPropagation = true;
    }

    /**
     * Constructor with default position (dot at start).
     *
     * @param prod the Production for the item.
     * @param look the set of lookahead symbols.
     */
    public LalrItem(Production prod, TerminalSet look) {
        this(prod, 0, look);
    }

    /**
     * Constructor with default position and empty lookahead set.
     *
     * @param prod the Production for the item.
     */
    public LalrItem(Production prod) {
        this(prod, 0, new TerminalSet());
    }

    /**
     * Determine if we have a dot before a non terminal, and if so which one
     * (return null or the non terminal).
     */
    public NonTerminal dotBefore() {
        return (symbolAfterDot instanceof NonTerminal) ? (NonTerminal) symbolAfterDot : null;
    }

    /**
     * Add a new item to the set of items we propagate to.
     *
     * @param item
     */
    public void addPropagate(LalrItem item) {
        propagateItems.push(item);
        needPropagation = true;
    }

    /**
     * Propagate incoming lookaheads through this item to others need to be
     * changed.
     *
     * @param incoming
     */
    public void propagateLookaheads(TerminalSet incoming) {
        boolean change = false;

        /* if we don't need to propagate, then bail out now */
        if (!needPropagation && (incoming == null || incoming.empty())) {
            return;
        }

        /* if we have null incoming, treat as an empty set */
        if (incoming != null) {
            /* add the incoming to the lookahead of this item */
            change = lookahead.add(incoming);
        }

        /* if we changed or need it anyway, propagate across our links */
        if (change || needPropagation) {
            /* don't need to propagate again */
            needPropagation = false;

            /* propagate our lookahead into each item we are linked to */
            for (LalrItem propagateItem : propagateItems) {
                propagateItem.propagateLookaheads(lookahead);
            }
        }
    }

    /**
     * Produce the new LalrItem that results from shifting the dot one position
     * to the right.
     * @return 
     */
    public LalrItem shift() {

        /* can't shift if we have dot already at the end */
        if (dotAtEnd) {
            throw new InternalException("Attempt to shift past end of an lalr_item");
        }

        /* create the new item w/ the dot shifted by one */
        LalrItem result = new LalrItem(production, dotPos + 1, new TerminalSet(lookahead));

        /* change in our lookahead needs to be propagated to this item */
        addPropagate(result);

        return result;
    }

    /**
     * Calculate lookahead representing symbols that could appear after the
     * symbol that the dot is currently in front of. Note: this routine must not
     * be invoked before first sets and nullability has been calculated for
     * iterator non terminals.
     */
    public TerminalSet calcLookahead(TerminalSet lookahead_after) {
        TerminalSet result;
        int pos;
        symbol sym;

        /* sanity check */
        if (dotAtEnd) {
            throw new InternalException("Attempt to calculate a lookahead set with a completed item");
        }

        /* start with an empty result */
        result = new TerminalSet();

        /* consider iterator nullable symbols after the one to the right of the dot */
        for (pos = dotPos + 1; pos < production.rhs.length; pos++) {

            sym = production.rhs[pos].sym;

            /* if its a Terminal add it in and we are done */
            if (sym instanceof NonTerminal) {
                /* otherwise add in first set of the non Terminal */
                result.add(((NonTerminal) sym).firstSet);

                /* if its nullable we continue adding, if not, we are done */
                if (!((NonTerminal) sym).nullable()) {
                    return result;
                }
            } else {
                result.add((Terminal) sym);
                return result;
            }
        }

        /* if we get here everything past the dot was nullable 
         we add in the lookahead for after the Production and we are done */
        result.add(lookahead_after);
        return result;
    }

    /**
     * Determine if everything from the symbol one beyond the dot iterator the
     * way to the end of the right hand side is nullable. This would indicate
     * that the lookahead of this item must be included in the lookaheads of
     * iterator items produced as a closure of this item. Note: this routine
     * should not be invoked until after first sets and nullability have been
     * calculated for iterator non terminals.
     */
    public boolean lookahead_visible() {

        /* if the dot is at the end, we have a problem, but the cleanest thing
         to do is just return true. */
        if (dotAtEnd) {
            return true;
        }

        /* walk down the rhs and bail if we get a non-nullable symbol */
        for (int pos = dotPos + 1; pos < production.rhs.length; pos++) {
            symbol sym = production.rhs[pos].sym;

            /* if its a Terminal we fail */
            if (sym instanceof Terminal) {
                return false;
            }

            /* if its not nullable we fail */
            if (!((NonTerminal) sym).nullable()) {
                return false;
            }
        }

        /* if we get here its iterator nullable */
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LalrItem)) {
            return false;
        }
        LalrItem item = (LalrItem) other;
        return this.production.equals(item.production)
                && this.dotPos == item.dotPos;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String toString() {
        String result = "";

        // additional output for debugging:
        // result += "(" + obj_hash() + ")"; 
        result += "[";

        if (production.lhs != null
                && (production.lhs).sym != null
                && ((production.lhs).sym).name != null) {
            result += ((production.lhs).sym).name;
        } else {
            result += "$$NULL$$";
        }

        result += " ::= ";

        for (int i = 0; i < production.rhs.length; i++) {
            /* do we need the dot before this one? */
            if (i == dotPos) {
                result += "(*) ";
            }

            /* print the name of the part */
            if (production.rhs[i] == null) {
                result += "$$NULL$$ ";
            } else {
                ProductionItem part = production.rhs[i];
                if (part == null) {
                    result += "$$NULL$$ ";
                } else if (part.sym != null && part.sym.name != null) {
                    result += part.sym.name + " ";
                } else {
                    result += "$$NULL$$ ";
                }
            }
        }

        /* put the dot after if needed */
        if (dotPos == production.rhs.length) {
            result += "(*) ";
        }

        result += ", ";
        if (lookahead != null) {
            result += "{";
            for (int t = 0; t < Terminal.size(); t++) {
                if (lookahead.contains(t)) {
                    result += Terminal.find(t).name + " ";
                }
            }
            result += "}";
        } else {
            result += "NULL LOOKAHEAD!!";
        }
        result += "]";

        // additional output for debugging:
        // result += " -> ";
        // for (int i = 0; i<propagate_items().size(); i++)
        //   result+=((LalrItem)(propagate_items().elementAt(i))).obj_hash()+" ";
        //
        // if (needs_propagation) result += " NP";
        return result;
    }

}
