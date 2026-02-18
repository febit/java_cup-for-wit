package java_cup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents a non-terminal symbol in the grammar. Each non terminal
 * has a textual name, an id, and a string which indicates the type of object it
 * will be implemented with at runtime (i.e. the class of object that will be
 * pushed on the parse stack to represent it).
 */
public class NonTerminal extends symbol {

    public static final List<NonTerminal> ALL = new ArrayList<>();

    public static void clear() {
        ALL.clear();
    }

    public static NonTerminal create(String name, String type) {
        NonTerminal nonTerminal = new NonTerminal(ALL.size(), name, type);
        ALL.add(nonTerminal);
        return nonTerminal;
    }

    public final Set<Production> productions = new HashSet<>(11);
    public final TerminalSet firstSet = new TerminalSet();

    protected boolean _nullable;

    private NonTerminal(int id, String name, String type) {
        super(id, name, type);
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
