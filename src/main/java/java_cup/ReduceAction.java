package java_cup;

/**
 * This class represents a reduce action within the parse table.
 */
public class ReduceAction implements Action {

    public final Production reduceWith;

    public ReduceAction(Production prod) {
        this.reduceWith = prod;
    }

    public int type() {
        return REDUCE;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReduceAction)) {
            return false;
        }
        return ((ReduceAction) other).reduceWith == this.reduceWith;
    }

    @Override
    public int hashCode() {
        return reduceWith.hashCode();
    }

    @Override
    public String toString() {
        return "REDUCE -> " + reduceWith.id;
    }
}
