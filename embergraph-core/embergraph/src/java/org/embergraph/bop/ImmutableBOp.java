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
 * Created on Oct 5, 2010
 */

package org.embergraph.bop;

import java.util.Map;

/**
 * Base class for immutable operators such as {@link Var} and {@link Constant}. These operators do
 * not deep copy their data and do not permit decoration with annotations.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class ImmutableBOp extends BOpBase {

  /** */
  private static final long serialVersionUID = 1L;

  /** @param op */
  public ImmutableBOp(ImmutableBOp op) {
    super(op);
  }

  /**
   * @param args
   * @param annotations
   */
  public ImmutableBOp(BOp[] args, Map<String, Object> annotations) {
    super(args, annotations);
  }

  /*
   * Overrides for the copy-on-write mutation API.
   */

  @Override
  protected final Object _setProperty(String name, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected final void _clearProperty(String name) {
    throw new UnsupportedOperationException();
  }
}
