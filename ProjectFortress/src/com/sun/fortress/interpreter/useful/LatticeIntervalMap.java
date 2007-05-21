/*
 * Created on May 14, 2007
 *
 */
package com.sun.fortress.interpreter.useful;

import java.util.Comparator;

public class LatticeIntervalMap <T, U, L extends LatticeOps<U>> extends
     LatticeIntervalMapBase<T, U, L> implements BoundingMap<T, U, L> {
    
 
    public LatticeIntervalMap(BATree2<T, U, U> table2, LatticeOps<U> lattice_operations,
            LatticeIntervalMapDual<T, U, L> supplied_dual) {
        super(table2, lattice_operations, supplied_dual);
    }

    public LatticeIntervalMap(BATree2<T, U, U> table2, LatticeOps<U> lattice_operations) {
        super(table2, lattice_operations, null);
    }

    public LatticeIntervalMap(LatticeOps<U> lattice_operations, Comparator<T> comparator) {
        super(new BATree2<T,U, U>(comparator), lattice_operations, null);
    }

    LatticeIntervalMap<T,U,L> copy() {
        return new LatticeIntervalMap<T,U,L>(table.copy(), lattice, null);
    }
   
    public LatticeIntervalMapBase<T, U, L> dual() {
        if (dualMap == null) {
            synchronized(table) {
                if (dualMap == null)
                    dualMap = new LatticeIntervalMapDual<T, U, L>(table, lattice.dual(), this);
            }
        }
        return dualMap;
    }
    
    protected U lower(BATree2Node<T,U,U> node) {
        return node.data1;
    }
    
    protected U upper(BATree2Node<T,U,U> node) {
        return node.data2;
    }
    
    protected void putPair(T k, U lower, U upper) {
        table.putPair(k, lower, upper);
    }
   
}
