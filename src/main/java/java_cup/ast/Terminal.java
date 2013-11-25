package java_cup.ast;

import java_cup.Assoc;
import java.util.Hashtable;
import java.util.Enumeration;
import java_cup.Assoc;
import java_cup.InternalException;
import java_cup.symbol;

/** This class represents a Terminal symbol in the grammar.  Each Terminal 
 *  has a textual name, an id, and a string which indicates the type of 
 *  object it will be implemented with at runtime (i.e. the class of object 
 *  that will be returned by the scanner and pushed on the parse stack to 
 *  represent it). 
 *
 * @version last updated: 7/3/96
 * @author  Frank Flannery
 */
public class Terminal extends symbol {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Full constructor.
   * @param nm the name of the Terminal.
   * @param tp the type of the Terminal.
   */
  public Terminal(String nm, String tp, int precedence_side, int precedence_num) 
    {
      /* superclass does most of the work */
      super(nm, tp);

      /* add to set of all terminals and check for duplicates */
      Object conflict = _all.put(nm,this);
      if (conflict != null)
	// can't throw an execption here because this is used in static 
	// initializers, so we do a crash instead
	// was:
	// throw new InternalException("Duplicate Terminal (" + nm + ") created");
	(new InternalException("Duplicate terminal (" + nm + ") created")).crash();

      /* assign a unique id */
      _id = next_index++;

      /* set the precedence */
      _precedence_num = precedence_num;
      _precedence_side = precedence_side;

      /* add to by_index set */
      _all_by_index.put(new Integer(_id), this);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constructor for non-precedented Terminal
    */ 

  public Terminal(String nm, String tp) 
    {
      this(nm, tp, Assoc.no_prec, -1);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constructor with default type. 
   * @param nm the name of the Terminal.
   */
  public Terminal(String nm) 
    {
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

  /** Table of all terminals.  Elements are stored using name strings as 
   *  the key 
   */
  protected static Hashtable _all = new Hashtable();

  //Hm Added clear  to clear all static fields
  public static void clear() {
      _all.clear();
      _all_by_index.clear();
      next_index=0;
      EOF = new Terminal("EOF");
      error = new Terminal ("error");
  }
  
  /** Access to all terminals. */
  public static Enumeration all() {return _all.elements();}

  /** Lookup a Terminal by name string. */ 
  public static Terminal find(String with_name)
    {
      if (with_name == null)
	return null;
      else 
	return (Terminal)_all.get(with_name);
    }


  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Table of all terminals indexed by their id number. */
  protected static Hashtable _all_by_index = new Hashtable();

  /** Lookup a Terminal by id. */
  public static Terminal find(int indx)
    {
      Integer the_indx = new Integer(indx);

      return (Terminal)_all_by_index.get(the_indx);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Total number of terminals. */
  public static int number() {return _all.size();}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
 
  /** Static counter to assign unique id. */
  protected static int next_index = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Special Terminal for end of input. */
  public static Terminal EOF = new Terminal("EOF");

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** special Terminal used for error recovery */
  public static Terminal error = new Terminal("error");

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Report this symbol as not being a non-Terminal. */
  public boolean is_non_term() 
    {
      return false;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Convert to a string. */
  public String toString()
    {
      return super.toString() + "[" + id() + "]";
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** get the precedence of a Terminal */
  public int precedence_num() {
    return _precedence_num;
  }
  public int precedence_side() {
    return _precedence_side;
  }

  /** set the precedence of a Terminal */
  public void set_precedence(int p, int new_prec) {
    _precedence_side = p;
    _precedence_num = new_prec;
  }

  /*-----------------------------------------------------------*/

}
