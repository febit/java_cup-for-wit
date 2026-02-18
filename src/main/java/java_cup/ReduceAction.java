package java_cup;

/**
 * This class represents a reduce code within the parse table.
 */
public record ReduceAction(
        Production reduceWith
) implements Action {

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
    public String toString() {
        return "REDUCE -> " + reduceWith.id;
    }
}
