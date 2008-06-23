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

package com.sun.fortress.interpreter.unit_tests;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.useful.TestCaseWrapper;
import com.sun.fortress.compiler.Parser;

import static com.sun.fortress.exceptions.ProgramError.error;

public class ParserJUTest extends TestCaseWrapper {

    private final static String PARSER_FAIL_TESTS_DIR =
        ProjectProperties.BASEDIR + "parser_tests/";
    private final static String PARSER_NYI_TESTS_DIR =
        ProjectProperties.BASEDIR + "not_passing_yet/";
    private static boolean isNYI(String parent) {
        int size = parent.length();
        return (size > 3 && parent.substring(size-3,size).equals("yet"));
    }

    public static TestSuite suite() {
        return new ParserTestSuite("ParserJUTest",
                                   PARSER_FAIL_TESTS_DIR,
                                   PARSER_NYI_TESTS_DIR);
    }

    private final static class ParserTestSuite extends TestSuite {
        private final static boolean VERBOSE = false;

        // relative to the top ProjectFortress directory
        private final String failTestDir;
        private final String nyiTestDir;

        public ParserTestSuite(String _name, String _failTestDir,
                               String _nyiTestDir) {
            super(_name);
            failTestDir = _failTestDir;
            nyiTestDir = _nyiTestDir;
            addParserTests();
        }

        private void addParserTests() {
            FilenameFilter fssFilter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return (name.endsWith("fss") || name.endsWith("fsi"));
                    }
                };

            for (String filename : new File(failTestDir).list(fssFilter)) {
                File f = new File(failTestDir + filename);
                addTest(new ParserTestCase(f));
            }
            for (String filename : new File(nyiTestDir).list(fssFilter)) {
                File f = new File(nyiTestDir + filename);
                addTest(new ParserTestCase(f));
            }
        }

        private class ParserTestCase extends TestCase {

            private final File file;

            /**
             * Construct a parser test case for the given file.
             */
            public ParserTestCase(File _file) {
                super(_file.getName());
                file = _file;
            }

            @Override
            protected void runTest() throws Throwable {
                String name = file.getName();
                String parent = file.getParent();
                if (!isNYI(parent)) {
                    assertParserFails(file);
                } else if (isNYI(parent)) {
                    assertParserSucceeds(file);
                } else {
                    error("Unexpected file in the parser_test directory: " +
                          name);
                }
            }

            private void assertParserFails(File f) throws IOException {
                try {
                    Parser.Result result = Parser.parse(f);
                    assertFalse("Source " + f + " was compiled without parser errors",
                                result.isSuccessful());
                } catch (Throwable e) {
                }
            }

            private void assertParserSucceeds(File f) throws IOException {
                Parser.Result result = Parser.parse(f);
                assertFalse("Source " + f + " was compiled with parser errors",
                            !result.isSuccessful());
            }
        }
    }
}