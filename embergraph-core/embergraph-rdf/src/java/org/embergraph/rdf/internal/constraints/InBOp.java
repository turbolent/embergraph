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
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.NV;
import org.embergraph.rdf.internal.IV;

/**
 * Abstract base class for "IN" {@link IConstraint} implementations.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: INConstraint.java 4286 2011-03-09 17:36:10Z mrpersonick $
 */
public abstract class InBOp extends XSDBooleanIVValueExpression {

  private static final long serialVersionUID = -774833617971700165L;

  public interface Annotations extends XSDBooleanIVValueExpression.Annotations {

    /** <code>true</code> iff this is "NOT IN" rather than "IN". */
    String NOT = InBOp.class.getName() + ".not";
  }

  @SuppressWarnings("rawtypes")
  private static BOp[] mergeArguments(
      final IValueExpression<? extends IV> var, final IConstant<? extends IV>... set) {

    final BOp[] args = new BOp[1 + (set != null ? set.length : 0)];

    args[0] = var;

    for (int i = 0; i < set.length; i++) {

      args[i + 1] = set[i];
    }

    return args;
  }

  @SuppressWarnings("rawtypes")
  public InBOp(
      final boolean not,
      final IValueExpression<? extends IV> var,
      final IConstant<? extends IV>... set) {

    this(mergeArguments(var, set), NV.asMap(Annotations.NOT, Boolean.valueOf(not)));
  }

  /** @param op */
  public InBOp(final InBOp op) {

    super(op);
  }

  /**
   * @param args
   * @param annotations
   */
  public InBOp(final BOp[] args, final Map<String, Object> annotations) {

    super(args, annotations);

    if (getProperty(Annotations.NOT) == null) throw new IllegalArgumentException();

    @SuppressWarnings("rawtypes")
    final IValueExpression<? extends IV> var = get(0);

    if (var == null) throw new IllegalArgumentException();

    if (arity() < 2) {
      throw new IllegalArgumentException();
    }
  }

  /** The value expression to be tested. */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public IValueExpression<IV> getValueExpression() {

    return (IValueExpression<IV>) get(0);
  }

  /** The remaining arguments to the IN/NOT IN function, which must be a set of constants. */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public IConstant<IV>[] getSet() {

    final IConstant<IV>[] set = new IConstant[arity() - 1];

    for (int i = 1; i < arity(); i++) {

      set[i - 1] = (IConstant<IV>) get(i);
    }

    return set;
  }
}
