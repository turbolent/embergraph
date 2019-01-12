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
 * Created on Sep 28, 2010
 */

package org.embergraph.rdf.lexicon;

import java.util.Map;
import org.embergraph.bop.BOp;
import org.embergraph.bop.ap.filter.BOpResolver;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphValue;

/*
 * Cache the {@link EmbergraphValue} on the {@link IV} (create a cross linkage). This is useful for
 * lexicon joins and SPARQL operators that need to use materialized RDF values.
 */
public class CacheValueFilter extends BOpResolver {

  /** */
  private static final long serialVersionUID = -7267351719878117114L;

  /** A default instance. */
  public static CacheValueFilter newInstance() {
    return new CacheValueFilter(BOp.NOARGS, BOp.NOANNS);
  }

  /** @param op */
  public CacheValueFilter(CacheValueFilter op) {
    super(op);
  }

  /*
   * @param args
   * @param annotations
   */
  public CacheValueFilter(BOp[] args, Map<String, Object> annotations) {
    super(args, annotations);
  }

  /** Cache the EmbergraphValue on its IV (cross-link). */
  @Override
  protected Object resolve(final Object obj) {

    final EmbergraphValue val = (EmbergraphValue) obj;

    // the link from EmbergraphValue to IV is pre-existing (set by the
    // materialization of the index tuple)
    final IV iv = val.getIV();

    // cache the value on the IV
    iv.setValue(val);

    return obj;
  }
}
