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

package com.sun.fortress.exceptions.transactions;

/**
 * Thrown by the BaseContentionManager when the benchmark time has elapsed.
 **/
public class GracefulException extends java.lang.RuntimeException {
  static final long serialVersionUID = 6572490566353395650L;

  /**
   * Creates a new <code>GracefulException</code> instance with no detail message.
   */
  public GracefulException() {
    super();
  }

}