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
/*
 * Created on Sep 22, 2011
 */

package org.embergraph.relation.accesspath;

import java.io.Serializable;
import org.embergraph.bop.ap.filter.SameVariableConstraint;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.filter.TupleFilter;

/*
* {@link TupleFilter} class wrapping the {@link SameVariableConstraint}.
 *
 * <p>Note: This filter can execute local to the index shard in scale-out. Therefore it MUST NOT
 * have a reference to the {@link AccessPath} in order to be {@link Serializable}. This used to be
 * an "inline" class in {@link AccessPath}. It was promoted to a top-level class for this reason.
 */
public class SameVariableConstraintTupleFilter<E> extends TupleFilter<E> {

  private static final long serialVersionUID = 1L;

  private final SameVariableConstraint<E> sameVariableConstraint;

  SameVariableConstraintTupleFilter(final SameVariableConstraint<E> sameVariableConstraint) {

    this.sameVariableConstraint = sameVariableConstraint;
  }

  @Override
  protected boolean isValid(final ITuple tuple) {

    return sameVariableConstraint.isValid(tuple.getObject());
  }
}
