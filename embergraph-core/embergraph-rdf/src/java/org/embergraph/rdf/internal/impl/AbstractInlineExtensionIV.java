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
 * Created on Mar 13, 2012
 */

package org.embergraph.rdf.internal.impl;

import org.embergraph.rdf.internal.DTE;
import org.embergraph.rdf.internal.IExtensionIV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.model.EmbergraphValue;

/*
 * Abstract base class for fully inline {@link IExtensionIV}s.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractInlineExtensionIV<V extends EmbergraphValue, T>
    extends AbstractInlineIV<V, T> implements IExtensionIV {

  /** */
  private static final long serialVersionUID = 1L;

  /*
   * @param vte
   * @param dte
   */
  public AbstractInlineExtensionIV(VTE vte, DTE dte) {
    super(vte, dte);
  }

  /*
   * @param vte
   * @param extension
   * @param dte
   */
  public AbstractInlineExtensionIV(VTE vte, boolean extension, DTE dte) {
    super(vte, extension, dte);
  }
}
