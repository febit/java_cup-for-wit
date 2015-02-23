package java_cup;

public class ProductionItem {

    public final String label;
    public final symbol sym;

    public ProductionItem(symbol sym, String lab) {
        this.label = lab;
        this.sym = sym;
    }

    public ProductionItem(symbol sym) {
        this(sym, null);
    }
}
