/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Box;
import edu.rice.cs.plt.lambda.SimpleBox;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportFrom;
import com.sun.fortress.nodes.AliasedDottedId;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes_util.NodeUtil;

import com.sun.fortress.useful.NI;

/**
 * Methods to parse a collection of files to a collection of ASTs. Automatically locates
 * and parses any additional API definitions that are needed.
 */
public class Parser {
  
  public static class Result extends StaticPhaseResult {
    private final Iterable<CompilationUnit> _asts;
    
    public Result() { _asts = IterUtil.empty(); }
    
    public Result(CompilationUnit ast) { _asts = IterUtil.singleton(ast); }
    
    public Result(Iterable<? extends StaticError> errors) {
      super(errors);
      _asts = IterUtil.empty();
    }
    
    public Result(Result r1, Result r2) {
      super(r1, r2);
      _asts = IterUtil.compose(r1._asts, r2._asts);
    }
  
    public Iterable<CompilationUnit> asts() { return _asts; }
  }
  
  /** Parse the given files and any additional files that are expected to contain referenced APIs. */
  public static Result parse(Iterable<? extends File> files, final GlobalEnvironment env) {
    final Box<Result> result = new SimpleBox<Result>(new Result()); // box allows mutation
    
    Set<File> fileSet = new HashSet<File>();
    for (File f : files) { fileSet.add(canonicalRepresentation(f)); }
    
    Lambda<File, Set<File>> parseAndGetDepends = new Lambda<File, Set<File>>() {
      public Set<File> value(File f) {
        Result r = parse(f);
        result.set(new Result(result.value(), r));
        if (r.isSuccessful()) {
          Set<File> newFiles = new HashSet<File>();
          for (CompilationUnit cu : r.asts()) { newFiles.addAll(extractNewDependencies(cu, env)); }
          return newFiles;
        }
        else { return Collections.emptySet(); }
      }
    };
    CollectUtil.graphClosure(fileSet, parseAndGetDepends); // parses all dependency files
    return result.value();
  }
  
  /** Parses a single file. */
  private static Result parse(File f) {
    return NI.nyi();
  }
  
  /** Get all files potentially containing APIs imported by cu that aren't currently in fortress. */
  private static Set<File> extractNewDependencies(CompilationUnit cu, GlobalEnvironment env) {
    Set<String> importedApiNames = new LinkedHashSet<String>();
    for (Import i : cu.getImports()) {
      if (i instanceof ImportApi) {
        for (AliasedDottedId id : ((ImportApi) i).getApis()) {
          importedApiNames.add(NodeUtil.getName(id.getDottedId()));
        }
      }
      else { // i instanceof ImportFrom
        importedApiNames.add(NodeUtil.getName(((ImportFrom) i).getSource()));
      }
    }
    
    Set<File> result = new HashSet<File>();
    for (String s : importedApiNames) {
      if (!env.definesApi(s)) {
        File f = canonicalRepresentation(fileForApiName(s));
        if (IOUtil.attemptExists(f)) { result.add(f); }
      }
    }
    return result;
  }
  
  /** Get the filename in which the given API should be defined. */
  private static File fileForApiName(String apiName) {
    return new File(apiName + ".fsi");
  }
  
  /** Convert to a filename that is canonical for each (logical) file, preventing reparsing the same file. */
  private static File canonicalRepresentation(File f) {
    // treat the same absolute path as the same file; different absolute path but
    // the same *canonical* path (a symlink, for example) is treated as two different files;
    // if absolute file can't be determined, assume they are distinct
    return IOUtil.canonicalCase(IOUtil.attemptAbsoluteFile(f));
  }
  
}
