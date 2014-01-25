package java_cup.ast;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java_cup.InternalException;
import java_cup.symbol;
import java_cup.terminal_set;

/** This class represents a non-terminal symbol in the grammar.  Each
 *  non terminal has a textual name, an id, and a string which indicates
 *  the type of object it will be implemented with at runtime (i.e. the class
 *  of object that will be pushed on the parse stack to represent it). 
 *
 * @version last updated: 11/25/95
 * @author  Scott Hudson
 */

public class NonTerminal extends symbol {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Full constructor.
   * @param nm  the name of the non terminal.
   * @param tp  the type string for the non terminal.
   */
  public NonTerminal(String nm, String tp) 
    {
      /* super class does most of the work */
      super(nm, tp);

      Object conflict = _all.put(nm,this);
      /* add to set of iterator non terminals and check for duplicates */
      if (conflict != null)
	// can't throw an exception here because these are used in static
	// initializers, so we crash instead
	// was: 
	// throw new InternalException("Duplicate non-terminal ("+nm+") created");
	(new InternalException("Duplicate non-terminal ("+nm+") created")).crash();

      /* assign a unique id */
      _id = _all_by_index.size();

      /* add to by_index set */
      _all_by_index.add(this);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constructor with default type. 
   * @param nm  the name of the non terminal.
   */
  public NonTerminal(String nm) 
    {
      this(nm, null);
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  /** Table of iterator non-terminals -- elements are stored using name strings 
   *  as the key 
   */
  protected static Map _all = new HashMap();

  //Hm Added clear  to clear iterator static fields
  public static void clear() {
      _all.clear();
      _all_by_index.clear();
      next_nt=0;
  }

  public static Iterator<NonTerminal> iterator() {return _all_by_index.iterator();}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Table of iterator non terminals indexed by their id size. */
  protected static ArrayList<NonTerminal> _all_by_index = new ArrayList<NonTerminal>();

  /** Lookup a non terminal by id. */
  public static NonTerminal find(int indx)
    {
      Integer the_indx = new Integer(indx);

      return (NonTerminal)_all_by_index.get(the_indx);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Total size of non-terminals. */
  public static int size() {return _all.size();}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Static counter to assign unique indexes. */
  protected static int next_index = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Static counter for creating unique non-terminal names */
  static protected int next_nt = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** special non-terminal for start symbol */
  public static final NonTerminal START_nt = new NonTerminal("$START");

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** flag non-terminals created to embed action productions */
  public boolean is_embedded_action = false; /* added 24-Mar-1998, CSA */

  /*-----------------------------------------------------------*/
  /*--- Static Methods ----------------------------------------*/
  /*-----------------------------------------------------------*/
	 
  /** Method for creating a new uniquely named hidden non-terminal using 
   *  the given string as a base for the name (or "NT$" if null is passed).
   * @param prefix base name to construct unique name from. 
   */
  static NonTerminal create_new(String prefix) throws InternalException
    {
      return create_new(prefix,null); // TUM 20060608 embedded actions patch
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** static routine for creating a new uniquely named hidden non-terminal */
  static NonTerminal create_new() throws InternalException
    { 
      return create_new(null); 
    }
    /**
     * TUM 20060608 bugfix for embedded action codes
     */
    static NonTerminal create_new(String prefix, String type) throws InternalException{
        if (prefix==null) prefix = "NT$";
        return new NonTerminal(prefix + next_nt++,type);
    }
  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Compute nullability of iterator non-terminals. */
  public static void compute_nullability() throws InternalException
    {
      boolean      change = true;


      /* repeat this process until there is no change */
      while (change)
	{
	  /* look for a new change */
	  change = false;

	  /* consider each non-terminal */
            
          for (Iterator<NonTerminal> it = _all_by_index.iterator(); it.hasNext();) {
              NonTerminal nt = it.next();
           

	      /* only look at things that aren't already marked nullable */
	      if (!nt.nullable())
		{
		  if (nt.looks_nullable())
		    {
		      nt._nullable = true;
		      change = true;
		    }
		}
	    }
	}
      
      /* do one last pass over the productions to finalize iterator of them */
      for (Iterator<Production> p = Production.all(); p.hasNext(); )
	{
          Production prod = p.next();
	  prod.set_nullable(prod.check_nullable());
	}
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Compute first sets for iterator non-terminals.  This assumes nullability has
   *  already computed.
   */
  public static void compute_first_sets() throws InternalException
    {
      boolean      change = true;
      Enumeration  n;
      Enumeration  p;

      Production   prod;
      terminal_set prod_first;

      /* repeat this process until we have no change */
      while (change)
	{
	  /* look for a new change */
	  change = false;

	  /* consider each non-terminal */
          for (Iterator<NonTerminal> it = _all_by_index.iterator(); it.hasNext();) {
              NonTerminal nt = it.next();

	      /* consider every Production of that non terminal */
	      for (p = nt.productions(); p.hasMoreElements(); )
		{
		  prod = (Production)p.nextElement();

		  /* get the updated first of that Production */
		  prod_first = prod.check_first_set();

		  /* if this going to add anything, add it */
		  if (!prod_first.is_subset_of(nt._first_set))
		    {
		      change = true;
		      nt._first_set.add(prod_first);
		    }
		}
	    }
	}
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** Table of iterator productions with this non terminal on the LHS. */
  protected Hashtable _productions = new Hashtable(11);

  /** Access to productions with this non terminal on the LHS. */
  public Enumeration productions() {return _productions.elements();}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Total size of productions with this non terminal on the LHS. */
  public int num_productions() {return _productions.size();}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Add a Production to our set of productions. */
  public void add_production(Production prod) throws InternalException
    {
      /* catch improper productions */
      if (prod == null || prod.lhs() == null || prod.lhs().the_symbol() != this)
	throw new InternalException(
	  "Attempt to add invalid production to non terminal production table");

      /* add it to the table, keyed with itself */
      _productions.put(prod,prod);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Nullability of this non terminal. */
  protected boolean _nullable;

  /** Nullability of this non terminal. */
  public boolean nullable() {return _nullable;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** First set for this non-terminal. */
  protected terminal_set _first_set = new terminal_set();

  /** First set for this non-terminal. */
  public terminal_set first_set() {return _first_set;}

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Indicate that this symbol is a non-terminal. */
  public boolean is_non_term() 
    {
      return true;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Test to see if this non terminal currently looks nullable. */
  protected boolean looks_nullable() throws InternalException
    {
      /* look and see if any of the productions now look nullable */
      for (Enumeration e = productions(); e.hasMoreElements(); )
	/* if the Production can go to empty, we are nullable */
	if (((Production)e.nextElement()).check_nullable())
	  return true;

      /* none of the productions can go to empty, so we are not nullable */
      return false;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** convert to string */
  public String toString()
    {
      return super.toString() + "[" + id() + "]" + (nullable() ? "*" : "");
    }

  /*-----------------------------------------------------------*/
}
