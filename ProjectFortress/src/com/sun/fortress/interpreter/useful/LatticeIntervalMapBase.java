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
/*
 * Created on May 9, 2007
 *
 */
package com.sun.fortress.interpreter.useful;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap;
import java.util.Comparator;

public abstract class LatticeIntervalMapBase<T, U, L extends LatticeOps<U>> extends
        AbstractMap<T, U> implements BoundingMap<T, U, L> {

    BATree2<T, U, U> table;
    LatticeOps<U> lattice;
    protected volatile LatticeIntervalMapBase<T, U, L> dualMap;
    // Must be volatile due to lazy initialization / double-checked locking.
    
    public LatticeIntervalMapBase(BATree2<T, U, U> table2, LatticeOps<U> lattice_operations,
            LatticeIntervalMapBase<T, U, L> supplied_dual) {
        table = table2;
        lattice = lattice_operations;
        dualMap = supplied_dual;
    }
    
    public boolean leq(U lower, U upper) {
        U lmu = lattice.meet(lower, upper);
        return lmu.equals(lower);
    }
    
    abstract protected U lower(BATree2Node<T,U,U> node);
    
    abstract protected U upper(BATree2Node<T,U,U> node);
    
    abstract protected void putPair(T k, U lower, U upper);
    
    /** puts min/intersection of v and old */
    public U meetPut(T k, U v) {
        BATree2Node<T,U,U> old = table.getNode(k);
        if (old != null) {
            U lower = lower(old);
            U upper = lattice.meet(upper(old), v);
            checkOrdered(lower, upper);
            putPair(k, lower, upper);
            return upper;
        } else {
            putPair(k, lattice.zero(), v);
            return v;
        }
    }

    /**
     * @param lower
     * @param upper
     * @throws Error
     */
    private void checkOrdered(U lower, U upper) throws Error {
        if (! leq(lower, upper))
            throw new Error("Empty lattice interval");
    }
    /** puts max/union of v and old */
    public U joinPut(T k, U v) {
        BATree2Node<T,U,U> old = table.getNode(k);
        if (old != null) {
            U lower = lattice.join(lower(old), v);
            U upper = upper(old);
            checkOrdered(lower, upper);
            putPair(k, lower, upper);
            return lower;
        } else {
            putPair(k, v, lattice.one());
            return v;
        }
    }

    /**
     * Puts the lower (bottom) end of an interval.
     * Differs from joinPut only in what it returns.
     */
    public U put(T k, U v) {
        BATree2Node<T,U,U> old = table.getNode(k);
        if (old != null) {
            U lower = lattice.join(lower(old), v);
            U upper = upper(old);
            checkOrdered(lower, upper);
            putPair(k, lower, upper);
            return lower(old);
        } else {
            putPair(k, v, lattice.one());
            return null;
        }
   }
    
    /**
     * Returns the lower (bottom) end of an interval.
     */
     public U get(Object k) {
        BATree2Node<T,U,U> old = table.getNode((T)k);
        return lower(old);
    }

     /**
      * Returns the lower (bottom) end of an interval.
      */
      public U getLower(Object k) {
         BATree2Node<T,U,U> old = table.getNode((T)k);
         return lower(old);
     }

      /**
       * Returns the lower (bottom) end of an interval.
       */
       public U getUpper(Object k) {
          BATree2Node<T,U,U> old = table.getNode((T)k);
          return upper(old);
      }

      @Override
    public Set<java.util.Map.Entry<T, U>> entrySet() {
        throw new Error("unimplemented");
    }

}
