package java_cup;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java_cup.ast.SymbolPart;
import java_cup.ast.ShiftAction;
import java_cup.ast.ReduceAction;
import java_cup.ast.Terminal;
import java_cup.ast.NonTerminal;
import java_cup.ast.Production;
import java_cup.ast.ActionProduction;
import java_cup.ast.Action;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;
import java.util.Date;
import java.util.Iterator;

/**
 * This class handles emitting generated code for the resulting parser. The
 * various parse tables must be constructed, etc. before citeratoring any
 * routines in this class.<p>
 *
 * Three classes are produced by this code:
 * <dl>
 * <dt> symbol constant class
 * <dd> this contains constant declarations for each Terminal (and
 * optioniteratory each non-Terminal).
 * <dt> action class
 * <dd> this non-public class contains code to invoke iterator the user actions
 * that were embedded in the parser specification.
 * <dt> parser class
 * <dd> the specialized parser class consisting primarily of some user supplied
 * general and initialization code, and the parse tables.
 * </dl><p>
 *
 * Three parse tables are created as part of the parser class:
 * <dl>
 * <dt> Production table
 * <dd> lists the LHS non Terminal size, and the length of the RHS of each
 * Production.
 * <dt> action table
 * <dd> for each state of the parse machine, gives the action to be taken
 * (shift, reduce, or error) under each lookahead symbol.<br>
 * <dt> reduce-goto table
 * <dd> when a reduce on a given Production is taken, the parse stack is popped
 * back a size of elements corresponding to the RHS of the Production. This
 * reveals a prior state, which we transition out of under the LHS non Terminal
 * symbol for the Production (as if we had seen the LHS symbol rather than
 * iterator the symbols matching the RHS). This table is indexed by non Terminal
 * sizes and indicates how to make these transitions.
 * </dl><p>
 *
 * In addition to the method interface, this class maintains a series of public
 * global variables and flags indicating how misc. parts of the code and other
 * output is to be produced, and counting things such as size of conflicts
 * detected (see the source code and public variables below for more details)
 * .<p>
 *
 * This class is "static" (contains only static data and methods)
 * .<p>
 *
 * @see java_cup.main
 * @Version last update: 11/25/95
 * @author Scott Hudson
 */

/* Major externally callable routines here include:
 symbols               - Emit the symbol constant class 
 parser                - Emit the parser class

 In addition the following major internal routines are provided:
 emit_package          - Emit a package declaration
 emit_action_code      - Emit the class containing the user's actions 
 emit_production_table - Emit declaration and init for the Production table
 do_action_table       - Emit declaration and init for the action table
 do_reduce_table       - Emit declaration and init for the reduce-goto table

 Finally, this class uses a size of public instance variables to communicate
 optional parameters and flags used to control how code is generated,
 as well as to report counts of various things (such as size of conflicts
 detected).  These include:

 prefix                  - a prefix string used to prefix names that would 
 otherwise "pollute" someone else's name space.
 package_name            - name of the package emitted code is placed in 
 (or null for an unnamed package.
 symbol_const_class_name - name of the class containing symbol constants.
 parser_class_name       - name of the class for the resulting parser.
 action_code             - user supplied declarations and other code to be 
 placed in action class.
 parser_code             - user supplied declarations and other code to be 
 placed in parser class.
 init_code               - user supplied code to be executed as the parser 
 is being initialized.
 scan_code               - user supplied code to get the next Symbol.
 start_production        - the start Production for the grammar.
 import_list             - list of imports for use with action class.
 num_conflicts           - size of conflicts detected. 
 nowarn                  - true if we are not to issue warning messages.
 not_reduced             - count of size of productions that never reduce.
 unused_term             - count of unused Terminal symbols.
 unused_non_term         - count of unused non Terminal symbols.
 *_time                  - a series of symbols indicating how long various
 sub-parts of code generation took (used to produce
 optional time reports in main).
 */
public class Emit {

    /*-----------------------------------------------------------*/
    /*--- Constructor(s) ----------------------------------------*/
    /*-----------------------------------------------------------*/
    /**
     * Only constructor is private so no instances can be created.
     */
    private Emit() {
    }

    /*-----------------------------------------------------------*/
    /*--- Static (Class) Variables ------------------------------*/
    /*-----------------------------------------------------------*/
    /**
     * The prefix placed on names that pollute someone else's name space.
     */
    public static String prefix = "CUP$";

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Package that the resulting code goes into (null is used for unnamed).
     */
    public static String package_name = null;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Name of the generated class for symbol constants.
     */
    public static String symbol_const_class_name = "sym";

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Name of the generated parser class.
     */
    public static String parser_class_name = "Parser";


    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * TUM changes; proposed by Henning Niss 20050628: Type arguments for class
     * declaration
     */
    public static String class_type_argument = null;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * User declarations for direct inclusion in user action class.
     */
    public static String action_code = null;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * User declarations for direct inclusion in parser class.
     */
    public static String parser_code = null;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * User code for user_init() which is citeratored during parser
     * initialization.
     */
    public static String init_code = null;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * User code for scan() which is citeratored to get the next Symbol.
     */
    public static String scan_code = null;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * The start Production of the grammar.
     */
    public static Production start_production = null;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * List of imports (Strings containing class names) to go with actions.
     */
    public static Stack import_list = new Stack();

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Number of conflict found while building tables.
     */
    public static int num_conflicts = 0;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Do we skip warnings?
     */
    public static boolean nowarn = false;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Count of the size on non-reduced productions found.
     */
    public static int not_reduced = 0;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Count of unused terminals.
     */
    public static int unused_term = 0;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Count of unused non terminals.
     */
    public static int unused_non_term = 0;

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

    /* Timing values used to produce timing report in main.*/
    /**
     * Time to produce symbol constant class.
     */
    public static long symbols_time = 0;

    /**
     * Time to produce parser class.
     */
    public static long parser_time = 0;

    /**
     * Time to produce action code class.
     */
    public static long action_code_time = 0;

    /**
     * Time to produce the Production table.
     */
    public static long production_table_time = 0;

    /**
     * Time to produce the action table.
     */
    public static long action_table_time = 0;

    /**
     * Time to produce the reduce-goto table.
     */
    public static long goto_table_time = 0;

    /* frankf 6/18/96 */
    protected static boolean _lr_values;

    /**
     * whether or not to Emit code for left and right values
     */
    public static boolean lr_values() {
        return _lr_values;
    }

    protected static void set_lr_values(boolean b) {
        _lr_values = b;
    }

    //Hm Added clear  to clear iterator static fields
    public static void clear() {
        _lr_values = true;
        action_code = null;
        import_list = new Stack();
        init_code = null;
        not_reduced = 0;
        num_conflicts = 0;
        package_name = null;
        parser_class_name = "Parser";
        parser_code = null;
        scan_code = null;
        start_production = null;
        symbol_const_class_name = "sym";
        unused_non_term = 0;
        unused_term = 0;
    }

    /*-----------------------------------------------------------*/
    /*--- General Methods ---------------------------------------*/
    /*-----------------------------------------------------------*/
    /**
     * Build a string with the standard prefix.
     *
     * @param str string to prefix.
     */
    /**
     * TUM changes; proposed by Henning Niss 20050628 Build a string with the
     * specified type arguments, if present, otherwise an empty string.
     */
    protected static String typeArgument() {
        return class_type_argument == null ? "" : "<" + class_type_argument + ">";
    }

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Emit a package spec if the user wants one.
     *
     * @param out stream to produce output on.
     */
    protected static void emit_package(PrintWriter out) {
        /* generate a package spec if we have a name for one */
        if (package_name != null) {
            out.println("package " + package_name + ";");
            out.println();
        }
    }

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Emit code for the symbol constant class, optioniteratory including non
     * terms, if they have been requested.
     *
     * @param out stream to produce output on.
     * @param emit_non_terms do we Emit constants for non terminals?
     * @param sym_interface should we Emit an interface, rather than a class?
     */
    public static void symbols(PrintWriter out,
            boolean emit_non_terms, boolean sym_interface) {
        Terminal term;
        String class_or_interface = (sym_interface) ? "interface" : "class";

        long start_time = System.currentTimeMillis();

        /* top of file */
        out.println();
        out.println("//----------------------------------------------------");
        out.println("// The following code was generated by "
                + Version.title_str);
        out.println("// " + new Date());
        out.println("//----------------------------------------------------");
        out.println();
        emit_package(out);

        /* class header */
        out.println("public " + class_or_interface + " "
                + symbol_const_class_name + " {");
        out.println();
        out.println("    /* terminals */");

        /* walk over the terminals */              /* later might sort these */
        for (Iterator<Terminal> it = Terminal.iterator(); it.hasNext();) {
            term = it.next();
            /* output a constant decl for the Terminal */
            out.println("    public static final int " + term.name() + " = "
                    + term.id() + ";");
        }

        /* do the non terminals if they want them (parser doesn't need them) */
//        if (emit_non_terms) {
        if (true) {
            out.println();
            out.println("    /* non terminals */");

            /* walk over the non terminals */       /* later might sort these */

            for (Iterator<NonTerminal> it = NonTerminal.iterator(); it.hasNext();) {
                NonTerminal nt = it.next();
                // ****
                // TUM Comment: here we could add a typesafe enumeration
                // ****

                /* output a constant decl for the Terminal */
                out.println("//    static final int " + nt.name() + " = "
                        + nt.id() + ";");
            }
        }

        /* end of class */
        out.println("}");
        out.println();

        symbols_time = System.currentTimeMillis() - start_time;
    }

    private static class ProductionCodeWrap implements Comparable<ProductionCodeWrap> {

        final int id;
        final String remark;
        final String caseBody;
        boolean useNext = false;

        public ProductionCodeWrap(int id, String remark, String caseBody) {
            this.id = id;
            this.remark = remark;
            this.caseBody = caseBody;
        }

        public int compareTo(ProductionCodeWrap o) {
            int result;
            if ((result = this.caseBody.compareTo(o.caseBody)) == 0) {
                result = this.id - o.id;
            }
            return result;
        }
    }

    private static ArrayList<ProductionCodeWrap> resolveProductionCodeWraps(Production start_prod) throws InternalException {
        //collect
        final ArrayList<Production> productions = Production.getAll();
        final ArrayList<ProductionCodeWrap> list = new ArrayList<ProductionCodeWrap>(productions.size());
        Production prod;
        //ProductionCodeWrap codeWrap;
        StringBuilder buffer;
        for (int pI = 0, size = productions.size(); pI < size; pI++) {
            prod = productions.get(pI);
            buffer = new StringBuilder();
            //

            /* if this was the start Production, do action for accept */
            if (prod == start_prod) {
                buffer.append("                /* ACCEPT */\n");
                buffer.append("                this.goonParse = false;\n");
            }

            /**
             * TUM 20060608 intermediate result patch
             */
            //buffer.append("\t\t//RESULT_DEBUG: " + prod.lhs().the_symbol().stack_type() + " RESULT;\n");
            if (prod instanceof ActionProduction) {
                int lastResult = ((ActionProduction) prod).getIndexOfIntermediateResult();
                if (lastResult != -1) {
                    //type: (" + prod.lhs().the_symbol().stack_type() + ")
                    buffer.append("                return myStack.peek(" + (lastResult - 1) + ").value;\n");
                }
            }


            /* Add code to propagate RESULT assignments that occur in
             * action code embedded in a Production (ie, non-rightmost
             * action code). 24-Mar-1998 CSA
             */
            for (int i = prod.rhs_length() - 1; i >= 0; i--) {
                // only interested in non-Terminal symbols.
                if (!(prod.rhs(i) instanceof SymbolPart)) {
                    continue;
                }
                symbol s = ((SymbolPart) prod.rhs(i)).the_symbol();
                if (!(s instanceof NonTerminal)) {
                    continue;
                }
                // skip this non-Terminal unless it corresponds to
                // an embedded action Production.
                if (((NonTerminal) s).is_embedded_action == false) {
                    continue;
                }
                // OK, it fits.  Make a conditional assignment to RESULT.
                int offset = prod.rhs_length() - i - 1; // last rhs is on top.
                // set comment to inform about where the intermediate result came from
                buffer.append("                " + "// propagate RESULT from " + s.name());
                buffer.append('\n');
//            // look out, whether the intermediate result is null or not
//	    buffer.append("              " + "if ( " +
//	      "((" + runtime_pkg_name + ".Symbol) " + Emit.pre("stack") + 
//			// TUM 20050917
//			((id==0)?".peek()":(".elementAt(" + Emit.pre("top") + "-" + id + ")"))+
//			").value != null )");

// TUM 20060608: even when its null: who cares?
                // store the intermediate result into RESULT
                buffer.append("                return (" + prod.lhs().the_symbol().stack_type() + ") myStack.peek(" + offset + ").value;\n");
                break;
            }

            /* if there is an action string, Emit it */
            if (prod.action() != null && prod.action().code_string() != null
                    && !prod.action().equals("")) {
                buffer.append(prod.action().code_string());
            }

            /* code to return lhs symbol */
            //buffer.append("\n\t\treturn RESULT;");
            if (buffer.indexOf("return ") < 0) {
                throw new RuntimeException("Production must has a 'return':" + prod.to_simple_string());
            }
            list.add(new ProductionCodeWrap(prod.id(), prod.to_simple_string(), buffer.toString()));
        }

        //Sort
        Collections.sort(list);
        //check
        ProductionCodeWrap preCodeWrap = list.get(0);
        for (int i = 1, len = list.size(); i < len; i++) {
            ProductionCodeWrap currentCodeWrap = list.get(i);
            if (preCodeWrap.caseBody.equals(currentCodeWrap.caseBody)) {
                preCodeWrap.useNext = true;
            }
            preCodeWrap = currentCodeWrap;
        }

        //return
        return list;
    }

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Emit code for the non-public class holding the actual action code.
     *
     * @param out stream to produce output on.
     * @param start_prod the start Production of the grammar.
     */
    protected static void emit_action_code(PrintWriter out, Production start_prod)
            throws InternalException {
        //Production prod;

        long start_time = System.currentTimeMillis();

        /* declaration of result symbol */
        /* New declaration!! now return Symbol
         6/13/96 frankf */
        //out.println("      /* Symbol object for return from actions */");
        //out.println("      " + runtime_pkg_name + ".Symbol " + pre("result") + ";");
        //out.println("      /* SymbolFactory object for create Symbol object */");
        //out.println("      " + runtime_pkg_name + ".SymbolFactory " + pre("SymbolFactory") + " = parser.getSymbolFactory();");
        out.println("        final Stack<Symbol> myStack = this.symbolStack;");
//        out.println("      //RESULT_DEBUG: /*");
//        out.println("      Object RESULT;");
//        out.println("      //RESULT_DEBUG: */");

        out.println();

        /* switch top */
        out.println("        switch (actionId){");

        ArrayList<ProductionCodeWrap> codeWraps = resolveProductionCodeWraps(start_prod);
        ProductionCodeWrap codeWrap;
        for (int i = 0, len = codeWraps.size(); i < len; i++) {
            codeWrap = codeWraps.get(i);

            /* case label */
            out.println("            case " + codeWrap.id + ": // " + codeWrap.remark);
            if (codeWrap.useNext == false) {
                /* give them their own block to work in */
                boolean needWrap = !codeWrap.caseBody.trim().startsWith("return ");
                if (needWrap) {
                    out.println("            {");
                }
                out.println(codeWrap.caseBody);
                if (needWrap) {
                    out.println("            }");
                }
            }

        }

        /* end of switch */
        out.println("            default:");
        out.println("                throw new ParseException(\"Invalid action id.\");");
        out.println("        }");

        action_code_time = System.currentTimeMillis() - start_time;
    }

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Emit the Production table.
     *
     * @param out stream to produce output on.
     */
    protected static void emit_production_table(PrintWriter out) {
        long start_time = System.currentTimeMillis();

        // make short[][]
        short[][] prod_table = new short[Production.size()][2];
        for (Iterator<Production> p = Production.all(); p.hasNext();) {
            Production prod = p.next();
            int i = prod.id();
            // { lhs symbol , rhs size }
            prod_table[i][0] = (short) prod.lhs().the_symbol().id();
            prod_table[i][1] = (short) prod.rhs_length();
        }
        /* do the top of the table */
//        out.println("    static final short[][] PRODUCTION_TABLE = loadFromDataFile(\"Production\");");
//        out.print("    unpackFromStrings(");
//        do_table_as_string(out, prod_table);
//        out.println(");");

        saveToDataFile(prod_table, "Production");

//      /* do the public accessor method */
//      out.println();
//      out.println("  /** Access to production table. */");
//      out.println("  protected short[][] production_table() " + 
//						 "{return _production_table;}");
        production_table_time = System.currentTimeMillis() - start_time;
    }

    public final static int DEFAULT_REDUCE = -1;

    final static short[] EMPTY_SHORT_ARRAY = new short[0];

    /**
     * Emit the action table.
     *
     * @param out stream to produce output on.
     * @param act_tab the internal representation of the action table.
     * @param compact_reduces do we use the most frequent reduce as default?
     */
    protected static void do_action_table(PrintWriter out, parse_action_table act_tab)
            throws InternalException {
        parse_action_row row;
        Action act;
        int red;

        long start_time = System.currentTimeMillis();

        /* collect values for the action table */
        final int size_i = act_tab.num_states();

        final int size_j = parse_action_row.size();
        final short[][] action_table = new short[size_i][];
        final short[] temp_table = new short[2 * size_j];

        /* do each state (row) of the action table */
        for (int i = 0; i < size_i; i++) {
            /* get the row */
            Action[] row_under_term = (row = act_tab.under_state[i]).under_term;
            int nentries = 0;
            /* do each column */
            for (int j = 0; j < parse_action_row.size(); j++) {
                act = row_under_term[j];
                switch (act.kind()) {
                    case Action.ERROR:
                        // skip error entries these are iterator defaulted out
                        break;
                    case Action.SHIFT:
                        // shifts get positive entries of state size + 1
                        temp_table[nentries++] = (short) j;
                        temp_table[nentries++] = (short) (((ShiftAction) act).shift_to().index() + 1);
                        break;
                    case Action.REDUCE:
                        red = ((ReduceAction) act).reduce_with().id();
                        if (red != DEFAULT_REDUCE) {
                            temp_table[nentries++] = (short) j;
                            temp_table[nentries++] = (short) (-(red + 1));
                        }
                        break;
                    case Action.NONASSOC:
                        // do nothing, since we just want a syntax error
                        break;
                    default:
                        throw new InternalException("Unrecognized action code "
                                + act.kind() + " found in parse table");
                }
            }

            if (nentries != 0) {
                System.arraycopy(temp_table, 0, action_table[i] = new short[nentries], 0, nentries);
            } else {
                action_table[i] = EMPTY_SHORT_ARRAY;
            }
        }

        /* finish off the init of the table */
//        out.println("    static final short[][] ACTION_TABLE = loadFromDataFile(\"Action\");");
//        out.print("    unpackFromStrings(");
//        do_table_as_string(out, action_table);
//        out.println(");");
        saveToDataFile(action_table, "Action");

//      /* do the public accessor method */
//      out.println();
//      out.println("  /** Access to parse-action table. */");
//      out.println("  protected short[][] action_table() {return _action_table;}");
        action_table_time = System.currentTimeMillis() - start_time;
    }

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Emit the reduce-goto table.
     *
     * @param out stream to produce output on.
     * @param red_tab the internal representation of the reduce-goto table.
     */
    protected static void do_reduce_table(
            PrintWriter out,
            parse_reduce_table red_tab) {
        lalr_state goto_st;
        Action act;

        long start_time = System.currentTimeMillis();
        final int size_i = red_tab.num_states();
        final int size_j = parse_reduce_row.size();
        final short[] temp_table = new short[2 * size_j];
        final short[][] reduce_goto_table = new short[size_i][];
        /* do each row of the reduce-goto table */
        for (int i = 0; i < size_i; i++) {
            int nentries = 0;
            lalr_state[] under_non_term = red_tab.under_state[i].under_non_term;
            /* do each entry in the row */
            for (int j = 0; j < size_j; j++) {
                /* get the entry */
                goto_st = under_non_term[j];
                /* if we have none, skip it */
                if (goto_st != null) {
                    /* make entries for the id and the value */
                    temp_table[nentries++] = (short) j;
                    temp_table[nentries++] = (short) goto_st.index();
                }
            }

            if (nentries != 0) {
                System.arraycopy(temp_table, 0, reduce_goto_table[i] = new short[nentries], 0, nentries);
            } else {
                reduce_goto_table[i] = null; //EMPTY_SHORT_ARRAY;
            }
        }

        /* Emit the table. */
//        out.println("    static final short[][] REDUCE_TABLE = loadFromDataFile(\"Reduce\");");
//        out.print("    unpackFromStrings(");
//        do_table_as_string(out, reduce_goto_table);
//        out.println(");");
        saveToDataFile(reduce_goto_table, "Reduce");

//      /* do the public accessor method */
//      out.println();
//      out.println("  /** Access to <code>reduce_goto</code> table. */");
//      out.println("  protected short[][] reduce_table() {return _reduce_table;}");
//      out.println();
        goto_table_time = System.currentTimeMillis() - start_time;
    }

    protected static void saveToDataFile(Object obj, String name) {
        ObjectOutputStream o = null;
        try {

            FileOutputStream out = new FileOutputStream(Main.getDataFilePath(name));

            o = new ObjectOutputStream(out);
            o.writeObject(obj);

        } catch (IOException ioex) {
            throw new RuntimeException(ioex);
        } finally {
            if (o != null) {
                try {
                    o.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
    /**
     * Emit the parser subclass with embedded tables.
     *
     * @param out stream to produce output on.
     * @param action_table internal representation of the action table.
     * @param reduce_table internal representation of the reduce-goto table.
     * @param start_st start state of the parse machine.
     * @param start_prod start Production of the grammar.
     * @param suppress_scanner should scanner be suppressed for compatibility?
     */
    public static void parser(
            PrintWriter out,
            parse_action_table action_table,
            parse_reduce_table reduce_table,
            int start_st,
            Production start_prod,
            boolean suppress_scanner)
            throws InternalException {
        long start_time = System.currentTimeMillis();

        /* top of file */
        out.println();
        out.println("//----------------------------------------------------");
        out.println("// The following code was generated by "
                + Version.title_str);
        out.println("//----------------------------------------------------");
        out.println();
        emit_package(out);

        /* user supplied imports */
        for (int i = 0; i < import_list.size(); i++) {
            out.println("import " + import_list.elementAt(i) + ";");
        }

        /* class header */
        out.println();
        out.println("/**");
        out.println(" * ");
        out.println(" * @version " + new Date());
        out.println(" */");
        /* TUM changes; proposed by Henning Niss 20050628: added typeArgument */
        out.println("public class " + parser_class_name + typeArgument()
                + " extends AbstractParser {");

        out.println();

//      out.println("  private static final String[] _nonTerminalNames = new String[]{"); 
//      int i = 0;
//          for (Iterator<NonTerminal> it = NonTerminal.iterator(); it.hasNext();) {
//              NonTerminal nt = it.next();
//	      out.print("\"" + nt.name() + "\"");
//              if (it.hasNext()) {
//                  out.print(",");
//                    if (i%10 == 9) {
//                        out.println();
//                    }
//              }else{
//                  out.println();
//              }
//              i++;
//	    }
//      out.println("  };");
//
//      out.println();
//      out.println("  /** Access to <code>reduce_goto</code> table. */");
//      out.println("  protected String[] nonTerminalNames(){return _nonTerminalNames;}");
//      out.println();
        emit_production_table(out);

        do_action_table(out, action_table);
        do_reduce_table(out, reduce_table);

        assert start_st == 0;
//
//      /* method to indicate start Production */
//      out.println("  /** Indicates start production. */");
//      out.println("  protected int start_production() {return " + 
//		     start_production.id() + ";}");
//      out.println();
//
//      /* methods to indicate EOF and error symbol indexes */
//      out.println("  /** <code>EOF</code> Symbol index. */");
//      out.println("  protected int EOF_sym() {return " + Terminal.EOF.id() + ";}");
//      out.println();
//      out.println("  /** <code>error</code> Symbol index. */");
//      out.println("  protected int error_sym() {return " + Terminal.error.id() + ";}");
//      out.println();

        /* user supplied code */
        if (parser_code != null) {
            out.println(parser_code);
        }

        /* access to action code */
        out.println("    @SuppressWarnings(\"unchecked\")");
        out.println("    final Object doAction(int actionId) throws ParseException {");
        emit_action_code(out, start_prod);
        out.println("    }");
        out.println();

        /* end of class */
        out.println("}");

        parser_time = System.currentTimeMillis() - start_time;
    }

    /*-----------------------------------------------------------*/
}
