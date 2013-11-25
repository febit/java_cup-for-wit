
package java_cup.ast;

import java_cup.InternalException;
import java_cup.ast.Action;
import java_cup.lalr_state;

/** This class represents a shift action within the parse table. 
 *  The action simply stores the state that it shifts to and responds 
 *  to queries about its type.
 *
 * @version last updated: 11/25/95
 * @author  Scott Hudson
 */
public class ShiftAction extends Action {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Simple constructor. 
   * @param shft_to the state that this action shifts to.
   */
  public ShiftAction(lalr_state shft_to) throws InternalException
    {
      /* sanity check */
      if (shft_to == null)
	throw new InternalException(
	  "Attempt to create a shift_action to a null state");

      _shift_to = shft_to;
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** The state we shift to. */
  protected lalr_state _shift_to;

  /** The state we shift to. */
  public lalr_state shift_to() {return _shift_to;}

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Quick access to type of action. */
  public int kind() {return SHIFT;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Equality test. */
  public boolean equals(ShiftAction other)
    {
      return other != null && other.shift_to() == shift_to();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Generic equality test. */
  public boolean equals(Object other)
    {
      if (other instanceof ShiftAction)
	return equals((ShiftAction)other);
      else
       return false;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Compute a hash code. */
  public int hashCode()
    {
      /* use the hash code of the state we are shifting to */
      return shift_to().hashCode();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Convert to a string. */
  public String toString() {return "SHIFT(to state " + shift_to().index() + ")";}

  /*-----------------------------------------------------------*/

}
