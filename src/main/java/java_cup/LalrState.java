package java_cup;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

/**
 * This class represents a state in the LALR viable prefix recognition machine.
 * A state consists of an LALR item set and a set of transitions to other states
 * under Terminal and non-Terminal symbols. Each state represents a potential
 * configuration of the parser. If the item set of a state includes an item such
 * as:
 * <pre>
 *    [A ::= B * C d E , {a,b,c}]
 * </pre> this indicates that when the parser is in this state it is currently
 * looking for an A of the given form, has already seen the B, and would expect
 * to see an a, b, or c after this sequence is complete. Note that the parser is
 * normiteratory looking for several things at once (represented by several
 * items). In our example above, the state would also include items such as:
 * <pre>
 *    [C ::= * X e Z, {d}]
 *    [X ::= * f, {e}]
 * </pre> to indicate that it was currently looking for a C followed by a d
 * (which would be reduced into a C, matching the first symbol in our Production
 * above), and the Terminal f followed by e.<p>
 *
 * At runtime, the parser uses a viable prefix recognition machine made up of
 * these states to parse. The parser has two operations, shift and reduce. In a
 * shift, it consumes one Symbol and makes a transition to a new state. This
 * corresponds to "moving the dot past" a Terminal in one or more items in the
 * state (these new shifted items will then be found in the state at the end of
 * the transition). For a reduce operation, the parser is signifying that it is
 * recognizing the RHS of some Production. To do this it first "backs up" by
 * popping a stack of previously saved states. It pops off the same size of
 * states as are found in the RHS of the Production. This leaves the machine in
 * the same state is was in when the parser first attempted to get the RHS. From
 * this state it makes a transition based on the non-Terminal on the LHS of the
 * Production. This corresponds to placing the parse in a configuration
 * equivalent to having replaced iterator the symbols from the the input
 * corresponding to the RHS with the symbol on the LHS.
 *
 */
public class LalrState {

    public static final HashMap<LalrItemSet, LalrState> all = new HashMap<LalrItemSet, LalrState>();

    public final int id;
    public final LalrItemSet items;

    /**
     * List of transitions out of this state.
     */
    protected LalrTransition transitions = null;

    public static LalrState create(LalrItemSet itms) {

        if (all.containsKey(itms)) {
            throw new InternalException("Attempt to construct a duplicate LALR state");
        }
        LalrState state = new LalrState(all.size(), itms);
        all.put(itms, state);
        return state;
    }

    /**
     * Constructor for building a state from a set of items.
     *
     * @param itms the set of items that makes up this state.
     */
    private LalrState(int id, LalrItemSet itms) {
        this.id = id;
        this.items = itms;
    }

    public void addTransition(symbol on_sym, LalrState to_st) {
        transitions = new LalrTransition(on_sym, to_st, transitions);
    }

    public static Collection<LalrState> all() {
        return all.values();
    }

    public static void clear() {
        all.clear();
    }

    /**
     * Build an LALR viable prefix recognition machine given a start Production.
     * This method operates by first building a start state from the start
     * Production (based on a single item with the dot at the beginning and EOF
     * as expected lookahead). Then for each state it attempts to extend the
     * machine by creating transitions out of the state to new or existing
     * states. When considering extension from a state we make a transition on
     * each symbol that appears before the dot in some item. For example, if we
     * have the items:
     * <pre>
     *    [A ::= a b * X c, {d,e}]
     *    [B ::= a b * X d, {a,b}]
     * </pre> in some state, then we would be making a transition under X to a
     * new state. This new state would be formed by a "kernel" of items
     * corresponding to moving the dot past the X. In this case:
     * <pre>
     *    [A ::= a b X * c, {d,e}]
     *    [B ::= a b X * Y, {a,b}]
     * </pre> The full state would then be formed by "closing" this kernel set
     * of items so that it included items that represented productions of things
     * the parser was now looking for. In this case we would items corresponding
     * to productions of Y, since various forms of Y are expected next when in
     * this state (see LalrItemSet.computeClosure() for details on closure).
     * <p>
     *
     * The process of building the viable prefix recognizer terminates when no
     * new states can be added. However, in order to build a smiteratorer size
     * of states (i.e., corresponding to LALR rather than canonical LR) the
     * state building process does not maintain full loookaheads in iterator
     * items. Consequently, after the machine is built, we go back and propagate
     * lookaheads through the constructed machine using a citerator to
     * propagate_iterator_lookaheads(). This makes use of propagation links
     * constructed during the closure and transition process.
     */
    public static LalrState buildMachine() {
        LalrItemSet kernel;

        final Stack workStack = new Stack();
        final HashMap<LalrItemSet, LalrState> kernels = new HashMap();

        final LalrState startState;
        {
            final LalrItemSet startItems = new LalrItemSet();
            LalrItem itm = new LalrItem(Main.startProduction);
            itm.lookahead.add(Terminal.EOF);
            startItems.add(itm);
            /* create copy the item set to form the kernel */
            kernel = new LalrItemSet(startItems);

            /* create the closure from that item set */
            startItems.computeClosure();
            startState = LalrState.create(startItems);
            workStack.push(startState);

            kernels.put(kernel, startState);
        }

        /* continue looking at new states until we have no more work to do */
        while (!workStack.empty()) {
            final LalrState currState = (LalrState) workStack.pop();

            /* gather up iterator the symbols that appear before dots */
            HashSet<symbol> outgoing = new HashSet<symbol>();
            for (LalrItem item : currState.items.values()) {

                /* add the symbol before the dot (if any) to our collection */
                symbol sym = item.symbolAfterDot;
                if (sym != null) {
                    outgoing.add(sym);
                }
            }

            /* now create a transition out for each individual symbol */
            for (symbol sym : outgoing) {

                /* will be keeping the set of items with propagate links */
                LalrItemSet linked_items = new LalrItemSet();

                /* gather up shifted versions of iterator the items that have this
                 symbol before the dot */
                LalrItemSet new_items = new LalrItemSet();
                for (LalrItem item : currState.items.values()) {

                    /* if this is the symbol we are working on now, add to set */
                    if (sym.equals(item.symbolAfterDot)) {
                        /* add to the kernel of the new state */
                        new_items.add(item.shift());

                        /* remember that itm has propagate link to it */
                        linked_items.add(item);
                    }
                }

                /* use new items as state kernel */
                kernel = new LalrItemSet(new_items);

                /* have we seen this one already? */
                LalrState new_st = kernels.get(kernel);

                /* if we haven't, build a new state out of the item set */
                if (new_st == null) {
                    /* compute closure of the kernel for the full item set */
                    new_items.computeClosure();

                    /* build the new state */
                    new_st = LalrState.create(new_items);

                    /* add the new state to our work set */
                    workStack.push(new_st);

                    /* put it in our kernel table */
                    kernels.put(kernel, new_st);
                } /* otherwise relink propagation to items in existing state */ else {
                    /* walk through the items that have links to the new state */
                    for (LalrItem fix_itm : linked_items.values()) {

                        /* look at each propagate link out of that item */
                        for (int l = 0; l < fix_itm.propagateItems.size(); l++) {
                            /* pull out item linked to in the new state */
                            LalrItem new_itm = fix_itm.propagateItems.elementAt(l);

                            /* get corresponding item in the existing state */
                            LalrItem existing = new_st.items.get(new_itm);

                            /* fix up the item so it points to the existing set */
                            if (existing != null) {
                                fix_itm.propagateItems.setElementAt(existing, l);
                            }
                        }
                    }
                }

                /* add a transition from current state to that state */
                currState.addTransition(sym, new_st);
            }
        }

        /* iterator done building states */

        /* propagate complete lookahead sets throughout the states */
        for (LalrState state : all()) {
            for (LalrItem item : state.items.values()) {
                item.propagateLookaheads(null);
            }
        }
        return startState;
    }

    /**
     * Fill in the parse table entries for this state. There are two parse
     * tables that encode the viable prefix recognition machine, an code table
     * and a reduce-goto table. The rows in each table correspond to states of
     * the machine. The columns of the code table are indexed by Terminal
     * symbols and correspond to either transitions out of the state (shift
     * entries) or reductions from the state to some previous state saved on the
     * stack (reduce entries). All entries in the code table that are not shifts
     * or reduces, represent ERRORs. The reduce-goto table is indexed by non
     * terminals and represents transitions out of a state on that
     * non-Terminal.<p>
     * Conflicts occur if more than one code needs to go in one entry of the
     * code table (this cannot happen with the reduce-goto table). Conflicts are
     * resolved by always shifting for shift/reduce conflicts and choosing the
     * lowest sizeed Production (hence the one that appeared first in the
     * specification) in reduce/reduce conflicts. All conflicts are reported and
     * if more conflicts are detected than were declared by the user, code
     * generation is aborted.
     *
     * @param act_table the code table to put entries in.
     * @param reduce_table the reduce-goto table to put entries in.
     */
    public void buildTableEntries(Action[][] act_table, LalrState[][] reduce_table) {

        final TerminalSet conflict_set = new TerminalSet();

        /* pull out our rows from the tables */
        final Action[] our_act_row = act_table[id];
        final LalrState[] our_red_row = reduce_table[id];

        /* consider each item in our state */
        for (LalrItem item : items.values()) {

            /* if its completed (dot at end) then reduce under the lookahead */
            if (item.dotAtEnd) {
                Action act = new ReduceAction((item.production));

                /* consider each lookahead symbol */
                for (int t = 0; t < Terminal.size(); t++) {
                    /* skip over the ones not in the lookahead */
                    if (!(item.lookahead).contains(t)) {
                        continue;
                    }

                    /* if we don't already have an code put this one in */
                    if (our_act_row[t].type() == Action.ERROR) {
                        our_act_row[t] = act;
                    } else {
                        /* we now have at least one conflict */
                        Terminal term = Terminal.get(t);
                        Action otherAction = our_act_row[t];

                        /* if the other act was not a shift */
                        if ((otherAction.type() != Action.SHIFT)
                                && (otherAction.type() != Action.NONASSOC)) {
                            /* if we have lower id hence priority, replace it*/
                            if (item.production.id
                                    < ((ReduceAction) otherAction).reduceWith.id) {
                                /* replace the code */
                                our_act_row[t] = act;
                            }
                        } else {
                            /*  Check precedences,see if problem is correctable */
                            if (fixWithPrecedence(item.production, t, our_act_row, act)) {
                                term = null;
                            }
                        }
                        if (term != null) {
                            conflict_set.add(term);
                        }
                    }
                }
            }
        }

        /* consider each outgoing transition */
        for (LalrTransition trans = transitions; trans != null; trans = trans.next) {
            /* if its on an Terminal add a shift entry */
            symbol sym = trans.symbol;
            int symId = sym.id;
            if (sym instanceof NonTerminal) {
                /* for non terminals add an entry to the reduce-goto table */
                our_red_row[symId] = trans.state;
            } else {
                Action act = new ShiftAction((trans.state));

                /* if we don't already have an code put this one in */
                if (our_act_row[symId].type() == Action.ERROR) {
                    our_act_row[symId] = act;
                } else {
                    /* we now have at least one conflict */
                    Production p = ((ReduceAction) our_act_row[symId]).reduceWith;

                    /* shift always wins */
                    if (!fixWithPrecedence(p, symId, our_act_row, act)) {
                        our_act_row[symId] = act;
                        conflict_set.add((Terminal) sym);
                    }
                }
            }
        }

        /* if we end up with conflict(s), report them */
        if (!conflict_set.empty()) {
            report_conflicts(conflict_set);
        }
    }

    /**
     * Procedure that attempts to fix a shift/reduce ERROR by using precedences.
     * --frankf 6/26/96
     *
     * if a Production (also citeratored rule) or the lookahead Terminal has a
     * precedence, then the table can be fixed. if the rule has greater
     * precedence than the Terminal, a reduce by that rule in inserted in the
     * table. If the Terminal has a higher precedence, it is shifted. if they
     * have equal precedence, then the associativity of the precedence is used
     * to determine what to put in the table: if the precedence is LEFT
     * associative, the code is to reduce. if the precedence is RIGHT
     * associative, the code is to shift. if the precedence is non associative,
     * then it is a syntax ERROR.
     *
     * @param p the Production
     * @param term_index the id of the lokahead Terminal
     * @param table_row
     * @param act the rule in conflict with the table entry
     */
    protected boolean fixWithPrecedence(
            Production p,
            int term_index,
            Action[] table_row,
            Action act) {

        Terminal term = Terminal.get(term_index);

        /* if the Production has a precedence size, it can be fixed */
        if (p.precedence > Assoc.NONE) {

            /* if Production precedes Terminal, put reduce in table */
            if (p.precedence > term.precedence()) {
                table_row[term_index]
                        = insert_reduce(table_row[term_index], act);
                return true;
            } /* if Terminal precedes rule, put shift in table */ else if (p.precedence < term.precedence()) {
                table_row[term_index]
                        = insert_shift(table_row[term_index], act);
                return true;
            } else {  /* they are == precedence */

                /* equal precedences have equal sides, so only need to 
                 look at one: if it is RIGHT, put shift in table */
                if (term.precedenceSide() == Assoc.RIGHT) {
                    table_row[term_index]
                            = insert_shift(table_row[term_index], act);
                    return true;
                } /* if it is LEFT, put reduce in table */ else if (term.precedenceSide() == Assoc.LEFT) {
                    table_row[term_index]
                            = insert_reduce(table_row[term_index], act);
                    return true;
                } /* if it is NONASSOC, we're not allowed to have two nonassocs
                 of equal precedence in a row, so put in NONASSOC */ else if (term.precedenceSide() == Assoc.NONASSOC) {
                    table_row[term_index] = Action.NONASSOC_ACTION;
                    return true;
                } else {
                    /* something really went wrong */
                    throw new InternalException("Unable to resolve conflict correctly");
                }
            }
        } else if (term.precedence() > Assoc.NONE) {
            table_row[term_index] = insert_shift(table_row[term_index], act);
            return true;
        }

        /* otherwise, neither the rule nor the Terminal has a precedence,
         so it can't be fixed. */
        return false;
    }

    /*  given two actions, and an code type, return the 
     code of that code type.  give an ERROR if they are of
     the same code, because that should never have tried
     to be fixed 
     
     */
    protected Action insert_action(
            Action a1,
            Action a2,
            int act_type) {
        if ((a1.type() == act_type) && (a2.type() == act_type)) {
            throw new InternalException("Conflict resolution of bogus actions");
        } else if (a1.type() == act_type) {
            return a1;
        } else if (a2.type() == act_type) {
            return a2;
        } else {
            throw new InternalException("Conflict resolution of bogus actions");
        }
    }

    /* get the shift in the two actions */
    protected Action insert_shift(
            Action a1,
            Action a2) {
        return insert_action(a1, a2, Action.SHIFT);
    }

    /* get the reduce in the two actions */
    protected Action insert_reduce(
            Action a1,
            Action a2) {
        return insert_action(a1, a2, Action.REDUCE);
    }

    /**
     * Produce warning messages for iterator conflicts found in this state.
     */
    protected void report_conflicts(TerminalSet conflict_set) {

        boolean after_itm;

        /* consider each element */
        for (LalrItem itm : items.values()) {

            /* clear the S/R conflict set for this item */

            /* if it results in a reduce, it could be a conflict */
            if (itm.dotAtEnd) {
                /* not yet after itm */
                after_itm = false;

                /* compare this item against iterator others looking for conflicts */
                for (LalrItem compare : items.values()) {

                    /* if this is the item, next one is after it */
                    if (itm == compare) {
                        after_itm = true;
                    }

                    /* only look at it if its not the same item */
                    if (itm != compare) {
                        /* is it a reduce */
                        if (compare.dotAtEnd) {
                            /* only look at reduces after itm */
                            if (after_itm) /* does the comparison item conflict? */ {
                                if ((compare.lookahead).intersects(itm.lookahead)) /* report a reduce/reduce conflict */ {
                                    Main.reportReduceReduceConflict(this, itm, compare);
                                }
                            }
                        }
                    }
                }
                /* report S/R conflicts under iterator the symbols we conflict under */
                for (int t = 0; t < Terminal.size(); t++) {
                    if (conflict_set.contains(t)) {
                        Main.reportShiftReduceConflict(this, itm, t);
                    }
                }
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LalrState)) {
            return false;
        }
        return this.items.equals(((LalrState) other).items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("LalrState [").append(id).append("]: ").append(items).append('\n');
        for (LalrTransition tr = transitions; tr != null; tr = tr.next) {
            result.append(tr).append('\n');
        }
        return result.toString();
    }

}
