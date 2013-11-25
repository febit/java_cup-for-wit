
package java_cup;

import java_cup.ast.NonTerminal;

/** This class represents one row (corresponding to one machine state) of the 
 *  reduce-goto parse table. 
 */
public class parse_reduce_row {
  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Simple constructor. Note: this should not be used until the size
   *  of terminals in the grammar has been established.
   */
  public parse_reduce_row()
    {
      /* make sure the size is set */
      if (_size <= 0 )  _size = NonTerminal.size();

      /* allocate the array */
      under_non_term = new lalr_state[size()];
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  /** Number of columns (non terminals) in every row. */
  protected static int _size = 0;

  /** Number of columns (non terminals) in every row. */
  public static int size() {return _size;}
   
  //Hm Added clear  to clear iterator static fields
  public static void clear() {
      _size = 0;
  }
  
  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** Actual entries for the row. */
  public lalr_state under_non_term[];
}

