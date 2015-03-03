package java_cup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java_cup.core.Parser;

/**
 * This class serves as the main driver for the JavaCup system.
 */
public class Main {

    public static final String version = "v1.0";
    public static final String title = "CUP-WIT " + version;

    private static int errors = 0;
    private static int warnings = 0;

    protected static boolean optDumpStates = false;
    protected static boolean optDumpTables = false;
    protected static boolean optDumpGrammar = false;
    protected static int expectConflicts = 0;
    protected static File destDir = null;
    protected static File destResourceDir = null;

    protected static LalrState startState;
    protected static Action[][] actionTable;
    protected static LalrState[][] reduceTable;

    public static String packageName;
    public static final List<String> imports = new ArrayList<String>();
    public static String parserClassName;
    public static int unusedTermCount = 0;
    public static int conflictCount = 0;
    public static String actionExceptionClassName;
    public static Production startProduction = null;
    public static String tokensClassName;
    public static int unusedNonTermCount = 0;
    static final short[] EMPTY_SHORT_ARRAY = new short[0];
    public static int notReducedCount = 0;

    private static void clear() {
        Main.imports.clear();
        Main.parserClassName = "Parser";
        Main.tokensClassName = "Tokens";
        Main.packageName = null;
        Main.actionExceptionClassName = null;
        Main.startProduction = null;
        Main.destDir = null;
        Main.destResourceDir = null;
        Main.errors = 0;
        Main.warnings = 0;
        Main.conflictCount = 0;
        Main.notReducedCount = 0;
        Main.unusedNonTermCount = 0;
        Main.unusedTermCount = 0;
        Main.expectConflicts = 0;
        Main.optDumpStates = Main.optDumpGrammar = Main.optDumpTables = false;
        LalrState.clear();
        Production.clear();
        NonTerminal.clear();
        Terminal.clear();
    }

    public static void main(String args[]) throws InternalException, java.io.IOException, java.lang.Exception {

        clear();

        parseArgs(args);

        System.err.println("Parsing...");
        new Parser().parse();

        boolean written = false;
        if (Main.errors == 0) {

            System.err.println("Checking...");
            checkUnused();

            System.err.println("Building tables...");
            buildParser();

            if (Main.errors != 0) {
                optDumpTables = false;
            } else {
                System.err.println("Writing...");

                emitProductionData();
                emitActionData();
                emitReduceData();

                PrintWriter parserWriter = new PrintWriter(
                        new BufferedOutputStream(new FileOutputStream(new File(destDir, parserClassName + ".java")), 4096));
                PrintWriter symbolWriter = new PrintWriter(
                        new BufferedOutputStream(new FileOutputStream(new File(destDir, tokensClassName + ".java")), 4096));

                emitTokens(symbolWriter);
                emitParser(parserWriter);
                parserWriter.close();
                symbolWriter.close();
                written = true;
            }
        }

        if (optDumpGrammar) {
            dumpGrammar();
        }
        if (optDumpStates) {
            dumpMachine();
        }
        if (optDumpTables) {
            dumpTables();
        }

        printSummary(written);

        if (Main.errors != 0) {
            System.exit(100);
        }
    }

    private static void usage(String message) {
        System.err.println();
        System.err.println(message);
        System.err.println();
        System.err.println(
                "Usage: jcup [options] [filename]\n"
                + "  Legal options:\n"
                + "    -destdir name  specify the destination directory\n"
                + "    -parser name   specify parser class name [default \"Parser\"]\n"
                + "    -symbols name  specify tokens class name [default \"Tokens\"]\n"
                + "    -dump_grammar  produce a dump of the symbols and grammar\n"
                + "    -dump_states   produce a dump of parse state machine\n"
                + "    -dump_tables   produce a dump of the parse tables\n"
                + "    -dump          produce a dump of all of the above\n"
                + "    -version\n"
        );
        System.exit(1);
    }

    private static void parseArgs(String args[]) {
        int len = args.length;
        for (int i = 0; i < len;) {
            final String arg = args[i++];
            if (arg.equals("-dump_states")) {
                optDumpStates = true;
            } else if (arg.equals("-dump_tables")) {
                optDumpTables = true;
            } else if (arg.equals("-dump_grammar")) {
                optDumpGrammar = true;
            } else if (arg.equals("-dump")) {
                optDumpStates = optDumpTables = optDumpGrammar = true;
            } else if (arg.equals("-version")) {
                System.out.println(Main.title);
                System.exit(1);
            } else if (i == len) {
                try {
                    System.setIn(new FileInputStream(arg));
                } catch (java.io.FileNotFoundException e) {
                    usage("Unable to open \"" + arg + "\" for input");
                }
            } else {
                final String nextArg = args[i++];
                if (i == len) {
                    usage("Unrecognized option or must have a value argument:" + arg);
                    return;
                }
                if (arg.equals("-destdir")) {
                    Main.destDir = new java.io.File(nextArg);
                    if (!Main.destDir.exists()) {
                        Main.destDir.mkdirs();
                    }
                } else if (arg.equals("-destresdir")) {
                    Main.destResourceDir = new java.io.File(nextArg);
                    if (!Main.destResourceDir.exists()) {
                        Main.destResourceDir.mkdirs();
                    }
                } else if (arg.equals("-parser")) {
                    parserClassName = nextArg;
                } else if (arg.equals("-exception")) {
                    actionExceptionClassName = nextArg;
                } else if (arg.equals("-symbols")) {
                    tokensClassName = nextArg;
                } else if (arg.equals("-expect")) {
                    try {
                        expectConflicts = Integer.parseInt(nextArg);
                    } catch (NumberFormatException e) {
                        usage("-expect must be followed by a int");
                    }
                } else {
                    usage("Unrecognized option \"" + arg + "\"");
                }
            }
        }
    }

    protected static void buildParser() {

        System.err.println("  Computing non-terminal nullability...");
        computeNullability();

        System.err.println("  Computing first sets...");
        computeFirstSets();

        System.err.println("  Building state machine...");
        startState = LalrState.buildMachine();
        if (startState.id != 0) {
            throw new InternalException("Start state must be zero!");
        }

        System.err.println("  Filling in tables...");

        {
            final int rowSize = LalrState.all.size();
            actionTable = new Action[rowSize][];
            int size = Terminal.size();
            for (int i = 0; i < rowSize; i++) {
                actionTable[i] = new Action[size];
                Arrays.fill(actionTable[i], Action.ERROR_ACTION);
            }
        }
        {
            final int rowSize = LalrState.all.size();
            reduceTable = new LalrState[rowSize][];
            int size = NonTerminal.all.size();
            for (int i = 0; i < rowSize; i++) {
                reduceTable[i] = new LalrState[size];
            }
        }

        for (LalrState state : LalrState.all()) {
            state.buildTableEntries(actionTable, reduceTable);
        }

        System.err.println("  Checking productions...");
        checkReductions();

        if (conflictCount > expectConflicts) {
            error("*** More conflicts encountered than expected -- parser generation aborted");
        }
    }

    private static void checkUnused() {
        for (Terminal term : Terminal.all) {
            if (!term.isUsed()) {
                unusedTermCount++;
                warning("Terminal \"" + term.name + "\" was declared but never used");
            }
        }
        for (NonTerminal nt : NonTerminal.all) {
            if (!nt.isUsed()) {
                unusedTermCount++;
                warning("Non terminal \"" + nt.name + "\" was declared but never used");
            }
        }
    }

    private static void checkReductions() {
        for (Action[] actions : actionTable) {
            for (Action act : actions) {
                if (act != null && act.type() == Action.REDUCE) {
                    (((ReduceAction) act).reduceWith).reductionUse();
                }
            }
        }
        for (Production prod : Production.all) {
            if (!prod.reductionUsed()) {
                notReducedCount++;
                warning("*** Production \"" + prod + "\" never reduced");
            }
        }
    }

    private static void computeNullability() {
        boolean change;
        do {
            change = false;
            for (NonTerminal nt : NonTerminal.all) {
                if (!nt.nullable()) {
                    if (nt.looksNullable()) {
                        change = true;
                    }
                }
            }
        } while (change);

        for (Production prod : Production.all) {
            prod.checkNullable();
        }
    }

    /**
     * Compute first sets for each non-terminal. This assumes nullability has
     * already computed.
     */
    private static void computeFirstSets() {
        boolean change;
        do {
            change = false;
            for (NonTerminal nt : NonTerminal.all) {
                for (Production prod : nt.productions) {
                    TerminalSet prodFirstSet = prod.checkFirstSet();
                    if (!prodFirstSet.isSubOf(nt.firstSet)) {
                        change = true;
                        nt.firstSet.add(prodFirstSet);
                    }
                }
            }
        } while (change);
    }

    public static void emitParser(PrintWriter out) {

        emitPackage(out);

        for (String item : Main.imports) {
            out.println("import " + item + ";");
        }

        out.println();
        out.println("/**");
        out.println(" * ");
        out.println(" * @version " + new Date());
        out.println(" */");
        out.println("public class " + Main.parserClassName + " extends AbstractParser {");
        out.println();

        out.println("    @SuppressWarnings(\"unchecked\")");
        out.println("    final Object doAction(int actionId)" + (Main.actionExceptionClassName != null ? (" throws " + Main.actionExceptionClassName) : "") + " {");

        out.println("        final Stack<Symbol> myStack = this.symbolStack;");
        out.println();
        out.println("        switch (actionId){");

        final ArrayList<Production> prods = new ArrayList<Production>(Production.all);
        Collections.sort(prods);
        String lastCode = null;
        for (Production prod : prods) {
            if (lastCode != null && !lastCode.equals(prod.code)) {
                emitParserActionCaseCode(out, lastCode);
            }
            lastCode = prod.code;
            out.println("            case " + prod.id + ": // " + prod);
        }
        emitParserActionCaseCode(out, lastCode);
        out.println("            default:");
        out.println("                throw new RuntimeException(\"Invalid action id.\");");
        out.println("        }");
        out.println("    }");
        out.println("}");
    }

    private static void emitParserActionCaseCode(PrintWriter out, String code) {
        boolean needWrap = !code.startsWith("return ");
        if (needWrap) {
            out.println("            {");
        }
        out.println(code);
        if (needWrap) {
            out.println("            }");
        }
    }

    public static void emitTokens(PrintWriter out) {
        emitPackage(out);
        out.println("public interface " + Main.tokensClassName + " {");
        out.println();
        out.println("    /* terminals */");
        for (Terminal term : Terminal.all) {
            out.println("    int " + term.name + " = " + term.id + ";");
        }
        out.println();
        out.println("    /* non terminals */");
        for (NonTerminal nt : NonTerminal.all) {
            out.println("    //int " + nt.name + " = " + nt.id + ";");
        }
        out.println("}");
        out.println();
    }

    private static void emitPackage(PrintWriter out) {
        out.println();
        out.println("//----------------------------------------------------");
        out.println("// The following code was generated by " + Main.title);
        out.println("//----------------------------------------------------");
        out.println();
        if (Main.packageName != null) {
            out.println("package " + Main.packageName + ";");
            out.println();
        }
    }

    private static void emitProductionData() {
        short[][] prod_table = new short[Production.all.size()][2];
        for (Production prod : Production.all) {
            int i = prod.id;
            // { lhs symbol , rhs size }
            prod_table[i][0] = (short) prod.lhs.sym.id;
            prod_table[i][1] = (short) prod.rhs.length;
        }

        saveToDataFile(prod_table, "Production");
    }

    private static void emitActionData() {

        final short[][] action_table = new short[actionTable.length][];
        final short[] temp_table = new short[2 * actionTable[0].length];

        for (int i = 0; i < actionTable.length; i++) {
            Action[] row_under_term = actionTable[i];
            int nentries = 0;
            for (int j = 0; j < row_under_term.length; j++) {
                Action act = row_under_term[j];
                switch (act.type()) {
                    case Action.NONASSOC:
                        // do nothing, since we just want a syntax error
                        break;
                    case Action.ERROR:
                        // skip error entries these are iterator defaulted out
                        break;
                    case Action.SHIFT:
                        temp_table[nentries++] = (short) j;
                        temp_table[nentries++] = (short) (((ShiftAction) act).shiftTo.id + 1);
                        break;
                    case Action.REDUCE:
                        temp_table[nentries++] = (short) j;
                        temp_table[nentries++] = (short) (-(((ReduceAction) act).reduceWith.id + 1));
                        break;
                    default:
                        throw new InternalException("Unrecognized action code " + act.type() + " found in parse table");
                }
            }

            if (nentries != 0) {
                System.arraycopy(temp_table, 0, action_table[i] = new short[nentries], 0, nentries);
            } else {
                action_table[i] = Main.EMPTY_SHORT_ARRAY;
            }
        }
        saveToDataFile(action_table, "Action");
    }

    private static void emitReduceData() {

        final short[][] reduce_goto_table = new short[reduceTable.length][];
        final short[] temp_table = new short[2 * reduceTable[0].length];
        for (int i = 0; i < reduceTable.length; i++) {
            int nentries = 0;
            LalrState[] states = reduceTable[i];
            for (int j = 0; j < states.length; j++) {
                LalrState state = states[j];
                if (state != null) {
                    temp_table[nentries++] = (short) j;
                    temp_table[nentries++] = (short) state.id;
                }
            }

            if (nentries != 0) {
                System.arraycopy(temp_table, 0, reduce_goto_table[i] = new short[nentries], 0, nentries);
            } else {
                reduce_goto_table[i] = null; //EMPTY_SHORT_ARRAY;
            }
        }

        saveToDataFile(reduce_goto_table, "Reduce");
    }

    private static void saveToDataFile(Object obj, String name) {
        ObjectOutputStream o = null;
        try {

            FileOutputStream out = new FileOutputStream(new File(Main.destResourceDir, Main.parserClassName + "$" + name + ".data"));

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

    public static void reportShiftReduceConflict(LalrState state, LalrItem red_itm, int conflictSymbol) {

        StringBuilder message = new StringBuilder()
                .append("*** Shift/Reduce conflict found in state #").append(state.id)
                .append("\n" + "  between ").append(red_itm).append("\n");

        /* get and report on iterator items that shift under our conflict symbol */
        for (LalrItem itm : state.items.values()) {

            /* only look if its not the same item and not a reduce */
            if (itm != red_itm && !itm.dotAtEnd) {
                /* is it a shift on our conflicting Terminal */
                symbol shift_sym = itm.symbolAfterDot;
                if ((shift_sym instanceof Terminal) && shift_sym.id == conflictSymbol) {
                    /* yes, report on it */
                    message.append("  and     ").append(itm).append('\n');
                }
            }
        }
        message.append("  under symbol ").append(Terminal.get(conflictSymbol).name)
                .append("\n  Resolved in favor of shifting.\n");

        /* count the conflict */
        Main.conflictCount++;
        Main.warning(message.toString());
    }

    /**
     * Produce a warning message for one reduce/reduce conflict.
     *
     * @param state
     * @param itm1 first item in conflict.
     * @param itm2 second item in conflict.
     */
    public static void reportReduceReduceConflict(LalrState state, LalrItem itm1, LalrItem itm2) {

        StringBuilder message = new StringBuilder()
                .append("*** Reduce/Reduce conflict found in state #").append(state.id)
                .append("\n  between ").append(itm1.toString())
                .append("\n  and     ").append(itm2.toString())
                .append("\n  under symbols: {");
        boolean comma_flag = false;
        for (int t = 0; t < Terminal.size(); t++) {
            if ((itm1.lookahead).contains(t) && (itm2.lookahead).contains(t)) {
                if (comma_flag) {
                    message.append(',');
                } else {
                    comma_flag = true;
                }
                message.append(Terminal.get(t).name);
            }
        }
        message.append("}\n  Resolved in favor of ");
        if ((itm1.production).id < (itm2.production).id) {
            message.append("the first production.\n");
        } else {
            message.append("the second production.\n");
        }
        /* count the conflict */
        Main.conflictCount++;
        Main.warning(message.toString());
    }

    private static void printSummary(boolean written) {

        System.err.println("------- " + Main.title + " Parser Generation Summary -------");

        System.err.println("  " + Main.errors + " errors and " + Main.warnings + " warnings");

        System.err.print("  " + Terminal.size() + " terminals, ");
        System.err.print(NonTerminal.all.size() + " non-terminals, and ");
        System.err.println(Production.all.size() + " productions declared, ");
        System.err.println("  producing " + LalrState.all.size() + " unique parse states.");

        System.err.println("  " + unusedTermCount + " terminals declared but not used.");
        System.err.println("  " + unusedNonTermCount + " non-terminals declared but not used.");
        System.err.println("  " + notReducedCount + " productions never reduced.");
        System.err.println("  " + conflictCount + " conflicts detected" + " (" + expectConflicts + " expected).");

        if (written) {
            System.err.println("  Code written to \"" + parserClassName + ".java\", and \"" + tokensClassName + ".java\".");
        } else {
            System.err.println("  No code produced.");
        }

        System.err.println("---------------------------------------------------- ");
    }

    public static void dumpGrammar() {
        System.err.println("===== Terminals =====");
        for (int i = 0; i < Terminal.size(); i++) {
            System.err.print("[" + i + ']' + Terminal.get(i).name + ' ');
            if ((i + 1) % 5 == 0) {
                System.err.println();
            }
        }
        System.err.println();
        System.err.println();

        System.err.println("===== Non terminals =====");
        for (int i = 0; i < NonTerminal.all.size(); i++) {
            System.err.print("[" + i + ']' + NonTerminal.all.get(i).name + ' ');
            if ((i + 1) % 5 == 0) {
                System.err.println();
            }
        }
        System.err.println();
        System.err.println();

        System.err.println("===== Productions =====");
        for (int i = 0; i < Production.all.size(); i++) {
            Production prod = Production.all.get(i);
            System.err.print("[" + i + "] " + ((prod.lhs).sym).name + " ::= ");
            for (ProductionItem rh : prod.rhs) {
                System.err.print((rh.sym).name + ' ');
            }
            System.err.println();
        }
        System.err.println();
    }

    public static void dumpMachine() {
        LalrState ordered[] = new LalrState[LalrState.all.size()];

        for (LalrState state : LalrState.all()) {
            ordered[(state.id)] = state;
        }

        System.err.println("===== Viable Prefix Recognizer =====");
        for (int i = 0; i < LalrState.all.size(); i++) {
            if (ordered[i] == startState) {
                System.err.print("START ");
            }
            System.err.println(ordered[i]);
            System.err.println("-------------------");
        }
    }

    public static void dumpTables() {
        final PrintStream err = System.err;

        err.println("-------- ACTION_TABLE -------- ");
        for (int row = 0; row < actionTable.length; row++) {
            err.println("From state #" + row);
            int cnt = 0;
            Action[] actions = actionTable[row];
            for (int col = 0; col < actions.length; col++) {
                /* if the code is not an error print it */
                if (actions[col].type() != Action.ERROR) {
                    err.print(" [term " + col + ':' + actions[col] + ']');
                    /* end the line after the 2nd one */
                    cnt++;
                    if (cnt == 2) {
                        err.println();
                        cnt = 0;
                    }
                }
            }
            /* finish the line if we haven't just done that */
            if (cnt != 0) {
                err.println();
            }
        }
        err.println("------------------------------");

        err.println("-------- REDUCE_TABLE --------");
        for (int row = 0; row < reduceTable.length; row++) {
            err.println("From state #" + row);
            int cnt = 0;
            LalrState[] states = reduceTable[row];
            for (int col = 0; col < states.length; col++) {
                LalrState goto_st = states[col];

                if (goto_st != null) {
                    err.print(" [non term " + col + " -> state " + goto_st.id + ']');
                    cnt++;
                    if (cnt == 3) {
                        err.println();
                        cnt = 0;
                    }
                }
            }
            if (cnt != 0) {
                err.println();
            }
        }
        err.println("------------------------------");
    }

    public static void error(String message) {
        System.err.println("Error : " + message);
        Main.errors++;
    }

    public static void warning(String message) {
        System.err.println("Warning : " + message);
        Main.warnings++;
    }

}
