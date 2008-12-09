/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.nodes_util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.Lhs;
import com.sun.fortress.nodes.Level;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.ModifierAbstract;
import com.sun.fortress.nodes.ModifierAtomic;
import com.sun.fortress.nodes.ModifierGetter;
import com.sun.fortress.nodes.ModifierHidden;
import com.sun.fortress.nodes.ModifierIO;
import com.sun.fortress.nodes.ModifierOverride;
import com.sun.fortress.nodes.ModifierPrivate;
import com.sun.fortress.nodes.ModifierSettable;
import com.sun.fortress.nodes.ModifierSetter;
import com.sun.fortress.nodes.ModifierTest;
import com.sun.fortress.nodes.ModifierValue;
import com.sun.fortress.nodes.ModifierVar;
import com.sun.fortress.nodes.ModifierWidens;
import com.sun.fortress.nodes.ModifierWrapped;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.InFixity;
import com.sun.fortress.nodes.PreFixity;
import com.sun.fortress.nodes.PostFixity;
import com.sun.fortress.nodes.NoFixity;
import com.sun.fortress.nodes.MultiFixity;
import com.sun.fortress.nodes.EnclosingFixity;
import com.sun.fortress.nodes.BigFixity;
import com.sun.fortress.nodes.UnknownFixity;
import com.sun.fortress.nodes.StaticParamKind;
import com.sun.fortress.nodes.KindType;
import com.sun.fortress.nodes.KindNat;
import com.sun.fortress.nodes.KindInt;
import com.sun.fortress.nodes.KindBool;
import com.sun.fortress.nodes.KindDim;
import com.sun.fortress.nodes.KindUnit;
import com.sun.fortress.nodes.KindOp;
import com.sun.fortress.interpreter.reader.Lex;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

/**
 * A collection of methods for converting from a readable AST to an AST data
 * structure. It assumes input generated by the companion class Printer.
 *
 * The Unprinter uses reflection to allocate com.sun.fortress.nodes and fill in fields. The first
 * element X in an S-sexpression is assumed to be a class name, which is assumed
 * to denote the class com.sun.fortress.nodes.X.
 *
 * Fields are specified by name, and data types are indicated either by
 * reflected field information, or by syntactic queues in the input (double
 * quotes, square brackets, at signs, and so on). For many types, there is a
 * canonical value for missing information, zero, false, empty string, empty
 * list, absent, and so on, so that the input can be less verbose. In
 * particular, a missing location is supposed to be the same as the previous
 * location.
 *
 * Sample input follows. The first line indicates the beginning of a component,
 * and further (after the "@") supplies location information. The fields of a
 * component include name (a APIName) and defs (a List).
 *
 * <pre>
 * (Component
 *
 * &#064;"../samples/let_fn.fss":7:3 name=(APIName
 * &#064;1:24 names=["samples" "let_fn"]) defs=[ (VarDecl
 * &#064;6:11 init=(Block exprs=[ (LetFn
 * &#064;4:21 body=[ (TightJuxt
 * &#064;5:10~12 exprs=[ (VarRef
 * &#064;5:10 var=(Id name="f")) (IntLiteralExpr
 * &#064;5:12 text="7" val=7 props=["parenthesized"])])] fns=[ (FnDecl
 * &#064;4:21 body=(OpExpr
 * &#064;4:17~21 op=(Opr
 * &#064;4:19 op=(Op name="+")) args=[ (VarRef
 * &#064;4:17 var=(Id name="y")) (IntLiteralExpr
 * &#064;4:21 text="1" val=1)]) contract=(Contract
 * &#064;4:13) name=(Fun name_=(Id
 * &#064;4:10 name="f")) params=[ (Param
 * &#064;4:12 name=(Id
 * &#064;4:11 name="y"))])])]) name=(Id
 * &#064;3:1 name="x") type=(Some val=(VarType
 * &#064;3:5 name=(APIName names=["int"]))))])
 * </pre>
 */
// In the above example, "&#064;" = "@".  A line starting with "@" has special meaning to javadoc.
public class Unprinter extends NodeReflection {

    public Span lastSpan = new Span(); // Default value is all empty and zero.

    Lex l;

    /**
     * An unprinter wraps a primitive Lexer.
     */
    public Unprinter(Lex l) {
        this.l = l;
    }

    /**
     * Reads the location-describing information that follows an at-sign (@)
     * following the class name in an S-expression. The most general location is
     * <br>
     * "startfile",startline:startcolumn~"endfile",endline:endcolumn <br>
     * The full list of accepted location forms appears below. Missing
     * information is inferred; missing file is copied from the previous
     * location's ending file, missing endpoint is copied from the start point,
     * and missing line is copied from the same line.
     * <p>
     * "f1",1:2~"f2",3:4<br>
     * "f3",5:6~7:8<br>
     * "f4",9:10~11 (range of columns)<br>
     * "f5",12:13<br>
     * 14:15~16:17<br>
     * 18:19~20 (range of columns)<br>
     * 21:22<br>
     *
     * @throws IOException
     */
    public String readSpan() throws IOException {
        // Begin by copying the old source span,
        // and then overwrite as information appears.
        SourceLoc b = new SourceLocRats(lastSpan.begin);
        SourceLoc e = new SourceLocRats(lastSpan.end);
        lastSpan = new Span(b, e);

        SourceLoc sloc = lastSpan.begin;

        String fname = sloc.getFileName();
        int line = sloc.getLine();
        int column = sloc.column();

        String next = l.name(false);
        if (next.startsWith("\"") && next.endsWith("\"")) {
            fname = deQuote(next);
            String colon = l.name(false);
            if (!":".equals(colon)) {
                bug("Expected colon, got " + colon);
            }
            next = l.name(false);
        }
        String l1 = next;
        String colon = l.name(false);
        String c1 = l.name(false);
        line = Integer.parseInt(l1, 10);
        column = Integer.parseInt(c1, 10);

        fname = fname.intern();
        sloc.setFileName(fname);
        sloc.setLine(line);
        sloc.setColumn(column);

        sloc = lastSpan.end;

        String sep = l.name(false);
        if (Printer.tilde.equals(sep)) {
            next = l.name(false);
            boolean sawFile = false;
            if (next.startsWith("\"") && next.endsWith("\"")) {
                fname = deQuote(next);
                colon = l.name(false);
                if (!":".equals(colon)) {
                    bug("Expected colon, got " + colon);
                }
                next = l.name(false);
                sawFile = true;
            }
            String l_or_c = next; // or perhaps column
            colon = l.name(false);
            if (":".equals(colon)) {
                l1 = l_or_c;
                c1 = l.name(false);
                next = l.name();
            } else if (sawFile) {
                next = bug("Saw f:l:c~f:l with no following colon");
            } else {
                c1 = l_or_c;
                if (")".equals(colon)) {
                    next = colon;
                } else if (colon.length() == 0) {
                    next = l.name();
                } else {
                    next = bug("Did we expect this?");
                }

            }

        } else if (sep.length() == 0) {
            next = l.name();
        } else {
            next = sep;
        }
        line = Integer.parseInt(l1, 10);
        column = Integer.parseInt(c1, 10);

        fname = fname.intern();
        sloc.setFileName(fname);
        sloc.setLine(line);
        sloc.setColumn(column);

        return next;
    }

    @SuppressWarnings("unchecked")
    static Class[] oneSpanArg = { Span.class };

    @Override
    protected Constructor defaultConstructorFor(Class cl) {
        try {
            return cl.getDeclaredConstructor(oneSpanArg);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {

        }
        return null;
    }

    /**
     * Expects a String representation of an AST, beginning with left
     * parentheses and name of the AST node being read.
     *
     * @return The AST whose represenation appears on the input, including all
     *         of its substructure.
     * @throws IOException
     */
    public Node read() throws IOException {
        l.lp();
        return readNode(l.name());
    }

    /**
     * Reads the remainder of the S expression for the class whose name is
     * passed in as a parameter. Expect that leading "(" and class name have
     * both already been read from the stream.
     *
     * @return The AST whose represenation appears on the input, including all
     *         of its substructure.
     * @throws IOException
     */
    public Node readNode(String class_name) throws IOException {
        classFor(class_name); // Loads other tables as a side-effect

        Node node = null;
        String next = l.name();

        // Next token is either a field, or a span.
        if ("@".equals(next)) {
            next = readSpan();
        }

        // Begins by constructing an empty node.
        // To be readable, a node class must supply a constructor for
        // a single Span argument.
        try {
            node = makeNodeFromSpan(class_name, null, lastSpan);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            bug("Error reading node type " + class_name);
        } catch (InstantiationException e) {
            e.printStackTrace();
            bug("Error reading node type " + class_name);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            bug("Error reading node type " + class_name);
        }
        // Iteratively read field names and values, and assign them into
        // the (increasingly less-) empty node.
        try {
            while (!")".equals(next)) {
                Field f = fieldFor(class_name, next);
                expectPrefix("=");
                // Try to figure out, based on reflected type, what we are
                // reading.
                // Fail if the syntax doesn't match.

                // There is some, not too much, consistency checking
                // between Field type and input syntax.
                if (f.getType() == List.class) {
                    expectPrefix("[");
                    // This is an actual hole. Might want to add a
                    // structure-verification
                    // frob to any methods containing List or Pair.
                    f.set(node, readList());
                } else if (f.getType() == Map.class ){
                    expectPrefix("(Map");
                    f.set(node, readMap());
                } else if (f.getType() == Level.class ){
                    expectPrefix("(Level");
                    f.set(node, readLevel());
                } else if (f.getType() == Pair.class) {
                    expectPrefix("(Pair");
                    // This is an actual hole. Might want to add a
                    // structure-verification
                    // frob to any methods containing List or Pair.
                    f.set(node, readPair());
                } else if (f.getType() == String.class) {
                    f.set(node, deQuote(l.name()).intern()); // Lexer returns an
                                                     // escape-containing
                                                     // string, deQuote converts to Unicode.
                } else if (f.getType() == Integer.TYPE) {
                    f.setInt(node, readInt(l.name()));
                } else if (f.getType() == Boolean.TYPE) {
                    f.setBoolean(node, readBoolean(l.name()));
                } else if (f.getType() == Double.TYPE) {
                    f.setDouble(node, readDouble(l.string()));
                } else if (f.getType() == BigInteger.class) {
                    f.set(node, readBigInteger(l.name()));
                } else if (Option.class.isAssignableFrom(f.getType())) {
                    f.set(node, readOption());
                } else if (Fixity.class.isAssignableFrom(f.getType())) {
                    f.set(node, readFixity());
                } else if (Modifier.class.isAssignableFrom(f.getType())) {
                    f.set(node, readModifier());
                } else if (StaticParamKind.class.isAssignableFrom(f.getType())) {
                    f.set(node, readStaticParamKind());
                } else if (AbstractNode.class.isAssignableFrom(f.getType())
                        || Lhs.class.isAssignableFrom(f.getType())) {
                    expectPrefix("(");
                    f.set(node, readNode(l.name()));
                }
                next = l.name();
            }

            // Check for unassigned fields, pick sensible defaults.
            for (Field f : fieldArrayFor(class_name)) {
                Class<? extends Object> fcl = f.getType();
                // Happily, primitives all wake up with good default values.
                if (!fcl.isPrimitive() && f.get(node) == null) {
                    if (fcl == List.class) {
                        // empty list
                        f.set(node, Collections.EMPTY_LIST);
                    } else if (fcl == String.class) {
                        // empty string
                        f.set(node, "");
                    } else if (fcl == Option.class) {
                        // missing option
                        f.set(node, Option.none());
                    } else {
                        bug("Unexpected missing data, field "
                                + f.getName() + " of class " + class_name);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            bug("Error reading node type " + class_name);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            bug("Error reading node type " + class_name);
        } catch (IOException e) {
            e.printStackTrace();
            bug("Error reading node type " + class_name);
        }
        return node;
    }

    public void expectPrefix(String string) throws IOException {
        l.expectPrefix(string);
    }

    private final static int NORMAL = 0;

    private final static int SAW_BACKSLASH = 1;

    private final static int SAW_BACKSLASH_TICK = 2;

    /**
     * Given a quoted string, return the Unicode string encoded within. The
     * input should be quoted, but the quotes do not appear in the resulting
     * Unicode.
     */
    public static String deQuote(CharSequence s) {
        int l = s.length();
        if (s.charAt(0) != '\"') {
            bug("Malformed input, missing initial \"");
        }
        if (s.charAt(l - 1) != '\"') {
            bug("Malformed input, missing final \"");
        }
        StringBuffer sb = new StringBuffer(l - 2);
        StringBuffer escaped = null;
        int state = NORMAL;
        for (int i = 1; i < l - 1; i++) {
            char c = s.charAt(i);
            if (state == NORMAL) {
                if (c == '\"') {
                    bug(
                            "Malformed input, unescaped \" seen at position "
                                    + i);
                } else if (c == '\\') {
                    state = SAW_BACKSLASH;
                } else {
                    sb.append(c);
                }
            } else if (state == SAW_BACKSLASH) {
                if (c == 'b') {
                    sb.append('\b');
                    state = NORMAL;
                } else if (c == 't') {
                    sb.append('\t');
                    state = NORMAL;
                } else if (c == 'n') {
                    sb.append('\n');
                    state = NORMAL;
                } else if (c == 'f') {
                    sb.append('\f');
                    state = NORMAL;
                } else if (c == 'r') {
                    sb.append('\r');
                    state = NORMAL;
                } else if (c == '\"') {
                    sb.append('\"');
                    state = NORMAL;
                } else if (c == '\\') {
                    sb.append('\\');
                    state = NORMAL;
                } else if (c == '\'') {
                    state = SAW_BACKSLASH_TICK;
                    escaped = new StringBuffer();
                } else if (File.separator.equals("\\")) {
                    sb.append('\\');
                    state = NORMAL;
                } else {
                    bug(
                            "Malformed input, unexpected backslash escape " + c
                                    + "(hex " + Integer.toHexString(c)
                                    + ") at index " + i);
                }
            } else if (state == SAW_BACKSLASH_TICK) {
                if (c == '\'') {
                    // Decipher string accumulated in escaped.
                    state = NORMAL;
                    if (escaped.length() == 0) {
                        sb.append('\'');
                    } else if (Character.isDigit(escaped.charAt(0))) {
                        int fromHex;
                        try {
                            fromHex = Integer.parseInt(escaped.toString(), 16);
                            if (fromHex < 0 || fromHex > 0xFFFF) {
                                bug("Unicode " + escaped
                                        + " too large for Java-hosted tool");
                            }
                            sb.append((char) fromHex);
                        } catch (NumberFormatException ex) {
                            bug("Malformed hex encoding " + escaped);
                        }
                    } else {
                        translateUnicode(escaped.toString(), sb);

                    }
                } else {
                    escaped.append(c);
                }
            }

        }
        return sb.toString();
    }

    /**
     * Appends to sb the Unicode characters specified by name in escaped.
     * Incomplete implementation of the Fortress Unicode escaping spec.
     *
     * @param escaped
     * @param sb
     */
    public static void translateUnicode(String escaped, StringBuffer sb) {
        // TODO Need to implement full generality of Unicode name encoding.
        StringTokenizer st = new StringTokenizer(escaped, "&", false);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            String mapped = Unicode.byNameLC(tok);
            if (Character.isUpperCase(tok.charAt(0))) {
                mapped = mapped.toUpperCase();
            }
            sb.append(mapped);
        }
    }

    public static String enQuote(CharSequence s) {
        StringBuffer sb = new StringBuffer(s.length() + 2);
        int l = s.length();
        for (int i = 0; i < l; i++) {
            char c = s.charAt(i);
            if (needsBackslash(c)) {
                sb.append('\\');
                sb.append(afterBackslash(c).charValue());
            } else if (needsUnicoding(c)) {
                // At least two characters following \',
                // and also beginning with 0-9.
                sb.append('\\');
                sb.append('\'');
                String hex = Integer.toHexString(c);
                if (hex.length() < 2 || hex.charAt(0) > '9') {
                    sb.append('0');
                }
                sb.append(hex);
                sb.append('\'');
            } else {
                sb.append(c);
            }

        }
        return sb.toString();
    }

    private static boolean needsUnicoding(char c) {
        return c < ' ' || c > '~';
    }

    private static boolean needsBackslash(char c) {
        return c == '\b' || c == '\t' || c == '\n' || c == '\f' || c == '\r'
                || c == '\"' || c == '\\';
    }

    private static Character afterBackslash(char c) {
        switch (c) {
        case '\b':
            return new Character('b');
        case '\t':
            return new Character('t');
        case '\n':
            return new Character('n');
        case '\f':
            return new Character('f');
        case '\r':
            return new Character('r');
        case '\"':
            return new Character('\"');
        case '\\':
            return new Character('\\');
        default:
            return bug("Invalid input, character value 0x" + Integer.toHexString(c));
        }
    }

    private Pair<Object, Object> readPair() throws IOException {
        Object x, y;
//        if ("(".equals(a)) {
//            x = readThing();
//            expectPrefix("(");
//            y = readThing();
//        } else if ("[".equals(a)) {
//            x = readList();
//            expectPrefix("[");
//            y = readList();
//        } else if (a.startsWith("\"")) {
//            x = deQuote(a).intern(); // Internal form is quoted
//            y = deQuote(l.name()).intern(); // Internal form is quoted
//        } else {
//            return bug("Pair of unknown stuff beginning " + a);
//        }
        x = readElement();
        y = readElement();
        expectPrefix(")");
        return new Pair<Object, Object>(x, y);
    }

    public Level readLevel() throws IOException {
        int level = 0;
        Option<Object> obj = Option.none();

        expectPrefix("_level=");
        String i = l.name();
        // level = readInt(l.name());
        level = readInt(i);
        expectPrefix("_object=");
        obj = Option.wrap(readElement());

        expectPrefix(")");

            // TODO Why don't levels write their spans out?
        return new Level(level, obj.unwrap() );
    }

    public Fixity readFixity() throws IOException {
        expectPrefix("(");
        String s = l.name();
        Fixity fixity;
        if ( "InFixity".equals(s) )
            fixity = new InFixity();
        else if ( "PreFixity".equals(s) )
            fixity = new PreFixity();
        else if ( "PostFixity".equals(s) )
            fixity = new PostFixity();
        else if ( "NoFixity".equals(s) )
            fixity = new NoFixity();
        else if ( "MultiFixity".equals(s) )
            fixity = new MultiFixity();
        else if ( "EnclosingFixity".equals(s) )
            fixity = new EnclosingFixity();
        else if ( "BigFixity".equals(s) )
            fixity = new BigFixity();
        else
            fixity = new UnknownFixity();
        expectPrefix(")");
        return fixity;
    }

    public Modifier readModifier() throws IOException {
        expectPrefix("(");
        String s = l.name();
        Modifier modifier;
        if ( "ModifierAbstract".equals(s) )
            modifier = new ModifierAbstract();
        else if ( "ModifierAtomic".equals(s) )
            modifier = new ModifierAtomic();
        else if ( "ModifierGetter".equals(s) )
            modifier = new ModifierGetter();
        else if ( "ModifierHidden".equals(s) )
            modifier = new ModifierHidden();
        else if ( "ModifierIO".equals(s) )
            modifier = new ModifierIO();
        else if ( "ModifierOverride".equals(s) )
            modifier = new ModifierOverride();
        else if ( "ModifierPrivate".equals(s) )
            modifier = new ModifierPrivate();
        else if ( "ModifierSettable".equals(s) )
            modifier = new ModifierSettable();
        else if ( "ModifierSetter".equals(s) )
            modifier = new ModifierSetter();
        else if ( "ModifierTest".equals(s) )
            modifier = new ModifierTest();
        else if ( "ModifierValue".equals(s) )
            modifier = new ModifierValue();
        else if ( "ModifierVar".equals(s) )
            modifier = new ModifierVar();
        else if ( "ModifierWidens".equals(s) )
            modifier = new ModifierWidens();
        else
            modifier = new ModifierWrapped();
        expectPrefix(")");
        return modifier;
    }

    public StaticParamKind readStaticParamKind() throws IOException {
        expectPrefix("(");
        String s = l.name();
        StaticParamKind kind;
        if ( "KindType".equals(s) )
            kind = new KindType();
        else if ( "KindNat".equals(s) )
            kind = new KindNat();
        else if ( "KindInt".equals(s) )
            kind = new KindInt();
        else if ( "KindBool".equals(s) )
            kind = new KindBool();
        else if ( "KindDim".equals(s) )
            kind = new KindDim();
        else if ( "KindUnit".equals(s) )
            kind = new KindUnit();
        else
            kind = new KindOp();
        expectPrefix(")");
        return kind;
    }

    public Map<String,Object> readMap() throws IOException {
        Map<String,Object> map = new HashMap<String,Object>();
        String s = l.name();
        while (true) {
            if ("!".equals(s)) {
                String name = readIdentifier();
                expectPrefix("=");
                Object obj = readElement();
                map.put( name, obj );
            } else if ( ")".equals(s) ){
                return map;
            }
            s = l.name();
        }
    }

    private Object readElement() throws IOException {
        String a = l.name();
         if ("(".equals(a)) {
            return readThing();
        } else if ("[".equals(a)) {
            return readList();
        } else if (a.startsWith("\"")) {
            return deQuote(a).intern(); // Internal form is quoted
        } else {
            return bug("Pair of unknown stuff beginning " + a);
        }
    }

    int readInt(String s) throws IOException {
        return Integer.parseInt(s, 10);

    }

    private String readIdentifier() throws IOException {
        return l.name();
    }

    double readDouble(String s) throws IOException {
        return Double.parseDouble(s);

    }

    boolean readBoolean(String s) throws IOException {
        return Boolean.parseBoolean(s);
    }

    BigInteger readBigInteger(String s) throws IOException {
        return new BigInteger(s);
    }

    public List<Object> readList() throws IOException {
        String s = l.name();
        ArrayList<Object> a = new ArrayList<Object>();
        Object x;
        while (true) {
            if ("(".equals(s)) {
                x = readThing();
            } else if ("[".equals(s)) {
                x = readList();
            } else if (s.startsWith("\"")) {
                x = deQuote(s).intern(); // Intermediate form is quoted.
            } else if (s.startsWith("]")) {
                return Useful.immutableTrimmedList(a);
            } else {
                return bug("List of unknown element beginning " + s);
            }
            a.add(x);
            s = l.name();
        }
    }

    private Object readThing() throws IOException {
        Object x;
        String s2 = l.name();
        if ("Pair".equals(s2)) {
            x = readPair();
        } else if ("Some".equals(s2)) {
            x = readOptionTail();
        } else if ("Map".equals(s2)) {
            x = readMap();
        } else if ( "Level".equals(s2) ){
            x = readLevel();
        } else {
            x = readNode(s2);
        }
        return x;
    }

    public Option<Object> readOption() throws IOException {
        // Reading options is slightly different because
        // there is no detailed reflection information for
        // generics in Java. This code simply chooses to
        // believes the types in the input.
        expectPrefix("(Some");
        return readOptionTail();
    }

    private Option<Object> readOptionTail() throws IOException {
        String s = l.name();
        if (")".equals(s)) { return Option.none(); }
        else if (!"_value".equals(s)) {
            return bug("Expected '_value' saw '" + s + "'");
        }
        expectPrefix("=");
        Object x;
        s = l.name();
        if ("(".equals(s)) {
            x = readThing();
        } else if ("[".equals(s)) {
            x = readList();
        } else if (s.startsWith("\"")) {
            x = deQuote(s).intern(); // Internal form is quoted. deQuoteQuoted(s);
        } else {
            x = null;
        }
        expectPrefix(")");
        return Option.some(x);
    }

}
