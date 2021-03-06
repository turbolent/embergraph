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
package org.embergraph.bop.constraint;

import java.util.Map;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpBase;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;

/** Imposes the constraint <code>x == y</code>. */
public class EQ extends BOpBase implements BooleanValueExpression {

  /** */
  private static final long serialVersionUID = 9207324734456820516L;

  public EQ(final IVariable<?> x, final IVariable<?> y) {

    this(new BOp[] {x, y}, null /* annotations */);
  }

  /** Required shallow copy constructor. */
  public EQ(final BOp[] args, final Map<String, Object> annotations) {
    super(args, annotations);

    if (args.length != 2 || args[0] == null || args[1] == null)
      throw new IllegalArgumentException();

    if (args[0] == args[1]) throw new IllegalArgumentException();
  }

  /** Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}. */
  public EQ(final EQ op) {
    super(op);
  }

  public Boolean get(final IBindingSet s) {

    // get binding for "x".
    final IConstant<?> x = s.get((IVariable<?>) get(0) /* x */);

    if (x == null) return true; // not yet bound.

    // get binding for "y".
    final IConstant<?> y = s.get((IVariable<?>) get(1) /* y */);

    if (y == null) return true; // not yet bound.

    return x.equals(y);
  }
}
