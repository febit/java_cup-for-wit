// Copyright (c) 2013, Webit Team. All Rights Reserved.
package java_cup.core;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 *
 * @author zqq
 */
abstract class BaseParser {

    private static final short[][] PRODUCTION_TABLE = loadData("Production");
    private static final short[][] ACTION_TABLE = loadData("Action");
    private static final short[][] REDUCE_TABLE = loadData("Reduce");

    final Stack<Symbol> symbolStack = new Stack<Symbol>(24);
    boolean goonParse;

    abstract Object doAction(int actionId) throws Exception;

    Symbol parse(final Lexer lexer) throws Exception {

        int act;
        Symbol currentToken;
        Symbol currentSymbol;
        final Stack<Symbol> stack = this.symbolStack;
        stack.clear();

        //Start Symbol
        currentSymbol = new Symbol(0, -1, -1, null);
        currentSymbol.state = 0;
        stack.push(currentSymbol);

        final short[][] actionTable = ACTION_TABLE;
        final short[][] reduceTable = REDUCE_TABLE;
        final short[][] productionTable = PRODUCTION_TABLE;

        currentToken = lexer.nextToken();

        /* continue until we are told to stop */
        goonParse = true;
        do {

            /* look up action out of the current state with the current input */
            act = getAction(actionTable[currentSymbol.state], currentToken.id);

            /* decode the action -- > 0 encodes shift */
            if (act > 0) {
                /* shift to the encoded state by pushing it on the _stack */
                currentToken.state = act - 1;
                stack.push(currentSymbol = currentToken);

                /* advance to the next Symbol */
                currentToken = lexer.nextToken();
            } else if (act < 0) {
                /* if its less than zero, then it encodes a reduce action */
                act = (-act) - 1;
                final int symId, handleSize;
                final Object result = doAction(act);
                final short[] row;
                symId = (row = productionTable[act])[0];
                handleSize = row[1];
                if (handleSize == 0) {
                    currentSymbol = new Symbol(symId, -1, -1, result);
                } else {
                    //position based on left
                    currentSymbol = new Symbol(symId, result, stack.peek(handleSize - 1));
                    //pop the handle
                    stack.pops(handleSize);
                }

                /* look up the state to go to from the one popped back to */
                /* shift to that state */
                currentSymbol.state = getReduce(reduceTable[stack.peek().state], symId);
                stack.push(currentSymbol);

            } else {
                //act == 0
                throw new RuntimeException("Syntax error at line " + lexer.getLine() + " column " + lexer.getColumn());
            }
        } while (goonParse);

        return stack.peek();
    }

    private static short getAction(final short[] row, final int sym) {
        final int len;
        int probe;
        /* linear search if we are < 10 entries, otherwise binary search */
        if ((len = row.length) < 20) {
            for (probe = 0; probe < len; probe++) {
                if (row[probe++] == sym) {
                    return row[probe];
                }
            }
        } else {
            int first, last;
            first = 0;
            last = ((len - 1) >> 1);

            int probe_2;
            while (first <= last) {
                probe = (first + last) >> 1;
                probe_2 = probe << 1;
                if (sym == row[probe_2]) {
                    return row[probe_2 + 1];
                } else if (sym > row[probe_2]) {
                    first = probe + 1;
                } else {
                    last = probe - 1;
                }
            }
        }
        //error
        return 0;
    }

    private static short getReduce(final short[] row, int sym) {
        if (row != null) {
            for (int probe = 0, len = row.length; probe < len; probe++) {
                if (row[probe++] == sym) {
                    return row[probe];
                }
            }
        }
        //error
        return -1;
    }

    private static short[][] loadData(String name) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("java_cup/core/Parser$" + name + ".data"));
            return (short[][]) in.readObject();
        } catch (Exception e) {
            throw new Error(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioex) {
                    // ignore
                }
            }
        }
    }
}
