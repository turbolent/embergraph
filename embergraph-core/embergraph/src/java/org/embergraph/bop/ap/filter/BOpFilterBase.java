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

package org.embergraph.bop.ap.filter;

import cutthecrap.utils.striterators.IFilter;
import java.util.Iterator;
import java.util.Map;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpBase;

/*
 * Base class for operators which apply striterator patterns for access paths.
 *
 * <p>The striterator pattern is enacted slightly differently here. The filter chain is formed by
 * stacking {@link BOpFilterBase}s as child operands. Each operand specified to a {@link
 * BOpFilterBase} must be a {@link BOpFilterBase} and layers on another filter, so you can stack
 * filters as nested operands or as a sequence of operands.
 *
 * <p>
 *
 * @todo state as newState() and/or as annotation?
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class BOpFilterBase extends BOpBase implements IFilter {

  /** */
  private static final long serialVersionUID = 1L;

  public interface Annotations extends BOp.Annotations {}

  /*
   * Deep copy.
   *
   * @param op
   */
  public BOpFilterBase(BOpFilterBase op) {
    super(op);
  }

  /*
   * Shallow copy.
   *
   * @param args
   * @param annotations
   */
  public BOpFilterBase(BOp[] args, Map<String, Object> annotations) {
    super(args, annotations);
  }

  public final Iterator filter(Iterator src, final Object context) {

    // wrap source with each additional filter from the filter chain.
    final Iterator<BOp> itr = argIterator();

    while (itr.hasNext()) {

      final BOp arg = itr.next();

      src = ((BOpFilterBase) arg).filter(src, context);
    }

    // wrap src with _this_ filter.
    src = filterOnce(src, context);

    return src;
  }

  /*
   * Wrap the source iterator with <i>this</i> filter.
   *
   * @param src The source iterator.
   * @param context The iterator evaluation context.
   * @return The wrapped iterator.
   */
  protected abstract Iterator filterOnce(Iterator src, final Object context);
}
