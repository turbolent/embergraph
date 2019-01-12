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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IValueExpression;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;

/*
 * A constraint that a value expression which may only take on the bindings enumerated by some set
 * of constants.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: INHashMap.java 4357 2011-03-31 17:11:26Z thompsonbry $
 */
public class InHashBOp extends InBOp {

  /** */
  private static final long serialVersionUID = 8032412126003678642L;

  /*
   * The value expression to be computed for each solution (cached).
   *
   * <p>Note: This cache is not serialized and is compiled on demand when the operator is used.
   */
  private transient volatile IValueExpression<IV> valueExpr;

  /*
   * The set of constants to be tested (cached).
   *
   * <p>Note: This cache is not serialized and is compiled on demand when the operator is used.
   */
  private transient volatile LinkedHashSet<IV> set;

  /*
   * <code>true</code> iff this is NOT IN (cached).
   *
   * <p>Note: This cache is not serialized and is compiled on demand when the operator is used.
   */
  private transient boolean not;

  /** Deep copy constructor. */
  public InHashBOp(final InHashBOp op) {
    super(op);
  }

  /** Shallow copy constructor. */
  public InHashBOp(final BOp[] args, final Map<String, Object> annotations) {

    super(args, annotations);
  }

  /*
   * @param x Some variable.
   * @param set A set of legal term identifiers providing a constraint on the allowable values for
   *     that variable.
   */
  @SuppressWarnings("rawtypes")
  public InHashBOp(
      final boolean not,
      final IValueExpression<? extends IV> var,
      final IConstant<? extends IV>... set) {

    super(not, var, set);
  }

  /*
   * Extends {@link org.embergraph.bop.CoreBaseBOp#mutation() CoreBaseBOp.mutation} method to
   * reflect args changes in cached IVs set.
   */
  @Override
  public void mutation() {
    super.mutation();
    if (set != null) {
      synchronized (this) {
        if (set != null) {
          // clearing cached set is guarded by double-checked locking pattern.
          set = null;
        }
      }
    }
  }

  private void init() {

    valueExpr = getValueExpression();

    // populate the cache.
    final IConstant<IV>[] a = getSet();

    set = new LinkedHashSet<IV>(a.length);

    for (IConstant<IV> IV : a) {

      final IV val = IV.get();

      set.add(val);
    }

    not = ((Boolean) getProperty(Annotations.NOT)).booleanValue();
  }

  public boolean accept(final IBindingSet bindingSet) {

    final Set<IV> _set;
    if (valueExpr == null || set == null) {
      synchronized (this) {
        if (valueExpr == null || set == null) {
          // init() is guarded by double-checked locking pattern.
          init();
        }
        // "set" is pulled into a local variable in this double-checked locking pattern
        // to avoid the possibility that set is resolved to non-null
        // and then cleared to null before it is used in accept()
        _set = set;
      }
    } else {
      _set = set;
    }

    // get as-bound value of the value expression.
    final IV v = valueExpr.get(bindingSet);

    if (v == null) throw new SparqlTypeErrorException.UnboundVarException();

    final boolean found = _set.contains(v);

    return not != found;
  }
}
