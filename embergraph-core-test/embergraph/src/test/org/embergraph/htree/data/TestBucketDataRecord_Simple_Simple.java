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
 * Created on Aug 5, 2009
 */

package org.embergraph.htree.data;

import org.embergraph.btree.data.DefaultLeafCoder;
import org.embergraph.btree.raba.codec.SimpleRabaCoder;

/*
 * Test with simple coding for keys and values (no compression).
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestBucketDataRecord_Simple_Simple extends AbstractHashBucketDataRecordTestCase {

  /** */
  public TestBucketDataRecord_Simple_Simple() {}

  /** @param name */
  public TestBucketDataRecord_Simple_Simple(String name) {
    super(name);
  }

  protected void setUp() throws Exception {

    super.setUp();

    coder =
        new DefaultLeafCoder(
            SimpleRabaCoder.INSTANCE, // keys
            SimpleRabaCoder.INSTANCE // vals
            );
  }
}
