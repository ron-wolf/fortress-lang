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

package com.sun.fortress.interpreter.evaluator.values;

import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.scopes.Scope;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.ModifierOverride;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.Applicable;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

/**
 * A Closure value is a function, plus some environment information.
 */
public class Closure extends NonPrimitive implements Scope {

    protected FType returnType;
    protected List<FType> instArgs;
    protected Applicable def;

    public Closure(Environment e, Applicable fndef) {
        super(e); // TODO verify that this is the proper environment
        def = NativeApp.checkAndLoadNative(fndef);
    }

    public Closure(Environment e, Applicable fndef, boolean isFunctionalMethod) {
        super(e); // TODO verify that this is the proper environment
        def = NativeApp.checkAndLoadNative(fndef,isFunctionalMethod);
    }

    protected Closure(Environment e, Applicable fndef, List<FType> args) {
        super(e);
        def = NativeApp.checkAndLoadNative(fndef);
        instArgs = args;
    }

    /*
     * Just like the PartiallyDefinedMethod, but used a specific environemnt
     */
    public Closure(TraitMethod method, Environment environment) {
        super(environment);
        def = NativeApp.checkAndLoadNative(method.def);
        instArgs = method.instArgs;
        setParamsAndReturnType(method.getParameters(), method.returnType);
    }

//    public Closure(BetterEnv e, FnExpr x, Option<Type> return_type,
//            List<Param> params) {
//        super(e);
//        def = NativeApp.checkAndLoadNative(x);
//        EvalType et = new EvalType(e);
//        setParamsAndReturnType(
//                et.paramsToParameters(e, params),
//                return_type.isPresent() ? et.evalType(return_type.getVal()) : BottomType.ONLY
//                );
//    }

    @Override
    public boolean isOverride() {
        if (def instanceof FnAbsDeclOrDecl) {
            List<Modifier> mods = ((FnAbsDeclOrDecl) def).getMods();
            for (Modifier mod : mods)
                if (mod instanceof ModifierOverride)
                    return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.Fcn#getFnName()
     */
    @Override
    public IdOrOpOrAnonymousName getFnName() {
        return def.getName();
    }

    protected HasAt getAt() {
        return def;
    }

    public String stringName() {
        return def.stringName();
    }

    public boolean hasSelfDotMethodInvocation() {
        return false;
    }

    public String selfName() {
        return NI.na();
    }

    public String toString() {
        return ((instArgs == null ? s(def) :
            (s(def) + Useful.listInOxfords(instArgs))) + " " +
            (type() != null ? type() : "NULL")) + " " + def.at();
    }

    public boolean seqv(FValue v) {
        if (!(v instanceof Closure)) return false;
        Closure c = (Closure) v;
        if (getDef() != c.getDef()) return false;
        if (type()   != c.type()) return false;
        if (getEnv() == c.getEnv()) return true;
        // TODO: environment walking and matching.  Worth it??
        // We'd need to compute FV(body).
        return false;
    }

    public int hashCode() {
        return def.hashCode() +
        System.identityHashCode(getEnv()) +
        (instArgs == null ? 0 : instArgs.hashCode());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o.getClass().equals(this.getClass())) {
            Closure oc = (Closure) o;
            return def == oc.def &&
            getEnv() == oc.getEnv() &&
            (instArgs == null ? (oc.instArgs == null) :
                oc.instArgs == null ? false : instArgs.equals(oc.instArgs));
        }
        return false;
    }

    private void setReturnType(FType rt) {
        // TODO need to get this test right
        if (this.returnType != null && !this.returnType.equals(rt)) {
            throw new IllegalStateException(
                    "Attempted second set of closure return type");
        }
        returnType = rt;
    }


    public Applicable getDef() {
        return def;
    }

    /**
     * @return Returns the closure_body.
     */
    public Expr getBody() {
        Option<Expr> optBody = NodeUtil.getBody(def);
        assert(optBody.isSome());
        return optBody.unwrap();
    }

    public Expr getBodyNull() {
        Option<Expr> optBody = NodeUtil.getBody(def);
        return optBody.unwrap(null);
    }


    public FValue applyInner(List<FValue> args, HasAt loc,
                             Environment envForInference) {
        if (def instanceof NativeApp) {
            args = typecheckParams(args,loc);
            try {
                return ((NativeApp)def).applyToArgs(args);
            } catch (RuntimeException ex) {
                return error(loc, errorMsg("Wrapped exception ", ex.toString()), ex);
            } catch (Error ex) {
                return error(loc, errorMsg("Wrapped error ", ex.toString()), ex);
            }
        } else {
            Evaluator eval = new Evaluator(buildEnvFromParams(args, loc));
            return eval.eval(getBody());
        }
    }

    /**
     * The environment, sort of, in which the closure's name is bound.
     */
    public Environment getEnv() {
        return getWithin();
    }

    /**
     * The environment used to evaluate the closure.
     */
    public Environment getEvalEnv() {
        return getWithin();
    }

    /**
     * Call this for Closures, not setParams.
     * @param fparams
     * @param ft
     */
    public void setParamsAndReturnType(List<Parameter> fparams, FType ft) {
        setReturnType(ft);
        setParams(fparams);
    }

    protected void setValueType() {
        setFtype(FTypeArrow.make(getDomain(), returnType));
    }

    public void finishInitializing() {
        // This needs to be done right with a generic.
        Applicable x = getDef();
        List<Param> params = x.getParams();
        Option<Type> rt = x.getReturnType();
        Environment env = getEvalEnv(); // should need this for types,
                                    // below.
        FType ft = EvalType.getFTypeFromOption(rt, env, BottomType.ONLY);
        List<Parameter> fparams = EvalType.paramsToParameters(env, params);

        setParamsAndReturnType(fparams, ft);

        return; //  this;
    }

    @Override
    boolean getFinished() {
       return returnType != null;
    }


}
