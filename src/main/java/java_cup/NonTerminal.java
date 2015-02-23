package java_cup;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * This class represents a non-terminal symbol in the grammar. Each non terminal
 * has a textual name, an id, and a string which indicates the type of object it
 * will be implemented with at runtime (i.e. the class of object that will be
 * pushed on the parse stack to represent it).
 */
public class NonTerminal extends symbol {

    public static final ArrayList<NonTerminal> all = new ArrayList<NonTerminal>();
    public static NonTerminal START;

    public static void clear() {
        all.clear();
        START = new NonTerminal("$START", null);
    }

    public final HashSet<Production> productions = new HashSet<Production>(11);
    public final TerminalSet firstSet = new TerminalSet();

    protected boolean _nullable;

    public NonTerminal(String nm, String tp) {
        super(all.size(), nm, tp);
        all.add(this);
    }

    public boolean nullable() {
        return _nullable;
    }

    public boolean looksNullable() {
        for (Production prod : productions) {
            if (prod.checkNullable()) {
                return _nullable = true;
            }
        }
        return false;
    }
}
