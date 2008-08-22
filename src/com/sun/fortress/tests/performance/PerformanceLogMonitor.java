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

package com.sun.fortress.tests.performance;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The purpose of this program is to collect performance time information
 * from the results of cruise control, append this information into a
 * persistent data structure, and then optionally output a set of charts
 * from this persistent data structure.
 * 
 * PerformanceLogMonitor accepts two arguments and an optional third argument.
 * 
 * The first argument is required.  It is the path to an xml log file that
 * is generated by Cruise Control. The program will parse this file, and append
 * the timing data to the data file that is written to disk (see second
 * argument). If the xml log file has already been processed, then any old
 * information stored in the data file will be overwritten by the new version
 * of the log file.
 * 
 * The second argument is required. It is the path to a gzipped data file
 * that is generated by PerformanceLogMonitor. The contents of this data
 * file are read at the start of the program.  At the end of the program
 * this data file is written back to disk, including any new timing
 * information.  This data file consists of a serialized version of a
 * {@link TestSuiteData} instance. 
 * 
 * The third argument is optional.  It is the path to a directory where 
 * png images and html files will be written, reflecting the data stored
 * in the {@link TestSuiteData} instance.  The set of test cases/suites
 * to be written to a graphs are stored in {@link TestSuiteData.SPECIAL_TESTCASES}.
 * 
 * HINT: If you want to produce a set of graphs but you do not want to 
 * process a new xml log file, then you must create an empty file on the
 * filesystem and use this empty file as the second argument to the program.
 * You will get the error message: "unexpected end of file" but the graphs 
 * will be created correctly.
 *
 */
public class PerformanceLogMonitor {

    public static TestSuiteData testingData;
    
    private static final DateFormat _format1 = new SimpleDateFormat("m 'min' s 'sec'");
    private static final DateFormat _format2 = new SimpleDateFormat("s 'sec'");    
    private static final GregorianCalendar _calendar = new GregorianCalendar();    

    /** 
     * Read from the contents of the file path 
     * specified by the second argument to {@link #main(String[])}. 
     * And save the contents in {@link #testingData}. 
     */
    private static void readDataFile(File dataFile) {
        FileInputStream inputStream = null;
        GZIPInputStream gzipStream = null;
        ObjectInputStream objectStream = null;        
        try {
            inputStream = new FileInputStream(dataFile);
            gzipStream = new GZIPInputStream(inputStream);
            objectStream = new ObjectInputStream(gzipStream);
            testingData = (TestSuiteData) objectStream.readObject();
        } catch (FileNotFoundException e) {
            System.err.println("Could not find file: " + dataFile.getPath());
            testingData = new TestSuiteData();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());                
        } catch (ClassCastException e) {
            System.err.println(e.getMessage());
        } finally {
            if (testingData == null) {
                testingData = new TestSuiteData();
            }
            closeStream(objectStream);
            closeStream(gzipStream);
            closeStream(inputStream);
        }
    }    
    
    /** 
     * Read from the contents of the Cruise Control log file 
     * specified by the first argument to {@link #main(String[])}. 
     * And save the contents in {@link #testingData}. 
     */  
    private static Integer readLogFile(File xmlLogFile) {
        FileInputStream logInputStream = null;        
        try {
            DOMParser parser = new DOMParser();
            logInputStream = new FileInputStream(xmlLogFile);
            InputSource inputSource = new InputSource(logInputStream);
            parser.parse(inputSource);
            Document doc = parser.getDocument();
            Integer revision = getRevisionNumber(doc);
            addCurrentRevision(doc, revision);
            return revision;
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (SAXException e) {
            System.err.println(e.getMessage());
        } finally {
            closeStream(logInputStream);
        }        
        return 0;        
    }

    
    /** 
     * Write the contents of {@link #testingData} out to 
     * the path specified by the second argument to 
     * {@link #main(String[])}. 
     */
    private static void writeDataFile(File dataFile) {
        FileOutputStream dataStream = null;
        GZIPOutputStream gzipStream = null;
        ObjectOutputStream objectStream = null;
        try {
            dataStream = new FileOutputStream(dataFile);
            gzipStream = new GZIPOutputStream(dataStream);
            objectStream = new ObjectOutputStream(gzipStream);        
            objectStream.writeObject(testingData);                    
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            closeStream(objectStream);
            closeStream(gzipStream);
            closeStream(dataStream);
        }
    }

    /**
     * Parse all test cases for this revision number.  Calls 
     * parseTestcase(Node testcase, Integer revision) on each test case
     * across all test suites.
     */
    private static void addCurrentRevision(Document doc, Integer revision) {
        NodeList cases = doc.getElementsByTagName("testcase");
        for(int i = 0; i < cases.getLength(); i++) {
                Node testcase = cases.item(i);
                parseTestcase(testcase, revision);
        }
        NodeList targets = doc.getElementsByTagName("target");
        for(int i = 0; i < targets.getLength(); i++) {
            Node target = targets.item(i);
            parseTarget(target, revision);
        }        
    }

    /**
     * Parse each target, and add the data to the appropriate data structures
     * in the TestSuiteData object.  Before the data can be added, first check
     * that this target did not throw an error.
     */    
    private static void parseTarget(Node target, Integer revision) {
        boolean targetPassed = true;
        NamedNodeMap attributes = target.getAttributes();        
        String targetName = attributes.getNamedItem("name").getNodeValue();
        NodeList children = target.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("error")) {
                targetPassed = false;
            }
        }
        if (targetPassed) {
                String timeString = attributes.getNamedItem("time").getNodeValue();
                timeString = timeString.replaceAll("min\\p{Alpha}*", "min");
                timeString = timeString.replaceAll("sec\\p{Alpha}*", "sec");                
                Date date = null;
                try {                            
                    date = _format1.parse(timeString);
                } catch (ParseException e) {
                    try {
                        date = _format2.parse(timeString);
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                    }
                }
                if (date != null) {
                    _calendar.setTime(date);
                    long seconds = (_calendar.get(Calendar.DAY_OF_MONTH) - 1) * 24 * 3600 + 
                        _calendar.get(Calendar.HOUR_OF_DAY) * 3600 +
                        _calendar.get(Calendar.MINUTE) * 60 + 
                        _calendar.get(Calendar.SECOND);
                    Double targetTime = Double.valueOf(seconds);
                    testingData.addTimingInformation(targetName, revision, targetTime);
                }
        }        
    }

    /**
     * Parse each test case, and add the data to the appropriate data structures
     * in the TestSuiteData object.  Before the data can be added, first check
     * that this test case did not throw an error.
     */
    private static void parseTestcase(Node testcase, Integer revision) {
        boolean testPassed = true;
        NamedNodeMap attributes = testcase.getAttributes();        
        String testName = attributes.getNamedItem("name").getNodeValue();
        testName = testName.substring(testName.lastIndexOf(File.separatorChar) + 1);
        NodeList children = testcase.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("error")) {
                testPassed = false;
            }
        }
        if (testPassed) {
            Double testTime = Double.parseDouble(attributes.getNamedItem("time").getNodeValue());
            testingData.addTimingInformation(testName, revision, testTime);
        }
    }

    /**
     * Look up the revision number in this log file.
     */
    private static Integer getRevisionNumber(Document doc) {
        NodeList revisions = doc.getElementsByTagName("revision");
        Integer revisionInt = 0;
        if (revisions.getLength() > 0) {
            Node revision = revisions.item(revisions.getLength() - 1);
            String revisionString = revision.getFirstChild().getNodeValue();
            revisionInt = Integer.parseInt(revisionString);
            Node parent = revision.getParentNode();
            Node child = parent.getFirstChild();
            while (child != null) {
                if (child.getNodeName().equals("date")) {
                    String dateString = child.getFirstChild().getNodeValue();
                    testingData.putRevisionDate(revisionInt, dateString);
                    child = null;
                } else {
                    child = child.getNextSibling();
                }
            }
        }
        return revisionInt;
    }
    
    /**
     * Close an input or output stream.
     */
    public static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();                
            }                
        }
    }

    private static void checkInputArguments(File xmlLogFile, File dataFile,
            File chartDirectory) {
        boolean ok = true;
        if (!xmlLogFile.isFile()) {
            System.err.println("First argument is not a valid file name: " +
                    xmlLogFile.getPath());
            ok = false;
        }
        if (dataFile.isDirectory()) {
            System.err.println("Second argument is not a valid file name: " + 
                    dataFile.getPath());
            ok = false;
        }
        if (chartDirectory != null) {
            if (!chartDirectory.isDirectory()) {
                System.err.println("Final argument is not a valid directory: " +
                        chartDirectory.getPath());
                ok = false;
            }
        }
        if (!ok) System.exit(-1);
    }
    
    
    public static void main(String[] args) {
        System.setProperty("java.awt.headless","true"); 
        if (!((args.length == 3) || (args.length == 2))) {
            System.err.println("First argument is the xml log file.");
            System.err.println("Second argument is the data file to read and write.");
            System.err.println("(optional) Third argument is the directory to put all the charts.");
            System.exit(-1);
        }
        File xmlLogFile = new File(args[0]);
        File dataFile = new File(args[1]);        
        File chartDirectory = null;
        if (args.length == 3) {
            chartDirectory = new File(args[2]);
        }
        checkInputArguments(xmlLogFile, dataFile, chartDirectory);
        readDataFile(dataFile);
        readLogFile(xmlLogFile);
        if (chartDirectory != null) {
            testingData.writeCharts(chartDirectory);
            testingData.writeHtml(chartDirectory);
        }
        writeDataFile(dataFile);
    }


}