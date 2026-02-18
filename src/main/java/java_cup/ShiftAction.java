package java_cup;

/**
 * This class represents a shift action within the parse table.
 */
public record ShiftAction(LalrState shiftTo) implements Action {

    public int type() {
        return SHIFT;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ShiftAction shift)) {
            return false;
        }
        return shift.shiftTo == this.shiftTo;
    }

    @Override
    public String toString() {
        return "SHIFT -> " + shiftTo.id;
    }
}
