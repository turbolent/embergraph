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

    final List<IV<?, ?>> ivs = new LinkedList<IV<?, ?>>();
    {

      // Fully inline
      {

        /*
         * BNODEs
         */
        if (fullyInlineUnicode) {
          // blank nodes with Unicode IDs.
          ivs.add(new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("FOO"));
          ivs.add(new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("_bar"));
          ivs.add(new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("bar"));
          ivs.add(new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("baz"));
          ivs.add(new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("12"));
          ivs.add(new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("1298"));
          ivs.add(new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("asassdao"));
          ivs.add(new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("1"));
        }

        // blank nodes with numeric IDs.
        ivs.add(new NumericBNodeIV<EmbergraphBNode>(-1));
        ivs.add(new NumericBNodeIV<EmbergraphBNode>(0));
        ivs.add(new NumericBNodeIV<EmbergraphBNode>(1));
        ivs.add(new NumericBNodeIV<EmbergraphBNode>(-52));
        ivs.add(new NumericBNodeIV<EmbergraphBNode>(52));
        ivs.add(new NumericBNodeIV<EmbergraphBNode>(Integer.MAX_VALUE));
        ivs.add(new NumericBNodeIV<EmbergraphBNode>(Integer.MIN_VALUE));

        // blank nodes with UUID IDs.
        for (int i = 0; i < 100; i++) {

          ivs.add(new UUIDBNodeIV<EmbergraphBNode>(UUID.randomUUID()));
        }

        /*
         * URIs
         */
        ivs.add(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org")));
        ivs.add(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org/")));
        ivs.add(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org/foo")));
        ivs.add(
            new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org:80/foo")));
        ivs.add(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org")));
        if (fullyInlineUnicode) {
          ivs.add(new FullyInlineURIIV<EmbergraphURI>(RDF.TYPE));
          ivs.add(new FullyInlineURIIV<EmbergraphURI>(RDF.SUBJECT));
          ivs.add(new FullyInlineURIIV<EmbergraphURI>(RDF.BAG));
          ivs.add(new FullyInlineURIIV<EmbergraphURI>(RDF.OBJECT));
          ivs.add(
              new URIExtensionIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("http://www.example.com/"),
                  new VocabURIByteIV<EmbergraphURI>((byte) 1)));
          ivs.add(
              new URIExtensionIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("http://www.example.com/foo"),
                  new VocabURIByteIV<EmbergraphURI>((byte) 1)));
          ivs.add(
              new URIExtensionIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("http://www.example.com/foobar"),
                  new VocabURIByteIV<EmbergraphURI>((byte) 1)));
        }

        /*
         * Literals
         */

        if (fullyInlineUnicode) {
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "foo", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "bar", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "baz", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "123", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "23", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "3", null /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "", null /* language */, null /* datatype */));
        }

        if (fullyInlineUnicode) {
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "foo", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "bar", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "goo", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "baz", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "foo", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "bar", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "goo", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "baz", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "1", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "1", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "12", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "12", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "2", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "2", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "23", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "23", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "123", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "123", "de" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "3", "en" /* language */, null /* datatype */));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                  "3", "de" /* language */, null /* datatype */));
        }

        if (fullyInlineUnicode) {
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", null /* language */, dt1));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", null /* language */, dt1));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", null /* language */, dt1));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>("goo", null /* language */, dt1));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", null /* language */, dt2));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", null /* language */, dt2));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", null /* language */, dt2));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>("goo", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12", null /* language */, dt2));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123", null /* language */, dt2));
          ivs.add(
              new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3", null /* language */, dt2));
          ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3", null /* language */, dt2));
        }

        ivs.add(
            new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                "foo", null /* language */, XSD.STRING /* datatype */));
        ivs.add(
            new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                "bar", null /* language */, XSD.STRING /* datatype */));
        ivs.add(
            new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                "baz", null /* language */, XSD.STRING /* datatype */));

        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>(""));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>(" "));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1"));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12"));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"));

        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("", "en", null /*datatype*/));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>(" ", "en", null /*datatype*/));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1", "en", null /*datatype*/));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12", "fr", null /*datatype*/));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123", "de", null /*datatype*/));

        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("", null, datatype));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>(" ", null, datatype));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1", null, datatype));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12", null, datatype));
        ivs.add(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123", null, datatype));

        // xsd:boolean
        ivs.add(new XSDBooleanIV<EmbergraphLiteral>(true));
        ivs.add(new XSDBooleanIV<EmbergraphLiteral>(false));

        // xsd:byte
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Byte.MIN_VALUE));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>((byte) -1));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>((byte) 0));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>((byte) 1));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Byte.MAX_VALUE));

        // xsd:short
        ivs.add(new XSDNumericIV<EmbergraphLiteral>((short) -1));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>((short) 0));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>((short) 1));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Short.MIN_VALUE));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Short.MAX_VALUE));

        // xsd:int
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(1));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(0));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(-1));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Integer.MAX_VALUE));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Integer.MIN_VALUE));

        // xsd:long
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(1L));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(0L));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(-1L));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Long.MIN_VALUE));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Long.MAX_VALUE));

        // xsd:float
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(1f));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(-1f));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(+0f));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Float.MAX_VALUE));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Float.MIN_VALUE));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Float.MIN_NORMAL));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Float.POSITIVE_INFINITY));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Float.NEGATIVE_INFINITY));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Float.NaN));

        // xsd:double
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(1d));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(-1d));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(+0d));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Double.MAX_VALUE));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Double.MIN_VALUE));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Double.MIN_NORMAL));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Double.POSITIVE_INFINITY));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Double.NEGATIVE_INFINITY));
        ivs.add(new XSDNumericIV<EmbergraphLiteral>(Double.NaN));

        // uuid (not an official xsd type, but one we handle natively).
        for (int i = 0; i < 100; i++) {
          ivs.add(new UUIDLiteralIV<EmbergraphLiteral>(UUID.randomUUID()));
        }

        // xsd:unsignedByte
        ivs.add(new XSDUnsignedByteIV<EmbergraphLiteral>(Byte.MIN_VALUE));
        ivs.add(new XSDUnsignedByteIV<EmbergraphLiteral>((byte) -1));
        ivs.add(new XSDUnsignedByteIV<EmbergraphLiteral>((byte) 0));
        ivs.add(new XSDUnsignedByteIV<EmbergraphLiteral>((byte) 1));
        ivs.add(new XSDUnsignedByteIV<EmbergraphLiteral>(Byte.MAX_VALUE));

        // xsd:unsignedShort
        ivs.add(new XSDUnsignedShortIV<EmbergraphLiteral>(Short.MIN_VALUE));
        ivs.add(new XSDUnsignedShortIV<EmbergraphLiteral>((short) -1));
        ivs.add(new XSDUnsignedShortIV<EmbergraphLiteral>((short) 0));
        ivs.add(new XSDUnsignedShortIV<EmbergraphLiteral>((short) 1));
        ivs.add(new XSDUnsignedShortIV<EmbergraphLiteral>(Short.MAX_VALUE));

        // xsd:unsignedInt
        ivs.add(new XSDUnsignedIntIV<EmbergraphLiteral>(Integer.MIN_VALUE));
        ivs.add(new XSDUnsignedIntIV<EmbergraphLiteral>(-1));
        ivs.add(new XSDUnsignedIntIV<EmbergraphLiteral>(0));
        ivs.add(new XSDUnsignedIntIV<EmbergraphLiteral>(1));
        ivs.add(new XSDUnsignedIntIV<EmbergraphLiteral>(Integer.MAX_VALUE));

        // xsd:unsignedLong
        ivs.add(new XSDUnsignedLongIV<EmbergraphLiteral>(Long.MIN_VALUE));
        ivs.add(new XSDUnsignedLongIV<EmbergraphLiteral>(-1L));
        ivs.add(new XSDUnsignedLongIV<EmbergraphLiteral>(0L));
        ivs.add(new XSDUnsignedLongIV<EmbergraphLiteral>(1L));
        ivs.add(new XSDUnsignedLongIV<EmbergraphLiteral>(Long.MAX_VALUE));

        // xsd:integer
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(-1L)));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(0L)));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(1L)));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(Long.MAX_VALUE)));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(Long.MIN_VALUE)));

        // xsd:decimal
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(1.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(2.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(0.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(1.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(-1.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(0.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(-2.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(-1.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(10.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(11.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(258.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(259.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(3.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(259.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(383.01)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(383.02)));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(new BigDecimal("1.5")));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(new BigDecimal("1.51")));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(new BigDecimal("-1.5")));
        ivs.add(new XSDDecimalIV<EmbergraphLiteral>(new BigDecimal("-1.51")));

        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(-1L)));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(0L)));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(1L)));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(Long.MAX_VALUE)));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(Long.MIN_VALUE)));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(new BigInteger("15")));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(new BigInteger("151")));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(new BigInteger("-15")));
        ivs.add(new XSDIntegerIV<EmbergraphLiteral>(new BigInteger("-151")));

        // byte vocabulary IVs.
        ivs.add(new VocabURIByteIV<EmbergraphURI>(Byte.MIN_VALUE));
        ivs.add(new VocabURIByteIV<EmbergraphURI>((byte) -1));
        ivs.add(new VocabURIByteIV<EmbergraphURI>((byte) 0));
        ivs.add(new VocabURIByteIV<EmbergraphURI>((byte) 1));
        ivs.add(new VocabURIByteIV<EmbergraphURI>(Byte.MAX_VALUE));

        // short vocabulary IVs.
        ivs.add(new VocabURIShortIV<EmbergraphURI>(Short.MIN_VALUE));
        ivs.add(new VocabURIShortIV<EmbergraphURI>((short) -1));
        ivs.add(new VocabURIShortIV<EmbergraphURI>((short) 0));
        ivs.add(new VocabURIShortIV<EmbergraphURI>((short) 1));
        ivs.add(new VocabURIShortIV<EmbergraphURI>(Short.MAX_VALUE));

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
            new SidIV<EmbergraphBNode>(spo1),
            new SidIV<EmbergraphBNode>(spo2),
            new SidIV<EmbergraphBNode>(spo3),
            new SidIV<EmbergraphBNode>(spo4),
            new SidIV<EmbergraphBNode>(spo5),
            new SidIV<EmbergraphBNode>(spo6),
            new SidIV<EmbergraphBNode>(spo7),
            new SidIV<EmbergraphBNode>(spo8),
            new SidIV<EmbergraphBNode>(spo9),
            new SidIV<EmbergraphBNode>(spo10),
            new SidIV<EmbergraphBNode>(spo11),
            new SidIV<EmbergraphBNode>(spo12),
            new SidIV<EmbergraphBNode>(spo13),
            new SidIV<EmbergraphBNode>(spo14),
            new SidIV<EmbergraphBNode>(spo15),
            new SidIV<EmbergraphBNode>(spo16),
            new SidIV<EmbergraphBNode>(spo17),
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
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), namespaceIV));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), namespaceIV));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), namespaceIV));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), namespaceIV));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), namespaceIV));
        }

        // LITERALs
        ivs.add(
            new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>(""), datatypeIV));

        ivs.add(
            new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("abc"), datatypeIV));

        ivs.add(
            new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>(" "), datatypeIV));

        ivs.add(
            new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1"), datatypeIV));

        ivs.add(
            new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12"), datatypeIV));

        if (fullyInlineUnicode) {
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>(""), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>(" "), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), datatypeIV));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), datatypeIV2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), datatypeIV2));
        }

        if (fullyInlineUnicode) {
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), // localName
                  new VocabURIShortIV<EmbergraphURI>((short) 1) // namespace
                  ));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), // localName
                  new VocabURIShortIV<EmbergraphURI>((short) 1) // namespace
                  ));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), // localName
                  new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                  ));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), // localName
                  new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                  ));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), // localName
                  new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                  ));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), // localName
                  new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                  ));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), // localName
                  new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                  ));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), // localName
                  new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                  ));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), // localName
                  new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                  ));
          ivs.add(
              new PartlyInlineURIIV<EmbergraphURI>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), // localName
                  new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                  ));
        }

        if (fullyInlineUnicode) {

          final IV<?, ?> datatypeIVa = new VocabURIShortIV<EmbergraphURI>((short) 1);
          final IV<?, ?> datatypeIVa2 = new VocabURIShortIV<EmbergraphURI>((short) 2);
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), datatypeIVa));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), datatypeIVa2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), datatypeIVa));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), datatypeIVa2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), datatypeIVa));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), datatypeIVa2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), datatypeIVa));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), datatypeIVa2));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), datatypeIVa));
          ivs.add(
              new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                  new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), datatypeIVa2));
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
              new DateTimeExtension<EmbergraphValue>(resolver, TimeZone.getDefault());

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
              new DerivedNumericsExtension<EmbergraphValue>(resolver);

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
              new XSDStringExtension<EmbergraphValue>(resolver, maxInlineStringLength);

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
              new ColorsEnumExtension<EmbergraphValue>(resolver);

          for (Color c : ColorsEnumExtension.Color.values()) {

            ivs.add(ext.createIV(new LiteralImpl(c.name(), ColorsEnumExtension.COLOR)));
          }
        }
      }
    }

    return ivs;
  }
}
