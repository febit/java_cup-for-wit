package java_cup;

import java.util.ArrayList;

/**
 * This class represents a Terminal symbol in the grammar.
 */
public class Terminal extends symbol {

    public static final ArrayList<Terminal> all = new ArrayList<Terminal>();

    public static Terminal EOF;
    public static Terminal ERROR;

    public static void clear() {
        all.clear();
        EOF = Terminal.create("EOF", null);
        ERROR = Terminal.create("ERROR", null);
    }

    public static Terminal get(int indx) {
        return (Terminal) all.get(indx);
    }

    public static int size() {
        return all.size();
    }

    public static Terminal create(String nm) {
        return create(nm, null);
    }

    public static Terminal create(String name, String type) {
        Terminal terminal = new Terminal(all.size(), name, type);
        all.add(terminal);
        return terminal;
    }

    private int _precedence;
    private int _precedenceSide;

    private Terminal(int id, String nm, String tp) {
        super(id, nm, tp);
        this._precedenceSide = Assoc.NONASSOC;
        this._precedence = Assoc.NONE;
    }

    public void setPrecedence(int side, int precedence) {
        _precedenceSide = side;
        _precedence = precedence;
    }

    public int precedence() {
        return _precedence;
    }

    public int precedenceSide() {
        return _precedenceSide;
    }
}
