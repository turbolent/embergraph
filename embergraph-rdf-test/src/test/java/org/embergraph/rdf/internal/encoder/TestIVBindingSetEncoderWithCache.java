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
 * Created on Feb 15, 2012
 */

package org.embergraph.rdf.internal.encoder;

import java.util.Collections;
import java.util.Map;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpBase;
import org.embergraph.bop.IPredicate;
import org.embergraph.rawstore.IRawStore;
import org.embergraph.rawstore.SimpleMemoryRawStore;

/*
 * Test suite for {@link IVBindingSetEncoderWithIVCache}. This class supports an {@link IV} to
 * {@link EmbergraphValue} cache which provides lookup to resolve the observed associations as
 * reported by {@link IVCache#getValue()}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: TestIVBindingSetEncoderWithCache.java 6036 2012-02-17 14:07:48Z thompsonbry $
 *     <p>This class is out of use. As part of https://jira.blazegraph.com/browse/BLZG-1899, we
 *     carried out some experiments illustrating that caching the IVs (i.e., preserving them in
 *     HTree hash joins) is far slower than just re-materializing them. We decided to retire the
 *     class and, instead, do remove variables from the doneSet within the {@link AST2BOpUtility}
 *     update process whenever we set up an analytic hash join.
 *     <p>This test is deprecated and has been disabled in
 *     org.embergraph.rdf.internal.encoder.TestAll. Note that, if at some point we decide to
 *     re-enable the test, we would need to fix the {@link IVBindingSetEncoderWithIVCache} (I've
 *     recently added some tests to this class which are failing). Unless the {@link
 *     IVBindingSetEncoderWithIVCache} is resurrected, these test failures can simply be ignored.
 */
@Deprecated
public class TestIVBindingSetEncoderWithCache extends AbstractBindingSetEncoderTestCase {

  /** */
  public TestIVBindingSetEncoderWithCache() {}

  public TestIVBindingSetEncoderWithCache(String name) {
    super(name);
  }

  /** Backing store for caches. */
  private IRawStore store = new SimpleMemoryRawStore();

  /** Empty operator - will use defaults for various annotations. */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private BOp op =
      new BOpBase(
          new BOp[] {} /* args */,
          (Map<String, Object>)
              (Map)
                  Collections.singletonMap(
                      IPredicate.Annotations.RELATION_NAME, new String[] {namespace}) /* anns */);

  protected void setUp() throws Exception {

    super.setUp();

    // The encoder under test.
    encoder = new IVBindingSetEncoderWithIVCache(store, false /* filter */, op);

    // The decoder is the same object.
    decoder = (IVBindingSetEncoderWithIVCache) encoder;
  }

  protected void tearDown() throws Exception {

    super.tearDown();

    // Clear references.
    store = null;
    op = null;
  }
}
