package java_cup;

/**
 * This abstract class serves as the base class for grammar symbols (i.e., both
 * terminals and non-terminals).
 */
public abstract class symbol {

    public final int id;
    public final String name;
    public final String type;

    protected boolean used = false;

    protected symbol(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type != null ? type : "Object";
    }

    public boolean isUsed() {
        return this.used;
    }

    public void use() {
        this.used = true;
    }
}
