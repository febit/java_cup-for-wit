
package java_cup.ast;

/** This class serves as the base class for entries in a parse action table.  
 *  Full entries will either be SHIFT(state_num), REDUCE(production), NONASSOC,
 *  or ERROR. Objects of this base class will default to ERROR, while
 *  the other three types will be represented by subclasses. 
 * 
 * @see     java_cup.reduce_action
 * @see     java_cup.shift_action
 * @version last updated: 7/2/96
 * @author  Frank Flannery
 */

public class Action {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Simple constructor. */
  public Action()
    {
      /* nothing to do in the base class */
    }

 
  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  /** Constant for action type -- error action. */
  public static final int ERROR = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constant for action type -- shift action. */
  public static final int SHIFT = 1;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constants for action type -- reduce action. */
  public static final int REDUCE = 2;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constants for action type -- reduce action. */
  public static final int NONASSOC = 3;

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/
	 
  /** Quick access to the type -- base class defaults to error. */
  public int kind() {return ERROR;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Equality test. */
  public boolean equals(Action other)
    {
      /* we match all error actions */
      return other != null && other.kind() == ERROR;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Generic equality test. */
  public boolean equals(Object other)
    {
      if (other instanceof Action)
	return equals((Action)other);
      else
	return false;
    }
  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Compute a hash code. */
  public int hashCode()
    {
      /* all objects of this class hash together */
      return 0xCafe123;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Convert to string. */
  public String toString() {return "ERROR";}

  /*-----------------------------------------------------------*/
}
    
