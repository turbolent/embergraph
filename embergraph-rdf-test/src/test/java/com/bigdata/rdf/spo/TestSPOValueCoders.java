/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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
 * Created on Aug 22, 2007
 */

package com.bigdata.rdf.spo;

import java.io.IOException;
import java.util.Random;

import junit.framework.TestCase2;

import com.bigdata.btree.AbstractBTreeTestCase;
import com.bigdata.btree.raba.IRaba;
import com.bigdata.btree.raba.ReadOnlyValuesRaba;
import com.bigdata.btree.raba.codec.ICodedRaba;
import com.bigdata.btree.raba.codec.IRabaCoder;
import com.bigdata.btree.raba.codec.SimpleRabaCoder;
import com.bigdata.io.AbstractFixedByteArrayBuffer;
import com.bigdata.io.DataOutputBuffer;
import com.bigdata.io.FixedByteArrayBuffer;
import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.internal.VTE;
import com.bigdata.rdf.model.StatementEnum;
import com.bigdata.test.MockTermIdFactory;

/**
 * Test suite for approaches to value compression for statement indices. The
 * values exist if either statement identifiers or truth maintenance is being
 * used, otherwise NO values are associated with the keys in the statement
 * indices. For statement identifiers, the value is the SID (int64). For truth
 * maintenance, the value is the {@link StatementEnum}. If statement identifiers
 * and truth maintenance are both enabled, then both the SID and the
 * {@link StatementEnum} are stored as the value under the key.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestSPOValueCoders extends TestCase2 {

    /**
     * 
     */
    public TestSPOValueCoders() {
        super();
    }

    /**
     * @param arg0
     */
    public TestSPOValueCoders(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {

        super.setUp();
        
        factory = new MockTermIdFactory();
        
        r = new Random();
        
    }

    protected void tearDown() throws Exception {

        super.tearDown();
        
        factory = null;
        
        r = null;
        
    }

    private Random r;
    private MockTermIdFactory factory;

    private IV<?, ?> getTermId() {

        return factory.newTermId(VTE.URI);

    }

    private IV<?, ?> getTermId(final long tidIsIgnored) {

        return factory.newTermId(VTE.URI);

    }
    
    /**
     * Return an array of {@link SPO}s.
     * 
     * @param n
     *            The #of elements in the array.
     * @param SIDs
     *            If statement identifiers should be generated.
     * @param inference
     *            if {@link StatementEnum}s should be assigned.
     * 
     * @return The array.
     */
    protected SPO[] getData(final int n, final boolean SIDs,
            final boolean inference) {

        final SPO[] a = new SPO[n];

        for (int i = 0; i < n; i++) {

            /*
             * Note: Only the {s,p,o} are assigned. The statement type and the
             * statement identifier are not part of the key for the statement
             * indices.
             */
            final SPO spo;
            
            if (SIDs && !inference) {
            
                // only explicit statements can have SIDs.
                spo = new SPO(getTermId(), getTermId(), getTermId(),
                        StatementEnum.Explicit);
                
//                spo.setStatementIdentifier(getSID());
//                spo.setStatementIdentifier(true);
                
            } else if (inference) {
               
                final int tmp = r.nextInt(100);
                final StatementEnum type;
                if (tmp < 4) {
                    type = StatementEnum.Axiom;
                } else if (tmp < 60) {
                    type = StatementEnum.Explicit;
                } else {
                    type = StatementEnum.Inferred;
                }

                spo = new SPO(getTermId(), getTermId(), getTermId(), type);

                if (SIDs && type == StatementEnum.Explicit
                        && r.nextInt(100) < 20) {

                    // explicit statement with SID.
//                    spo.setStatementIdentifier(getSID());
//                	spo.setStatementIdentifier(true);

                }
                
            } else {

                // Explicit statement (no inference, no SIDs).
                spo = new SPO(getTermId(), getTermId(), getTermId(),
                        StatementEnum.Explicit);

            }
            
            a[i] = spo;
            
        }
        
        return a;
        
    }

    public void test_simpleCoder() {

        doRoundTripTests(SimpleRabaCoder.INSTANCE, false/* sids */, false/* inference */);
        doRoundTripTests(SimpleRabaCoder.INSTANCE, true/* sids */, false/* inference */);
        doRoundTripTests(SimpleRabaCoder.INSTANCE, false/* sids */, true/* inference */);
        doRoundTripTests(SimpleRabaCoder.INSTANCE, true/* sids */, true/* inference */);
        
    }

//    public void test_FastRDFValueCoder() {
//
//        doRoundTripTests(new FastRDFValueCoder(), false/* sids */, true/* inference */);
//
//    }

    /**
     * Simple tests for {@link FastRDFValueCoder2}.
     * <P>
     * Note: this does not cover nulls or overrides, but the stress tests covers
     * both.
     */
    public void test_FastRDFValueCoder2_001() {

        final IV<?,?> _0 = getTermId(0);

        doRoundTripTest(new SPO[] { new SPO(_0, _0, _0, StatementEnum.Axiom) },
                new FastRDFValueCoder2(), false);

        doRoundTripTest(new SPO[] { new SPO(_0, _0, _0, StatementEnum.Explicit) },
                new FastRDFValueCoder2(), false);

        doRoundTripTest(new SPO[] { new SPO(_0, _0, _0, StatementEnum.Inferred) },
                new FastRDFValueCoder2(), false);

        doRoundTripTest(new SPO[] { new SPO(_0, _0, _0, StatementEnum.Axiom),
                new SPO(_0, _0, _0, StatementEnum.Inferred) },
                new FastRDFValueCoder2(), false);

        doRoundTripTest(new SPO[] { new SPO(_0, _0, _0, StatementEnum.Explicit),
                new SPO(_0, _0, _0, StatementEnum.Axiom) },
                new FastRDFValueCoder2(), false);

    }

    public void test_FastRDFValueCoder2() {

        doRoundTripTests(new FastRDFValueCoder2(), false/* sids */, true/* inference */);

    }

    protected void doRoundTripTests(final IRabaCoder rabaCoder,
            final boolean SIDs, final boolean inference) {

        doRoundTripTest(getData(0, SIDs, inference), rabaCoder, SIDs);

        doRoundTripTest(getData(1, SIDs, inference), rabaCoder, SIDs);

        doRoundTripTest(getData(2, SIDs, inference), rabaCoder, SIDs);

        doRoundTripTest(getData(10, SIDs, inference), rabaCoder, SIDs);

        for (int i = 0; i < 1000; i++) {

            doRoundTripTest(getData(r.nextInt(64), SIDs, inference), rabaCoder, SIDs);

        }

        doRoundTripTest(getData(100, SIDs, inference), rabaCoder, SIDs);

        doRoundTripTest(getData(1000, SIDs, inference), rabaCoder, SIDs);

        doRoundTripTest(getData(10000, SIDs, inference), rabaCoder, SIDs);

//        doRoundTripTest(getData(100000, SIDs, inference), rabaCoder, SIDs);

    }
    
    /**
     * Do a round-trip test test.
     * 
     * @param a
     *            The array of {@link SPO}s.
     * @param rabaCoder
     *            The compression provider.
     * 
     * @throws IOException
     */
    protected void doRoundTripTest(final SPO[] a, final IRabaCoder rabaCoder,
    		final boolean sids) {

        /*
         * Generate keys from the SPOs.
         */

        final SPOTupleSerializer tupleSer = new SPOTupleSerializer(
                SPOKeyOrder.SPO, sids);
        
        final byte[][] vals = new byte[a.length][];
        {

            for (int i = 0; i < a.length; i++) {

                vals[i] = tupleSer.serializeVal(a[i]);

            }

        }
        final IRaba expected = new ReadOnlyValuesRaba(vals);

        /*
         * Compress the keys.
         * 
         * Note: A zero-length initialCapacity is specified since the byte[]
         * allocation is generally tiny for this the SPO value coders. This
         * choice effectively defers the allocation until the coder can specify
         * a preferred capacity.
         */
        final AbstractFixedByteArrayBuffer originalData;
        {
            final DataOutputBuffer buf = new DataOutputBuffer(0/* initialCapacity */);
            originalData = rabaCoder.encode(expected, buf);
            buf.trim();
        }

        try {

            // verify we can decode the encoded data.
            {

                // decode.
                final ICodedRaba actual0 = rabaCoder.decode(originalData);

                // Verify encode() results in object which can decode the
                // byte[]s.
                AbstractBTreeTestCase.assertSameRaba(expected, actual0);

                // Verify decode when we build the decoder from the serialized
                // format.
                AbstractBTreeTestCase.assertSameRaba(expected, rabaCoder
                        .decode(actual0.data()));
            }

            // Verify encode with a non-zero offset for the DataOutputBuffer
            // returns a slice which has the same data.
            {

                // buffer w/ non-zero offset.
                final int off = 10;
                final DataOutputBuffer out = new DataOutputBuffer(off,
                        new byte[100 + off]);

                // encode onto that buffer.
                final AbstractFixedByteArrayBuffer slice = rabaCoder.encode(
                        expected, out);

                // verify same encoded data for the slice.
                assertEquals(originalData.toByteArray(), slice.toByteArray());

            }

            // Verify decode when we build the decoder from a slice with a
            // non-zero offset
            {

                final int off = 10;
                final byte[] tmp = new byte[off + originalData.len()];
                System.arraycopy(originalData.array(), originalData.off(), tmp,
                        off, originalData.len());

                // create slice
                final FixedByteArrayBuffer slice = new FixedByteArrayBuffer(
                        tmp, off, originalData.len());

                // verify same slice.
                assertEquals(originalData.toByteArray(), slice.toByteArray());

                // decode the slice.
                final IRaba actual = rabaCoder.decode(slice);

                // verify same raba.
                AbstractBTreeTestCase.assertSameRaba(expected, actual);

            }

        } catch (Throwable t) {

            fail("Cause=" + t + ", expectedRaba=" + expected, t);

        }

    }

}
