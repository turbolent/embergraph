/*

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*/
package org.embergraph.rdf.internal.constraints;

import java.util.Map;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;

/** Always evaluates to false. */
public class FalseBOp extends XSDBooleanIVValueExpression {

  /** */
  private static final long serialVersionUID = 1531344906063447800L;

  public static final FalseBOp INSTANCE = new FalseBOp();

  private FalseBOp() {

    this(BOp.NOARGS, BOp.NOANNS);
  }

  /** Required shallow copy constructor. */
  public FalseBOp(final BOp[] args, final Map<String, Object> anns) {

    super(args, anns);
  }

  /** Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}. */
  public FalseBOp(final FalseBOp op) {
    super(op);
  }

  public boolean accept(final IBindingSet bs) {

    return false;
  }
}
