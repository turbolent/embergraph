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
 * Created on Jul 3, 2008
 */

package org.embergraph.btree.keys;

import org.embergraph.btree.IIndex;

/**
 * A thread-local implementation.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class ThreadLocalKeyBuilderFactory implements IKeyBuilderFactory {

  private final IKeyBuilderFactory delegate;

  public ThreadLocalKeyBuilderFactory(final IKeyBuilderFactory delegate) {

    if (delegate == null) throw new IllegalArgumentException();

    this.delegate = delegate;
  }

  /**
   * A {@link ThreadLocal} variable providing access to thread-specific instances of a {@link
   * IKeyBuilder} as configured by the delegate {@link IKeyBuilderFactory}.
   *
   * <p>Note: this {@link ThreadLocal} is not static since we need configuration properties from the
   * constructor - those properties can be different for different {@link IIndex}s on the same
   * machine.
   */
  private ThreadLocal<IKeyBuilder> threadLocalKeyBuilder =
      new ThreadLocal<IKeyBuilder>() {

        @Override
        protected synchronized IKeyBuilder initialValue() {

          return delegate.getKeyBuilder();
        }
      };

  /**
   * {@inheritDoc}
   *
   * <p>Return a {@link ThreadLocal} {@link IKeyBuilder} instance configured using the {@link
   * IKeyBuilderFactory} specified to the ctor.
   */
  @Override
  public IKeyBuilder getKeyBuilder() {

    return threadLocalKeyBuilder.get();
  }

  private ThreadLocal<IKeyBuilder> threadLocalPrimaryKeyBuilder =
      new ThreadLocal<IKeyBuilder>() {

        @Override
        protected synchronized IKeyBuilder initialValue() {

          return delegate.getPrimaryKeyBuilder();
        }
      };

  /**
   * {@inheritDoc}
   *
   * <p>Return a {@link ThreadLocal} {@link IKeyBuilder} instance configured using the {@link
   * IKeyBuilderFactory} specified to the ctor but with the {@link StrengthEnum} overriden as {@link
   * StrengthEnum#Primary}.
   */
  @Override
  public IKeyBuilder getPrimaryKeyBuilder() {

    return threadLocalPrimaryKeyBuilder.get();
  }
}
