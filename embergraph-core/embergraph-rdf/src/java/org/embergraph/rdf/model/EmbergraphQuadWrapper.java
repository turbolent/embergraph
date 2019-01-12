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
 * Created onJan 24, 2014
 */

package org.embergraph.rdf.model;

import java.util.Objects;

/*
* This class wraps a {@link EmbergraphStatement} and provides {@link #hashCode()} and {@link
 * #equals(Object)} respecting all four fields rather than SPO as per the {@link
 * org.openrdf.model.Statement} contract.
 *
 * @author jeremycarroll
 */
public class EmbergraphQuadWrapper {

  private final EmbergraphStatement delegate;

  public EmbergraphQuadWrapper(final EmbergraphStatement cspo) {
    delegate = cspo;
  }

  @Override
  public int hashCode() {
    if (hash == 0) {

      if (delegate.getContext() == null) {
        hash = delegate.hashCode();
      } else {
        hash = delegate.getContext().hashCode() + 31 * delegate.hashCode();
      }
    }

    return hash;
  }

  private int hash = 0;

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof EmbergraphQuadWrapper)) {
      return false;
    }
    final EmbergraphStatement oo = ((EmbergraphQuadWrapper) o).delegate;
    return delegate.equals(oo) && equals(delegate.getContext(), oo.getContext());
  }

  private boolean equals(final EmbergraphResource a, final EmbergraphResource b) {
    return Objects.equals(a, b);
  }

  public EmbergraphStatement statement() {
    return delegate;
  }
}
