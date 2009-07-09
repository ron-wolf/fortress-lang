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

package com.sun.fortress.compiler.index;

import java.util.Collections;
import java.util.List;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Binding;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.tuple.Option;

public class FieldGetterMethod extends Method {

    private final Binding _ast;
    private final Id _declaringTrait;

    public FieldGetterMethod(Binding ast, Id declaringTrait) {
        _ast = ast;
        _declaringTrait = declaringTrait;
        if (_ast.getIdType().isSome())
            putThunk(SimpleBox.make(_ast.getIdType()));
    }

    public Binding ast() { return _ast; }

    @Override
    public Span getSpan() { return NodeUtil.getSpan(_ast); }

	@Override
	public Option<Expr> body() {
		return Option.none();
	}

	@Override
	public List<Param> parameters() {
		return Collections.emptyList();
	}

	@Override
	public List<StaticParam> staticParameters() {
		return Collections.emptyList();
	}

	@Override
	public List<BaseType> thrownTypes() {
		return Collections.emptyList();
	}

	@Override
	public Functional instantiate(List<StaticParam> params, List<StaticArg> args) {
		return this;
	}

	@Override
	public Id getDeclaringTrait() {
		return this._declaringTrait;
	}

	@Override
	public Functional acceptNodeUpdateVisitor(NodeUpdateVisitor visitor) {
		return new FieldGetterMethod((Binding)this._ast.accept(visitor), this._declaringTrait);
	}


}
