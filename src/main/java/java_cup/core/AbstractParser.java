// Copyright (c) 2013-2014, Webit Team. All Rights Reserved.
package java_cup.core;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java_cup.InternalException;
import java_cup.Main;
import java_cup.NonTerminal;
import java_cup.Production;
import java_cup.ProductionItem;
import java_cup.Terminal;
import java_cup.symbol;

/**
 *
 * @author Zqq
 */
abstract class AbstractParser extends BaseParser {

    private static final int MAX_RHS = 200;

    private final HashMap<String, symbol> symbols = new HashMap();
    private final Object[] rightSymbolPool = new Object[MAX_RHS];
    private int rightSymbolPoolCount = 0;

    private NonTerminal startSymbol;
    private NonTerminal leftSymbol;
    private int _cur_prec = 0;

    public void parse() throws Exception {
        declearSymbol(Terminal.ERROR);
        declearSymbol(NonTerminal.START);
        parse(new Lexer(new InputStreamReader(System.in)));
    }

    private void new_rhs() {
        rightSymbolPoolCount = 0;
    }

    void addRightHandler(String sym, String label) {
        pushRightHandler(new ProductionItem(getSymbol(sym), label));
    }

    void addRightHandler(symbol sym, String label) {
        pushRightHandler(new ProductionItem(sym, label));
    }

    void addRightActionHandler(String code) {
        pushRightHandler(code);
    }

    void declearSymbol(symbol sym) {
        symbol old = this.symbols.put(sym.name, sym);
        if (old != null) {
            throw new InternalException("Symbol \"" + sym.name + "\" has already been declared");
        }
    }

    void declearNonTerminals(List<String> names, String type) {
        for (String name : names) {
            declearSymbol(NonTerminal.create(name, type));
        }
    }

    void declearTerminals(List<String> names, String type) {
        for (String name : names) {
            declearSymbol(Terminal.create(name, type));
        }
    }

    void setLeftHandler(String name) {
        if (startSymbol == null) {
            registStartNonTerminal(name);
        }
        leftSymbol = getNonTerminal(name);
        new_rhs();
    }

    private void pushRightHandler(Object part) {
        if (rightSymbolPoolCount >= MAX_RHS) {
            throw new InternalException("Internal Error: Productions limited to " + MAX_RHS + " symbols and actions");
        }
        rightSymbolPool[rightSymbolPoolCount] = part;
        rightSymbolPoolCount++;
    }

    symbol getSymbol(String name) {
        symbol nt = symbols.get(name);
        if (nt == null) {
            throw new InternalException("Symbol \"" + name + "\" has not been declared");
        }
        return nt;
    }

    NonTerminal getNonTerminal(String name) {
        symbol nt = symbols.get(name);
        if (nt == null) {
            throw new InternalException("Non-Terminal \"" + name + "\" has not been declared");
        }
        if (nt instanceof NonTerminal) {
            return (NonTerminal) nt;
        }
        throw new InternalException("Symbol \"" + name + "\" is not a Non-Terminal");
    }

    Terminal getTerminal(String name) {
        symbol nt = symbols.get(name);
        if (nt == null) {
            throw new InternalException("Terminal \"" + name + "\" has not been declared");
        }
        if (nt instanceof Terminal) {
            return (Terminal) nt;
        }
        throw new InternalException("Symbol \"" + name + "\" is not a Terminal");
    }

    private void registStartNonTerminal(String name) {
        startSymbol = getNonTerminal(name);
        // build start Production
        new_rhs();
        addRightHandler(startSymbol, null);
        addRightHandler(Terminal.EOF, null);
        addRightActionHandler("return myStack.peek(1).value;");
        Main.startProduction = Production.create(NonTerminal.START, rightSymbolPool, rightSymbolPoolCount);
        new_rhs();
    }

    void createProductionWithPrecedence(String termName) {
        Terminal terminal = getTerminal(termName);
        terminal.use();
        Production.create(leftSymbol, rightSymbolPool, rightSymbolPoolCount, terminal.precedence());
        new_rhs();
    }

    void createProduction() {
        Production.create(leftSymbol, rightSymbolPool, rightSymbolPoolCount);
        new_rhs();
    }

    protected Object addPrecedence(int p, List<String> names) {
        final int value = ++_cur_prec;
        for (String name : names) {
            getTerminal(name).setPrecedence(p, value);
        }
        return null;
    }
}
