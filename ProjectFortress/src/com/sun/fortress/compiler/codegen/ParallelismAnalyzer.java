/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.codegen;

import java.util.*;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.useful.Debug;


import edu.rice.cs.plt.tuple.Option;

public class ParallelismAnalyzer extends NodeAbstractVisitor<Boolean> {
    private final HashMap<ASTNode, Boolean> worthy;

    private final static Boolean f = Boolean.FALSE;
    private final static Boolean t = Boolean.TRUE;

    public Boolean worthParallelizing(ASTNode n) {
        return worthy.get(n);
    }

    public ParallelismAnalyzer() {
        worthy = new HashMap<ASTNode, Boolean>();
    }

    private void debug(ASTNode x) {
        Debug.debug(Debug.Type.CODEGEN,1, "ParallelismAnalyzer: node =" + x);
    }

    private void debug(ASTNode x, String message){
        Debug.debug(Debug.Type.CODEGEN,1, "ParallelismAnalyzer: node =" + x + "::" + message);
    }

    private void debug(String message) {
        Debug.debug(Debug.Type.CODEGEN,1, "ParallelismAnalyzer: " + "::" + message);
    }

    public Boolean forBlock(Block x) {
        debug(x,"forBlock");
        for (Expr e : x.getExprs()) {
            e.accept(this);
        }
        return f;
    }

    public Boolean forComponent(Component x) {
        debug(x, "forComponent");

        for (Decl d : x.getDecls()) {
            d.accept(this);
        }
        worthy.put(x,f);
        return f;
    }

    public Boolean forFnDecl(FnDecl x) {
        debug(x,"forFnDecl");
        boolean result = false;
        List<Param> params = x.getHeader().getParams();
        boolean paramsWorthy[] = new boolean[params.size()];
        int i = 0;

        for (Param p : params)
            paramsWorthy[i++] = p.accept(this);


        debug(x,"ZZZZZZ" + x.getBody().unwrap());
        x.getBody().unwrap().accept(this);
        return f;
    }

    // The FnRef is not parallel, but as an argument it is worth parallelizing.
    public Boolean forFnRef(FnRef x) {
        debug(x,"forFnRef");
        worthy.put(x, f);
        return t;
    }

    public Boolean forIf(If x) {
        debug(x,"forIf");
        for (IfClause clause : x.getClauses()) clause.accept(this);
        Option<Block> maybe_else = x.getElseClause();
        if (maybe_else.isSome()) {
            maybe_else.unwrap().accept(this);
        }
        worthy.put(x,f);
        return f;
    }

    public Boolean forOpExpr(OpExpr x) {
        debug(x,"forOpExpr");
        List<Expr> args = x.getArgs();
        int count = 0;

        for (Expr e : args)
            if (e.accept(this)) count++;

        if (count >= 2)
            worthy.put(x,t);
        else
            worthy.put(x, f);
        return t;
    }

    public Boolean forTraitDecl(TraitDecl x) {
        debug(x,"forTraitDecl");
        for (Decl d : x.getHeader().getDecls()) d.accept(this);
        worthy.put(x,f);
        return f;
    }


    public Boolean for_RewriteFnApp(_RewriteFnApp x) {
        debug(x,"for_RewriteFnApp");

        Expr arg = x.getArgument();

        if (arg instanceof TupleExpr) {
            TupleExpr targ = (TupleExpr) arg;
            List<Expr> exprs = targ.getExprs();
            int count = 0;

            for (Expr expr : exprs)
                if (expr.accept(this)) count++;

            if (count >= 2)
                worthy.put(x,t);
            else
                worthy.put(x,f);

        } else {
            arg.accept(this);
            worthy.put(x,f);
        }
        x.getFunction().accept(this);
        return t;
    }

    public Boolean defaultCase(Node that) {
        return f;
    }

    // Why doesn't the defaultCase handle these?


    // public Boolean forChainExpr(ChainExpr x) {debug(x,"forChainExpr"); return f;}
    // public Boolean forDecl(Decl x) {debug(x,"forDecl"); return f;}
    // public Boolean forDo(Do x) {debug(x,"forDo"); return f;}
    // public Boolean forIfClause(IfClause x) {debug(x,"forIfClause"); return f;}
    // public Boolean forImportNames(ImportNames x) {debug(x,"forImportNames"); return f;}
    // public Boolean forIntLiteralExpr(IntLiteralExpr x) {debug(x,"forIntLiteralExpr"); return f;}
    // public Boolean forObjectDecl(ObjectDecl x) {debug(x,"forObjectDecl"); return f;}
    // public Boolean forOpRef(OpRef x) {debug(x,"forOpRef"); return f;}
    // public Boolean forParam(Param x) {debug(x,"forParam"); return f;}
    // public Boolean forStringLiteralExpr(StringLiteralExpr x) {debug(x,"forStringLiteralExpr"); return f;}
    // public Boolean forSubscriptExpr(SubscriptExpr x) {debug(x,"forSubscriptExpr"); return f;}
    // public Boolean forVarRef(VarRef x) {debug(x,"forVarRef"); return f;}
    // public Boolean forVoidLiteralExpr(VoidLiteralExpr x) {debug(x,"forVoidLiteralExpr"); return f;}
    // public Boolean for_RewriteFnOverloadDecl(_RewriteFnOverloadDecl x) {debug(x,"for"); return f;}


    public void printTable() {
        Set<ASTNode> keys = worthy.keySet();
        for (ASTNode key : keys)
            debug("Parallelizable table entry " + key  + " has value " + worthy.get(key));
    }
}