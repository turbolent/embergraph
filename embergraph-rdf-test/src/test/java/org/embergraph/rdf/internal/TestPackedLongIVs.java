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
 * Created on Nov 12, 2015
 */

package org.embergraph.rdf.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import junit.framework.TestCase2;

import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.openrdf.model.URI;

import org.embergraph.rdf.internal.constraints.MathBOp.MathOp;
import org.embergraph.rdf.internal.constraints.MathUtility;
import org.embergraph.rdf.internal.impl.extensions.CompressedTimestampExtension;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.internal.impl.literal.NumericIV;
import org.embergraph.rdf.internal.impl.literal.PackedLongIV;
import org.embergraph.rdf.internal.impl.literal.XSDDecimalIV;
import org.embergraph.rdf.internal.impl.literal.XSDIntegerIV;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.test.MockTermIdFactory;

/**
 * Test suite for math operations on {@link PackedLongIV} and
 * {@link CompressedTimestampExtension}. 
 * 
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public class TestPackedLongIVs extends TestCase2 {

    public TestPackedLongIVs() {
    }

    public TestPackedLongIVs(final String name) {
        super(name);
    }

    /**
     * Test math operations such as +, -, *, /, MIN and MAX over the datatype.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testMath() {
        
        final EmbergraphValueFactory vf =
            EmbergraphValueFactoryImpl.getInstance(getName() + UUID.randomUUID());

        final MockTermIdFactory termIdFactory = new MockTermIdFactory();
        
        final CompressedTimestampExtension<EmbergraphValue> ext =
            new CompressedTimestampExtension<EmbergraphValue>(
                new IDatatypeURIResolver() {
                      @Override
                      public EmbergraphURI resolve(final URI uri) {
                         final EmbergraphURI buri = vf.createURI(uri.stringValue());
                         buri.setIV(termIdFactory.newTermId(VTE.URI));
                         return buri;
                      }
                });

        
        final EmbergraphValue bvZero =
            vf.createLiteral("0", CompressedTimestampExtension.COMPRESSED_TIMESTAMP);
        final LiteralExtensionIV zero = ext.createIV(bvZero);
        zero.setValue(bvZero);

        final EmbergraphValue bfOne =
            vf.createLiteral("1", CompressedTimestampExtension.COMPRESSED_TIMESTAMP);
        final LiteralExtensionIV one = ext.createIV(bfOne);
        one.setValue(bfOne);
        
        final EmbergraphValue bfTen =
            vf.createLiteral("10", CompressedTimestampExtension.COMPRESSED_TIMESTAMP);
        final LiteralExtensionIV ten = ext.createIV(bfTen);
        ten.setValue(bfTen);
        
        final EmbergraphValue bfTwenty =
            vf.createLiteral("20", CompressedTimestampExtension.COMPRESSED_TIMESTAMP);
        final LiteralExtensionIV twenty = ext.createIV(bfTwenty);
        twenty.setValue(bfTwenty);

        final NumericIV<EmbergraphLiteral, ?> result10a_int_act = MathUtility.literalMath(zero, ten, MathOp.PLUS);
        final NumericIV<EmbergraphLiteral, ?> result10b_int_act = MathUtility.literalMath(twenty, ten, MathOp.MINUS);
        final NumericIV<EmbergraphLiteral, ?> result10c_int_act = MathUtility.literalMath(ten, one, MathOp.MULTIPLY);
        final NumericIV<EmbergraphLiteral, ?> result10d_dec_act = MathUtility.literalMath(ten, one, MathOp.DIVIDE);
        final NumericIV<EmbergraphLiteral, ?> result10e_int_act = MathUtility.literalMath(ten, twenty, MathOp.MIN);
        final NumericIV<EmbergraphLiteral, ?> result10f_int_act = MathUtility.literalMath(twenty, ten, MathOp.MIN);
        final NumericIV<EmbergraphLiteral, ?> result20a_int_act = MathUtility.literalMath(ten, ten, MathOp.PLUS);
        final NumericIV<EmbergraphLiteral, ?> result20b_int_act = MathUtility.literalMath(ten, twenty, MathOp.MAX);
        final NumericIV<EmbergraphLiteral, ?> result20c_int_act = MathUtility.literalMath(twenty, ten, MathOp.MAX);
        
        final XSDIntegerIV<?> result10_int = new XSDIntegerIV<>(new BigInteger("10"));
        final XSDDecimalIV<?> result10_dec = new XSDDecimalIV<>(new BigDecimal(new BigInteger("10")));
        final XSDIntegerIV<?> result20_int = new XSDIntegerIV<>(new BigInteger("20"));

        assertEquals(result10_int, result10a_int_act);
        assertEquals(result10_int, result10b_int_act);
        assertEquals(result10_int, result10c_int_act);
        assertEquals(result10_dec, result10d_dec_act);
        assertEquals(result10_int, result10e_int_act);
        assertEquals(result10_int, result10f_int_act);
        assertEquals(result20_int, result20a_int_act);
        assertEquals(result20_int, result20b_int_act);
        assertEquals(result20_int, result20c_int_act);

    }

}
