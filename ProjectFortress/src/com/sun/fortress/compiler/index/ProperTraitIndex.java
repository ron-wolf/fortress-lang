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

package com.sun.fortress.compiler.index;

import java.util.Map;
import java.util.Set;
import edu.rice.cs.plt.collect.Relation;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TypeRef;

import com.sun.fortress.useful.NI;

/** Wraps a (non-object) trait declaration. */
public class ProperTraitIndex extends TraitIndex {

  public ProperTraitIndex(TraitAbsDeclOrDecl ast,
                          Map<String, Method> getters,
                          Map<String, Method> setters,
                          Set<Function> coercions,
                          Relation<String, Method> dottedMethods,
                          Relation<String, FunctionalMethod> functionalMethods) {
    super(ast, getters, setters, coercions, dottedMethods, functionalMethods);
  }
  
  
  public Set<TypeRef> excludesTypes() {
    return NI.nyi();
  }
  
  public Set<TypeRef> comprisesTypes() {
    return NI.nyi();
  }
  
}
