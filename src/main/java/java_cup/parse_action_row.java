
package java_cup;

import java_cup.ast.Terminal;
import java_cup.ast.Action;

/** This class represents one row (corresponding to one machine state) of the 
 *  parse action table.
 */
public class parse_action_row {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/
	       
  /** Simple constructor.  Note: this should not be used until the size of
   *  terminals in the grammar has been established.
   */
  public parse_action_row()
    {
      /* make sure the size is set */
      if (_size <= 0 )  _size = Terminal.number();

      /* allocate the array */
      under_term = new Action[size()];

      /* set each element to an error action */
      for (int i=0; i<_size; i++)
	under_term[i] = new Action();
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  /** Number of columns (terminals) in every row. */
  protected static int _size = 0;

  /** Number of columns (terminals) in every row. */
  public static int size() {return _size;}

  //Hm Added clear  to clear all static fields
  public static void clear() {
      _size = 0;
      reduction_count = null;
  }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Table of reduction counts (reused by compute_default()). */
  protected static int reduction_count[] = null;

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** Actual action entries for the row. */
  public Action under_term[];

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Default (reduce) action for this row.  -1 will represent default 
   *  of error. 
   */
  public int default_reduce;

}

