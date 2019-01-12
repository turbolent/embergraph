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
 * Created on May 26, 2011
 */

package org.embergraph.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link OutputStream} which discards anything written on it.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class NullOutputStream extends OutputStream {

  private boolean open = true;

  public NullOutputStream() {}

  @Override
  public final void write(int b) throws IOException {
    if (!open) throw new IOException();
  }

  @Override
  public final void write(byte[] b) throws IOException {
    if (!open) throw new IOException();
  }

  @Override
  public final void write(byte[] b, int len, int off) throws IOException {
    if (!open) throw new IOException();
  }

  public final void close() {
    open = false;
  }
}
