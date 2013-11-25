
package java_cup;

import java_cup.ast.SymbolPart;
import java_cup.ast.NonTerminal;
import java_cup.ast.ProductionPart;
import java_cup.ast.Production;

/** The "core" of an LR item.  This includes a Production and the position
 *  of a marker (the "dot") within the Production.  Typically item cores 
 *  are written using a Production with an embedded "dot" to indicate their 
 *  position.  For example: <pre>
 *     A ::= B * C d E
 *  </pre>
 *  This represents a point in a parse where the parser is trying to match
 *  the given Production, and has succeeded in matching everything before the 
 *  "dot" (and hence is expecting to see the symbols after the dot next).  See 
 *  lalr_item, lalr_item_set, and lalr_start for full details on the meaning 
 *  and use of items.
 *
 * @see     java_cup.lalr_item
 * @see     java_cup.lalr_item_set
 * @see     java_cup.lalr_state
 * @version last updated: 11/25/95
 * @author  Scott Hudson
*/

public class lr_item_core {
   
  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Full constructor.
   * @param prod Production this item uses.
   * @param pos  position of the "dot" within the item.
   */
  public lr_item_core(Production prod, int pos) throws InternalException
    {
      symbol          after_dot = null;
      ProductionPart part;

      if (prod == null)
	throw new InternalException(
	  "Attempt to create an lr_item_core with a null production");

      _the_production = prod;

      if (pos < 0 || pos > _the_production.rhs_length())
	throw new InternalException(
	  "Attempt to create an lr_item_core with a bad dot position");

      _dot_pos = pos;

      /* compute and cache hash code now */
      _core_hash_cache = 13*_the_production.hashCode() + pos;

      /* cache the symbol after the dot */
      if (_dot_pos < _the_production.rhs_length())
	{
	  part = _the_production.rhs(_dot_pos);
	  if (!part.is_action())
	    _symbol_after_dot = ((SymbolPart)part).the_symbol();
	}
    } 

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constructor for dot at start of right hand side. 
   * @param prod Production this item uses.
   */
  public lr_item_core(Production prod) throws InternalException
    {
      this(prod,0);
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** The Production for the item. */
  protected Production _the_production;

  /** The Production for the item. */
  public Production the_production() {return _the_production;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The position of the "dot" -- this indicates the part of the Production 
   *  that the marker is before, so 0 indicates a dot at the beginning of 
   *  the RHS.
   */
  protected int _dot_pos;

  /** The position of the "dot" -- this indicates the part of the Production 
   *  that the marker is before, so 0 indicates a dot at the beginning of 
   *  the RHS.
   */
  public int dot_pos() {return _dot_pos;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Cache of the hash code. */
  protected int _core_hash_cache;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Cache of symbol after the dot. */
  protected symbol _symbol_after_dot = null;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Is the dot at the end of the Production? */
  public boolean dot_at_end() 
    {
       return _dot_pos >= _the_production.rhs_length();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Return the symbol after the dot.  If there is no symbol after the dot
   *  we return null. */
  public symbol symbol_after_dot()
    {
      /* use the cached symbol */
      return _symbol_after_dot;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Determine if we have a dot before a non terminal, and if so which one 
   *  (return null or the non terminal). 
   */
  public NonTerminal dot_before_nt()
    {
      symbol sym;

      /* get the symbol after the dot */
      sym = symbol_after_dot();

      /* if it exists and is a non terminal, return it */
      if (sym != null && sym.is_non_term())
	return (NonTerminal)sym;
      else
	return null;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce a new lr_item_core that results from shifting the dot one 
   *  position to the right. 
   */
  public lr_item_core shift_core() throws InternalException
    {
      if (dot_at_end()) 
	throw new InternalException(
	  "Attempt to shift past end of an lr_item_core");

      return new lr_item_core(_the_production, _dot_pos+1);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Equality comparison for the core only.  This is separate out because we 
   *  need separate access in a super class. 
   */
  public boolean core_equals(lr_item_core other)
    {
      return other != null && 
	     _the_production.equals(other._the_production) && 
	     _dot_pos == other._dot_pos;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Equality comparison. */
  public boolean equals(lr_item_core other) {return core_equals(other);}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Generic equality comparison. */
  public boolean equals(Object other)
    {
      if (!(other instanceof lr_item_core))
	return false;
      else
	return equals((lr_item_core)other);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Hash code for the core (separated so we keep non overridden version). */
  public int core_hashCode()
    {
      return _core_hash_cache;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Hash code for the item. */
  public int hashCode() 
    {
      return _core_hash_cache;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Return the hash code that object would have provided for us so we have 
   *  a (nearly) unique id for debugging.
   */
  protected int obj_hash()
    {
      return super.hashCode();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Convert to a string (separated out from toString() so we can call it
   *  from subclass that overrides toString()).
   */
  public String to_simple_string() throws InternalException
    {
      String result;
      ProductionPart part;

      if (_the_production.lhs() != null && 
	  _the_production.lhs().the_symbol() != null &&
	  _the_production.lhs().the_symbol().name() != null)
	result = _the_production.lhs().the_symbol().name();
      else
	result = "$$NULL$$";

      result += " ::= ";

      for (int i = 0; i<_the_production.rhs_length(); i++)
	{
	  /* do we need the dot before this one? */
	  if (i == _dot_pos)
	    result += "(*) ";
	  
	  /* print the name of the part */
	  if (_the_production.rhs(i) == null)
	    {
	      result += "$$NULL$$ ";
	    }
	  else
	    {
	      part = _the_production.rhs(i);
	      if (part == null)
		result += "$$NULL$$ ";
	      else if (part.is_action())
		result += "{ACTION} ";
	      else if (((SymbolPart)part).the_symbol() != null &&
                       ((SymbolPart)part).the_symbol().name() != null)
		result += ((SymbolPart)part).the_symbol().name() + " ";
	      else
		result += "$$NULL$$ ";
	    }
	}

      /* put the dot after if needed */
      if (_dot_pos == _the_production.rhs_length())
	result += "(*) ";

      return result;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Convert to a string */
  public String toString() 
    {
      /* can't throw here since super class doesn't, so we crash instead */
      try {
        return to_simple_string();
      } catch(InternalException e) {
	e.crash();
	return null;
      }
    }

  /*-----------------------------------------------------------*/

}
   
