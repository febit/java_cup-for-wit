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
        EOF = new Terminal("EOF");
        ERROR = new Terminal("ERROR");
    }

    public static Terminal find(int indx) {
        return (Terminal) all.get(indx);
    }

    public static int size() {
        return all.size();
    }

    private int _precedence;
    private int _precedenceSide;

    public Terminal(String nm, String tp) {
        super(all.size(), nm, tp);
        this._precedenceSide = Assoc.NONASSOC;
        this._precedence = Assoc.NONE;
        all.add(this);
    }

    public Terminal(String nm) {
        this(nm, null);
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
