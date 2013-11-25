
package java_cup;

/** Exception subclass for reporting internal errors in JavaCup. */
public class InternalException extends Exception
  {
    /** Constructor with a message */
    public InternalException(String msg)
      {
	super(msg);
      }

    /** Method called to do a forced error exit on an internal error
	for cases when we can't actually throw the exception.  */
    public void crash()
      {
          ErrorManager.getManager().emit_fatal("JavaCUP Internal Error Detected: "+getMessage());
          printStackTrace();
          System.exit(-1);
      }
  }
