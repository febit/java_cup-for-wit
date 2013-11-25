
package java_cup.runtime;

/**
 *
 * @author Zqq
 */
public interface Stack<E> {

    public E push(E item);

    public E peek();
    
    public E peek(int offset);

    public boolean empty();

    public E pop();
    
    public int size();
    
    public void clear();
}
