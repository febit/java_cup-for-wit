package java_cup;

/**
 * Defines integers that represent the associativity of terminals.
 */
public interface Assoc {

    int LEFT = 0;
    int RIGHT = 1;
    int NONASSOC = 2;
    int NONE = -1;

}
