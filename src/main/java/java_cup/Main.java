package java_cup;

import java_cup.core.Parser;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class serves as the main driver for the JavaCup system.
 */
public class Main {

    public static final String VERSION = "v1.0";
    public static final String TITLE = "CUP-WIT " + VERSION;

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

    private static final PrintStream err = System.err;

    public static final List<String> IMPORTS = new ArrayList<>();

    public static String packageName;
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
        Main.IMPORTS.clear();
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

    public static void main(String[] args) throws java.lang.Exception {

        clear();

        parseArgs(args);

        err.println("Parsing...");
        new Parser().parse();

        boolean written = false;
        if (Main.errors == 0) {

            err.println("Checking...");
            checkUnused();

            err.println("Building tables...");
            buildParser();

            if (Main.errors != 0) {
                optDumpTables = false;
            } else {
                err.println("Writing...");

                emitProductionData();
                emitActionData();
                emitReduceData();

                PrintWriter parserWriter = new PrintWriter(
                        new BufferedOutputStream(new FileOutputStream(new File(destDir,
                                parserClassName + ".java")), 4096));
                PrintWriter symbolWriter = new PrintWriter(
                        new BufferedOutputStream(new FileOutputStream(new File(destDir,
                                tokensClassName + ".java")), 4096));

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
        err.println();
        err.println(message);
        err.println();
        err.println(
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
        for (int i = 0; i < len; ) {
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
                System.out.println(Main.TITLE);
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

        err.println("  Computing non-terminal nullability...");
        computeNullability();

        err.println("  Computing first sets...");
        computeFirstSets();

        err.println("  Building state machine...");
        startState = LalrState.buildMachine();
        if (startState.id != 0) {
            throw new InternalException("Start state must be zero!");
        }

        err.println("  Filling in tables...");

        {
            final int rowSize = LalrState.ALL.size();
            actionTable = new Action[rowSize][];
            int size = Terminal.size();
            for (int i = 0; i < rowSize; i++) {
                actionTable[i] = new Action[size];
                Arrays.fill(actionTable[i], Action.ERROR_ACTION);
            }
        }
        {
            final int rowSize = LalrState.ALL.size();
            reduceTable = new LalrState[rowSize][];
            int size = NonTerminal.ALL.size();
            for (int i = 0; i < rowSize; i++) {
                reduceTable[i] = new LalrState[size];
            }
        }

        for (LalrState state : LalrState.all()) {
            state.buildTableEntries(actionTable, reduceTable);
        }

        err.println("  Checking productions...");
        checkReductions();

        if (conflictCount > expectConflicts) {
            error("*** More conflicts encountered than expected -- parser generation aborted");
        }
    }

    private static void checkUnused() {
        for (Terminal term : Terminal.ALL) {
            if (!term.isUsed()) {
                unusedTermCount++;
                warning("Terminal \"" + term.name + "\" was declared but never used");
            }
        }
        for (NonTerminal nt : NonTerminal.ALL) {
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
                    (((ReduceAction) act).reduceWith()).reductionUse();
                }
            }
        }
        for (Production prod : Production.ALL) {
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
            for (NonTerminal nt : NonTerminal.ALL) {
                if (!nt.nullable() && nt.looksNullable()) {
                    change = true;
                }
            }
        } while (change);

        for (Production prod : Production.ALL) {
            prod.checkNullable();
        }
    }

    /**
     * Compute first sets for each non-terminal. This assumes nullability has already computed.
     */
    private static void computeFirstSets() {
        boolean change;
        do {
            change = false;
            for (var nt : NonTerminal.ALL) {
                for (Production prod : nt.productions) {
                    var prodFirstSet = prod.checkFirstSet();
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

        for (String item : Main.IMPORTS) {
            out.println("import " + item + ";");
        }

        out.println();
        out.println("/**");
        out.println(" * ");
        out.println(" * @version " + Instant.ofEpochMilli(System.currentTimeMillis()));
        out.println(" */");
        out.println("public class " + Main.parserClassName + " extends AbstractParser {");
        out.println();

        out.println("    ");
        out.println("    @SuppressWarnings({");
        out.println("            \"unchecked\",");
        out.println("            \"DataFlowIssue\",");
        out.println("            \"java:S1479\" // too many case clauses");
        out.println("    })");
        out.println("    final Object doAction(int actionId)" + (
                Main.actionExceptionClassName != null ? (" throws " + Main.actionExceptionClassName) : "") + " {");

        out.println("        var myStack = this.tokenStack;");
        out.println();
        out.println("        return switch (actionId) {");

        Production.ALL.stream()
                .sorted(Comparator.comparing(p -> p.id))
                .collect(Collectors.groupingBy(
                        p -> p.code,
                        TreeMap::new,
                        Collectors.toList()
                ))
                .forEach((code, prods) -> {
                    var last = prods.size() - 1;
                    for (int i = 0; i < prods.size(); i++) {
                        var prod = prods.get(i);

                        out.println((i == 0
                                ? "            case "
                                : "                 ")
                                + prod.id
                                + (i == last ? " ->" : ",  ")
                                + " // " + prod);
                    }
                    emitParserActionCaseCode(out, code);
                });

        out.println("            default -> throw new RuntimeException(\"Invalid action id.\");");
        out.println("        };");
        out.println("    }");
        out.println("}");
    }

    private static void emitParserActionCaseCode(PrintWriter out, String code) {
        if (code.startsWith("yield ")) {
            out.println(code.substring("yield ".length()).trim());
            return;
        }

        out.println("            {");
        out.println(code);
        out.println("            }");
    }

    public static void emitTokens(PrintWriter out) {
        emitPackage(out);
        out.println("public interface " + Main.tokensClassName + " {");
        out.println();
        out.println("    /* terminals */");
        for (Terminal term : Terminal.ALL) {
            out.println("    int " + term.name + " = " + term.id + ";");
        }
        out.println();
        out.println("    /* non terminals */");
        for (NonTerminal nt : NonTerminal.ALL) {
            out.println("    //int " + nt.name + " = " + nt.id + ";");
        }
        out.println("}");
        out.println();
    }

    private static void emitPackage(PrintWriter out) {
        out.println();
        out.println("//----------------------------------------------------");
        out.println("// The following code was generated by " + Main.TITLE);
        out.println("//----------------------------------------------------");
        out.println();
        if (Main.packageName != null) {
            out.println("package " + Main.packageName + ";");
            out.println();
        }
    }

    private static void emitProductionData() {
        short[][] table = new short[Production.ALL.size()][2];
        for (Production prod : Production.ALL) {
            int i = prod.id;
            // [lhs symbol, rhs size]
            table[i][0] = (short) prod.lhs.sym().id;
            table[i][1] = (short) prod.rhs.length;
        }

        saveToDataFile(table, "Production");
    }

    private static void emitActionData() {

        var action_table = new short[actionTable.length][];
        var temp_table = new short[2 * actionTable[0].length];

        for (int i = 0; i < actionTable.length; i++) {
            var row_under_term = actionTable[i];
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
                        temp_table[nentries++] = (short) (((ShiftAction) act).shiftTo().id + 1);
                        break;
                    case Action.REDUCE:
                        temp_table[nentries++] = (short) j;
                        temp_table[nentries++] = (short) (-(((ReduceAction) act).reduceWith().id + 1));
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

            FileOutputStream out = new FileOutputStream(new File(Main.destResourceDir,
                    Main.parserClassName + "$" + name + ".data"));

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
        int relevancecounter = 0;
        /* get and report on iterator items that shift under our conflict symbol */
        for (LalrItem itm : state.items.values()) {

            /* only look if its not the same item and not a reduce */
            if (itm != red_itm && !itm.dotAtEnd) {
                /* is it a shift on our conflicting Terminal */
                symbol shift_sym = itm.symbolAfterDot;
                if ((shift_sym instanceof Terminal) && shift_sym.id == conflictSymbol) {
                    relevancecounter++;
                    /* yes, report on it */
                    message.append("  and     ").append(itm).append('\n');
                }
            }
        }
        message.append("  under symbol ").append(Terminal.get(conflictSymbol).name)
                .append("\n  Resolved in favor of shifting.\n");
        if (relevancecounter == 0) {
            return;
        }
        /* count the conflict */
        Main.conflictCount++;
        Main.warning(message.toString());
    }

    /**
     * Produce a warning message for one reduce/reduce conflict.
     *
     * @param state
     * @param itm1  first item in conflict.
     * @param itm2  second item in conflict.
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

        err.println("------- " + Main.TITLE + " Parser Generation Summary -------");

        err.println("  " + Main.errors + " errors and " + Main.warnings + " warnings");

        err.print("  " + Terminal.size() + " terminals, ");
        err.print(NonTerminal.ALL.size() + " non-terminals, and ");
        err.println(Production.ALL.size() + " productions declared, ");
        err.println("  producing " + LalrState.ALL.size() + " unique parse states.");

        err.println("  " + unusedTermCount + " terminals declared but not used.");
        err.println("  " + unusedNonTermCount + " non-terminals declared but not used.");
        err.println("  " + notReducedCount + " productions never reduced.");
        err.println("  " + conflictCount + " conflicts detected" + " (" + expectConflicts + " expected).");

        if (written) {
            err.println(
                    "  Code written to \"" + parserClassName + ".java\", and \"" + tokensClassName + ".java\".");
        } else {
            err.println("  No code produced.");
        }

        err.println("---------------------------------------------------- ");
    }

    public static void dumpGrammar() {
        err.println("===== Terminals =====");
        for (int i = 0; i < Terminal.size(); i++) {
            err.print("[" + i + ']' + Terminal.get(i).name + ' ');
            if ((i + 1) % 5 == 0) {
                err.println();
            }
        }
        err.println();
        err.println();

        err.println("===== Non terminals =====");
        for (int i = 0; i < NonTerminal.ALL.size(); i++) {
            err.print("[" + i + ']' + NonTerminal.ALL.get(i).name + ' ');
            if ((i + 1) % 5 == 0) {
                err.println();
            }
        }
        err.println();
        err.println();

        err.println("===== Productions =====");
        for (int i = 0; i < Production.ALL.size(); i++) {
            Production prod = Production.ALL.get(i);
            err.print("[" + i + "] " + ((prod.lhs).sym()).name + " ::= ");
            for (ProductionItem rh : prod.rhs) {
                err.print((rh.sym()).name + ' ');
            }
            err.println();
        }
        err.println();
    }

    public static void dumpMachine() {
        LalrState ordered[] = new LalrState[LalrState.ALL.size()];

        for (LalrState state : LalrState.all()) {
            ordered[(state.id)] = state;
        }

        err.println("===== Viable Prefix Recognizer =====");
        for (int i = 0; i < LalrState.ALL.size(); i++) {
            if (ordered[i] == startState) {
                err.print("START ");
            }
            err.println(ordered[i]);
            err.println("-------------------");
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
        err.println("Error : " + message);
        Main.errors++;
    }

    public static void warning(String message) {
        err.println("Warning : " + message);
        Main.warnings++;
    }

}
