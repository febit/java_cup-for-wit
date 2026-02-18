package java_cup;

/**
 * This class represents a transition in an LALR viable prefix recognition
 * machine. Transitions can be under terminals for non-terminals. They are
 * internally linked together into singly linked lists containing all the
 * transitions out of a single state via the next field.
 */
public record LalrTransition(
        symbol symbol,
        LalrState state,
        LalrTransition next
) {

    @Override
    public String toString() {
        return "transition on " + symbol.name + " to state [" + state.id + "]";
    }
}
