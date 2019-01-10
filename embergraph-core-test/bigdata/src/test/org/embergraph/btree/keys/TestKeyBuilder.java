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
 * Created on Jan 16, 2007
 */

package org.embergraph.btree.keys;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import junit.framework.TestCase2;

import org.embergraph.io.LongPacker;
import org.embergraph.util.BytesUtil;
import org.embergraph.util.BytesUtil.UnsignedByteArrayComparator;

/**
 * Test suite for high level operations that build variable length _unsigned_
 * byte[] keys from various data types and unicode strings.
 * 
 * @see <a href="http://docs.hp.com/en/B3906-90004/ch02s02.html#d0e1095>ranges
 *      on negative float and double values</a>
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestKeyBuilder extends TestCase2 {

    /**
     * Used to unbox an application key (convert it to an unsigned byte[]).
     */
    static private final IKeyBuilder _keyBuilder = KeyBuilder.newUnicodeInstance();

    /**
     * 
     */
    public TestKeyBuilder() {
    }

    /**
     * @param name
     */
    public TestKeyBuilder(String name) {
        super(name);
    }

    /**
     * ctor tests, including correct rejection.
     */
    public void test_ctor() {

        {
            KeyBuilder keyBuilder = new KeyBuilder();
            
            assertNotNull(keyBuilder.array());
            assertEquals(KeyBuilder.DEFAULT_INITIAL_CAPACITY,keyBuilder.array().length);
            assertEquals(0,keyBuilder.len());

        }
        
        {
            KeyBuilder keyBuilder = new KeyBuilder(0);
            assertNotNull(keyBuilder.array());
            assertEquals(KeyBuilder.DEFAULT_INITIAL_CAPACITY,keyBuilder.array().length);
            assertEquals(0,keyBuilder.len());
        }
        
        {
            KeyBuilder keyBuilder = new KeyBuilder(20);
            assertNotNull(keyBuilder.array());
            assertEquals(20,keyBuilder.array().length);
            assertEquals(0,keyBuilder.len());
        }
        
        {
            final byte[] expected = new byte[]{1,2,3,4,5,6,7,8,9,10};
            KeyBuilder keyBuilder = new KeyBuilder(4,expected);
            assertNotNull(keyBuilder.array());
            assertEquals(4,keyBuilder.len());
            assertEquals(10,keyBuilder.array().length);
            assertTrue(expected==keyBuilder.array());
        }

        /*
         * correct rejection tests.
         */
        {
            try {
                new KeyBuilder(-1);
                fail("Expecting: "+IllegalArgumentException.class);
            } catch(IllegalArgumentException ex) {
                System.err.println("Ignoring expected exception: "+ex);
            }
        }

        {
            try {
                new KeyBuilder(20,null);
                fail("Expecting: "+IllegalArgumentException.class);
            } catch(IllegalArgumentException ex) {
                System.err.println("Ignoring expected exception: "+ex);
            }
        }
        
        {
            try {
                new KeyBuilder(20,new byte[3]);
                fail("Expecting: "+IllegalArgumentException.class);
            } catch(IllegalArgumentException ex) {
                System.err.println("Ignoring expected exception: "+ex);
            }
        }
        
    }
    
    public void test_keyBuilder_ensureCapacity() {
        
        final int initialCapacity = 1;
        
        KeyBuilder keyBuilder = new KeyBuilder(initialCapacity);

        assertEquals(0,keyBuilder.len());
        assertNotNull( keyBuilder.array());
        assertEquals(initialCapacity,keyBuilder.array().length);

        final byte[] originalBuffer = keyBuilder.array();
        
        // correct rejection.
        try {
            keyBuilder.ensureCapacity(-1);
            fail("Expecting: "+IllegalArgumentException.class);
        } catch(IllegalArgumentException ex) {
            System.err.println("Ignoring expected exception: "+ex);
        }
        assertTrue(originalBuffer==keyBuilder.array()); // same buffer.
        
        // no change.
        keyBuilder.ensureCapacity(initialCapacity);
        assertEquals(0,keyBuilder.len());
        assertNotNull( keyBuilder.array());
        assertEquals(initialCapacity,keyBuilder.array().length);
        assertTrue(originalBuffer==keyBuilder.array()); // same buffer.
    }
    
    public void test_keyBuilder_ensureCapacity02() {
        
        final int initialCapacity = 1;
        
        KeyBuilder keyBuilder = new KeyBuilder(initialCapacity);

        assertEquals(0,keyBuilder.len());
        assertNotNull( keyBuilder.array());
        assertEquals(initialCapacity,keyBuilder.array().length);

        final byte[] originalBuffer = keyBuilder.array();
        
        // extends buffer.
        keyBuilder.ensureCapacity(100);
        assertEquals(0,keyBuilder.len());
        assertNotNull( keyBuilder.array());
        assertEquals(100,keyBuilder.array().length);
        assertTrue(originalBuffer!=keyBuilder.array()); // different buffer.
    }
    
    /**
     * verify that existing data is preserved if the capacity is extended.
     */
    public void test_keyBuilder_ensureCapacity03() {

        Random r = new Random();
        byte[] expected = new byte[20];
        r.nextBytes(expected);

        KeyBuilder keyBuilder = new KeyBuilder(20,expected);

        assertEquals(20,keyBuilder.len());
        assertNotNull( keyBuilder.array());
        assertTrue(expected==keyBuilder.array());

        keyBuilder.ensureCapacity(30);
        assertEquals(20,keyBuilder.len());
        assertEquals(30,keyBuilder.array().length);

        assertEquals(0, BytesUtil.compareBytesWithLenAndOffset(0,
                expected.length, expected, 0, expected.length, keyBuilder.array()));
        
        for (int i = 21; i < 30; i++) {

            assertEquals(0, keyBuilder.array()[i]);

        }
        
    }

    public void test_keyBuilder_ensureFree() {
        
        final int initialCapacity = 1;
        
        KeyBuilder keyBuilder = new KeyBuilder(initialCapacity);

        assertEquals(0,keyBuilder.len());
        assertNotNull( keyBuilder.array());
        assertEquals(initialCapacity,keyBuilder.array().length);
    
        keyBuilder.ensureFree(2);
        
        assertEquals(0,keyBuilder.len());
        assertNotNull( keyBuilder.array());
        assertTrue(keyBuilder.array().length>=2);
        
    }
    
    /**
     * Tests ability to append to the buffer, including with overflow of the
     * buffer capacity.
     */
    public void test_keyBuilder_append_bytes() {
        
        // setup buffer with some data and two(2) free bytes.
        KeyBuilder keyBuilder = new KeyBuilder(5,new byte[]{1,2,3,4,5,0,0});
        
        /*
         * fill to capacity by copying two bytes from the middle of another
         * array. since this does not overflow we know the exact capacity of the
         * internal buffer (it is not reallocated).
         */
        byte[] tmp = new byte[]{4,5,6,7,8,9};
        keyBuilder.append(tmp,2,2);
        assertEquals(7,keyBuilder.len());
        assertEquals(new byte[]{1,2,3,4,5,6,7}, keyBuilder.array());
        assertEquals(0,BytesUtil.compareBytes(new byte[]{1,2,3,4,5,6,7}, keyBuilder.array()));
        
        // overflow capacity (new capacity is not known in advance).
        tmp = new byte[] { 8, 9, 10 };
        byte[] expected = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        keyBuilder.append(tmp);
        assertEquals(10, keyBuilder.len());
        assertEquals(0, BytesUtil.compareBytesWithLenAndOffset(0, expected.length, expected, 0,
                keyBuilder.len(), keyBuilder.array()));

        // possible overflow (old and new capacity are unknown).
        tmp = new byte[] { 11, 12 };
        expected = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
        keyBuilder.append(tmp);
        assertEquals(12, keyBuilder.len());
        assertEquals(0, BytesUtil.compareBytesWithLenAndOffset(0, expected.length, expected, 0,
                keyBuilder.len(), keyBuilder.array()));
        
    }

    /**
     * Test ability to extract and return a key.
     */
    public void test_keyBuilder_getKey() {
        
        IKeyBuilder keyBuilder = new KeyBuilder(5,new byte[]{1,2,3,4,5,6,7,8,9,10});
        
        byte[] key = keyBuilder.getKey();
        
        assertEquals(5,key.length);
        assertEquals(new byte[]{1,2,3,4,5},key);
        
    }
    
    /**
     * Verify returns zero length byte[] when the key has zero bytes.
     */
    public void test_keyBuilder_getKey_len0() {

        IKeyBuilder keyBuilder = new KeyBuilder();
        
        byte[] key = keyBuilder.getKey();

        assertEquals(0,key.length);
        
    }
    
    /**
     * Test ability to reset the key buffer (simply zeros the #of valid bytes
     * in the buffer without touching the buffer itself).
     */
    public void test_keyBuilder_reset() {

        byte[] expected = new byte[10];
    
        KeyBuilder keyBuilder = new KeyBuilder(5,expected);
        
        assertEquals(5,keyBuilder.len());
        assertTrue(expected==keyBuilder.array());
        
        assertTrue(keyBuilder == keyBuilder.reset());
        
        assertEquals(0,keyBuilder.len());
        assertTrue(expected==keyBuilder.array());

    }
    
    /*
     * test append keys for each data type, including that sort order of
     * successors around zero is correctly defined by the resulting key.
     */

    /**
     * Note: The {@link KeyBuilder} uses an order preserving transfrom from
     * signed bytes to unsigned bytes. This transform preserves the order of
     * values in the signed space by translating them such that the minimum
     * signed value (-128) is represented by an unsigned 0x00. For example, zero
     * (signed) becomes 0x80 (unsigned) while -1 (signed) is becomes to 0x79
     * (0x79 LT 0x80).
     */
    public void test_keyBuilder_byte_key() {
        
        IKeyBuilder keyBuilder = new KeyBuilder();

        final byte bmin = Byte.MIN_VALUE;
        final byte bm1  = (byte)-1;
        final byte b0   = (byte) 0;
        final byte bp1  = (byte) 1;
        final byte bmax = Byte.MAX_VALUE;
        
        byte[] kmin = keyBuilder.reset().appendSigned(bmin).getKey();
        byte[] km1 = keyBuilder.reset().appendSigned(bm1).getKey();
        byte[] k0 = keyBuilder.reset().appendSigned(b0).getKey();
        byte[] kp1 = keyBuilder.reset().appendSigned(bp1).getKey();
        byte[] kmax = keyBuilder.reset().appendSigned(bmax).getKey();

        assertEquals(1,kmin.length);
        assertEquals(1,km1.length);
        assertEquals(1,k0.length);
        assertEquals(1,kp1.length);
        assertEquals(1,kmax.length);
        
        System.err.println("kmin("+bmin+")="+BytesUtil.toString(kmin));
        System.err.println("km1("+bm1+")="+BytesUtil.toString(km1));
        System.err.println("k0("+b0+")="+BytesUtil.toString(k0));
        System.err.println("kp1("+bp1+")="+BytesUtil.toString(kp1));
        System.err.println("kmax("+bmax+")="+BytesUtil.toString(kmax));
        
        assertTrue("kmin<km1",BytesUtil.compareBytes(kmin, km1)<0);
        assertTrue("km1<k0",BytesUtil.compareBytes(km1, k0)<0);
        assertTrue("k0<kp1",BytesUtil.compareBytes(k0, kp1)<0);
        assertTrue("kp1<kmax",BytesUtil.compareBytes(kp1, kmax)<0);

        /*
         * verify decoding.
         */
        assertEquals("kmin",bmin,KeyBuilder.decodeByte(kmin[0]));
        assertEquals("km1" ,bm1 ,KeyBuilder.decodeByte(km1[0]));
        assertEquals("k0"  ,b0  ,KeyBuilder.decodeByte(k0[0]));
        assertEquals("kp1" ,bp1 ,KeyBuilder.decodeByte(kp1[0]));
        assertEquals("kmax",bmax,KeyBuilder.decodeByte(kmax[0]));

    }
    
    public void test_keyBuilder_short_key() {
        
        final IKeyBuilder keyBuilder = new KeyBuilder();

        final short smin = Short.MIN_VALUE;
        final short sm1  = (short)-1;
        final short s0   = (short) 0;
        final short sp1  = (short) 1;
        final short smax = Short.MAX_VALUE;
        
        byte[] kmin = keyBuilder.reset().append(smin).getKey();
        byte[] km1 = keyBuilder.reset().append(sm1).getKey();
        byte[] k0 = keyBuilder.reset().append(s0).getKey();
        byte[] kp1 = keyBuilder.reset().append(sp1).getKey();
        byte[] kmax = keyBuilder.reset().append(smax).getKey();

        assertEquals(2,kmin.length);
        assertEquals(2,km1.length);
        assertEquals(2,k0.length);
        assertEquals(2,kp1.length);
        assertEquals(2,kmax.length);

        System.err.println("kmin(" + smin + ")=" + BytesUtil.toString(kmin));
        System.err.println("km1(" + sm1 + ")=" + BytesUtil.toString(km1));
        System.err.println("k0(" + s0 + ")=" + BytesUtil.toString(k0));
        System.err.println("kp1(" + sp1 + ")=" + BytesUtil.toString(kp1));
        System.err.println("kmax(" + smax + ")=" + BytesUtil.toString(kmax));

        assertTrue("kmin<km1", BytesUtil.compareBytes(kmin, km1) < 0);
        assertTrue("km1<k0", BytesUtil.compareBytes(km1, k0) < 0);
        assertTrue("k0<kp1", BytesUtil.compareBytes(k0, kp1) < 0);
        assertTrue("kp1<kmax", BytesUtil.compareBytes(kp1, kmax) < 0);

        assertEquals((short) 0, KeyBuilder.decodeShort(TestKeyBuilder
                .asSortKey(Short.valueOf((short) 0)), 0/* off */));

        assertEquals((short) -1, KeyBuilder.decodeShort(TestKeyBuilder
                .asSortKey(Short.valueOf((short) -1)), 0/* off */));

        assertEquals((short) 1, KeyBuilder.decodeShort(TestKeyBuilder
                .asSortKey(Short.valueOf((short) 1)), 0/* off */));

        assertEquals(Short.MIN_VALUE, KeyBuilder.decodeShort(TestKeyBuilder
                .asSortKey(Short.valueOf(Short.MIN_VALUE)), 0/* off */));

        assertEquals(Short.MAX_VALUE, KeyBuilder.decodeShort(TestKeyBuilder
                .asSortKey(Short.valueOf(Short.MAX_VALUE)), 0/* off */));

    }
    
    public void test_keyBuilder_int_key() {
        
        IKeyBuilder keyBuilder = new KeyBuilder();

        final int imin = Integer.MIN_VALUE;
        final int im1 = -1;
        final int i0 = 0;
        final int ip1 = 1;
        final int imax = Integer.MAX_VALUE;

        byte[] kmin = keyBuilder.reset().append(imin).getKey();
        byte[] km1 = keyBuilder.reset().append(im1).getKey();
        byte[] k0 = keyBuilder.reset().append(i0).getKey();
        byte[] kp1 = keyBuilder.reset().append(ip1).getKey();
        byte[] kmax = keyBuilder.reset().append(imax).getKey();

        assertEquals(4,kmin.length);
        assertEquals(4,km1.length);
        assertEquals(4,k0.length);
        assertEquals(4,kp1.length);
        assertEquals(4,kmax.length);

        System.err.println("kmin("+imin+")="+BytesUtil.toString(kmin));
        System.err.println("km1("+im1+")="+BytesUtil.toString(km1));
        System.err.println("k0("+i0+")="+BytesUtil.toString(k0));
        System.err.println("kp1("+ip1+")="+BytesUtil.toString(kp1));
        System.err.println("kmax("+imax+")="+BytesUtil.toString(kmax));
        
        assertTrue("kmin<km1",BytesUtil.compareBytes(kmin, km1)<0);
        assertTrue("km1<k0",BytesUtil.compareBytes(km1, k0)<0);
        assertTrue("k0<kp1",BytesUtil.compareBytes(k0, kp1)<0);
        assertTrue("kp1<kmax",BytesUtil.compareBytes(kp1, kmax)<0);

        /*
         * verify decoding.
         * 
         * @todo test decoding at offsets != 0.
         */
        assertEquals("kmin",imin,KeyBuilder.decodeInt(kmin, 0));
        assertEquals("km1" ,im1 ,KeyBuilder.decodeInt(km1 , 0));
        assertEquals("k0"  ,i0  ,KeyBuilder.decodeInt(k0  , 0));
        assertEquals("kp1" ,ip1 ,KeyBuilder.decodeInt(kp1 , 0));
        assertEquals("kmax",imax,KeyBuilder.decodeInt(kmax, 0));

    }

    public void test_keyBuilder_long_key() {
        
        IKeyBuilder keyBuilder = new KeyBuilder();
        
        final long lmin = Long.MIN_VALUE;
        final long lm1 = -1L;
        final long l0 = 0L;
        final long lp1 = 1L;
        final long lmax = Long.MAX_VALUE;
        
        byte[] kmin = keyBuilder.reset().append(lmin).getKey();
        byte[] km1 = keyBuilder.reset().append(lm1).getKey();
        byte[] k0 = keyBuilder.reset().append(l0).getKey();
        byte[] kp1 = keyBuilder.reset().append(lp1).getKey();
        byte[] kmax = keyBuilder.reset().append(lmax).getKey();

        assertEquals(8,kmin.length);
        assertEquals(8,km1.length);
        assertEquals(8,k0.length);
        assertEquals(8,kp1.length);
        assertEquals(8,kmax.length);

        System.err.println("kmin("+lmin+")="+BytesUtil.toString(kmin));
        System.err.println("km1("+lm1+")="+BytesUtil.toString(km1));
        System.err.println("k0("+l0+")="+BytesUtil.toString(k0));
        System.err.println("kp1("+lp1+")="+BytesUtil.toString(kp1));
        System.err.println("kmax("+lmax+")="+BytesUtil.toString(kmax));
        
        assertTrue("kmin<km1",BytesUtil.compareBytes(kmin, km1)<0);
        assertTrue("km1<k0",BytesUtil.compareBytes(km1, k0)<0);
        assertTrue("k0<kp1",BytesUtil.compareBytes(k0, kp1)<0);
        assertTrue("kp1<kmax",BytesUtil.compareBytes(kp1, kmax)<0);
        assertTrue("kmin<kmax",BytesUtil.compareBytes(kmin, kmax)<0);

        /*
         * verify decoding.
         * 
         * @todo test decoding at offsets != 0.
         */
        assertEquals("kmin",lmin,KeyBuilder.decodeLong(kmin, 0));
        assertEquals("km1" ,lm1 ,KeyBuilder.decodeLong(km1 , 0));
        assertEquals("k0"  ,l0  ,KeyBuilder.decodeLong(k0  , 0));
        assertEquals("kp1" ,lp1 ,KeyBuilder.decodeLong(kp1 , 0));
        assertEquals("kmax",lmax,KeyBuilder.decodeLong(kmax, 0));
        
    }


    public void test_keyBuilder_float_key() throws NoSuccessorException {
        
        IKeyBuilder keyBuilder = new KeyBuilder();
        
        final byte[] kmin = keyBuilder.reset().append(SuccessorUtil.FNEG_MAX).getKey(); // largest negative float.
        final byte[] kn1 = keyBuilder.reset().append(SuccessorUtil.FNEG_ONE).getKey(); // -1f
        final byte[] kneg = keyBuilder.reset().append(SuccessorUtil.FNEG_MIN).getKey(); // smallest negative float.
        final byte[] km0 = keyBuilder.reset().append(SuccessorUtil.FNEG_ZERO).getKey(); // -0.0f
        final byte[] kp0 = keyBuilder.reset().append(SuccessorUtil.FPOS_ZERO).getKey(); // +0.0f
        final byte[] kpos = keyBuilder.reset().append(SuccessorUtil.FPOS_MIN).getKey(); // smallest positive float.
        final byte[] kp1 = keyBuilder.reset().append(SuccessorUtil.FPOS_ONE).getKey(); // +1f;
        final byte[] kmax = keyBuilder.reset().append(SuccessorUtil.FPOS_MAX).getKey(); // max pos float.

        assertEquals(4,kmin.length);
        assertEquals(4,kn1.length);
        assertEquals(4,kneg.length);
        assertEquals(4,km0.length);
        assertEquals(4,kp0.length);
        assertEquals(4,kpos.length);
        assertEquals(4,kp1.length);
        assertEquals(4,kmax.length);

        System.err.println("kmin("+SuccessorUtil.FNEG_MAX+")="+BytesUtil.toString(kmin));
        System.err.println("kn1("+SuccessorUtil.FNEG_ONE+")="+BytesUtil.toString(kn1));
        System.err.println("kneg("+SuccessorUtil.FNEG_MIN+")="+BytesUtil.toString(kneg));
        System.err.println("km0("+SuccessorUtil.FNEG_ZERO+")="+BytesUtil.toString(km0));
        System.err.println("kp0("+SuccessorUtil.FPOS_ZERO+")="+BytesUtil.toString(kp0));
        System.err.println("kpos("+SuccessorUtil.FPOS_MIN+")="+BytesUtil.toString(kpos));
        System.err.println("kp1("+SuccessorUtil.FPOS_ONE+")"+BytesUtil.toString(kp1));
        System.err.println("kmax("+SuccessorUtil.FPOS_MAX+")="+BytesUtil.toString(kmax));
        
        assertTrue("kmin<kn1",BytesUtil.compareBytes(kmin, kn1)<0);
        assertTrue("kn1<kneg",BytesUtil.compareBytes(kn1, kneg)<0);
        assertTrue("kneg<km0",BytesUtil.compareBytes(kneg, km0)<0);
        assertTrue("km0 == kp0",BytesUtil.compareBytes(km0, kp0) == 0);
        assertTrue("kp0<kpos",BytesUtil.compareBytes(kp0, kpos)<0);
        assertTrue("kpos<kp1",BytesUtil.compareBytes(kpos, kp1)<0);
        assertTrue("kp1<kmax",BytesUtil.compareBytes(kp1, kmax)<0);
        
        /*
         * verify decoding.
         * 
         * @todo test decoding at offsets != 0.
         */
        assertEquals("kmin",SuccessorUtil.FNEG_MAX,KeyBuilder.decodeFloat(kmin, 0));
        assertEquals("kn1",SuccessorUtil.FNEG_ONE,KeyBuilder.decodeFloat(kn1, 0));
        assertEquals("kneg",SuccessorUtil.FNEG_MIN,KeyBuilder.decodeFloat(kneg, 0));
        assertEquals("km0",SuccessorUtil.FNEG_ZERO,KeyBuilder.decodeFloat(km0, 0));
        assertEquals("kp0",SuccessorUtil.FPOS_ZERO,KeyBuilder.decodeFloat(kp0, 0));
        assertEquals("kpos",SuccessorUtil.FPOS_MIN,KeyBuilder.decodeFloat(kpos, 0));
        assertEquals("kp1",SuccessorUtil.FPOS_ONE,KeyBuilder.decodeFloat(kp1, 0));
        assertEquals("kmax",SuccessorUtil.FPOS_MAX,KeyBuilder.decodeFloat(kmax, 0));
        
    }

    public void test_keyBuilder_double_key() throws NoSuccessorException {
        
        IKeyBuilder keyBuilder = new KeyBuilder();
        
        final byte[] kmin = keyBuilder.reset().append(SuccessorUtil.DNEG_MAX).getKey(); // largest negative double.
        final byte[] kn1 = keyBuilder.reset().append(SuccessorUtil.DNEG_ONE).getKey(); // -1f
        final byte[] kneg = keyBuilder.reset().append(SuccessorUtil.DNEG_MIN).getKey(); // smallest negative double.
        final byte[] km0 = keyBuilder.reset().append(SuccessorUtil.DNEG_ZERO).getKey(); // -0.0f
        final byte[] kp0 = keyBuilder.reset().append(SuccessorUtil.DPOS_ZERO).getKey(); // +0.0f
        final byte[] kpos = keyBuilder.reset().append(SuccessorUtil.DPOS_MIN).getKey(); // smallest positive double.
        final byte[] kp1 = keyBuilder.reset().append(SuccessorUtil.DPOS_ONE).getKey(); // +1f;
        final byte[] kmax = keyBuilder.reset().append(SuccessorUtil.DPOS_MAX).getKey(); // max pos double.

        assertEquals(8,kmin.length);
        assertEquals(8,kn1.length);
        assertEquals(8,kneg.length);
        assertEquals(8,km0.length);
        assertEquals(8,kp0.length);
        assertEquals(8,kpos.length);
        assertEquals(8,kp1.length);
        assertEquals(8,kmax.length);

        System.err.println("kmin("+SuccessorUtil.DNEG_MAX+")="+BytesUtil.toString(kmin));
        System.err.println("kn1("+SuccessorUtil.DNEG_ONE+")="+BytesUtil.toString(kn1));
        System.err.println("kneg("+SuccessorUtil.DNEG_MIN+")="+BytesUtil.toString(kneg));
        System.err.println("km0("+SuccessorUtil.DNEG_ZERO+")="+BytesUtil.toString(km0));
        System.err.println("kp0("+SuccessorUtil.DPOS_ZERO+")="+BytesUtil.toString(kp0));
        System.err.println("kpos("+SuccessorUtil.DPOS_MIN+")="+BytesUtil.toString(kpos));
        System.err.println("kp1("+SuccessorUtil.DPOS_ONE+")"+BytesUtil.toString(kp1));
        System.err.println("kmax("+SuccessorUtil.DPOS_MAX+")="+BytesUtil.toString(kmax));
        
        assertTrue("kmin<kn1",BytesUtil.compareBytes(kmin, kn1)<0);
        assertTrue("kn1<kneg",BytesUtil.compareBytes(kn1, kneg)<0);
        assertTrue("kneg<km0",BytesUtil.compareBytes(kneg, km0)<0);
        assertTrue("km0 == kp0",BytesUtil.compareBytes(km0, kp0) == 0);
        assertTrue("kp0<kpos",BytesUtil.compareBytes(kp0, kpos)<0);
        assertTrue("kpos<kp1",BytesUtil.compareBytes(kpos, kp1)<0);
        assertTrue("kp1<kmax",BytesUtil.compareBytes(kp1, kmax)<0);
        
        /*
         * verify decoding.
         * 
         * @todo test decoding at offsets != 0.
         */
        assertEquals("kmin",SuccessorUtil.DNEG_MAX,KeyBuilder.decodeDouble(kmin, 0));
        assertEquals("kn1",SuccessorUtil.DNEG_ONE,KeyBuilder.decodeDouble(kn1, 0));
        assertEquals("kneg",SuccessorUtil.DNEG_MIN,KeyBuilder.decodeDouble(kneg, 0));
        assertEquals("km0",SuccessorUtil.DNEG_ZERO,KeyBuilder.decodeDouble(km0, 0));
        assertEquals("kp0",SuccessorUtil.DPOS_ZERO,KeyBuilder.decodeDouble(kp0, 0));
        assertEquals("kpos",SuccessorUtil.DPOS_MIN,KeyBuilder.decodeDouble(kpos, 0));
        assertEquals("kp1",SuccessorUtil.DPOS_ONE,KeyBuilder.decodeDouble(kp1, 0));
        assertEquals("kmax",SuccessorUtil.DPOS_MAX,KeyBuilder.decodeDouble(kmax, 0));

    }

    /**
     * Test verifies encode/decode of {@link UUID}s and also verifies that the
     * natural order of the encoded {@link UUID}s respects the order imposed
     * by {@link UUID#compareTo(UUID)}.
     */
    public void test_keyBuilder_UUID() {

        final IKeyBuilder keyBuilder = new KeyBuilder();
        
        final int limit = 1000;
        
        final UUID[] a = new UUID[limit];

        final byte[][] b = new byte[limit][];

        for (int i = 0; i < limit; i++) {

            final UUID expected = UUID.randomUUID();

            final byte[] key = keyBuilder.reset().append(expected).getKey();

            final UUID actual = KeyBuilder.decodeUUID(key, 0/* offset */);

            a[i] = expected;
            
            b[i] = key;

            // verify decode.
            assertEquals(expected, actual);

        }
        
        // Put the UUIDs into their natural order.
        Arrays.sort(a);

        // Put the keys into their natural order.
        Arrays.sort(b, UnsignedByteArrayComparator.INSTANCE);

        // Verify that the natural orders are the same.
        for (int i = 0; i < limit; i++) {

            final UUID expected = a[i];

            final byte[] key = b[i];

            final UUID actual = KeyBuilder.decodeUUID(key, 0/* offset */);

            // verify decode.
            assertEquals(expected, actual);

        }
        
    }
    
    /**
     * Test ordering imposed by encoding a single ASCII key.
     * 
     * @todo test ability to decode an ASCII field in a non-terminal position of
     *       a multi-field key.
     */
    public void test_keyBuilder_ascii() {
        
        IKeyBuilder keyBuilder = new KeyBuilder();
            
        byte[] key1 = keyBuilder.reset().appendASCII("abc").getKey();
        byte[] key2 = keyBuilder.reset().appendASCII("ABC").getKey();
        byte[] key3 = keyBuilder.reset().appendASCII("Abc").getKey();

        System.err.println("abc: "+BytesUtil.toString(key1));
        System.err.println("ABC: "+BytesUtil.toString(key2));
        System.err.println("Abc: "+BytesUtil.toString(key3));
        
        // unlike a unicode encoding, this produces one byte per character.
        assertEquals(3,key1.length);
        assertEquals(3,key2.length);
        assertEquals(3,key3.length);

        /*
         * verify ordering for US-ASCII comparison.
         * 
         * Note: unlike the default unicode sort order, lowercase ASCII sorts
         * after uppercase ASCII.
         */
        assertTrue(BytesUtil.compareBytes(key1, key2)>0);
        assertTrue(BytesUtil.compareBytes(key2, key3)<0);
        
        assertEquals("abc",KeyBuilder.decodeASCII(key1,0,3));
        assertEquals("ABC",KeyBuilder.decodeASCII(key2,0,3));
        assertEquals("Abc",KeyBuilder.decodeASCII(key3,0,3));
        
    }

    /**
     * Test verifies the order for ASCII sort keys, including verifying that
     * the pad byte causes a prefix such as "bro" to sort before a term which
     * extends that prefix, such as "brown".
     */
    public void test_keyBuilder_ascii_order() {        

        KeyBuilder keyBuilder = (KeyBuilder) KeyBuilder.newInstance();
        
        KVO<String>[] a = new KVO[] {
          
                new KVO<String>(TestKeyBuilder.asSortKey("bro"),null,"bro"),
                new KVO<String>(TestKeyBuilder.asSortKey("brown"),null,"brown"),
                new KVO<String>(TestKeyBuilder.asSortKey("bre"),null,"bre"),
                new KVO<String>(TestKeyBuilder.asSortKey("break"),null,"break"),
                
        };
        
        // sort by the assigned sort keys.
        Arrays.sort(a);
        
        /*
         * verify that "bre(ak)" is before "bro(wn)" and that "bre" is before
         * "break" and "bro" is before "brown".
         */
        assertEquals("bre", a[0].obj);
        assertEquals("break", a[1].obj);
        assertEquals("bro", a[2].obj);
        assertEquals("brown", a[3].obj);
        
    }
    
    /**
     * <p>
     * Test that lexiographic order is maintain when a variable length ASCII
     * field is followed by another field. This test works by comparing the
     * original multi-field key with the multi-field key formed from the
     * successor of the ASCII field followed by the other field:
     * </p>
     * 
     * <pre>
     *  
     *  [text][nextValue] LT [successor(text)][nextValue]
     *  
     * </pre>
     */
    public void test_keyBuilder_multiField_ascii_long() {

        final KeyBuilder keyBuilder = (KeyBuilder) KeyBuilder.newInstance();

        doMultiFieldTests(false/*unicode*/,keyBuilder);
        
    }
    
/*
 * Moved to TestKeyBuilderCollation.  bbt 7/15/2010.
 */
//    /**
//     * Test of the ability to normalize trailing pad characters.
//     */
//    public void test_keyBuilder_normalizeTrailingPadCharacters() {
//        
//        KeyBuilder keyBuilder = (KeyBuilder)KeyBuilder.newInstance();
//        
//        assertEquals(//
//                keyBuilder.normalizeText(""),//
//                keyBuilder.normalizeText(" ")//
//                );
//        assertEquals(//
//                keyBuilder.normalizeText(""),//
//                keyBuilder.normalizeText("  ")//
//                );
//        assertEquals(//
//                keyBuilder.normalizeText(""),//
//                keyBuilder.normalizeText("      ")//
//                );
//        assertEquals(//
//                keyBuilder.normalizeText(" "),//
//                keyBuilder.normalizeText("      ")//
//                );
//        assertEquals(//
//                keyBuilder.normalizeText("abc"),//
//                keyBuilder.normalizeText("abc      ")//
//                );
//        assertEquals(//
//                keyBuilder.normalizeText("   abc"),//
//                keyBuilder.normalizeText("   abc      ")//
//                );
//        assertNotSame(//
//                keyBuilder.normalizeText("abc"),//
//                keyBuilder.normalizeText("   abc      ")//
//                );
//        
//    }
//    
//    /**
//     * Test verifies that very long strings are truncated.
//     * 
//     * @todo verify that trailing whitespace is removed after truncation rather
//     *       than before truncation.
//     */
//    public void test_keyBuilder_normalizeTruncatesVeryLongStrings() {
//
//        KeyBuilder keyBuilder = (KeyBuilder)KeyBuilder.newInstance();
//
//        final String text = getMaximumLengthText();
//
//        assertEquals(//
//                keyBuilder.normalizeText(text),//
//                keyBuilder.normalizeText(text+"abc")//
//                );
//        
//    }
//    
//    /**
//     * Test verifies the order among unicode sort keys, including verifying that
//     * the pad byte causes a prefix such as "bro" to sort before a term which
//     * extends that prefix, such as "brown".
//     */
//    public void test_keyBuilder_unicode_order() {        
//
//        KeyBuilder keyBuilder = (KeyBuilder) KeyBuilder.newUnicodeInstance();
//        
//        KVO<String>[] a = new KVO[] {
//          
//                new KVO<String>(keyBuilder.asSortKey("bro"),null,"bro"),
//                new KVO<String>(keyBuilder.asSortKey("brown"),null,"brown"),
//                new KVO<String>(keyBuilder.asSortKey("bre"),null,"bre"),
//                new KVO<String>(keyBuilder.asSortKey("break"),null,"break"),
//                
//        };
//        
//        // sort by the assigned sort keys.
//        Arrays.sort(a);
//        
//        /*
//         * verify that "bre(ak)" is before "bro(wn)" and that "bre" is before
//         * "break" and "bro" is before "brown".
//         */
//        assertEquals("bre", a[0].obj);
//        assertEquals("break", a[1].obj);
//        assertEquals("bro", a[2].obj);
//        assertEquals("brown", a[3].obj);
//        
//    }
//
//    /**
//     * <p>
//     * Test that lexiographic order is maintain when a variable length Unicode
//     * field is followed by another field. This test works by comparing the
//     * original multi-field key with the multi-field key formed from the
//     * successor of the Unicode field followed by the other field:
//     * </p>
//     * 
//     * <pre>
//     *   
//     *   [text][nextValue] LT [successor(text)][nextValue]
//     *   
//     * </pre>
//     */
//    public void test_keyBuilder_multiField_unicode() {
//        
//        doMultiFieldTests(true/*unicode*/);
//
//        /*
//         * Now test some strings that contain code points outside of the 8-bit
//         * range.
//         */
//        
//        final KeyBuilder keyBuilder = (KeyBuilder) KeyBuilder
//                .newUnicodeInstance();
//
//        final boolean unicode = true;
//        {
//            
//            // Note: This is "Japanese" in kanji.
//            String text = "\u65E5\u672C\u8A9E / \u306B\u307B\u3093\u3054";
//            
//            doMultiFieldTest(keyBuilder, unicode, text, (byte) 0);
//            doMultiFieldTest(keyBuilder, unicode, text, (byte) 1);
//            doMultiFieldTest(keyBuilder, unicode, text, (byte) -1);
//            doMultiFieldTest(keyBuilder, unicode, text, Byte.MIN_VALUE);
//            doMultiFieldTest(keyBuilder, unicode, text, Byte.MAX_VALUE);
//        }
//
//    }
    
    /**
     * Test helper.
     * 
     * @param unicode
     *            When <code>true</code> tests Unicode semantics. Otherwise
     *            tests ASCII semantics.
     */
    static void doMultiFieldTests(final boolean unicode,
            final KeyBuilder keyBuilder) {

        if (unicode) {
          assertTrue(keyBuilder.isUnicodeSupported());
      }
//        final KeyBuilder keyBuilder = (KeyBuilder) (unicode ? KeyBuilder
//                .newUnicodeInstance() : KeyBuilder.newInstance());

        /*
         * example: zero length string will be padded.
         */
        doMultiFieldTest(keyBuilder,unicode,"", (byte)0);
        doMultiFieldTest(keyBuilder,unicode,"", (byte)1);
        doMultiFieldTest(keyBuilder,unicode,"", (byte)-1);
        doMultiFieldTest(keyBuilder,unicode,"", Byte.MIN_VALUE);
        doMultiFieldTest(keyBuilder,unicode,"", Byte.MAX_VALUE);


        /*
         * example: middle length string will be padded.
         */
        doMultiFieldTest(keyBuilder,unicode,"abc", (byte)0);
        doMultiFieldTest(keyBuilder,unicode,"abc", (byte)1);
        doMultiFieldTest(keyBuilder,unicode,"abc", (byte)-1);
        doMultiFieldTest(keyBuilder,unicode,"abc", Byte.MIN_VALUE);
        doMultiFieldTest(keyBuilder,unicode,"abc", Byte.MAX_VALUE);

        /*
         * example: maximum length string.
         * 
         * Note: For cases such as this one the encoded key is actually larger
         * than the original text since we have to encode a zero-length sequence
         * of the pad bytes using the order-preserving encoding method.
         */
        {
            
            String text = getMaximumLengthText();
            
            doMultiFieldTest(keyBuilder,unicode,text,(byte)0);
            doMultiFieldTest(keyBuilder,unicode,text,(byte)1);
            doMultiFieldTest(keyBuilder,unicode,text,(byte)-1);
            doMultiFieldTest(keyBuilder,unicode,text, Byte.MIN_VALUE);
            doMultiFieldTest(keyBuilder,unicode,text, Byte.MAX_VALUE);
            
        }

        /*
         * Test for all possible next values (or stress test for large value
         * space).
         */
        {
            
            for(int i=Byte.MIN_VALUE; i<=Byte.MAX_VALUE; i++) {

                Byte nextValue = (byte)i;
                
                doMultiFieldTest(keyBuilder,unicode,"abc", nextValue);
                
            }
            
        }
        
        {
            
            for(int i=Short.MIN_VALUE; i<=Short.MAX_VALUE; i++) {

                Short nextValue = (short)i;
                
                doMultiFieldTest(keyBuilder,unicode,"abc", nextValue);
                
            }
            
        }
        
        {
            
            Random r = new Random();
            
            final int LIMIT = 100000;
            
            for(int i=0; i<LIMIT; i++) {

                Long nextValue = r.nextLong();
                
                doMultiFieldTest(keyBuilder,unicode,"abc", nextValue);
                
            }
            
        }

    }

    /**
     * Return a string consisting of a repeating sequence of the digits zero
     * through nine whose length is {@link IKeyBuilder#maxlen}.
     */
    static String getMaximumLengthText() {

        final int len = IKeyBuilder.maxlen;

        StringBuilder sb = new StringBuilder(len);

        char[] data = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9' };

        for (int i = 0; i < len; i++) {

            sb.append(data[i % 10]);

        }

        String text = sb.toString();
        
        return text;

    }
    
    /**
     * Test helper forms two keys and verifies successor semantics:
     * <pre>
     *  
     *  [text][nextValue] LT [successor(text)][nextValue]
     *  
     * </pre> 
     * 
     * @param unicode
     *            When <code>true</code> the text will be encoded as Unicode
     *            using the default collator. Otherwise the text will be encoded
     *            as ASCII.
     * @param text
     *            The text to be encoded into the 1st field of the key.
     * @param nextValue
     *            The value to be encoded into the next field of the key.
     */
    static void doMultiFieldTest(KeyBuilder keyBuilder, final boolean unicode,
            final String text, final Object nextValue) {

        // form a key from [text][nextValue].
        keyBuilder.reset();
        final byte[] k1 = keyBuilder
                .appendText(text, unicode, false/* successor */).append(
                        nextValue).getKey();

        // form a key from [successor(text)][nextValue].
        keyBuilder.reset();
        final byte[] k2 = keyBuilder
                .appendText(text, unicode, true/* successor */).append(
                        nextValue).getKey();

        if(false && text.length()<200) {
            System.err.println("-----\n");
            System.err.println("text=[" + text + "]");
//        if (nextValue instanceof Number) {
//            int i = ((Number) nextValue).intValue();
//            System.err.println("nextValue=" + nextValue + ", signed(0x"
//                    + Integer.toHexString(i) + "), unsigned(0x"
//                    + Integer.toHexString(KeyBuilder.encode(i)) + ")");
//        }
            System.err.println("k1=" + Arrays.toString(k1));
            System.err.println("k2=" + Arrays.toString(k2));
        }
        
        // verify the ordering.
        assertTrue(BytesUtil.compareBytes(k1, k2)<0);

    }
    
    // Note: this is now allowed and interprets the data as ASCII.
//    public void test_keyBuilder_unicode_String_key() {
//        
//        IKeyBuilder keyBuilder = new KeyBuilder();
//        
//        try {
//            keyBuilder.reset().append("a");
//            fail("Expecting: "+UnsupportedOperationException.class);
//        } catch(UnsupportedOperationException ex) {
//            System.err.println("Ignoring expected exception: "+ex);
//        }        
//
//    }

//    public void test_keyBuilder_unicode_char_key() {
//        
//        IKeyBuilder keyBuilder = new KeyBuilder();
//        
//        try {
//            keyBuilder.reset().append('a');
//            fail("Expecting: "+UnsupportedOperationException.class);
//        } catch(UnsupportedOperationException ex) {
//            System.err.println("Ignoring expected exception: "+ex);
//        }        
//
//    }
    
//    public void test_keyBuilder_unicode_chars_key() {
//        
//        IKeyBuilder keyBuilder = new KeyBuilder();
//        
//        try {
//            keyBuilder.reset().append(new char[]{'a'});
//            fail("Expecting: "+UnsupportedOperationException.class);
//        } catch(UnsupportedOperationException ex) {
//            System.err.println("Ignoring expected exception: "+ex);
//        }        
//
//    }

    /*
     * verify that the ordering of floating point values when converted to
     * unsigned byte[]s is maintained.
     */
    
    /**
     * Verify that we can convert float keys to unsigned byte[]s while
     * preserving the value space order.
     */
    public void test_float_order() {

        Random r = new Random();

        // #of distinct keys to generate.
        final int limit = 100000;
        
        /*
         * Generate a set of distinct float keys distributed across the value
         * space. For each generated key, obtain the representation of that
         * float as a unsigned byte[].
         */
        class X {
            final float val;
            final byte[] key;
            public X(float val,byte[] key) {
                this.val = val;
                this.key = key;
            }
        };
        /**
         * imposes ordering based on {@link X#val}.
         */
        class XComp implements Comparator<X> {
            public int compare(X o1, X o2) {
                float ret = o1.val - o2.val;
                if( ret <0 ) return -1;
                if( ret > 0 ) return 1;
                return 0;
            }
        };
//        final float[] vals = new float[limit];
//        final byte[][] keys = new byte[limit][];
        Set<Float> set = new HashSet<Float>(limit);
        final X[] data = new X[limit];
        IKeyBuilder keyBuilder = new KeyBuilder();
        {

            int nkeys = 0;
            
            while (set.size() < limit) {

                float val = r.nextFloat();

                if (set.add(val)) {

                    // this is a new point in the value space.

                    byte[] key = keyBuilder.reset().append(val).getKey();

                    data[nkeys] = new X(val,key);

                    nkeys++;
                    
                }
                
            }
            
        }

        /*
         * sort the tuples.
         */
        Arrays.sort(data,new XComp());
        
        /*
         * Insert the int keys paired to their float values into an ordered map.
         * We insert the data in random order (paranoia), but that should not
         * matter.
         */

        System.err.println("Populating map");
        
        TreeMap<byte[],Float> map = new TreeMap<byte[],Float>(BytesUtil.UnsignedByteArrayComparator.INSTANCE);

        int[] order = getRandomOrder(limit);
        
        for( int i=0; i<limit; i++) {

            float val = data[order[i]].val;
            
            byte[] key = data[order[i]].key;
            
            if( key == null ) {
            
                fail("key is null at index="+i+", val="+val);
                
            }
            
            Float oldval = map.put(key,val);
            
            if( oldval != null ) {

                fail("Key already exists: " + BytesUtil.toString(key)
                        + " with value=" + oldval);
                
            }
            
        }
        
        assertEquals(limit,map.size());
        
        /*
         * traverse the map in key order and verify that the total ordering
         * maintained by the keys is correct for the values.
         */
        
        System.err.println("Testing map order");
        
        Iterator<Map.Entry<byte[],Float>> itr = map.entrySet().iterator();
        
        int i = 0;
        
        while(itr.hasNext() ) {
        
            Map.Entry<byte[], Float> entry = itr.next();
            
            byte[] key = entry.getKey();
            
            assert key != null;
            
            float val = entry.getValue();
            
            if (BytesUtil.compareBytes(data[i].key, key) != 0) {
                fail("keys[" + i + "]: expected=" + BytesUtil.toString(data[i].key)
                        + ", actual=" + BytesUtil.toString(key));
            }
            
            if(data[i].val != val) {
                assertEquals("vals["+i+"]", data[i].val, val);
            }
            
            i++;
            
        }
        
    }
    
    /**
     * Verify that we can convert double keys to unsigned byte[]s while
     * preserving the value space order.
     */
    public void test_double_order() {

        Random r = new Random();

        // #of distinct keys to generate.
        final int limit = 100000;
        
        /*
         * Generate a set of distinct double keys distributed across the value
         * space. For each generated key, obtain the representation of that
         * double as a unsigned byte[].
         */
        class X {
            final double val;
            final byte[] key;
            public X(double val,byte[] key) {
                this.val = val;
                this.key = key;
            }
        };
        /**
         * imposes ordering based on {@link X#val}.
         */
        class XComp implements Comparator<X> {
            public int compare(X o1, X o2) {
                double ret = o1.val - o2.val;
                if( ret <0 ) return -1;
                if( ret > 0 ) return 1;
                return 0;
            }
        };
        Set<Double> set = new HashSet<Double>(limit);
        final X[] data = new X[limit];
        IKeyBuilder keyBuilder = new KeyBuilder();
        {

            int nkeys = 0;
            
            while (set.size() < limit) {

                double val = r.nextDouble();

                if (set.add(val)) {

                    // this is a new point in the value space.

                    byte[] key = keyBuilder.reset().append(val).getKey();

                    data[nkeys] = new X(val,key);

                    nkeys++;
                    
                }
                
            }
            
        }

        /*
         * sort the tuples.
         */
        Arrays.sort(data,new XComp());
        
        /*
         * Insert the int keys paired to their double values into an ordered map.
         * We insert the data in random order (paranoia), but that should not
         * matter.
         */

        System.err.println("Populating map");
        
        TreeMap<byte[],Double> map = new TreeMap<byte[],Double>(BytesUtil.UnsignedByteArrayComparator.INSTANCE);

        int[] order = getRandomOrder(limit);
        
        for( int i=0; i<limit; i++) {

            double val = data[order[i]].val;
            
            byte[] key = data[order[i]].key;
            
            if( key == null ) {
            
                fail("key is null at index="+i+", val="+val);
                
            }
            
            Double oldval = map.put(key,val);
            
            if( oldval != null ) {

                fail("Key already exists: " + BytesUtil.toString(key)
                        + " with value=" + oldval);
                
            }
            
        }
        
        assertEquals(limit,map.size());
        
        /*
         * traverse the map in key order and verify that the total ordering
         * maintained by the keys is correct for the values.
         */
        
        System.err.println("Testing map order");
        
        Iterator<Map.Entry<byte[],Double>> itr = map.entrySet().iterator();
        
        int i = 0;
        
        while(itr.hasNext() ) {
        
            Map.Entry<byte[], Double> entry = itr.next();
            
            byte[] key = entry.getKey();
            
            assert key != null;
            
            double val = entry.getValue();
            
            if (BytesUtil.compareBytes(data[i].key, key) != 0) {
                fail("keys[" + i + "]: expected=" + BytesUtil.toString(data[i].key)
                        + ", actual=" + BytesUtil.toString(key));
            }
            
            if(data[i].val != val) {
                assertEquals("vals["+i+"]", data[i].val, val);
            }
            
            i++;
            
        }
        
    }

    /**
     * Unit test for {@link KeyBuilder#encodeByte(byte)} and
     * {@link KeyBuilder#decodeByte(byte)}. The former should have the same
     * behavior as {@link KeyBuilder#appendSigned(byte)} while the latter should
     * reverse the mapping.
     * 
     * @todo It fact, it appears that the operation is symmetric. So perhaps get
     *       rid of one? Or just make a note of this on the KeyBuilder methods?
     * 
     * @todo KeyBuilder#encodeByte(byte) is only used by the RDF package to
     *       generate the prefix for the terms index. if the order is wrong then
     *       that prefix could be unsigned.
     */
    public void test_encodeDecodeByte() {
        
        for (int b = Byte.MIN_VALUE; b < Byte.MAX_VALUE; b++) {

            assertTrue(b != KeyBuilder.encodeByte(b));

//            assertTrue(b == KeyBuilder.decodeByte(KeyBuilder.encodeByte(b)));

            final byte actual = KeyBuilder.decodeByte(KeyBuilder.encodeByte(b));

            if (b != actual) {

                fail("b=" + b + ", but actual=" + actual);

            }
            
        }

        assertTrue(BytesUtil.compareBytes(//
                TestKeyBuilder.asSortKey(Byte.valueOf((byte)-1)),
                TestKeyBuilder.asSortKey(Byte.valueOf((byte)0))
                )<0);
        
        assertTrue(BytesUtil.compareBytes(//
                TestKeyBuilder.asSortKey(Byte.valueOf((byte)0)),
                TestKeyBuilder.asSortKey(Byte.valueOf((byte)1))
                )<0);

        assertTrue(BytesUtil.compareBytes(//
                TestKeyBuilder.asSortKey(Byte.MAX_VALUE-1),
                TestKeyBuilder.asSortKey(Byte.MAX_VALUE)
                )<0);

        assertTrue(BytesUtil.compareBytes(//
                TestKeyBuilder.asSortKey(Byte.MIN_VALUE),
                TestKeyBuilder.asSortKey(Byte.MIN_VALUE+1)
                )<0);
     
        assertTrue(BytesUtil.compareBytes(//
                TestKeyBuilder.asSortKey(Byte.MIN_VALUE),
                TestKeyBuilder.asSortKey(Byte.valueOf((byte)-1))
                )<0);
        
        assertTrue(BytesUtil.compareBytes(//
                TestKeyBuilder.asSortKey(Byte.MIN_VALUE),
                TestKeyBuilder.asSortKey(Byte.valueOf((byte)0))
                )<0);
        
        assertTrue(BytesUtil.compareBytes(//
                TestKeyBuilder.asSortKey(Byte.MIN_VALUE),
                TestKeyBuilder.asSortKey(Byte.valueOf((byte)1))
                )<0);

        assertTrue(BytesUtil.compareBytes(//
                TestKeyBuilder.asSortKey(Byte.valueOf((byte)-1)),
                TestKeyBuilder.asSortKey(Byte.MAX_VALUE)
                )<0);
        
        assertTrue(BytesUtil.compareBytes(//
                TestKeyBuilder.asSortKey(Byte.valueOf((byte)0)),
                TestKeyBuilder.asSortKey(Byte.MAX_VALUE)
                )<0);
        
        assertTrue(BytesUtil.compareBytes(//
                TestKeyBuilder.asSortKey(Byte.valueOf((byte)1)),
                TestKeyBuilder.asSortKey(Byte.MAX_VALUE)
                )<0);
        

    }

//    /*
//     * Packed long integers.
//     * 
//     * These are decodable (no loss) but negative longs are not allowed.
//     */
//    public void test_packLong() {
//
//        final KeyBuilder keyBuilder = new KeyBuilder();
//
//        /*
//         * TODO Do loop, appending into the buffer. Then do decode of each
//         * packed value in turn.
//         */
//        final long v = 1;
//        final int off = keyBuilder.off();
//        keyBuilder.pack(1);
//        final int nbytes = LongPacker.getByteLength(v);
//        assertEquals("nbytes", off + nbytes, keyBuilder.off());
//
//        final long d = KeyBuilder.unpackLong(keyBuilder.array(), off, off
//                + nbytes);
//        assertEquals("decodedValue", v, d);
//        
//    }
    
    /*
     * BigInteger.
     * 
     * Note: The code below does not work correctly yet.
     */
    
    public void test_BigInteger_ctor() {
        
        final Random r = new Random();

        for (int i = 0; i < 10000; i++) {

            final BigInteger v1 = BigInteger.valueOf(r.nextLong());
  
            // Note: This DOES NOT work.
//            final BigInteger v2 = new BigInteger(v1.signum(), v1.toByteArray());
            
            // Note: This does.
            final BigInteger v2 = new BigInteger(v1.toByteArray());
            
            assertEquals(v1, v2);
            
        }
        
    }
    
    private BigInteger decodeBigInteger(final byte[] key) {

        return KeyBuilder.decodeBigInteger(0/*offset*/, key);
        
//        final int offset = 0;
//        final int tmp = KeyBuilder.decodeShort(key, offset);
//        final int runLength = tmp < 0 ? -tmp : tmp;
//        final byte[] b = new byte[runLength];
//        System.arraycopy(key/* src */, offset + 2/* srcpos */, b/* dst */,
//                0/* destPos */, runLength);
//        return new BigInteger(b);
        
    }

    private BigDecimal decodeBigDecimal(final byte[] key) {

        return KeyBuilder.decodeBigDecimal(0/*offset*/, key);
    }
    
    /**
     * FIXME The 2 byte run length limits the maximum key length for a
     * BigInteger to ~32k. Write unit tests which verify that we detect and
     * throw an IllegalArgumentException rather than just truncating the run
     * length!
     */
    private byte[] encodeBigInteger(final BigInteger i) {

        return new KeyBuilder().append(i).getKey();
        
//        final KeyBuilder keyBuilder = new KeyBuilder();
//
//        // Note: BigInteger.ZERO is represented as byte[]{0}.
//        final byte[] b = i.toByteArray();
//        final int runLength = i.signum() == -1 ? -b.length : b.length;
//        keyBuilder.ensureFree(b.length + 2);
//        keyBuilder.append((short) runLength);
//        keyBuilder.append(b);
//
//        final byte[] key = keyBuilder.getKey();
//        
//        return key;
        
    }
    
    private byte[] encodeBigDecimal(final BigDecimal d) {

        return new KeyBuilder().append(d).getKey();
        
    }
    
    protected void doEncodeDecodeTest(final BigInteger expected) {

        byte[] encoded = null;
        BigInteger actual = null;
        Throwable cause = null;
        try {

            encoded = encodeBigInteger(expected);

            actual = decodeBigInteger(encoded);

        } catch (Throwable t) {

            cause = t;

        }

        if (cause != null || !expected.equals(actual)) {

            final String msg = "BigInteger" + //
                    "\nexpected=" + expected + //
                    "\nsigned  =" + Arrays.toString(expected.toByteArray())+//
                    "\nunsigned=" + BytesUtil.toString(expected.toByteArray())+//
                    "\nencoded =" + BytesUtil.toString(encoded) + //
                    "\nactual  =" + actual+//
                    (actual != null ? "\nactualS ="
                            + Arrays.toString(actual.toByteArray())
                            + //
                            "\nactualU ="
                            + BytesUtil.toString(actual.toByteArray()) //
                    : "")
                    ;

            if (cause == null) {

                fail(msg);
                
            } else {
                
                fail(msg, cause);
                
            }
            
        }

    }

    protected void doEncodeDecodeTest(final BigDecimal expected) {

        byte[] encoded = null;
        BigDecimal actual = null;
        Throwable cause = null;
        try {

            encoded = encodeBigDecimal(expected);

            actual = decodeBigDecimal(encoded);

        } catch (Throwable t) {

            cause = t;

        }

        if (cause != null || !(expected.compareTo(actual) == 0)) {

            final String msg = "BigDecimal" + //
                    "\nexpected=" + expected + //
//                    "\nsigned  =" + Arrays.toString(expected.toByteArray())+//
//                    "\nunsigned=" + BytesUtil.toString(expected.toByteArray())+//
                    "\nencoded =" + BytesUtil.toString(encoded) + //
                    "\nactual  =" + actual//
//                    +(actual != null ? "\nactualS ="
//                            + Arrays.toString(actual.toByteArray())
//                            + //
//                            "\nactualU ="
//                            + BytesUtil.toString(actual.toByteArray()) //
//                    : "")
                    ;

            if (cause == null) {

                fail(msg);
                
            } else {
                
                fail(msg, cause);
                
            }
            
        }

    }

    protected void doLTTest(final BigInteger i1, final BigInteger i2) {
        
        final byte[] k1 = encodeBigInteger(i1);

        final byte[] k2 = encodeBigInteger(i2);

        final int ret = BytesUtil.compareBytes(k1, k2);

        if (ret >= 0) {

            fail("BigInteger" + //
                    "\ni1=" + i1 + //
                    "\ni2=" + i2 + //
                    "\ns1=" + Arrays.toString(i1.toByteArray())+//
                    "\ns2=" + Arrays.toString(i2.toByteArray())+//
                    "\nu1=" + BytesUtil.toString(i1.toByteArray())+//
                    "\nu2=" + BytesUtil.toString(i2.toByteArray())+//
                    "\nk1=" + BytesUtil.toString(k1) + //
                    "\nk2=" + BytesUtil.toString(k2) + //
                    "\nret=" + (ret == 0 ? "EQ" : (ret < 0 ? "LT" : "GT"))//
            );

        }

    }

    protected void doEQTest(final BigDecimal i1, final BigDecimal i2) {
        
        final byte[] k1 = encodeBigDecimal(i1);

        final byte[] k2 = encodeBigDecimal(i2);

        final int ret = BytesUtil.compareBytes(k1, k2);

        if (ret != 0) {

            fail("BigDecimal" + //
                    "\ni1=" + i1 + //
                    "\ni2=" + i2 + //
//                    "\ns1=" + Arrays.toString(i1.toByteArray())+//
//                    "\ns2=" + Arrays.toString(i2.toByteArray())+//
//                    "\nu1=" + BytesUtil.toString(i1.toByteArray())+//
//                    "\nu2=" + BytesUtil.toString(i2.toByteArray())+//
                    "\nk1=" + BytesUtil.toString(k1) + //
                    "\nk2=" + BytesUtil.toString(k2) + //
                    "\nret=" + (ret == 0 ? "EQ" : (ret < 0 ? "LT" : "GT"))//
            );

        }

    }

    protected void doLTTest(final BigDecimal i1, final BigDecimal i2) {
        
        final byte[] k1 = encodeBigDecimal(i1);

        final byte[] k2 = encodeBigDecimal(i2);

        final int ret = BytesUtil.compareBytes(k1, k2);

        if (ret >= 0) {

            fail("BigDecimal" + //
                    "\ni1=" + i1 + //
                    "\ni2=" + i2 + //
//                    "\ns1=" + Arrays.toString(i1.toByteArray())+//
//                    "\ns2=" + Arrays.toString(i2.toByteArray())+//
//                    "\nu1=" + BytesUtil.toString(i1.toByteArray())+//
//                    "\nu2=" + BytesUtil.toString(i2.toByteArray())+//
                    "\nk1=" + BytesUtil.toString(k1) + //
                    "\nk2=" + BytesUtil.toString(k2) + //
                    "\nret=" + (ret == 0 ? "EQ" : (ret < 0 ? "LT" : "GT"))//
            );

        }

    }

    public void test_BigInteger_383() {

        final BigInteger v1 = BigInteger.valueOf(383);
        final BigInteger v2 = BigInteger.valueOf(383+1);
        doLTTest(v1,v2);

    }
    
    public void test_BigDecimal_383() {

        final BigDecimal v1 = new BigDecimal("383.00000000000001");
        final BigDecimal v2 = new BigDecimal("383.00000000000002");
        doLTTest(v1,v2);

    }
    
    public void test_BigInteger_m1() {
        
        final BigInteger v = BigInteger.valueOf(-1);
        
        doEncodeDecodeTest(v);

    }
    
    public void test_BigDecimal_m1() {
        
        final BigDecimal v = BigDecimal.valueOf(-1.00000000001);
        
        doEncodeDecodeTest(v);

    }

    /**
     * Unit test demonstrates that precision is not preserved by the encoding.
     * Thus, ZEROs are encoded in the same manner regardless of their precision
     * (this is true of other values with trailing zeros after the decimal point
     * as well).
     */
    public void test_BigDecimal_zeroPrecisionNotPreserved() {

        // Three ZEROs with different precision.
        final BigDecimal z0 = new BigDecimal("0");
        final BigDecimal z1 = new BigDecimal("0.0");
        final BigDecimal z2 = new BigDecimal("0.00");

        // Encode each of those BigDecimal values.
        final byte[] b0 = new KeyBuilder().append(z0).getKey();
        final byte[] b1 = new KeyBuilder().append(z1).getKey();
        final byte[] b2 = new KeyBuilder().append(z2).getKey();

        // The encoded representations are the same.
        assertEquals(b0, b1);
        assertEquals(b0, b2);
        
    }

    /* Note: I've a question in to Martyn about this one.  It decodes as "5E+2"
     * rather than "500".
     */
//    public void test_BigDecimal_500() {
//
//        final BigDecimal expected = new BigDecimal("500");
//
//        final byte[] key = new KeyBuilder().append(expected).getKey();
//
//        final BigDecimal actual = KeyBuilder.decodeBigDecimal(0/* offset */,
//                key);
//
//        assertEquals(expected, actual);
//        
//    }
    
    public void test_BigDecimal_zeros() {
        
        final BigDecimal z1 = new BigDecimal("0.0");
        final BigDecimal negz1 = new BigDecimal("-0.0");
        final BigDecimal z2 = new BigDecimal("0.00");
        final BigDecimal p1 = new BigDecimal("0.01");
        final BigDecimal negp1 = new BigDecimal("-0.01");
        final BigDecimal z3 = new BigDecimal("0000.00");
        final BigDecimal m1 = new BigDecimal("1.5");
        final BigDecimal m2 = new BigDecimal("-1.51");
        final BigDecimal m5 = new BigDecimal("5");
        final BigDecimal m53 = new BigDecimal("5.000");
        final BigDecimal m500 = new BigDecimal("00500");
        final BigDecimal m5003 = new BigDecimal("500.000");
        
        doEncodeDecodeTest(m5);
        doEncodeDecodeTest(negz1);
        doEncodeDecodeTest(z1);
        doEncodeDecodeTest(z2);
        doEncodeDecodeTest(z3);
        doEncodeDecodeTest(m1);
        doEncodeDecodeTest(m2);

        doLTTest(z1, p1);
        doLTTest(negp1, z1);
        doLTTest(negp1, p1);
        doEQTest(z1, negz1);

        doEQTest(m5, m53);
        doEQTest(m500, m5003);
        doEQTest(z3, z2);
        doEQTest(z1, z2);
        doEQTest(z1, z3);
        doLTTest(z1, m1);
        doLTTest(m2, z2);
        doLTTest(z3, m1);

    }
    /**
     * Unit tests for encoding {@link BigInteger} keys.
     */
    public void test_bigIntegerKey() {

        doEncodeDecodeTest(BigInteger.valueOf(0));
        
        doEncodeDecodeTest(BigInteger.valueOf(1));
        doEncodeDecodeTest(BigInteger.valueOf(8));
        doEncodeDecodeTest(BigInteger.valueOf(255));
        doEncodeDecodeTest(BigInteger.valueOf(256));
        doEncodeDecodeTest(BigInteger.valueOf(512));
        doEncodeDecodeTest(BigInteger.valueOf(1028));

        doEncodeDecodeTest(BigInteger.valueOf(-1));
        doEncodeDecodeTest(BigInteger.valueOf(-8));
        doEncodeDecodeTest(BigInteger.valueOf(-255));
        doEncodeDecodeTest(BigInteger.valueOf(-256));
        doEncodeDecodeTest(BigInteger.valueOf(-512));
        doEncodeDecodeTest(BigInteger.valueOf(-1028));

        doEncodeDecodeTest(BigInteger.valueOf(Long.MIN_VALUE));
        doEncodeDecodeTest(BigInteger.valueOf(Long.MAX_VALUE));
        doEncodeDecodeTest(BigInteger.valueOf(Long.MIN_VALUE - 1));
        doEncodeDecodeTest(BigInteger.valueOf(Long.MAX_VALUE + 1));

        doLTTest(BigInteger.valueOf(1), BigInteger.valueOf(2));

        doLTTest(BigInteger.valueOf(0), BigInteger.valueOf(1));

        doLTTest(BigInteger.valueOf(-1), BigInteger.valueOf(0));

        doLTTest(BigInteger.valueOf(-2), BigInteger.valueOf(-1));

        doLTTest(BigInteger.valueOf(10), BigInteger.valueOf(11));

        doLTTest(BigInteger.valueOf(258), BigInteger.valueOf(259));

        doLTTest(BigInteger.valueOf(3), BigInteger.valueOf(259));

        doLTTest(BigInteger.valueOf(383), BigInteger.valueOf(383 + 1));

        /*
         * Complete coverage for 2 byte long values (with edge coverage into 3
         * byte long values).
         */
        for (int i = 0; i <= 516; i++) {

            doEncodeDecodeTest(BigInteger.valueOf(i));

            doLTTest(BigInteger.valueOf(i), BigInteger.valueOf(i + 1));

        }
        for (int i = 0; i >= -516; i--) {

            doEncodeDecodeTest(BigInteger.valueOf(i));

            doLTTest(BigInteger.valueOf(i), BigInteger.valueOf(i + 1));

        }       

    }

    /**
     * Unit tests for encoding {@link BigDecimal} keys.
     */
    public void test_bigDecimalKey() {

        doEncodeDecodeTest(BigDecimal.valueOf(0));
        
        doEncodeDecodeTest(BigDecimal.valueOf(-123450));
        doEncodeDecodeTest(BigDecimal.valueOf(-99));
        doEncodeDecodeTest(BigDecimal.valueOf(-9));
        
        doEncodeDecodeTest(BigDecimal.valueOf(1.001));
        doEncodeDecodeTest(BigDecimal.valueOf(8.0001));
        doEncodeDecodeTest(BigDecimal.valueOf(255.0001));
        doEncodeDecodeTest(BigDecimal.valueOf(256.0001));
        doEncodeDecodeTest(BigDecimal.valueOf(512.0001));
        doEncodeDecodeTest(BigDecimal.valueOf(1028.001));

        doEncodeDecodeTest(BigDecimal.valueOf(-1.0001));
        doEncodeDecodeTest(BigDecimal.valueOf(-8.0001));
        doEncodeDecodeTest(BigDecimal.valueOf(-255.0001));
        doEncodeDecodeTest(BigDecimal.valueOf(-256.0001));
        doEncodeDecodeTest(BigDecimal.valueOf(-512.0001));
        doEncodeDecodeTest(BigDecimal.valueOf(-1028.001));

        doEncodeDecodeTest(BigDecimal.valueOf(Double.MIN_VALUE));
        doEncodeDecodeTest(BigDecimal.valueOf(Double.MAX_VALUE));
        doEncodeDecodeTest(BigDecimal.valueOf(Double.MIN_VALUE - 1));
        doEncodeDecodeTest(BigDecimal.valueOf(Double.MAX_VALUE + 1));

        doLTTest(BigDecimal.valueOf(1.01), BigDecimal.valueOf(2.01));

        doLTTest(BigDecimal.valueOf(0.01), BigDecimal.valueOf(1.01));

        doLTTest(BigDecimal.valueOf(-1.01), BigDecimal.valueOf(0.01));

        doLTTest(BigDecimal.valueOf(-2.01), BigDecimal.valueOf(-1.01));

        doLTTest(BigDecimal.valueOf(10.01), BigDecimal.valueOf(11.01));

        doLTTest(BigDecimal.valueOf(258.01), BigDecimal.valueOf(259.01));

        doLTTest(BigDecimal.valueOf(3.01), BigDecimal.valueOf(259.01));

        doLTTest(BigDecimal.valueOf(383.01), BigDecimal.valueOf(383.02));

        for (int i = 0; i <= 516; i++) {

            doEncodeDecodeTest(BigDecimal.valueOf(i));

            doLTTest(BigDecimal.valueOf(i), BigDecimal.valueOf(i + 1));

        }
        for (int i = 0; i >= -516; i--) {

            doEncodeDecodeTest(BigDecimal.valueOf(i));

            doLTTest(BigDecimal.valueOf(i), BigDecimal.valueOf(i + 1));

        }       

    }

    /**
     * Stress test with random <code>long</code> values.
     */
    public void test_BigInteger_stress_long_values() {
        
        final Random r = new Random();
        
        for (int i = 0; i < 100000; i++) {
            
            final BigInteger t1 = BigInteger.valueOf(r.nextLong());
            
            final BigInteger v2 = BigInteger.valueOf(Math.abs(r.nextLong()));
            
            final BigInteger v4 = BigInteger.valueOf(r.nextLong());
            
            // x LT t1
            final BigInteger t2 = t1.subtract(v2);
            final BigInteger t4 = t1.subtract(BigInteger.valueOf(5));
            final BigInteger t5 = t1.subtract(BigInteger.valueOf(9));

            // t1 LT x
            final BigInteger t3 = t1.add(v2);
            final BigInteger t6 = t1.add(BigInteger.valueOf(5));
            final BigInteger t7 = t1.add(BigInteger.valueOf(9));
            
            doEncodeDecodeTest(t1);
            doEncodeDecodeTest(t2);
            doEncodeDecodeTest(t3);
            doEncodeDecodeTest(t4);
            doEncodeDecodeTest(t5);
            doEncodeDecodeTest(t6);
            doEncodeDecodeTest(t7);

            doLTTest(t2, t1);
            doLTTest(t4, t1);
            doLTTest(t5, t1);

            doLTTest(t1, t3);
            doLTTest(t1, t6);
            doLTTest(t1, t7);

            final int ret = t1.compareTo(v4);
            
            if (ret < 0) {

                doLTTest(t1, v4);
                
            } else if (ret > 0) {
                
                doLTTest(v4, t1);
                
            } else {

                // equal
                
            }
            
        }

    }
    
    /**
     * Stress test with random <code>double</code> values.
     */
    public void test_BigDecimal_stress_double_values() {
        
        final Random r = new Random();
        
        for (int i = 0; i < 100000; i++) {
            
            final BigDecimal t1 = BigDecimal.valueOf(r.nextDouble());
            
            final BigDecimal v2 = BigDecimal.valueOf(Math.abs(r.nextDouble()));
            
            final BigDecimal v4 = BigDecimal.valueOf(r.nextDouble());
            
            // x LT t1
            final BigDecimal t2 = t1.subtract(v2);
            final BigDecimal t4 = t1.subtract(BigDecimal.valueOf(5));
            final BigDecimal t5 = t1.subtract(BigDecimal.valueOf(9));

            // t1 LT x
            final BigDecimal t3 = t1.add(v2);
            final BigDecimal t6 = t1.add(BigDecimal.valueOf(5));
            final BigDecimal t7 = t1.add(BigDecimal.valueOf(9));
            
            doEncodeDecodeTest(t1);
            doEncodeDecodeTest(t2);
            doEncodeDecodeTest(t3);
            doEncodeDecodeTest(t4);
            doEncodeDecodeTest(t5);
            doEncodeDecodeTest(t6);
            doEncodeDecodeTest(t7);

            doLTTest(t2, t1);
            doLTTest(t4, t1);
            doLTTest(t5, t1);

            doLTTest(t1, t3);
            doLTTest(t1, t6);
            doLTTest(t1, t7);

            final int ret = t1.compareTo(v4);
            
            if (ret < 0) {

                doLTTest(t1, v4);
                
            } else if (ret > 0) {
                
                doLTTest(v4, t1);
                
            } else {

                // equal
                
            }
            
        }

    }

    /**
     * Test with positive and negative {@link BigInteger}s having a common
     * prefix with varying digits after the prefix.
     */
    public void test_BigInteger_sortOrder() {
        
        final BigInteger p1 = new BigInteger("15");
        final BigInteger p2 = new BigInteger("151");
        final BigInteger m1 = new BigInteger("-15");
        final BigInteger m2 = new BigInteger("-151");
        
        doEncodeDecodeTest(p1);
        doEncodeDecodeTest(p2);
        doEncodeDecodeTest(m1);
        doEncodeDecodeTest(m2);

        doLTTest(p1, p2); // 15 LT 151
        doLTTest(m1, p1); // -15 LT 15
        doLTTest(m2, m1); // -151 LT -15

    }

    /**
     * Test with positive and negative {@link BigDecimal}s having varying
     * digits after the decimals. 
     */
    public void test_BigDecimal_negativeSortOrder() {
        
        final BigDecimal p1 = new BigDecimal("1.5");
        final BigDecimal p2 = new BigDecimal("1.51");
        final BigDecimal m1 = new BigDecimal("-1.5");
        final BigDecimal m2 = new BigDecimal("-1.51");
        
        doEncodeDecodeTest(p1);
        doEncodeDecodeTest(p2);
        doEncodeDecodeTest(m1);
        doEncodeDecodeTest(m2);

        doLTTest(p1, p2); // 1.5 LT 1.51
        doLTTest(m1, p1); // -1.5 LT 1.5
        doLTTest(m2, m1); // -1.51 LT -1.5

    }

    /**
     * Test with positive and negative {@link BigDecimal}s with large
     * exponents 
     */
    public void test_BigDecimal_largeExponents() {
        
        final BigDecimal p1 = new BigDecimal("12000000000000000000000000");
        final BigDecimal p2 = new BigDecimal("12000000000000000000000001");
        final BigDecimal p3 = new BigDecimal("1.201E25");
        final BigDecimal p4 = new BigDecimal("12020000000000000000000000");
        final BigDecimal p5 = new BigDecimal("1.201E260");
        final BigDecimal n1 = new BigDecimal("-12000000000000000000000000");
        final BigDecimal n2 = new BigDecimal("-12000000000000000000000001");
        final BigDecimal n3 = new BigDecimal("-1.2E260");
        final BigDecimal n4 = new BigDecimal("-1.201E260");
        
        doEncodeDecodeTest(p1);
        doEncodeDecodeTest(p2);
        doEncodeDecodeTest(p3);
        doEncodeDecodeTest(p4);
        doEncodeDecodeTest(p5);
        doEncodeDecodeTest(n1);
        doEncodeDecodeTest(n2);
        doEncodeDecodeTest(n3);
        doEncodeDecodeTest(n4);

        doLTTest(p1, p2); // 1.5 LT 1.51
        doLTTest(p1, p3); // 1.5 LT 1.51
        doLTTest(p3, p4); // 1.5 LT 1.51
        doLTTest(p3, p5); // 1.5 LT 1.51
        doLTTest(n1, p1); // -1.5 LT 1.5
        doLTTest(n2, n1); // -1.51 LT -1.5
        doLTTest(n3, n1); // -1.51 LT -1.5
        doLTTest(n4, n3); // -1.51 LT -1.5

    }

    /**
     * Stress test with random byte[]s from which we then construct
     * {@link BigInteger}s.
     */
    public void test_BigInteger_stress_byteArray_values() {
        
        final Random r = new Random();
        
        final int maxlen = 1024;
        
        for (int i = 0; i < 100000; i++) {

            final int len1 = r.nextInt(maxlen) + 1;

            final int len2 = r.nextInt(maxlen) + 1;

            final byte[] b1 = new byte[len1];

            final byte[] b2 = new byte[len2];

            r.nextBytes(b1);

            r.nextBytes(b2);

            final BigInteger t1 = new BigInteger(b1);
            
            final BigInteger v2 = BigInteger.valueOf(Math.abs(r.nextLong()));
            
            final BigInteger v4 = new BigInteger(b2);
            
            // x LT t1
            final BigInteger t2 = t1.subtract(v2);
            final BigInteger t4 = t1.subtract(BigInteger.valueOf(5));
            final BigInteger t5 = t1.subtract(BigInteger.valueOf(9));

            // t1 LT x
            final BigInteger t3 = t1.add(v2);
            final BigInteger t6 = t1.add(BigInteger.valueOf(5));
            final BigInteger t7 = t1.add(BigInteger.valueOf(9));
            
            doEncodeDecodeTest(t1);
            doEncodeDecodeTest(t2);
            doEncodeDecodeTest(t3);
            doEncodeDecodeTest(t4);
            doEncodeDecodeTest(t5);
            doEncodeDecodeTest(t6);
            doEncodeDecodeTest(t7);

            doLTTest(t2, t1);
            doLTTest(t4, t1);
            doLTTest(t5, t1);

            doLTTest(t1, t3);
            doLTTest(t1, t6);
            doLTTest(t1, t7);

            final int ret = t1.compareTo(v4);
            
            if (ret < 0) {

                doLTTest(t1, v4);
                
            } else if (ret > 0) {
                
                doLTTest(v4, t1);
                
            } else {

                // equal
                
            }
            
        }

    }
    /**
     * Stress test with random byte[]s from which we then construct
     * {@link BigDecimal}s.
     */
    public void badTest_BigDecimal_stress_byteArray_values() {
        
        final Random r = new Random();
        
        final int maxlen = 64;
        
        for (int i = 0; i < 100000; i++) {

            final int len1 = r.nextInt(maxlen) + 1;

            final int len2 = r.nextInt(maxlen) + 1;

            final byte[] b1 = new byte[len1];

            final byte[] b2 = new byte[len2];

            r.nextBytes(b1);

            r.nextBytes(b2);

            // final BigDecimal t1 = new BigDecimal(new BigInteger(b1), -100 + r.nextInt(200));
            final BigDecimal t1 = new BigDecimal(new BigInteger(b1));
            
            final BigDecimal v2 = BigDecimal.valueOf(Math.abs(r.nextDouble()));
            
            // final BigDecimal v4 = new BigDecimal(new BigInteger(b2), -100 + r.nextInt(200));
            final BigDecimal v4 = new BigDecimal(new BigInteger(b2));
            
            // x LT t1
            final BigDecimal t2 = t1.subtract(v2);
            final BigDecimal t4 = t1.subtract(BigDecimal.valueOf(5));
            final BigDecimal t5 = t1.subtract(BigDecimal.valueOf(9));

            // t1 LT x
            final BigDecimal t3 = t1.add(v2);
            final BigDecimal t6 = t1.add(BigDecimal.valueOf(5));
            final BigDecimal t7 = t1.add(BigDecimal.valueOf(9));
            
            doEncodeDecodeTest(t1);
            doEncodeDecodeTest(t2);
            doEncodeDecodeTest(t3);
            doEncodeDecodeTest(t4);
            doEncodeDecodeTest(t5);
            doEncodeDecodeTest(t6);
            doEncodeDecodeTest(t7);

            doLTTest(t2, t1);
            doLTTest(t4, t1);
            doLTTest(t5, t1);

            doLTTest(t1, t3);
            doLTTest(t1, t6);
            doLTTest(t1, t7);

            final int ret = t1.compareTo(v4);
            
            if (ret < 0) {

                doLTTest(t1, v4);
                
            } else if (ret > 0) {
                
                doLTTest(v4, t1);
                
            } else {

                // equal
                
            }
            
        }

    }

//    /*
//     * BigDecimal (w/o precision).
//     */
//
//    /*
//     * Note: signum is in the key twice. Once as the first thing in the key to
//     * put the values into the correct total order and once in the byte[]
//     * representation of the unscaled BigInteger value. The first occurrence of
//     * the signum is thrown away when we decode the key.
//     */
//    private BigDecimal decodeBigDecimal(final byte[] key) {
//
//        final int offset = 0;
//        final int signum = KeyBuilder.decodeByte(key[offset]);
//        final int scale = -KeyBuilder.decodeInt(key, offset + 1);
//        final int runLength = KeyBuilder.decodeShort(key, offset + 1 + 4);
//        final byte[] b = new byte[runLength];
//        System.arraycopy(key/* src */, offset + (1 + 4 + 2)/* srcpos */,
//                b/* dst */, 0/* destPos */, runLength);
//        final BigInteger i = new BigInteger(b);// unscaled value.
//        final BigDecimal d = new BigDecimal(i, scale);
//        return d;
//        
//      }
//
//    /*
//     * Note: This relies on front-coding to compress common leading bytes
//     * (signum, scale, and runLength).
//     * 
//     * @todo limit runLength to 32kbits.
//     * @todo do we really need signum first or just scale?
//     */
//      private byte[] encodeBigDecimal(final BigDecimal i) {
//
//          // @todo When elevating into the KeyBuilder, normalize here.  We are
//          // normalizing in the unit tests in order to be able to compare the
//          // normalized values for EQ since BigDecimal compares value and
//          // scale in BigDecimal#equals().
//          final KeyBuilder keyBuilder = new KeyBuilder();
//
//          // Extract the scale of the BigDecimal. This has 32bits of significance.
//          // We flip the sign since it represents digits after the decimal, so
//          // negative scale() means smaller values.
//          final int scale = -i.scale();
//          // Extract the unscaled BigInteger component.
//          final byte[] b = i.unscaledValue().toByteArray();
//          // key := [signum(1)][scale(4)][runLength(2)][b.length]
//          keyBuilder.ensureFree(1 + 4 + 2 + b.length);
//          keyBuilder.append((byte)i.signum()); // signum (front-coding will compress)
//          keyBuilder.append(scale); // int32 scale.
//          keyBuilder.append((short) b.length); // run-length.
//          keyBuilder.append(b); // unscaled BigInteger bytes.
//
//          final byte[] key = keyBuilder.getKey();
//
//          return key;
//
//      }

    /**
     * Normalize the {@link BigDecimal} by setting the scale such that there are
     * no digits before the decimal point.
     * 
     * FIXME This fails for "0" and "0.0". The trailing .0 is considered a
     * significant digit and is not being stripped. We need to also strip
     * trailing zeros which are significant.
     * 
     * <pre>
     * i=0   (scale=0,prec=1) : 0,   scale=0, precision=1, unscaled=0, unscaled_byte[]=[0]
     * i=0.0 (scale=1,prec=1) : 0.0, scale=1, precision=1, unscaled=0, unscaled_byte[]=[0]
     * </pre>
     */
      private BigDecimal normalizeBigDecimal(final BigDecimal i) {
          
          return i.stripTrailingZeros();
          
      }

      /**
       * Dumps out interesting bits of the {@link BigDecimal} state.
       * 
       * @return The dump.
       */
      private String dumpBigDecimal(final BigDecimal i) {

        final BigInteger unscaled = i.unscaledValue();

        final String msg = i.toString() + ", scale=" + i.scale()
                + //
                ", precision=" + i.precision()
                + //
                ", unscaled=" + unscaled
                + //
                ", unscaled_byte[]="
                + BytesUtil.toString(unscaled.toByteArray())//
        ;

          return msg;

      }
//      
//    /**
//     * Note: must have normalized representation of the BigDecimal to do
//     * equals(). BigDecimal#equals(foo) compares both value and scale, while we
//     * can only test on value here.
//     */
//    protected void doEncodeDecodeTest(BigDecimal expected) {
//
//          expected = normalizeBigDecimal(expected);
//          
//          byte[] encoded = null;
//          BigDecimal actual = null;
//          Throwable cause = null;
//          try {
//
//              encoded = encodeBigDecimal(expected);
//
//              actual = decodeBigDecimal(encoded);
//
//          } catch (Throwable t) {
//
//              cause = t;
//
//          }
//
//          if (cause != null || !expected.equals(actual)) {
//
//              final String msg = "BigDecimal" + //
//                      "\nexpected=" + expected + //
//                      "\nsigned  =" + Arrays.toString(expected.unscaledValue().toByteArray())+//
//                      "\nunsigned=" + BytesUtil.toString(expected.unscaledValue().toByteArray())+//
//                      "\nencoded =" + BytesUtil.toString(encoded) + //
//                      "\nactual  =" + actual+//
//                      (actual != null ? "\nactualS ="
//                              + Arrays.toString(actual.unscaledValue().toByteArray())
//                              + //
//                              "\nactualU ="
//                              + BytesUtil.toString(actual.unscaledValue().toByteArray()) //
//                      : "")
//                      ;
//
//              if (cause == null) {
//
//                  fail(msg);
//                  
//              } else {
//                  
//                  fail(msg, cause);
//                  
//              }
//              
//          }
//
//      }
//
    private enum CompareEnum {

        LT(-1), EQ(0), GT(1);
        
        private CompareEnum(final int ret) {
            this.ret = ret;
        }

        private int ret;
        
        static public CompareEnum valueOf(final int ret) {
            if(ret<0) return LT;
            if(ret>0) return GT;
            return EQ;
        }
        
    }

    protected void doCompareTest(BigDecimal i1, BigDecimal i2, final CompareEnum cmp) {

      i1 = normalizeBigDecimal(i1);
      i2 = normalizeBigDecimal(i2);

      final byte[] k1 = encodeBigDecimal(i1);

      final byte[] k2 = encodeBigDecimal(i2);

      final int ret = BytesUtil.compareBytes(k1, k2);

      final CompareEnum cmp2 = CompareEnum.valueOf(ret);

        if (cmp2 != cmp) {

              fail("BigDecimal" + //
                      "\ni1=" + dumpBigDecimal(i1) + //
                      "\ni2=" + dumpBigDecimal(i2) + //
                      "\nk1=" + BytesUtil.toString(k1) + //
                      "\nk2=" + BytesUtil.toString(k2) + //
                      "\nret=" + cmp2 +", but expected="+cmp//
              );

          }

      }

    /**
     * Test encode/decode for various values of zero.
     */
    public void test_BigDecimal0() {

        final BigDecimal[] a = new BigDecimal[] {
                new BigDecimal("0"),    // scale=0, precision=1
                new BigDecimal("0."),   // scale=0, precision=1
                new BigDecimal("0.0"),  // scale=1, precision=1
                new BigDecimal("0.00"), // scale=2, precision=1
                new BigDecimal("00.0"), // scale=1, precision=1
                new BigDecimal("00.00"),// scale=2, precision=1
                new BigDecimal(".0"),   // scale=1, precision=1
                // NB: The precision is the #of decimal digits in the unscaled value.
                // NB: scaled := unscaled * (10 ^ -scale) 
//                new BigDecimal(".010"), // scale=3, precision=2
//                new BigDecimal(".01"),  // scale=2, precision=1
//                new BigDecimal(".1"),   // scale=1, precision=1
//                new BigDecimal("1."),   // scale=0, precision=1
//                new BigDecimal("10."),  // scale=0, precision=2
//                new BigDecimal("10.0"), // scale=1, precision=3
//                new BigDecimal("010.0"), // scale=1, precision=3
//                new BigDecimal("0010.00"), // scale=2, precision=4
                // @todo Test with cases where scale is negative (large powers of 10).
                };

        for (BigDecimal i : a) {
            i = i.stripTrailingZeros();
            if(log.isInfoEnabled())
                log.info("i="
                    + i
                    + "\t(scale="
                    + i.scale()
                    + ",prec="
                    + i.precision()
                    + ") : "
                    + dumpBigDecimal(i)
//                    i.scaleByPowerOfTen(i.scale()- i.precision()))
                            );
        }

        for (BigDecimal i : a) {
            doEncodeDecodeTest(i);
        }

        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a.length; j++) {
                doCompareTest(a[i], a[j], CompareEnum.EQ);
            }
        }

    }

    /**
     * Utility method converts an application key to a sort key (an unsigned
     * byte[] that imposes the same sort order).
     * <p>
     * Note: This method is thread-safe.
     * <p>
     * Note: Strings are Unicode safe for the default locale. See
     * {@link Locale#getDefault()}. If you require a specific local or different
     * locals at different times or for different indices then you MUST
     * provision and apply your own {@link KeyBuilder}.
     * <p>
     * Note: This method circumvents explicit configuration of the
     * {@link KeyBuilder} and is used nearly exclusively by unit tests. While
     * explicit configuration is not required for keys which do not include
     * Unicode sort key components, this method also relies on a single global
     * {@link KeyBuilder} instance protected by a lock. That lock is therefore a
     * bottleneck. The correct practice is to use thread-local or per task
     * {@link IKeyBuilder}s to avoid lock contention.
     * 
     * @param val
     *            An application key.
     * 
     * @return The unsigned byte[] equivalent of that key. This will be
     *         <code>null</code> iff the <i>key</i> is <code>null</code>. If the
     *         <i>key</i> is a byte[], then the byte[] itself will be returned.
     */
    public static final byte[] asSortKey(final Object val) {

        if (val == null) {

            return null;

        }

        if (val instanceof byte[]) {

            return (byte[]) val;

        }

        /*
         * Synchronize on the keyBuilder to avoid concurrent modification of its
         * state.
         */

        synchronized (_keyBuilder) {

            return _keyBuilder.getSortKey(val);

        }

    }

}
