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
 * Created on Jul 7, 2008
 */

package org.embergraph.btree.keys;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/*
 * Factory for instances that do NOT support Unicode.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class ASCIIKeyBuilderFactory implements IKeyBuilderFactory, Externalizable {

  /** */
  private static final long serialVersionUID = -8823261532997841046L;

  private int initialCapacity;

  public int getInitialCapacity() {

    return initialCapacity;
  }

  /** Representation includes all aspects of the {@link Serializable} state. */
  @Override
  public String toString() {

    String sb = getClass().getName() + "{ initialCapacity=" + initialCapacity
        + "}";
    return sb;
  }

  /** De-serialization ctor. */
  public ASCIIKeyBuilderFactory() {}

  public ASCIIKeyBuilderFactory(int initialCapacity) {

    if (initialCapacity < 0) throw new IllegalArgumentException();

    this.initialCapacity = initialCapacity;
  }

  @Override
  public IKeyBuilder getKeyBuilder() {

    return KeyBuilder.newInstance(initialCapacity);
  }

  /*
   * {@inheritDoc}
   *
   * <p>Note: The PRIMARY is identical to the as-configured {@link IKeyBuilder} for ASCII.
   */
  @Override
  public IKeyBuilder getPrimaryKeyBuilder() {

    return getKeyBuilder();
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException {

    initialCapacity = in.readInt();
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {

    out.writeInt(initialCapacity);
  }
}