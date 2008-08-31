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

package com.sun.fortress.parser_util.precedence_opexpr;

import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.TypeOrDomain;
import com.sun.fortress.nodes.Effect;

/**
 * Class TypeInfixFrame, a component of the InfixFrame composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public abstract class TypeInfixFrame extends Object implements InfixFrame {
   private final Op _op;
   private final Effect _effect;
   private final TypeOrDomain _arg;
   private int _hashCode;
   private boolean _hasHashCode = false;

   /**
    * Constructs a TypeInfixFrame.
    * @throws java.lang.IllegalArgumentException if any parameter to the constructor is null.
    */
   public TypeInfixFrame(Op in_op, Effect in_effect, TypeOrDomain in_arg) {
      super();
      _op = in_op;
      _effect = in_effect;
      _arg = in_arg;
   }

   public Op getOp() { return _op; }
   public Effect getEffect() { return _effect; }
   public TypeOrDomain getArg() { return _arg; }

   public abstract <RetType> RetType accept(InfixFrameVisitor<RetType> visitor);
   public abstract void accept(InfixFrameVisitor_void visitor);
   public abstract void outputHelp(TabPrintWriter writer);
   protected abstract int generateHashCode();
   public final int hashCode() {
      if (! _hasHashCode) { _hashCode = generateHashCode(); _hasHashCode = true; }
      return _hashCode;
   }
}