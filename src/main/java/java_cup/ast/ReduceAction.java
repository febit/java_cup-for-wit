
package java_cup.ast;

import java_cup.InternalException;
import java_cup.ast.Production;
import java_cup.ast.Action;

/** This class represents a reduce action within the parse table. 
 *  The action simply stores the Production that it reduces with and 
 *  responds to queries about its type.
 *
 * @version last updated: 11/25/95
 * @author  Scott Hudson
 */
public class ReduceAction extends Action {
 
  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Simple constructor. 
   * @param prod the Production this action reduces with.
   */
  public ReduceAction(Production prod ) throws InternalException
    {
      /* sanity check */
      if (prod == null)
	throw new InternalException(
	  "Attempt to create a reduce_action with a null production");

      _reduce_with = prod;
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/
  
  /** The Production we reduce with. */
  protected Production _reduce_with;

  /** The Production we reduce with. */
  public Production reduce_with() {return _reduce_with;}

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Quick access to type of action. */
  public int kind() {return REDUCE;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Equality test. */
  public boolean equals(ReduceAction other)
    {
      return other != null && other.reduce_with() == reduce_with();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Generic equality test. */
  public boolean equals(Object other)
    {
      if (other instanceof ReduceAction)
	return equals((ReduceAction)other);
      else
       return false;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Compute a hash code. */
  public int hashCode()
    {
      /* use the hash code of the Production we are reducing with */
      return reduce_with().hashCode();
    }


  /** Convert to string. */
  public String toString() 
    {
      return "REDUCE(with prod " + reduce_with().id() + ")";
    }

  /*-----------------------------------------------------------*/

}
