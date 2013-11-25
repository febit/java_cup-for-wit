package java_cup.ast;

import java_cup.InternalException;
import java_cup.ast.ProductionPart;
import java_cup.symbol;

/** This class represents a part of a production which is a symbol (terminal
 *  or non terminal).  This simply maintains a reference to the symbol in 
 *  question.
 *
 * @see     java_cup.production
 * @version last updated: 11/25/95
 * @author  Scott Hudson
 */
public class SymbolPart extends ProductionPart {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Full constructor. 
   * @param sym the symbol that this part is made up of.
   * @param lab an optional label string for the part.
   */
  public SymbolPart(symbol sym, String lab) throws InternalException
    {
      super(lab);

      if (sym == null)
	throw new InternalException(
	  "Attempt to construct a symbol_part with a null symbol");
      _the_symbol = sym;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constructor with no label. 
   * @param sym the symbol that this part is made up of.
   */
  public SymbolPart(symbol sym) throws InternalException
    {
      this(sym,null);
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** The symbol that this part is made up of. */
  protected symbol _the_symbol;

  /** The symbol that this part is made up of. */
  public symbol the_symbol() {return _the_symbol;}

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Respond that we are not an action part. */
  public boolean is_action() { return false; }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Equality comparison. */
  public boolean equals(SymbolPart other)
    {
      return other != null && super.equals(other) && 
	     the_symbol().equals(other.the_symbol());
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Generic equality comparison. */
  public boolean equals(Object other)
    {
      if (!(other instanceof SymbolPart))
	return false;
      else
	return equals((SymbolPart)other);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce a hash code. */
  public int hashCode()
    {
      return super.hashCode() ^ 
	     (the_symbol()==null ? 0 : the_symbol().hashCode());
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Convert to a string. */
  public String toString()
    {
      if (the_symbol() != null)
	return super.toString() + the_symbol();
      else
	return super.toString() + "$$MISSING-SYMBOL$$";
    }

  /*-----------------------------------------------------------*/

}
