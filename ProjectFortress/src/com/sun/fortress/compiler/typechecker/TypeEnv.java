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

package com.sun.fortress.compiler.typechecker;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.compiler.index.ParamVariable;
import com.sun.fortress.compiler.index.SingletonVariable;
import com.sun.fortress.compiler.index.DeclaredVariable;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

import static com.sun.fortress.nodes_util.NodeFactory.*;

/** 
 * This class is used by the type checker to represent static type environments,
 * mapping bound variables to their types. 
 */
public abstract class TypeEnv {
    public static TypeEnv make(LValueBind... entries) {
        return EmptyTypeEnv.ONLY.extend(entries);
    }
    
    public abstract Option<LValueBind> binding(IdName var);
    public abstract Option<Type> type(IdName var);
    public abstract Option<List<Modifier>> mods(IdName var);
    public abstract Option<Boolean> mutable(IdName var);
    
    public Option<Type> type(String var) { return type(makeIdName(var)); }
    public Option<List<Modifier>> mods(String var) { return mods(makeIdName(var)); }
    public Option<Boolean> mutable(String var) { return mutable(makeIdName(var)); }
        
    /**
     * Produce a new type environment extending this with the given variable bindings.
     */
    public TypeEnv extend(LValueBind... entries) {
        if (entries.length == 0) { return EmptyTypeEnv.ONLY; }
        else { return new NonEmptyTypeEnv(entries, this); }
    }
    
    public TypeEnv extend(Map<IdName, Variable> vars) {
        ArrayList<LValueBind> lvals = new ArrayList<LValueBind>();
        
        for (Variable var: vars.values()) {
            if (var instanceof ParamVariable) {
                Param ast = ((ParamVariable)var).ast();
                if (ast instanceof NormalParam) {
                    lvals.add(NodeFactory.makeLValue((NormalParam)ast));
                } else { // ast instanceof VarargsParam
                    lvals.add(NodeFactory.makeLValue(ast.getName(),
                        NodeFactory.makeInstantiatedType(ast.getSpan(), false, 
                                                         makeQualifiedIdName(Arrays.asList(makeId("FortressBuiltin")), makeId("ImmutableHeapSequence")
                                                         ),
                                                         new TypeArg(((VarargsParam)ast).getVarargsType().getType()))));
                }
            } else if (var instanceof SingletonVariable) {
                // Singleton objects declare both a value and a type with the same name.
                IdName nameAndType = ((SingletonVariable)var).declaringTrait();
                lvals.add(NodeFactory.makeLValue(nameAndType, nameAndType));
            } else { // entry instanceof DeclaredVariable
                lvals.add(((DeclaredVariable)var).ast());
            }
        }
        LValueBind[] result = new LValueBind[lvals.size()];
        return this.extend(lvals.toArray(result));
    }
}
