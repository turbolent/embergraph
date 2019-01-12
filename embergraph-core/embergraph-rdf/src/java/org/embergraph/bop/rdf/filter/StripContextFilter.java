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

package org.embergraph.bop.rdf.filter;

import java.util.Map;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.ap.filter.BOpResolver;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPO;

/*
* Strips the context information from an {@link SPO}. This is used in default graph access paths.
 * It operators on {@link ISPO}s so it must be applied using {@link
 * IPredicate.Annotations#ACCESS_PATH_FILTER}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class StripContextFilter extends BOpResolver {

  /** */
  private static final long serialVersionUID = 1L;

  /** A default instance. */
  public static StripContextFilter newInstance() {
    return new StripContextFilter(BOp.NOARGS, BOp.NOANNS);
  }

  /** @param op */
  public StripContextFilter(StripContextFilter op) {
    super(op);
  }

  /*
   * @param args
   * @param annotations
   */
  public StripContextFilter(BOp[] args, Map<String, Object> annotations) {
    super(args, annotations);
  }

  /** Strips the context position off of a visited {@link ISPO}. */
  @Override
  protected Object resolve(final Object obj) {

    final ISPO tmp = (ISPO) obj;

    return new SPO(tmp.s(), tmp.p(), tmp.o());
  }
}
