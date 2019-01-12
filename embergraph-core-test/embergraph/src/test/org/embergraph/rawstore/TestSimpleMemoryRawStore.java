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
 * Created on Jan 31, 2007
 */

package org.embergraph.rawstore;

/*
 * Test suite for {@link SimpleMemoryRawStore}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestSimpleMemoryRawStore extends AbstractRawStoreTestCase {

  /** */
  public TestSimpleMemoryRawStore() {}

  /** @param name */
  public TestSimpleMemoryRawStore(String name) {
    super(name);
  }

  protected IRawStore getStore() {

    return new SimpleMemoryRawStore();
  }

  public void test_ctor() {

    new SimpleMemoryRawStore();

    new SimpleMemoryRawStore(0);

    new SimpleMemoryRawStore(10);

    try {

      new SimpleMemoryRawStore(-1);

      fail("Expecting: " + IllegalArgumentException.class);

    } catch (IllegalArgumentException ex) {

      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
  }
}
