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
 * Created on Aug 19, 2010
 */

package org.embergraph.striterator;

import org.embergraph.bop.IElement;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.btree.DefaultTupleSerializer;
import org.embergraph.btree.ITupleSerializer;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.SuccessorUtil;

/*
* Abstract base class provides default behavior for generating keys for a given index order.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public abstract class AbstractKeyOrder<E> implements IKeyOrder<E> {

  /*
   * {@inheritDoc}
   *
   * <p>FIXME This needs to be reconciled with {@link ITupleSerializer#serializeKey(Object)}. For
   * example, this does not play well with the {@link DefaultTupleSerializer}.
   */
  @Override
  public byte[] getKey(final IKeyBuilder keyBuilder, final E element) {

    keyBuilder.reset();

    final int keyArity = getKeyArity(); // use the key's "arity".

    for (int i = 0; i < keyArity; i++) {

      /*
       * Note: If you need to override the default IKeyBuilder behavior do
       * it in the invoked method.
       */
      appendKeyComponent(keyBuilder, i, ((IElement) element).get(getKeyOrder(i)));
    }

    return keyBuilder.getKey();
  }

  @Override
  public byte[] getFromKey(final IKeyBuilder keyBuilder, final IPredicate<E> predicate) {

    keyBuilder.reset();

    final int keyArity = getKeyArity(); // use the key's "arity".

    boolean noneBound = true;

    for (int i = 0; i < keyArity; i++) {

      final IVariableOrConstant<?> term = predicate.get(getKeyOrder(i));

      // Note: term MAY be null for the context position.
      if (term == null || term.isVar()) break;

      /*
       * Note: If you need to override the default IKeyBuilder behavior do
       * it in the invoked method.
       */
      appendKeyComponent(keyBuilder, i, term.get());

      noneBound = false;
    }

    final byte[] key = noneBound ? null : keyBuilder.getKey();

    return key;
  }

  @Override
  public byte[] getToKey(final IKeyBuilder keyBuilder, final IPredicate<E> predicate) {

    keyBuilder.reset();

    final int keyArity = getKeyArity(); // use the key's "arity".

    boolean noneBound = true;

    for (int i = 0; i < keyArity; i++) {

      final IVariableOrConstant<?> term = predicate.get(getKeyOrder(i));

      // Note: term MAY be null for the context position.
      if (term == null || term.isVar()) break;

      /*
       * Note: If you need to override the default IKeyBuilder behavior do
       * it in the invoked method.
       */
      appendKeyComponent(keyBuilder, i, term.get());

      noneBound = false;
    }

    final byte[] key = noneBound ? null : keyBuilder.getKey();

    return key == null ? null : SuccessorUtil.successor(key);
  }

  /*
   * Encodes an value into the key. This implementation uses the default behavior of {@link
   * IKeyBuilder}. If you need to specialize how a value gets encoded into the key then you can
   * override this method.
   */
  protected void appendKeyComponent(
      final IKeyBuilder keyBuilder, final int index, final Object keyComponent) {

    keyBuilder.append(keyComponent);
  }
}
