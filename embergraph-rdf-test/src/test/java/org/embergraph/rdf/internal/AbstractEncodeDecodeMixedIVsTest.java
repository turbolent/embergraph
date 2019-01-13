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
 * Created on Oct 4, 2011
 */

package org.embergraph.rdf.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.embergraph.rdf.internal.ColorsEnumExtension.Color;
import org.embergraph.rdf.internal.impl.BlobIV;
import org.embergraph.rdf.internal.impl.bnode.FullyInlineUnicodeBNodeIV;
import org.embergraph.rdf.internal.impl.bnode.NumericBNodeIV;
import org.embergraph.rdf.internal.impl.bnode.SidIV;
import org.embergraph.rdf.internal.impl.bnode.UUIDBNodeIV;
import org.embergraph.rdf.internal.impl.extensions.DateTimeExtension;
import org.embergraph.rdf.internal.impl.extensions.DerivedNumericsExtension;
import org.embergraph.rdf.internal.impl.extensions.XSDStringExtension;
import org.embergraph.rdf.internal.impl.literal.FullyInlineTypedLiteralIV;
import org.embergraph.rdf.internal.impl.literal.PartlyInlineTypedLiteralIV;
import org.embergraph.rdf.internal.impl.literal.UUIDLiteralIV;
import org.embergraph.rdf.internal.impl.literal.XSDBooleanIV;
import org.embergraph.rdf.internal.impl.literal.XSDDecimalIV;
import org.embergraph.rdf.internal.impl.literal.XSDIntegerIV;
import org.embergraph.rdf.internal.impl.literal.XSDNumericIV;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedByteIV;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedIntIV;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedLongIV;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedShortIV;
import org.embergraph.rdf.internal.impl.uri.FullyInlineURIIV;
import org.embergraph.rdf.internal.impl.uri.PartlyInlineURIIV;
import org.embergraph.rdf.internal.impl.uri.URIExtensionIV;
import org.embergraph.rdf.internal.impl.uri.VocabURIByteIV;
import org.embergraph.rdf.internal.impl.uri.VocabURIShortIV;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.spo.SPO;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;

/*
 * Test of encode/decode and especially <em>comparator</em> semantics for mixed {@link IV}s.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractEncodeDecodeMixedIVsTest extends AbstractEncodeDecodeKeysTestCase {

  /** */
  public AbstractEncodeDecodeMixedIVsTest() {}

  /** @param name */
  public AbstractEncodeDecodeMixedIVsTest(String name) {
    super(name);
  }

  /*
   * Flag may be used to enable/disable the inclusion of the {@link IV}s having fully include
   * Unicode data. These are the ones whose proper ordering is most problematic as they need to obey
   * the collation order imposed by the {@link AbstractTripleStore.Options}.
   */
  private static boolean fullyInlineUnicode = true;

  protected List<IV<?, ?>> prepareIVs() throws DatatypeConfigurationException {

    final Random r = new Random();

    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance(getName());

    final URI datatype = new URIImpl("http://www.embergraph.org");
    final URI dt1 = new URIImpl("http://www.embergraph.org/mock-datatype-1");
    final URI dt2 = new URIImpl("http://www.embergraph.org/mock-datatype-2");

    final IV<?, ?> namespaceIV = newTermId(VTE.URI);
    final IV<?, ?> datatypeIV = newTermId(VTE.URI);
    final IV<?, ?> datatypeIV2 = newTermId(VTE.URI);

    final IV<?, ?> colorIV = newTermId(VTE.URI); // ColorsEnumExtension.COLOR;
    final IV<?, ?> xsdStringIV = newTermId(VTE.URI); // XSD.STRING;
    final IV<?, ?> xsdDateTimeIV = newTermId(VTE.URI); // XSD.DATETIME;

    final IDatatypeURIResolver resolver =
        new IDatatypeURIResolver() {
          public EmbergraphURI resolve(final URI uri) {
            final EmbergraphURI buri = vf.createURI(uri.stringValue());
            if (ColorsEnumExtension.COLOR.equals(uri)) {
              buri.setIV(colorIV);
            } else if (XSD.STRING.equals(uri)) {
              buri.setIV(xsdStringIV);
            } else if (XSD.DATETIME.equals(uri)) {
              buri.setIV(xsdDateTimeIV);
            } else if (XSD.DATE.equals(uri)) {
              buri.setIV(newTermId(VTE.URI));
            } else if (XSD.TIME.equals(uri)) {
              buri.setIV(newTermId(VTE.URI));
            } else if (XSD.GDAY.equals(uri)) {
              buri.setIV(newTermId(VTE.URI));
            } else if (XSD.GMONTH.equals(uri)) {
              buri.setIV(newTermId(VTE.URI));
            } else if (XSD.GMONTHDAY.equals(uri)) {
              buri.setIV(newTermId(VTE.URI));
            } else if (XSD.GYEAR.equals(uri)) {
              buri.setIV(newTermId(VTE.URI));
            } else if (XSD.GYEARMONTH.equals(uri)) {
              buri.setIV(newTermId(VTE.URI));
            } else if (XSD.POSITIVE_INTEGER.equals(uri)) {
              buri.setIV(newTermId(VTE.URI));
            } else if (XSD.NEGATIVE_INTEGER.equals(uri)) {
              buri.setIV(newTermId(VTE.URI));
            } else if (XSD.NON_POSITIVE_INTEGER.equals(uri)) {
              buri.setIV(newTermId(VTE.URI));
            } else if (XSD.NON_NEGATIVE_INTEGER.equals(uri)) {
              buri.setIV(newTermId(VTE.URI));
            } else throw new UnsupportedOperationException();
            return buri;
          }
        };

    final List<IV<?, ?>> ivs = new LinkedList<>();
    {

      // Fully inline
      {

        /*
         * BNODEs
         */
        if (fullyInlineUnicode) {
          // blank nodes with Unicode IDs.
          ivs.add(new FullyInlineUnicodeBNodeIV<>("FOO"));
          ivs.add(new FullyInlineUnicodeBNodeIV<>("_bar"));
          ivs.add(new FullyInlineUnicodeBNodeIV<>("bar"));
          ivs.add(new FullyInlineUnicodeBNodeIV<>("baz"));
          ivs.add(new FullyInlineUnicodeBNodeIV<>("12"));
          ivs.add(new FullyInlineUnicodeBNodeIV<>("1298"));
          ivs.add(new FullyInlineUnicodeBNodeIV<>("asassdao"));
          ivs.add(new FullyInlineUnicodeBNodeIV<>("1"));
        }

        // blank nodes with numeric IDs.
        ivs.add(new NumericBNodeIV<>(-1));
        ivs.add(new NumericBNodeIV<>(0));
        ivs.add(new NumericBNodeIV<>(1));
        ivs.add(new NumericBNodeIV<>(-52));
        ivs.add(new NumericBNodeIV<>(52));
        ivs.add(new NumericBNodeIV<>(Integer.MAX_VALUE));
        ivs.add(new NumericBNodeIV<>(Integer.MIN_VALUE));

        // blank nodes with UUID IDs.
        for (int i = 0; i < 100; i++) {

          ivs.add(new UUIDBNodeIV<>(UUID.randomUUID()));
        }

        /*
         * URIs
         */
        ivs.add(new FullyInlineURIIV<>(new URIImpl("http://www.embergraph.org")));
        ivs.add(new FullyInlineURIIV<>(new URIImpl("http://www.embergraph.org/")));
        ivs.add(new FullyInlineURIIV<>(new URIImpl("http://www.embergraph.org/foo")));
        ivs.add(
            new FullyInlineURIIV<>(new URIImpl("http://www.embergraph.org:80/foo")));
        ivs.add(new FullyInlineURIIV<>(new URIImpl("http://www.embergraph.org")));
        if (fullyInlineUnicode) {
          ivs.add(new FullyInlineURIIV<>(RDF.TYPE));
          ivs.add(new FullyInlineURIIV<>(RDF.SUBJECT));
          ivs.add(new FullyInlineURIIV<>(RDF.BAG));
          ivs.add(new FullyInlineURIIV<>(RDF.OBJECT));
          ivs.add(
              new URIExtensionIV<>(
                  new FullyInlineTypedLiteralIV<>("http://www.example.com/"),
                  new VocabURIByteIV<>((byte) 1)));
          ivs.add(
              new URIExtensionIV<>(
                  new FullyInlineTypedLiteralIV<>("http://www.example.com/foo"),
                  new VocabURIByteIV<>((byte) 1)));
          ivs.add(
              new URIExtensionIV<>(
                  new FullyInlineTypedLiteralIV<>("http://www.example.com/foobar"),
                  new VocabURIByteIV<>((byte) 1)));
        }

        /*
         * Literals
         */

        if (fullyInlineUnicode) {
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "foo", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "bar", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "baz", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "123", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "23", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "3", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "", null /* language */, null /* datatype */));
        }

        if (fullyInlineUnicode) {
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "foo", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "bar", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "goo", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "baz", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "foo", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "bar", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "goo", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "baz", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "1", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "1", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "12", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "12", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "2", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "2", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "23", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "23", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "123", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "123", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "3", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<>(
                  "3", "de" /* language */, null /* datatype */));
        }

        if (fullyInlineUnicode) {
          ivs.add(
              new FullyInlineTypedLiteralIV<>("foo", null /* language */, dt1));
          ivs.add(
              new FullyInlineTypedLiteralIV<>("bar", null /* language */, dt1));
          ivs.add(
              new FullyInlineTypedLiteralIV<>("baz", null /* language */, dt1));
          ivs.add(
              new FullyInlineTypedLiteralIV<>("goo", null /* language */, dt1));
          ivs.add(
              new FullyInlineTypedLiteralIV<>("foo", null /* language */, dt2));
          ivs.add(
              new FullyInlineTypedLiteralIV<>("bar", null /* language */, dt2));
          ivs.add(
              new FullyInlineTypedLiteralIV<>("baz", null /* language */, dt2));
          ivs.add(
              new FullyInlineTypedLiteralIV<>("goo", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<>("", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<>("", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<>("1", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<>("1", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<>("12", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<>("12", null /* language */, dt2));
          ivs.add(
              new FullyInlineTypedLiteralIV<>("123", null /* language */, dt2));
          ivs.add(
              new FullyInlineTypedLiteralIV<>("123", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<>("23", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<>("23", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<>("3", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<>("3", null /* language */, dt2));
        }

        ivs.add(
            new FullyInlineTypedLiteralIV<>(
                "foo", null /* language */, XSD.STRING /* datatype */));
        ivs.add(
            new FullyInlineTypedLiteralIV<>(
                "bar", null /* language */, XSD.STRING /* datatype */));
        ivs.add(
            new FullyInlineTypedLiteralIV<>(
                "baz", null /* language */, XSD.STRING /* datatype */));

        ivs.add(new FullyInlineTypedLiteralIV<>(""));
        ivs.add(new FullyInlineTypedLiteralIV<>(" "));
        ivs.add(new FullyInlineTypedLiteralIV<>("1"));
        ivs.add(new FullyInlineTypedLiteralIV<>("12"));
        ivs.add(new FullyInlineTypedLiteralIV<>("123"));

        ivs.add(new FullyInlineTypedLiteralIV<>("", "en", null /*datatype*/));
        ivs.add(new FullyInlineTypedLiteralIV<>(" ", "en", null /*datatype*/));
        ivs.add(new FullyInlineTypedLiteralIV<>("1", "en", null /*datatype*/));
        ivs.add(new FullyInlineTypedLiteralIV<>("12", "fr", null /*datatype*/));
        ivs.add(new FullyInlineTypedLiteralIV<>("123", "de", null /*datatype*/));

        ivs.add(new FullyInlineTypedLiteralIV<>("", null, datatype));
        ivs.add(new FullyInlineTypedLiteralIV<>(" ", null, datatype));
        ivs.add(new FullyInlineTypedLiteralIV<>("1", null, datatype));
        ivs.add(new FullyInlineTypedLiteralIV<>("12", null, datatype));
        ivs.add(new FullyInlineTypedLiteralIV<>("123", null, datatype));

        // xsd:boolean
        ivs.add(new XSDBooleanIV<>(true));
        ivs.add(new XSDBooleanIV<>(false));

        // xsd:byte
        ivs.add(new XSDNumericIV<>(Byte.MIN_VALUE));
        ivs.add(new XSDNumericIV<>((byte) -1));
        ivs.add(new XSDNumericIV<>((byte) 0));
        ivs.add(new XSDNumericIV<>((byte) 1));
        ivs.add(new XSDNumericIV<>(Byte.MAX_VALUE));

        // xsd:short
        ivs.add(new XSDNumericIV<>((short) -1));
        ivs.add(new XSDNumericIV<>((short) 0));
        ivs.add(new XSDNumericIV<>((short) 1));
        ivs.add(new XSDNumericIV<>(Short.MIN_VALUE));
        ivs.add(new XSDNumericIV<>(Short.MAX_VALUE));

        // xsd:int
        ivs.add(new XSDNumericIV<>(1));
        ivs.add(new XSDNumericIV<>(0));
        ivs.add(new XSDNumericIV<>(-1));
        ivs.add(new XSDNumericIV<>(Integer.MAX_VALUE));
        ivs.add(new XSDNumericIV<>(Integer.MIN_VALUE));

        // xsd:long
        ivs.add(new XSDNumericIV<>(1L));
        ivs.add(new XSDNumericIV<>(0L));
        ivs.add(new XSDNumericIV<>(-1L));
        ivs.add(new XSDNumericIV<>(Long.MIN_VALUE));
        ivs.add(new XSDNumericIV<>(Long.MAX_VALUE));

        // xsd:float
        ivs.add(new XSDNumericIV<>(1f));
        ivs.add(new XSDNumericIV<>(-1f));
        ivs.add(new XSDNumericIV<>(+0f));
        ivs.add(new XSDNumericIV<>(Float.MAX_VALUE));
        ivs.add(new XSDNumericIV<>(Float.MIN_VALUE));
        ivs.add(new XSDNumericIV<>(Float.MIN_NORMAL));
        ivs.add(new XSDNumericIV<>(Float.POSITIVE_INFINITY));
        ivs.add(new XSDNumericIV<>(Float.NEGATIVE_INFINITY));
        ivs.add(new XSDNumericIV<>(Float.NaN));

        // xsd:double
        ivs.add(new XSDNumericIV<>(1d));
        ivs.add(new XSDNumericIV<>(-1d));
        ivs.add(new XSDNumericIV<>(+0d));
        ivs.add(new XSDNumericIV<>(Double.MAX_VALUE));
        ivs.add(new XSDNumericIV<>(Double.MIN_VALUE));
        ivs.add(new XSDNumericIV<>(Double.MIN_NORMAL));
        ivs.add(new XSDNumericIV<>(Double.POSITIVE_INFINITY));
        ivs.add(new XSDNumericIV<>(Double.NEGATIVE_INFINITY));
        ivs.add(new XSDNumericIV<>(Double.NaN));

        // uuid (not an official xsd type, but one we handle natively).
        for (int i = 0; i < 100; i++) {
          ivs.add(new UUIDLiteralIV<>(UUID.randomUUID()));
        }

        // xsd:unsignedByte
        ivs.add(new XSDUnsignedByteIV<>(Byte.MIN_VALUE));
        ivs.add(new XSDUnsignedByteIV<>((byte) -1));
        ivs.add(new XSDUnsignedByteIV<>((byte) 0));
        ivs.add(new XSDUnsignedByteIV<>((byte) 1));
        ivs.add(new XSDUnsignedByteIV<>(Byte.MAX_VALUE));

        // xsd:unsignedShort
        ivs.add(new XSDUnsignedShortIV<>(Short.MIN_VALUE));
        ivs.add(new XSDUnsignedShortIV<>((short) -1));
        ivs.add(new XSDUnsignedShortIV<>((short) 0));
        ivs.add(new XSDUnsignedShortIV<>((short) 1));
        ivs.add(new XSDUnsignedShortIV<>(Short.MAX_VALUE));

        // xsd:unsignedInt
        ivs.add(new XSDUnsignedIntIV<>(Integer.MIN_VALUE));
        ivs.add(new XSDUnsignedIntIV<>(-1));
        ivs.add(new XSDUnsignedIntIV<>(0));
        ivs.add(new XSDUnsignedIntIV<>(1));
        ivs.add(new XSDUnsignedIntIV<>(Integer.MAX_VALUE));

        // xsd:unsignedLong
        ivs.add(new XSDUnsignedLongIV<>(Long.MIN_VALUE));
        ivs.add(new XSDUnsignedLongIV<>(-1L));
        ivs.add(new XSDUnsignedLongIV<>(0L));
        ivs.add(new XSDUnsignedLongIV<>(1L));
        ivs.add(new XSDUnsignedLongIV<>(Long.MAX_VALUE));

        // xsd:integer
        ivs.add(new XSDIntegerIV<>(BigInteger.valueOf(-1L)));
        ivs.add(new XSDIntegerIV<>(BigInteger.valueOf(0L)));
        ivs.add(new XSDIntegerIV<>(BigInteger.valueOf(1L)));
        ivs.add(new XSDIntegerIV<>(BigInteger.valueOf(Long.MAX_VALUE)));
        ivs.add(new XSDIntegerIV<>(BigInteger.valueOf(Long.MIN_VALUE)));

        // xsd:decimal
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(1.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(2.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(0.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(1.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(-1.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(0.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(-2.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(-1.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(10.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(11.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(258.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(259.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(3.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(259.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(383.01)));
        ivs.add(new XSDDecimalIV<>(BigDecimal.valueOf(383.02)));
        ivs.add(new XSDDecimalIV<>(new BigDecimal("1.5")));
        ivs.add(new XSDDecimalIV<>(new BigDecimal("1.51")));
        ivs.add(new XSDDecimalIV<>(new BigDecimal("-1.5")));
        ivs.add(new XSDDecimalIV<>(new BigDecimal("-1.51")));

        ivs.add(new XSDIntegerIV<>(BigInteger.valueOf(-1L)));
        ivs.add(new XSDIntegerIV<>(BigInteger.valueOf(0L)));
        ivs.add(new XSDIntegerIV<>(BigInteger.valueOf(1L)));
        ivs.add(new XSDIntegerIV<>(BigInteger.valueOf(Long.MAX_VALUE)));
        ivs.add(new XSDIntegerIV<>(BigInteger.valueOf(Long.MIN_VALUE)));
        ivs.add(new XSDIntegerIV<>(new BigInteger("15")));
        ivs.add(new XSDIntegerIV<>(new BigInteger("151")));
        ivs.add(new XSDIntegerIV<>(new BigInteger("-15")));
        ivs.add(new XSDIntegerIV<>(new BigInteger("-151")));

        // byte vocabulary IVs.
        ivs.add(new VocabURIByteIV<>(Byte.MIN_VALUE));
        ivs.add(new VocabURIByteIV<>((byte) -1));
        ivs.add(new VocabURIByteIV<>((byte) 0));
        ivs.add(new VocabURIByteIV<>((byte) 1));
        ivs.add(new VocabURIByteIV<>(Byte.MAX_VALUE));

        // short vocabulary IVs.
        ivs.add(new VocabURIShortIV<>(Short.MIN_VALUE));
        ivs.add(new VocabURIShortIV<>((short) -1));
        ivs.add(new VocabURIShortIV<>((short) 0));
        ivs.add(new VocabURIShortIV<>((short) 1));
        ivs.add(new VocabURIShortIV<>(Short.MAX_VALUE));

        // SIDs
        {
          final IV<?, ?> s1 = newTermId(VTE.URI);
          final IV<?, ?> s2 = newTermId(VTE.URI);
          final IV<?, ?> p1 = newTermId(VTE.URI);
          final IV<?, ?> p2 = newTermId(VTE.URI);
          final IV<?, ?> o1 = newTermId(VTE.URI);
          final IV<?, ?> o2 = newTermId(VTE.BNODE);
          final IV<?, ?> o3 = newTermId(VTE.LITERAL);

          final SPO spo1 = new SPO(s1, p1, o1, StatementEnum.Explicit);
          final SPO spo2 = new SPO(s1, p1, o2, StatementEnum.Explicit);
          final SPO spo3 = new SPO(s1, p1, o3, StatementEnum.Explicit);
          final SPO spo4 = new SPO(s1, p2, o1, StatementEnum.Explicit);
          final SPO spo5 = new SPO(s1, p2, o2, StatementEnum.Explicit);
          final SPO spo6 = new SPO(s1, p2, o3, StatementEnum.Explicit);
          final SPO spo7 = new SPO(s2, p1, o1, StatementEnum.Explicit);
          final SPO spo8 = new SPO(s2, p1, o2, StatementEnum.Explicit);
          final SPO spo9 = new SPO(s2, p1, o3, StatementEnum.Explicit);
          final SPO spo10 = new SPO(s2, p2, o1, StatementEnum.Explicit);
          final SPO spo11 = new SPO(s2, p2, o2, StatementEnum.Explicit);
          final SPO spo12 = new SPO(s2, p2, o3, StatementEnum.Explicit);
          //                    spo1.setStatementIdentifier(true);
          //                    spo2.setStatementIdentifier(true);
          //                    spo3.setStatementIdentifier(true);
          //                    spo6.setStatementIdentifier(true);
          final SPO spo13 = new SPO(spo1.getStatementIdentifier(), p1, o1, StatementEnum.Explicit);
          final SPO spo14 = new SPO(spo2.getStatementIdentifier(), p2, o2, StatementEnum.Explicit);
          final SPO spo15 = new SPO(s1, p1, spo3.getStatementIdentifier(), StatementEnum.Explicit);
          //                    spo15.setStatementIdentifier(true);
          final SPO spo16 = new SPO(s1, p1, spo6.getStatementIdentifier(), StatementEnum.Explicit);
          final SPO spo17 =
              new SPO(
                  spo1.getStatementIdentifier(),
                  p1,
                  spo15.getStatementIdentifier(),
                  StatementEnum.Explicit);

          final IV<?, ?>[] e = {
              new SidIV<>(spo1),
              new SidIV<>(spo2),
              new SidIV<>(spo3),
              new SidIV<>(spo4),
              new SidIV<>(spo5),
              new SidIV<>(spo6),
              new SidIV<>(spo7),
              new SidIV<>(spo8),
              new SidIV<>(spo9),
              new SidIV<>(spo10),
              new SidIV<>(spo11),
              new SidIV<>(spo12),
              new SidIV<>(spo13),
              new SidIV<>(spo14),
              new SidIV<>(spo15),
              new SidIV<>(spo16),
              new SidIV<>(spo17),
          };
          ivs.addAll(Arrays.asList(e));
        }
      }

      // Not inline
      {

        /*
         * TermIds
         */
        for (int i = 0; i < 100; i++) {

          for (VTE vte : VTE.values()) {

            //                        // 64 bit random term identifier.
            //                        final long termId = r.nextLong();
            //
            //                        final TermId<?> v = new TermId<EmbergraphValue>(vte,
            //                                termId);
            //
            //                        ivs.add(v);

            ivs.add(newTermId(vte));
          }
        }

        /*
         * BLOBS
         */
        {
          for (int i = 0; i < 100; i++) {

            for (VTE vte : VTE.values()) {

              final int hashCode = r.nextInt();

              final int counter = Short.MAX_VALUE - r.nextInt(2 ^ 16);

              @SuppressWarnings("rawtypes")
              final BlobIV<?> v = new BlobIV(vte, hashCode, (short) counter);

              ivs.add(v);
            }
          }
        }
      } // Not inline.

      /*
       * Partly inline
       */
      {

        // URIs
        if (fullyInlineUnicode) {
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("bar"), namespaceIV));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("baz"), namespaceIV));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("123"), namespaceIV));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("23"), namespaceIV));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("3"), namespaceIV));
        }

        // LITERALs
        ivs.add(
            new PartlyInlineTypedLiteralIV<>(
                new FullyInlineTypedLiteralIV<>(""), datatypeIV));

        ivs.add(
            new PartlyInlineTypedLiteralIV<>(
                new FullyInlineTypedLiteralIV<>("abc"), datatypeIV));

        ivs.add(
            new PartlyInlineTypedLiteralIV<>(
                new FullyInlineTypedLiteralIV<>(" "), datatypeIV));

        ivs.add(
            new PartlyInlineTypedLiteralIV<>(
                new FullyInlineTypedLiteralIV<>("1"), datatypeIV));

        ivs.add(
            new PartlyInlineTypedLiteralIV<>(
                new FullyInlineTypedLiteralIV<>("12"), datatypeIV));

        if (fullyInlineUnicode) {
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>(""), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>(" "), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("1"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("1"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("12"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("12"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("123"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("123"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("23"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("23"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("3"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("3"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("bar"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("baz"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("bar"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("baz"), datatypeIV2));
        }

        if (fullyInlineUnicode) {
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("bar"), // localName
                  new VocabURIShortIV<>((short) 1) // namespace
              ));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("baz"), // localName
                  new VocabURIShortIV<>((short) 1) // namespace
              ));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("bar"), // localName
                  new VocabURIShortIV<>((short) 2) // namespace
              ));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("baz"), // localName
                  new VocabURIShortIV<>((short) 2) // namespace
              ));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("123"), // localName
                  new VocabURIShortIV<>((short) 2) // namespace
              ));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("123"), // localName
                  new VocabURIShortIV<>((short) 2) // namespace
              ));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("23"), // localName
                  new VocabURIShortIV<>((short) 2) // namespace
              ));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("23"), // localName
                  new VocabURIShortIV<>((short) 2) // namespace
              ));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("3"), // localName
                  new VocabURIShortIV<>((short) 2) // namespace
              ));
          ivs.add(
              new PartlyInlineURIIV<>(
                  new FullyInlineTypedLiteralIV<>("3"), // localName
                  new VocabURIShortIV<>((short) 2) // namespace
              ));
        }

        if (fullyInlineUnicode) {

          final IV<?, ?> datatypeIVa = new VocabURIShortIV<>((short) 1);
          final IV<?, ?> datatypeIVa2 = new VocabURIShortIV<>((short) 2);
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("bar"), datatypeIVa));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("bar"), datatypeIVa2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("baz"), datatypeIVa));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("baz"), datatypeIVa2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("123"), datatypeIVa));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("123"), datatypeIVa2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("23"), datatypeIVa));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("23"), datatypeIVa2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("3"), datatypeIVa));
          ivs.add(
              new PartlyInlineTypedLiteralIV<>(
                  new FullyInlineTypedLiteralIV<>("3"), datatypeIVa2));
        }
      } // partly inline.

      /*
       * Extension IVs
       */
      {

        // xsd:dateTime extension
        {
          final DatatypeFactory df = DatatypeFactory.newInstance();

          final DateTimeExtension<EmbergraphValue> ext =
              new DateTimeExtension<>(resolver, TimeZone.getDefault());

          final EmbergraphLiteral[] dt = {
            vf.createLiteral(df.newXMLGregorianCalendar("2001-10-26T21:32:52")),
            vf.createLiteral(df.newXMLGregorianCalendar("2001-10-26T21:32:52+02:00")),
            vf.createLiteral(df.newXMLGregorianCalendar("2001-10-26T19:32:52Z")),
            vf.createLiteral(df.newXMLGregorianCalendar("2001-10-26T19:32:52+00:00")),
            vf.createLiteral(df.newXMLGregorianCalendar("-2001-10-26T21:32:52")),
            vf.createLiteral(df.newXMLGregorianCalendar("2001-10-26T21:32:52.12679")),
            vf.createLiteral(df.newXMLGregorianCalendar("1901-10-26T21:32:52")),
          };

          for (int i = 0; i < dt.length; i++) {

            ivs.add(ext.createIV(dt[i]));
          }
        }

        // derived numerics extension
        {
          final DatatypeFactory df = DatatypeFactory.newInstance();

          final DerivedNumericsExtension<EmbergraphValue> ext =
              new DerivedNumericsExtension<>(resolver);

          final EmbergraphLiteral[] dt = {
            vf.createLiteral("1", XSD.POSITIVE_INTEGER),
            vf.createLiteral("-1", XSD.NEGATIVE_INTEGER),
            vf.createLiteral("-1", XSD.NON_POSITIVE_INTEGER),
            vf.createLiteral("1", XSD.NON_NEGATIVE_INTEGER),
            vf.createLiteral("0", XSD.NON_POSITIVE_INTEGER),
            vf.createLiteral("0", XSD.NON_NEGATIVE_INTEGER),
          };

          for (int i = 0; i < dt.length; i++) {

            ivs.add(ext.createIV(dt[i]));
          }
        }

        // xsd:string extension IVs
        if (fullyInlineUnicode) {

          final int maxInlineStringLength = 128;

          final XSDStringExtension<EmbergraphValue> ext =
              new XSDStringExtension<>(resolver, maxInlineStringLength);

          final IV<?, ?>[] e = {
            ext.createIV(new LiteralImpl("", XSD.STRING)),
            ext.createIV(new LiteralImpl(" ", XSD.STRING)),
            ext.createIV(new LiteralImpl("  ", XSD.STRING)),
            ext.createIV(new LiteralImpl("1", XSD.STRING)),
            ext.createIV(new LiteralImpl("12", XSD.STRING)),
            ext.createIV(new LiteralImpl("123", XSD.STRING)),
            ext.createIV(new LiteralImpl("234", XSD.STRING)),
            ext.createIV(new LiteralImpl("34", XSD.STRING)),
            ext.createIV(new LiteralImpl("4", XSD.STRING)),
            ext.createIV(new LiteralImpl("a", XSD.STRING)),
            ext.createIV(new LiteralImpl("ab", XSD.STRING)),
            ext.createIV(new LiteralImpl("abc", XSD.STRING)),
          };
          ivs.addAll(Arrays.asList(e));
        }

        // "color" extension IV.
        if (true) {

          final ColorsEnumExtension<EmbergraphValue> ext =
              new ColorsEnumExtension<>(resolver);

          for (Color c : ColorsEnumExtension.Color.values()) {

            ivs.add(ext.createIV(new LiteralImpl(c.name(), ColorsEnumExtension.COLOR)));
          }
        }
      }
    }

    return ivs;
  }
}
