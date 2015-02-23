package java_cup;

/**
 * This class represents a shift action within the parse table.
 */
public class ShiftAction implements Action {

    public final LalrState shiftTo;

    public ShiftAction(LalrState shft_to) {
        this.shiftTo = shft_to;
    }

    public int type() {
        return SHIFT;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ShiftAction)) {
            return false;
        }
        return ((ShiftAction) other).shiftTo == this.shiftTo;
    }

    @Override
    public int hashCode() {
        return shiftTo.hashCode();
    }

    @Override
    public String toString() {
        return "SHIFT -> " + shiftTo.id;
    }
}
