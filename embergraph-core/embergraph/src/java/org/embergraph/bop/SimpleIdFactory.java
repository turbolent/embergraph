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
package org.embergraph.bop;

import java.util.concurrent.atomic.AtomicInteger;

public class SimpleIdFactory implements IdFactory {

  /**
   * Note: The ids are assigned using {@link AtomicInteger#incrementAndGet()} so ONE (1) is the
   * first id that will be assigned when we pass in ZERO (0) as the initial state of the {@link
   * AtomicInteger}.
   */
  private final AtomicInteger nextId = new AtomicInteger(0);

  /** {@inheritDoc} */
  @Override
  public int nextId() {

    return nextId.incrementAndGet();
  }
}
