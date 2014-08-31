package java_cup.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java_cup.Assoc;
import java_cup.InternalException;
import java_cup.symbol;

/**
 * This class represents a Terminal symbol in the grammar. Each Terminal has a
 * textual name, an id, and a string which indicates the type of object it will
 * be implemented with at runtime (i.e. the class of object that will be
 * returned by the scanner and pushed on the parse stack to represent it).
 *
 * @version last updated: 7/3/96
 * @author Frank Flannery
 */
public class Terminal extends symbol {

    /*-----------------------------------------------------------*/
    /*--- Constructor(s) ----------------------------------------*/
    /*-----------------------------------------------------------*/
    /**
     * Full constructor.
     *
     * @param nm the name of the Terminal.
     * @param tp the type of the Terminal.
     */
    public Terminal(String nm, String tp, int precedence_side, int precedence_num) {
        /* superclass does most of the work */
        super(nm, tp);

        this._precedence_num = precedence_num;
        this._precedence_side = precedence_side;
        
        /* add to set of iterator terminals and check for duplicates */
        Object conflict = _all.put(nm, this);
        if (conflict != null) // can't throw an execption here because this is used in static 
        // initializers, so we do a crash instead
        // was:
        // throw new InternalException("Duplicate Terminal (" + nm + ") created");
        {
            (new InternalException("Duplicate terminal (" + nm + ") created")).crash();
        }

        /* assign a unique id */
        this._id = _all_by_index.size();
        _all_by_index.add(this);
    }

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Constructor for non-precedented Terminal
     */
    public Terminal(String nm, String tp) {
        this(nm, tp, Assoc.no_prec, -1);
    }

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Constructor with default type.
     *
     * @param nm the name of the Terminal.
     */
    public Terminal(String nm) {
        this(nm, null);
    }

    /*-----------------------------------------------------------*/
    /*-------------------  Class Variables  ---------------------*/
    /*-----------------------------------------------------------*/
    private int _precedence_num;
    private int _precedence_side;

    /*-----------------------------------------------------------*/
    /*--- (Access to) Static (Class) Variables ------------------*/
    /*-----------------------------------------------------------*/
    /**
     * Table of iterator terminals. Elements are stored using name strings as the key
     */
    protected static Map _all = new HashMap();

    //Hm Added clear  to clear iterator static fields
    public static void clear() {
        _all.clear();
        _all_by_index.clear();
        EOF = new Terminal("EOF");
        ERROR = new Terminal("ERROR");
    }

    public static Iterator<Terminal> iterator() {return _all_by_index.iterator();}


    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Table of iterator terminals indexed by their id size.
     */
    protected static ArrayList<Terminal> _all_by_index = new ArrayList<Terminal>();

    /**
     * Lookup a Terminal by id.
     */
    public static Terminal find(int indx) {
        return (Terminal) _all_by_index.get(indx);
    }

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Total size of terminals.
     */
    public static int size() {
        return _all.size();
    }

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Special Terminal for end of input.
     */
    public static Terminal EOF;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * special Terminal used for ERROR recovery
     */
    public static Terminal ERROR;

    static {
        clear();
    }

    /*-----------------------------------------------------------*/
    /*--- General Methods ---------------------------------------*/
    /*-----------------------------------------------------------*/
    /**
     * Report this symbol as not being a non-Terminal.
     */
    public boolean is_non_term() {
        return false;
    }

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * get the precedence of a Terminal
     */
    public int precedence_num() {
        return _precedence_num;
    }

    public int precedence_side() {
        return _precedence_side;
    }

    /**
     * set the precedence of a Terminal
     */
    public void set_precedence(int p, int new_prec) {
        _precedence_side = p;
        _precedence_num = new_prec;
    }

    /*-----------------------------------------------------------*/
}
