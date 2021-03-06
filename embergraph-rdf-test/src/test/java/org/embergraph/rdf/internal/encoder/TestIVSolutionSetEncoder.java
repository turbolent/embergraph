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

import java.util.ArrayList;
import java.util.List;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.io.DataInputBuffer;
import org.embergraph.io.DataOutputBuffer;
import org.embergraph.rdf.internal.IV;

/*
 * Test suite for {@link IVSolutionSetEncoder} and {@link IVSolutionSetDecoder}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestIVSolutionSetEncoder extends AbstractBindingSetEncoderTestCase {

  /** */
  public TestIVSolutionSetEncoder() {}

  /** @param name */
  public TestIVSolutionSetEncoder(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {

    super.setUp();

    // The encoder under test.
    encoder = new IVSolutionSetEncoder();

    // The decoder under test (same object as the encoder).
    decoder = new IVSolutionSetDecoder();
  }

  //    protected void tearDown() throws Exception {
  //
  //        super.tearDown();
  //
  //        // Clear references.
  //        encoder.release();
  //        encoder = null;
  //        decoder = null;
  //
  //    }

  /** Unit test of the stream-oriented API. */
  @SuppressWarnings("rawtypes")
  public void test_streamAPI() {

    final List<IBindingSet> expectedSolutions = new ArrayList<>();

    {
      final IBindingSet expected = new ListBindingSet();
      expected.set(Var.var("x"), new Constant<IV>(termId));

      expectedSolutions.add(expected);
    }

    {
      final IBindingSet expected = new ListBindingSet();
      expected.set(Var.var("x"), new Constant<IV>(termId));
      expected.set(Var.var("y"), new Constant<IV>(blobIV));

      expectedSolutions.add(expected);
    }

    doEncodeDecodeTest(expectedSolutions);
  }

  /** Multiple solutions where an empty solution appears in the middle of the sequence. */
  @SuppressWarnings("rawtypes")
  public void test_streamAPI2() {

    final List<IBindingSet> expectedSolutions = new ArrayList<>();
    {
      final IBindingSet expected = new ListBindingSet();
      expected.set(Var.var("x"), new Constant<IV>(termId));

      expectedSolutions.add(expected);
    }

    {
      final IBindingSet expected = new ListBindingSet();

      expectedSolutions.add(expected);
    }

    {
      final IBindingSet expected = new ListBindingSet();
      expected.set(Var.var("x"), new Constant<IV>(termId));
      expected.set(Var.var("y"), new Constant<IV>(blobIV));

      expectedSolutions.add(expected);
    }

    doEncodeDecodeTest(expectedSolutions);
  }

  protected void doEncodeDecodeTest(final List<IBindingSet> expectedSolutions) {

    final int nsolutions = expectedSolutions.size();

    final DataOutputBuffer out = new DataOutputBuffer();

    for (IBindingSet bset : expectedSolutions) {

      ((IVSolutionSetEncoder) encoder).encodeSolution(out, bset);
    }

    final DataInputBuffer in = new DataInputBuffer(out.array());

    for (final IBindingSet expected : expectedSolutions) {

      final IBindingSet actual =
          ((IVSolutionSetDecoder) decoder).decodeSolution(in, true /* resolveCachedValues */);

      assertEquals(expected, actual, true /* testCache */);
    }
  }
}
