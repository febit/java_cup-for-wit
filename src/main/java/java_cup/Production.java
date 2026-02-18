package java_cup;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a Production in the grammar. It contains a LHS non Terminal, and an array of RHS symbols. As
 * various transformations are done on the RHS of the Production, it may shrink. As a result a separate length is always
 * maintained to indicate how much of the RHS array is still valid.
 */
public class Production implements Comparable<Production> {

    public static final List<Production> ALL = new ArrayList<>();

    public static Production create(NonTerminal lhsSymbol, Object[] rhsCandi) {
        lhsSymbol.use();

        ProductionItem[] temp = new ProductionItem[rhsCandi.length];
        int count = 0;
        String lasAction = null;
        int prec = -1;
        int resultSym = -1;
        boolean calPrec = true;
        for (Object part : rhsCandi) {
            if (part instanceof ProductionItem) {
                if (lasAction != null) {
                    temp[count++] = createInsidePart(lasAction);
                    lasAction = null;
                }
                var item = (ProductionItem) part;
                if ("$".equals(item.label())) {
                    if (resultSym >= 0) {
                        throw new InternalException("Too much symbol marked by `$` for `" + lhsSymbol.name + '\'');
                    }
                    resultSym = count;
                }
                temp[count++] = item;
                symbol sym = item.sym();
                sym.use();
                if (calPrec && sym instanceof Terminal) {
                    prec = ((Terminal) sym).precedence();
                }
            } else if (part instanceof Integer) {
                calPrec = false;
                prec = (Integer) part;
            } else {
                if (lasAction == null) {
                    lasAction = ((String) part).trim();
                } else {
                    lasAction += ((String) part).trim();
                }
            }
        }

        ProductionItem[] rhs = new ProductionItem[count];
        if (count > 0) {
            System.arraycopy(temp, 0, rhs, 0, count);
        }

        String code;
        if (lasAction != null) {
            code = resolveCode(rhs, lasAction.trim());
        } else {
            if (resultSym >= 0) {
                code = "yield myStack.peek(" + (count - resultSym - 1) + ").value;";
            } else {
                code = "yield null;";
            }
        }
        code = code.trim();
        Production prod = new Production(ALL.size(), new ProductionItem(lhsSymbol), rhs, code, prec);

        //XXX check if have a yield statement
        if (!code.contains("yield ")) {
            throw new InternalException("Production must has a 'yield':" + prod);
        }

        ALL.add(prod);
        lhsSymbol.productions.add(prod);
        return prod;
    }

    public static void clear() {
        ALL.clear();
    }

    public final int id;
    public final ProductionItem lhs;
    public final ProductionItem[] rhs;
    public final String code;
    public final int precedence;
    /**
     * Count of size of reductions using this Production.
     */
    protected boolean reductionUsed = false;

    /**
     * Is the nullability of the Production known or unknown?
     */
    protected boolean _nullable_known = false;

    /**
     * Nullability of the Production (can it derive the empty string).
     */
    protected boolean _nullable = false;

    /**
     * First set of the Production. This is the set of terminals that could appear at the front of some string derived
     * from this Production.
     */
    protected TerminalSet _first_set = new TerminalSet();

    private Production(int id, ProductionItem lhs, ProductionItem[] rhs, String code, int precedence) {
        this.id = id;
        this.lhs = lhs;
        this.rhs = rhs;
        this.code = code;
        this.precedence = precedence;
    }

    /**
     * Count of size of reductions using this Production.
     */
    public boolean reductionUsed() {
        return reductionUsed;
    }

    /**
     * Increment the count of reductions with this non-Terminal
     */
    public void reductionUse() {
        reductionUsed = true;
    }

    /**
     * First set of the Production. This is the set of terminals that could appear at the front of some string derived
     * from this Production.
     */
    public TerminalSet first_set() {
        return _first_set;
    }

    @Override
    public int compareTo(Production o) {
        int result;
        if ((result = this.code.compareTo(o.code)) == 0) {
            result = this.id - o.id;
        }
        return result;
    }

    protected static ProductionItem createInsidePart(String code) {
        NonTerminal terminal = NonTerminal.create("$NT" + NonTerminal.ALL.size(), null);
        Production.create(terminal, new Object[]{code});
        ProductionItem symbolPart = new ProductionItem(terminal);
        return symbolPart;
    }

    protected static String resolveCode(ProductionItem[] rhs, String code) {
        if (code.indexOf('%') < 0) {
            return code;
        }
        final StringBuilder declaration = new StringBuilder();

        for (int i = 0; i < rhs.length; i++) {
            ProductionItem part = (ProductionItem) rhs[i];
            if (part.label() == null) {
                continue;
            }
            String labelName = part.label();
            String stackType = part.sym().type;
            String stackTypeString = "Object".equals(stackType) ? "" : ("(" + stackType + ") ");
            int offset = rhs.length - i - 1;
            //
            boolean count_value = false;
            String repalce_value = '%' + labelName + '%';
            String repalce_line = '%' + labelName + ".line%";
            String repalce_column = '%' + labelName + ".column%";
            String repalce_symbol = '%' + labelName + ".symbol%";
            //
            int index;
            index = code.indexOf(repalce_value);
            if (index >= 0) {
                count_value = (code.indexOf(repalce_value, index + repalce_value.length()) >= 0);
            }
            if (!count_value) {
                count_value = code.contains(repalce_line)
                        || code.contains(repalce_column)
                        || code.contains(repalce_symbol);
            }
            if (count_value) {
                declaration.append("                var ").append(labelName).append("Symbol = myStack.peek(").append(offset).append(");\n");
                code = StringUtil.replace(code, new String[]{
                    repalce_value, repalce_line, repalce_column, repalce_symbol
                }, new String[]{
                    stackTypeString + labelName + "Symbol.value",
                    labelName.concat("Symbol.line"),
                    labelName.concat("Symbol.column"),
                    labelName.concat("Symbol")
                });
            } else {
                code = StringUtil.replace(code, repalce_value, stackTypeString + "myStack.peek(" + offset + ").value");
                code = code.replaceAll("yield \\([a-zA-Z0-9_$]+\\) (myStack\\.peek\\([0-9]+\\)\\.value;)", "yield $1");
            }
        }
        return declaration.append(code).toString();
    }

    /**
     * Check to see if the Production (now) appears to be nullable. A Production is nullable if its RHS could derive the
     * empty string. This results when the RHS is empty or contains only non terminals which themselves are nullable.
     */
    public boolean checkNullable() {
        /* if we already know bail out early */
        if (_nullable_known) {
            return _nullable;
        }

        /* if we have a zero size RHS we are directly nullable */
        if (this.rhs.length == 0) {
            _nullable_known = true;
            _nullable = true;
            return true;
        }

        /* otherwise we need to test iterator of our parts */
        for (int pos = 0; pos < this.rhs.length; pos++) {
            symbol sym = this.rhs[pos].sym();

            /* if its a Terminal we are definitely not nullable */
            if (sym instanceof Terminal) {
                _nullable_known = true;
                _nullable = false;
                return false;
            } else if (!((NonTerminal) sym).nullable()) {
                /* this one not (yet) nullable, so we aren't */
                return false;
            }
        }

        /* if we make it here iterator parts are nullable */
        _nullable_known = true;
        _nullable = true;
        return true;
    }

    /**
     * Update (and return) the first set based on current NT firsts. This assumes that nullability has already been
     * computed for iterator non terminals and productions.
     */
    public TerminalSet checkFirstSet() {
        /* walk down the right hand side till we get past iterator nullables */
        for (ProductionItem rh : this.rhs) {
            symbol sym = rh.sym();
            /* is it a non-Terminal?*/
            if (sym instanceof NonTerminal) {
                /* add in current firsts from that NT */
                _first_set.add(((NonTerminal) sym).firstSet);

                /* if its not nullable, we are done */
                if (!((NonTerminal) sym).nullable()) {
                    break;
                }
            } else {
                /* its a Terminal -- add that to the set */
                _first_set.add((Terminal) sym);

                /* we are done */
                break;
            }
        }

        return first_set();
    }

    @Override
    public String toString() {
        StringBuilder result
                = new StringBuilder(lhs.sym().name)
                        .append(" ::= ");
        for (ProductionItem rh : this.rhs) {
            result.append(rh.sym().name).append(' ');
        }
        return result.toString();
    }
}
