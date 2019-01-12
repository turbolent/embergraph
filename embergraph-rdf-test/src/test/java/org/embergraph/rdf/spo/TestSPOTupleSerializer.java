/*
* The Notice below must appear in each file of the Source Code of any copy you distribute of the
 * Licensed Product. Contributors to any Modifications may add their own copyright notices to
 * identify their own contributions.
 *
 * <p>License:
 *
 * <p>The contents of this file are subject to the CognitiveWeb Open Source License Version 1.1 (the
 * License). You may not copy or use this file, in either source code or executable form, except in
 * compliance with the License. You may obtain a copy of the License from
 *
 * <p>http://www.CognitiveWeb.org/legal/license/
 *
 * <p>Software distributed under the License is distributed on an AS IS basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyrights:
 *
 * <p>Portions created by or assigned to CognitiveWeb are Copyright (c) 2003-2003 CognitiveWeb. All
 * Rights Reserved. Contact information for CognitiveWeb is available at
 *
 * <p>http://www.CognitiveWeb.org
 *
 * <p>Portions Copyright (c) 2002-2003 Bryan Thompson.
 *
 * <p>Acknowledgements:
 *
 * <p>Special thanks to the developers of the Jabber Open Source License 1.0 (JOSL), from which this
 * License was derived. This License contains terms that differ from JOSL.
 *
 * <p>Special thanks to the CognitiveWeb Open Source Contributors for their suggestions and support
 * of the Cognitive Web.
 *
 * <p>Modifications:
 */
/*
 * Created on Jul 8, 2008
 */

package org.embergraph.rdf.spo;

import junit.framework.TestCase2;
import org.embergraph.btree.AbstractTuple;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITupleSerializer;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.test.MockTermIdFactory;
import org.embergraph.util.BytesUtil;

/*
* Test suite for {@link SPOTupleSerializer}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @see TestSPO#test_valueEncodingRoundTrip()
 */
public class TestSPOTupleSerializer extends TestCase2 {

  /** */
  public TestSPOTupleSerializer() {}

  /** @param arg0 */
  public TestSPOTupleSerializer(String arg0) {
    super(arg0);
  }

  private IV<?, ?> _1, _2, _3, _4;

  private MockTermIdFactory factory;

  protected void setUp() throws Exception {

    super.setUp();

    factory = new MockTermIdFactory();

    _1 = tid(1);
    _2 = tid(2);
    _3 = tid(3);
    _4 = tid(4);
  }

  protected void tearDown() throws Exception {

    super.tearDown();

    factory = null;

    _1 = _2 = _3 = _4 = null;
  }

  private IV<?, ?> tid(final long tidIsIgnored) {

    return factory.newTermId(VTE.URI);
  }

  public void test_statementOrder() {

    SPOTupleSerializer fixture = new SPOTupleSerializer(SPOKeyOrder.SPO, false /* sids */);

    byte[] k_1 = fixture.serializeKey(new SPO(_1, _2, _3));
    byte[] k_2 = fixture.serializeKey(new SPO(_2, _2, _3));
    byte[] k_3 = fixture.serializeKey(new SPO(_2, _2, _4));

    if (log.isInfoEnabled()) {
      log.info("k_1(_1,_2,_2) = " + BytesUtil.toString(k_1));
      log.info("k_2(_2,_2,_3) = " + BytesUtil.toString(k_2));
      log.info("k_3(_2,_2,_4) = " + BytesUtil.toString(k_3));
    }

    assertTrue(BytesUtil.compareBytes(k_1, k_2) < 0);
    assertTrue(BytesUtil.compareBytes(k_2, k_3) < 0);
  }

  public void test_encodeDecodeTriple() {

    doEncodeDecodeTest(new SPO(_1, _2, _3, StatementEnum.Axiom), SPOKeyOrder.SPO);

    doEncodeDecodeTest(new SPO(_1, _2, _3, StatementEnum.Explicit), SPOKeyOrder.POS);

    doEncodeDecodeTest(new SPO(_1, _2, _3, StatementEnum.Inferred), SPOKeyOrder.OSP);
  }

  public void test_encodeDecodeTripleWithSID() {

    {
      final SPO spo = new SPO(_3, _1, _2, StatementEnum.Explicit);

      //            spo.setStatementIdentifier(true);

      doEncodeDecodeTest(spo, SPOKeyOrder.SPO);
    }

    {
      final SPO spo = new SPO(_3, _1, _2, StatementEnum.Explicit);

      //            spo.setStatementIdentifier(true);

      doEncodeDecodeTest(spo, SPOKeyOrder.POS);
    }

    {
      final SPO spo = new SPO(_3, _1, _2, StatementEnum.Explicit);

      //            spo.setStatementIdentifier(true);

      doEncodeDecodeTest(spo, SPOKeyOrder.OSP);
    }
  }

  public void test_encodeDecodeQuad() {

    for (int i = SPOKeyOrder.FIRST_QUAD_INDEX; i <= SPOKeyOrder.LAST_QUAD_INDEX; i++) {

      final SPOKeyOrder keyOrder = SPOKeyOrder.valueOf(i);

      doEncodeDecodeTest(new SPO(_1, _2, _3, _4, StatementEnum.Axiom), keyOrder);

      doEncodeDecodeTest(new SPO(_1, _2, _3, _4, StatementEnum.Inferred), keyOrder);

      doEncodeDecodeTest(new SPO(_1, _2, _3, _4, StatementEnum.Explicit), keyOrder);
    }
  }

  protected void doEncodeDecodeTest(final SPO expected, SPOKeyOrder keyOrder) {

    final SPOTupleSerializer fixture =
        new SPOTupleSerializer(keyOrder, expected.hasStatementIdentifier());

    // encode key
    final byte[] key = fixture.serializeKey(expected);

    // encode value.
    final byte[] val = fixture.serializeVal(expected);

    /*
     * verify decoding.
     */
    final TestTuple<SPO> tuple =
        new TestTuple<SPO>(IRangeQuery.KEYS | IRangeQuery.VALS) {

          public ITupleSerializer getTupleSerializer() {
            return fixture;
          }
        };

    // copy data into the test tuple.
    tuple.copyTuple(key, val);

    final SPO actual = tuple.getObject();

    if (!expected.equals(actual)) {

      fail("Expected: " + expected + ", but actual=" + actual);
    }

    // Note: equals() does not test the context position.
    assertEquals("c", expected.c(), actual.c());

    if (expected.hasStatementType()) {

      assertEquals("type", expected.getStatementType(), actual.getStatementType());
    }

    assertEquals(expected.hasStatementIdentifier(), actual.hasStatementIdentifier());

    if (expected.hasStatementIdentifier()) {

      assertEquals(
          "statementIdentifier",
          expected.getStatementIdentifier(),
          actual.getStatementIdentifier());
    }
  }

  private abstract static class TestTuple<E> extends AbstractTuple<E> {

    public TestTuple(int flags) {

      super(flags);
    }

    public int getSourceIndex() {
      throw new UnsupportedOperationException();
    }

    // exposed for unit test.
    public void copyTuple(byte[] key, byte[] val) {

      super.copyTuple(key, val);
    }
  }
}
