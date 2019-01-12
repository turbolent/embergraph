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
 * Created on Apr 16, 2008
 */

package org.embergraph.rdf.model;

import java.io.IOException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.IVUtility;

/*
 * Abstract base class for {@link EmbergraphValue} implementations.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class EmbergraphValueImpl implements EmbergraphValue {

  /** */
  private static final long serialVersionUID = 3114316856174115308L;

  private transient volatile EmbergraphValueFactory valueFactory;

  protected volatile IV iv;

  @Override
  public final EmbergraphValueFactory getValueFactory() {

    return valueFactory;
  }

  //    Note: unused.
  //    /*
  //     *
  //     * @param valueFactory
  //     *
  //     * @throws IllegalArgumentException
  //     *             if the argument is <code>null</code>.
  //     * @throws IllegalStateException
  //     *             if a different {@link EmbergraphValueFactoryImpl} has already been
  //     *             set.
  //     */
  //    public final void setValueFactory(final EmbergraphValueFactory valueFactory) {
  //
  //        if (valueFactory == null)
  //            throw new IllegalArgumentException();
  //
  //        if (this.valueFactory != null && this.valueFactory != valueFactory)
  //            throw new IllegalStateException();
  //
  //        this.valueFactory = valueFactory;
  //
  //    }

  /*
   * @param valueFactory The value factory that created this object (optional).
   * @param iv The internal value (optional).
   */
  protected EmbergraphValueImpl(final EmbergraphValueFactory valueFactory, final IV iv) {

    //        if (valueFactory == null)
    //            throw new IllegalArgumentException();

    this.valueFactory = valueFactory;

    this.iv = iv;
  }

  @Override
  public final void clearInternalValue() {

    iv = null;
  }

  public final boolean isRealIV() {

    if (iv == null) return false;

    return !iv.isNullIV();
  }

  @Override
  public IV getIV() {

    return iv;
  }

  @Override
  public final void setIV(final IV iv) {

    if (iv == null) {

      throw new IllegalArgumentException("Can not set IV to null: term=" + this);
    }

    if (this.iv != null && !IVUtility.equals(this.iv, iv)) {

      throw new IllegalStateException(
          "Already assigned: old=" + this.iv + ", new=" + iv + ", this: " + this);
    }

    this.iv = iv;
  }

  /*
   * Extends the serialization format to include the namespace of the lexicon so we can recover the
   * {@link EmbergraphValueFactory} singleton reference for that namespace when the value is
   * deserialized.
   */
  private void writeObject(final java.io.ObjectOutputStream out) throws IOException {

    out.defaultWriteObject();

    out.writeUTF(valueFactory.getNamespace());
  }

  /*
   * Imposes the canonicalizing mapping on the non-Serializable EmbergraphValueFactory during object
   * de-serialization.
   */
  private void readObject(final java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    final String namespace = in.readUTF();

    valueFactory = EmbergraphValueFactoryImpl.getInstance(namespace);
  }

  /*
   * Implements {@link IElement}. EmbergraphValue acts as a lexicon element, with the term in the
   * 0th index position and the IV in the 1st index position.
   */
  @Override
  public Object get(int index) {

    if (index == 0) {

      return this;

    } else if (index == 1) {

      return getIV();

    } else {

      throw new IllegalArgumentException();
    }
  }
}
