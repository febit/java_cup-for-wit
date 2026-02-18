package java_cup;

public record ProductionItem(
        symbol sym,
        String label
) {

    public ProductionItem(symbol sym) {
        this(sym, null);
    }
}
