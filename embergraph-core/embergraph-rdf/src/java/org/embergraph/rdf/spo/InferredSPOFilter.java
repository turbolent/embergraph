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
 * Created on Nov 5, 2007
 */

package org.embergraph.rdf.spo;

import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.relation.accesspath.IElementFilter;

/*
 * Filter matches only {@link StatementEnum#Inferred} statements.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class InferredSPOFilter<E extends ISPO> extends SPOFilter<ISPO> {

  /** */
  private static final long serialVersionUID = -7026008442779748082L;

  /** Shared instance. */
  public static final transient IElementFilter<ISPO> INSTANCE = new InferredSPOFilter<>();

  /** De-serialization ctor. */
  private InferredSPOFilter() {

    super();
  }

  public boolean isValid(Object o) {

    if (!canAccept(o)) {

      return true;
    }

    return accept((ISPO) o);
  }

  private boolean accept(final ISPO o) {

    final ISPO spo = o;

    return spo.getStatementType() == StatementEnum.Inferred;
  }

  /** Imposes the canonicalizing mapping during object de-serialization. */
  private Object readResolve() {

    return INSTANCE;
  }

  private void writeObject(java.io.ObjectOutputStream out) {

    // NOP - stateless.

  }

  private void readObject(java.io.ObjectInputStream in) {

    // NOP - stateless.

  }
}
