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

package com.sun.fortress.nodes_util;

import java.io.IOException;
import java.util.*;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;

public class ErrorMsgMaker extends NodeAbstractVisitor<String> {
    public static final ErrorMsgMaker ONLY = new ErrorMsgMaker();

    public static String makeErrorMsg(AbstractNode node) {
        return node.accept(ErrorMsgMaker.ONLY);
    }

    private ErrorMsgMaker() {}

    private final List<String> mapSelf(List<? extends AbstractNode> that) {
        LinkedList<String> result = new LinkedList<String>();
        for (AbstractNode elt : that) {
            result.add(elt.accept(this));
        }
        return result;
    }

    private final String acceptIfPresent(Option<? extends Node> possibleNode) {
        return possibleNode.apply(new OptionVisitor<Node, String>() {
            public String forSome(Node n) { return n.accept(ErrorMsgMaker.this); }
            public String forNone() { return ""; }
        });
    }

    public String forAbstractNode(AbstractNode node) {
        return node.getClass().getSimpleName() + "@" + node.getSpan().begin.at();
    }

    public String forAbsVarDecl(AbsVarDecl node) {
        return "abs " + Useful.listInParens(mapSelf(node.getLhs())) + node.getSpan();
    }

    public String forArrowType(ArrowType node) {
        return
            node.getDomain().accept(this)
            + "->"
            + node.getRange().accept(this)
            + (node.getThrowsClause().isSome() ?
                   (" throws " + Useful.listInCurlies(mapSelf(Option.unwrap(node.getThrowsClause())))) :
                   "");
    }

    public String forNumberConstraint(NumberConstraint node) {
        return "" + node.getVal().toString();
    }

    public String forBoolParam(BoolParam node) {
        return "int " + NodeUtil.nameString(node.getName());
    }

    public String forFnAbsDeclOrDecl(FnAbsDeclOrDecl node) {
        return NodeUtil.nameString(node.getName())
                + Useful.listInOxfords(mapSelf(node.getStaticParams()))
                + Useful.listInParens(mapSelf(node.getParams()))
                + (node.getReturnType().isSome() ? (":" + Option.unwrap(node.getReturnType()).accept(this)) : "")
                ;//+ "@" + NodeUtil.getAt(node.getFnName());
    }


    public String forId(Id node) {
        return node.getText();
    }

    public String forOp(Op node) {
        return node.getText();
    }

    public String forIdType(IdType node) {
        return node.getName().accept(this);
    }

    public String forIntLiteral(IntLiteral node) {
        return node.getVal().toString();
    }

    public String forIntParam(IntParam node) {
        return "int " + NodeUtil.nameString(node.getName());
    }

    public String forKeywordType(KeywordType node) {
        return "" + NodeUtil.nameString(node.getName()) + ":" + node.getType().accept(this);
    }

    public String forLValueBind(LValueBind node) {
        String r = "";
        if (node.getType().isSome()) {
            r = ":" + Option.unwrap(node.getType()).accept(this);
        }
        return NodeUtil.nameString(node.getName()) + r;
    }

    public String forNatParam(NatParam node) {
        return "nat " + NodeUtil.nameString(node.getName());
    }

    public String forName(Name n) {
        return NodeUtil.nameString(n);
    }

    public String forOperatorParam(OperatorParam node) {
        return "opr " + NodeUtil.nameString(node.getName());
    }

    public String forNormalParam(NormalParam node) {
        StringBuffer sb = new StringBuffer();
        sb.append(NodeUtil.nameString(node.getName()));
        if (node.getType().isSome()) {
            sb.append(":");
            sb.append(Option.unwrap(node.getType()).accept(this));
        }
        if (node.getDefaultExpr().isSome()) {
            sb.append("=");
            sb.append(Option.unwrap(node.getDefaultExpr()).accept(this));
        }
        return sb.toString();
    }

    public String forVarargsParam(VarargsParam node) {
        StringBuffer sb = new StringBuffer();
        sb.append(NodeUtil.nameString(node.getName()));
        sb.append(":");
        sb.append(node.getVarargsType().accept(this));

        return sb.toString();
    }

    public String forInstantiatedType(InstantiatedType node) {
        return NodeUtil.nameString(node.getName()) +
            Useful.listInOxfords(mapSelf(node.getArgs()));
    }

    public String forVarargsType(VarargsType node) {
        return node.getType().accept(this) + "...";
    }

    public String forSimpleTypeParam(SimpleTypeParam node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forTupleType(TupleType node) {
        return
            "(" +
            Useful.listInDelimiters("", mapSelf(node.getElements()), "") +
            acceptIfPresent(node.getVarargs()) +
            Useful.listInDelimiters("", mapSelf(node.getKeywords()), "") +
            ")";
    }

    public String forTypeArg(TypeArg node) {
        return String.valueOf(node.getType().accept(this));
    }

    public String forVarDecl(VarDecl node) {
        return Useful.listInParens(mapSelf(node.getLhs())) + "=" + node.getInit().accept(this) + node.getSpan();
    }
}
