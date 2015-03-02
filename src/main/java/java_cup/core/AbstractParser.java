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

    private final HashMap<String, symbol> symbols = new HashMap();

    private NonTerminal startSymbol;
    private NonTerminal leftSymbol;
    private int _cur_prec = 0;

    public void parse() throws Exception {
        declearSymbol(Terminal.ERROR);
        declearSymbol(NonTerminal.START);
        parse(new Lexer(new InputStreamReader(System.in)));
    }

    ProductionItem createProductionItem(String sym, String label) {
        return createProductionItem(getSymbol(sym), label);
    }

    ProductionItem createProductionItem(symbol sym, String label) {
        return new ProductionItem(sym, label);
    }

    void declearSymbol(symbol sym) {
        symbol old = this.symbols.put(sym.name, sym);
        if (old != null) {
            throw new InternalException("Symbol \"" + sym.name + "\" has already been declared");
        }
    }

    NonTerminal createListNonTerminalIfAbsent(String compSymName, String split) {
        final String name = compSymName + "$$lst$" + (split != null ? split : "");
        NonTerminal result = (NonTerminal) this.symbols.get(name);
        if (result == null) {
            //create NonTerminal
            symbol compSym = getSymbol(compSymName);
            result = NonTerminal.create(name, "java.util.List<" + compSym.type + ">");
            declearSymbol(result);
            //create Production for nt
            Production.create(result, new Object[]{new ProductionItem(compSym), "java.util.List list = new java.util.ArrayList(); list.add(myStack.peek(0).value); return list;"});
            if (split != null) {
                Production.create(result, new Object[]{new ProductionItem(result), new ProductionItem(getSymbol(split)), new ProductionItem(compSym), "java.util.List list = (java.util.List) (myStack.peek(2).value); list.add(myStack.peek(0).value); return list;"});
            } else {
                Production.create(result, new Object[]{new ProductionItem(result), new ProductionItem(compSym), "java.util.List list = (java.util.List) (myStack.peek(1).value); list.add(myStack.peek(0).value); return list;"});
            }
        }
        return result;
    }

    NonTerminal createOptionableNonTerminalIfAbsent(String compSymName) {
        final String name = compSymName + "$$opt";
        NonTerminal result = (NonTerminal) this.symbols.get(name);
        if (result == null) {
            //create NonTerminal
            symbol compSym = getSymbol(compSymName);
            result = NonTerminal.create(name, compSym.type);
            declearSymbol(result);
            //create Production for nt
            Production.create(result, new Object[]{"return null;"});
            Production.create(result, new Object[]{new ProductionItem(compSym), "return myStack.peek(0).value;"});
        }
        return result;
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
        Main.startProduction = Production.create(NonTerminal.START,
                new Object[]{createProductionItem(startSymbol, null), createProductionItem(Terminal.EOF, null), "return myStack.peek(1).value;"});
    }

    void createProduction(List parts, String termName) {
        if (termName != null) {
            Terminal terminal = getTerminal(termName);
            terminal.use();
            Production.create(leftSymbol, parts.toArray(), terminal.precedence());
        } else {
            Production.create(leftSymbol, parts.toArray());
        }
    }

    void addPrecedence(int p, List<String> names) {
        final int value = ++_cur_prec;
        for (String name : names) {
            getTerminal(name).setPrecedence(p, value);
        }
    }
}
