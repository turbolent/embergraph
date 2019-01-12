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
 * Created on Apr 19, 2010
 */

package org.embergraph.rdf.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.embergraph.rdf.internal.ColorsEnumExtension.Color;
import org.embergraph.rdf.internal.impl.AbstractIV;
import org.embergraph.rdf.internal.impl.BlobIV;
import org.embergraph.rdf.internal.impl.bnode.NumericBNodeIV;
import org.embergraph.rdf.internal.impl.bnode.SidIV;
import org.embergraph.rdf.internal.impl.bnode.UUIDBNodeIV;
import org.embergraph.rdf.internal.impl.extensions.DateTimeExtension;
import org.embergraph.rdf.internal.impl.extensions.DerivedNumericsExtension;
import org.embergraph.rdf.internal.impl.extensions.GeoSpatialLiteralExtension;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.internal.impl.literal.UUIDLiteralIV;
import org.embergraph.rdf.internal.impl.literal.XSDBooleanIV;
import org.embergraph.rdf.internal.impl.literal.XSDNumericIV;
import org.embergraph.rdf.internal.impl.uri.VocabURIByteIV;
import org.embergraph.rdf.internal.impl.uri.VocabURIShortIV;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.spo.SPO;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.service.geospatial.GeoSpatialConfig;
import org.embergraph.service.geospatial.GeoSpatialDatatypeConfiguration;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;

/**
 * Unit tests for encoding and decoding compound keys (such as are used by the statement indices) in
 * which some of the key components are inline values having variable component lengths while others
 * are term identifiers.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @author <a href="mailto:ms@metaphacts.com">M</a>
 * @version $Id: TestEncodeDecodeKeys.java 2756 2010-05-03 22:26:18Z thompsonbry$
 *     <p>FIXME Test heterogenous sets of {@link IV}s, ideally randomly generated for each type of
 *     VTE and DTE.
 */
public class TestEncodeDecodeKeys extends AbstractEncodeDecodeKeysTestCase {

  final String GEO_SPATIAL_DATATYPE = "http://www.embergraph.org/rdf/geospatial#geoSpatialLiteral";

  final URI GEO_SPATIAL_DATATYPE_URI = new URIImpl(GEO_SPATIAL_DATATYPE);

  final String GEO_SPATIAL_DATATYPE_CONFIG =
      "{\"config\": "
          + "{ \"uri\": \""
          + GEO_SPATIAL_DATATYPE
          + "\", "
          + "\"fields\": [ "
          + "{ \"valueType\": \"DOUBLE\", \"multiplier\": \"100000\", \"serviceMapping\": \"LATITUDE\" }, "
          + "{ \"valueType\": \"DOUBLE\", \"multiplier\": \"100000\", \"serviceMapping\": \"LONGITUDE\" }, "
          + "{ \"valueType\": \"LONG\", \"serviceMapping\" : \"TIME\"  } "
          + "]}}";

  public TestEncodeDecodeKeys() {
    super();
  }

  public TestEncodeDecodeKeys(String name) {
    super(name);
  }

  public void test_InlineValue() {

    for (VTE vte : VTE.values()) {

      for (DTE dte : DTE.values()) {

        @SuppressWarnings("rawtypes")
        final IV<?, ?> v =
            new AbstractIV(vte, true /* inline */, false /* extension */, dte) {

              private static final long serialVersionUID = 1L;

              @Override
              public boolean equals(Object o) {
                return this == o;
              }

              @Override
              public int byteLength() {
                throw new UnsupportedOperationException();
              }

              @Override
              public int hashCode() {
                return 0;
              }

              @Override
              public IV<?, ?> clone(boolean clearCache) {
                throw new UnsupportedOperationException();
              }

              @Override
              public int _compareTo(IV o) {
                throw new UnsupportedOperationException();
              }

              @Override
              public EmbergraphValue asValue(final LexiconRelation lex)
                  throws UnsupportedOperationException {
                return null;
              }

              @Override
              public Object getInlineValue() throws UnsupportedOperationException {
                return null;
              }

              @Override
              public boolean isInline() {
                return true;
              }

              @Override
              public boolean needsMaterialization() {
                return false;
              }

              @Override
              public String stringValue() {
                throw new UnsupportedOperationException();
              }
            };

        assertTrue(v.isInline());

        //                if (termId == 0L) {
        //                    assertTrue(v.toString(), v.isNull());
        //                } else {
        //                    assertFalse(v.toString(), v.isNull());
        //                }
        //
        //                assertEquals(termId, v.getTermId());

        // should not throw an exception.
        v.getInlineValue();

        assertEquals("flags=" + v.flags(), vte, v.getVTE());

        assertEquals(dte, v.getDTE());

        switch (vte) {
          case URI:
            assertTrue(v.isURI());
            assertFalse(v.isBNode());
            assertFalse(v.isLiteral());
            assertFalse(v.isStatement());
            break;
          case BNODE:
            assertFalse(v.isURI());
            assertTrue(v.isBNode());
            assertFalse(v.isLiteral());
            assertFalse(v.isStatement());
            break;
          case LITERAL:
            assertFalse(v.isURI());
            assertFalse(v.isBNode());
            assertTrue(v.isLiteral());
            assertFalse(v.isStatement());
            break;
          case STATEMENT:
            assertFalse(v.isURI());
            assertFalse(v.isBNode());
            assertFalse(v.isLiteral());
            assertTrue(v.isStatement());
            break;
          default:
            fail("vte=" + vte);
        }
      }
    }
  }

  /** Unit test for encoding and decoding a statement formed from {@link BlobIV}s. */
  public void test_encodeDecode_allTermIds() {

    final IV<?, ?>[] e = {
      newTermId(VTE.URI), newTermId(VTE.URI), newTermId(VTE.URI), newTermId(VTE.URI)
    };

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /** Unit test where the RDF Object position is an xsd:boolean. */
  public void test_encodeDecode_XSDBoolean() {

    final IV<?, ?>[] e = {
      new XSDBooleanIV<EmbergraphLiteral>(true), new XSDBooleanIV<EmbergraphLiteral>(false),
    };

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /** Unit test for {@link XSDNumericIV}. */
  public void test_encodeDecode_XSDByte() {

    final IV<?, ?>[] e = {
      new XSDNumericIV<EmbergraphLiteral>(Byte.MIN_VALUE),
      new XSDNumericIV<EmbergraphLiteral>((byte) -1),
      new XSDNumericIV<EmbergraphLiteral>((byte) 0),
      new XSDNumericIV<EmbergraphLiteral>((byte) 1),
      new XSDNumericIV<EmbergraphLiteral>(Byte.MAX_VALUE),
    };

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /** Unit test for {@link XSDNumericIV}. */
  public void test_encodeDecode_XSDShort() {

    final IV<?, ?>[] e = {
      new XSDNumericIV<EmbergraphLiteral>((short) -1),
      new XSDNumericIV<EmbergraphLiteral>((short) 0),
      new XSDNumericIV<EmbergraphLiteral>((short) 1),
      new XSDNumericIV<EmbergraphLiteral>(Short.MIN_VALUE),
      new XSDNumericIV<EmbergraphLiteral>(Short.MAX_VALUE),
    };

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /** Unit test for {@link XSDNumericIV}. */
  public void test_encodeDecode_XSDInt() {

    final IV<?, ?>[] e = {
      new XSDNumericIV<EmbergraphLiteral>(1),
      new XSDNumericIV<EmbergraphLiteral>(0),
      new XSDNumericIV<EmbergraphLiteral>(-1),
      new XSDNumericIV<EmbergraphLiteral>(Integer.MAX_VALUE),
      new XSDNumericIV<EmbergraphLiteral>(Integer.MIN_VALUE),
    };

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /** Unit test for {@link XSDNumericIV}. */
  public void test_encodeDecode_XSDLong() {

    final IV<?, ?>[] e = {
      new XSDNumericIV<EmbergraphLiteral>(1L),
      new XSDNumericIV<EmbergraphLiteral>(0L),
      new XSDNumericIV<EmbergraphLiteral>(-1L),
      new XSDNumericIV<EmbergraphLiteral>(Long.MIN_VALUE),
      new XSDNumericIV<EmbergraphLiteral>(Long.MAX_VALUE),
    };

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /** Unit test for {@link XSDNumericIV}. */
  public void test_encodeDecode_XSDFloat() {

    /*
     * Note: -0f and +0f are converted to the same point in the value space.
     */
    //        new XSDNumericIV<EmbergraphLiteral>(-0f);

    final IV<?, ?>[] e = {
      new XSDNumericIV<EmbergraphLiteral>(1f),
      new XSDNumericIV<EmbergraphLiteral>(-1f),
      new XSDNumericIV<EmbergraphLiteral>(+0f),
      new XSDNumericIV<EmbergraphLiteral>(Float.MAX_VALUE),
      new XSDNumericIV<EmbergraphLiteral>(Float.MIN_VALUE),
      new XSDNumericIV<EmbergraphLiteral>(Float.MIN_NORMAL),
      new XSDNumericIV<EmbergraphLiteral>(Float.POSITIVE_INFINITY),
      new XSDNumericIV<EmbergraphLiteral>(Float.NEGATIVE_INFINITY),
      new XSDNumericIV<EmbergraphLiteral>(Float.NaN),
    };

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /** Unit test for {@link XSDNumericIV}. */
  public void test_encodeDecode_XSDDouble() {

    /*
     * Note: -0d and +0d are converted to the same point in the value space.
     */
    //      new XSDNumericIV<EmbergraphLiteral>(-0d);

    final IV<?, ?>[] e = {
      new XSDNumericIV<EmbergraphLiteral>(1d),
      new XSDNumericIV<EmbergraphLiteral>(-1d),
      new XSDNumericIV<EmbergraphLiteral>(+0d),
      new XSDNumericIV<EmbergraphLiteral>(Double.MAX_VALUE),
      new XSDNumericIV<EmbergraphLiteral>(Double.MIN_VALUE),
      new XSDNumericIV<EmbergraphLiteral>(Double.MIN_NORMAL),
      new XSDNumericIV<EmbergraphLiteral>(Double.POSITIVE_INFINITY),
      new XSDNumericIV<EmbergraphLiteral>(Double.NEGATIVE_INFINITY),
      new XSDNumericIV<EmbergraphLiteral>(Double.NaN),
    };

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /** Unit test for {@link UUIDLiteralIV}. */
  public void test_encodeDecode_UUID() {

    final IV<?, ?>[] e = new IV[100];

    for (int i = 0; i < e.length; i++) {

      e[i] = new UUIDLiteralIV<EmbergraphLiteral>(UUID.randomUUID());
    }

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /**
   * Unit test for {@link UUIDBNodeIV}, which provides support for inlining a told blank node whose
   * <code>ID</code> can be parsed as a {@link UUID}.
   */
  public void test_encodeDecode_BNode_UUID_ID() {

    final IV<?, ?>[] e = new IV[100];

    for (int i = 0; i < e.length; i++) {

      e[i] = new UUIDBNodeIV<EmbergraphBNode>(UUID.randomUUID());
    }

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /**
   * Unit test for {@link NumericBNodeIV}, which provides support for inlining a told blank node
   * whose <code>ID</code> can be parsed as an {@link Integer}.
   */
  public void test_encodeDecode_BNode_INT_ID() {

    final IV<?, ?>[] e = {
      new NumericBNodeIV<EmbergraphBNode>(-1),
      new NumericBNodeIV<EmbergraphBNode>(0),
      new NumericBNodeIV<EmbergraphBNode>(1),
      new NumericBNodeIV<EmbergraphBNode>(-52),
      new NumericBNodeIV<EmbergraphBNode>(52),
      new NumericBNodeIV<EmbergraphBNode>(Integer.MAX_VALUE),
      new NumericBNodeIV<EmbergraphBNode>(Integer.MIN_VALUE),
    };

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /** Unit test for the {@link EpochExtension}. */
  public void test_encodeDecodeEpoch() {

    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance("test");

    final EpochExtension<EmbergraphValue> ext =
        new EpochExtension<EmbergraphValue>(
            new IDatatypeURIResolver() {
              public EmbergraphURI resolve(final URI uri) {
                final EmbergraphURI buri = vf.createURI(uri.stringValue());
                buri.setIV(newTermId(VTE.URI));
                return buri;
              }
            });

    final Random r = new Random();

    final IV<?, ?>[] e = new IV[100];

    for (int i = 0; i < e.length; i++) {

      final long v = r.nextLong();

      final String s = Long.toString(v);

      final Literal lit = new LiteralImpl(s, EpochExtension.EPOCH);

      final IV<?, ?> iv = ext.createIV(lit);

      if (iv == null) fail("Did not create IV: lit=" + lit);

      e[i] = iv;
    }

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /** Unit test for the {@link ColorsEnumExtension}. */
  public void test_encodeDecodeColor() {

    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance("test");

    final ColorsEnumExtension<EmbergraphValue> ext =
        new ColorsEnumExtension<EmbergraphValue>(
            new IDatatypeURIResolver() {
              public EmbergraphURI resolve(URI uri) {
                final EmbergraphURI buri = vf.createURI(uri.stringValue());
                buri.setIV(newTermId(VTE.URI));
                return buri;
              }
            });

    final List<IV<?, ?>> a = new LinkedList<IV<?, ?>>();

    for (Color c : Color.values()) {

      a.add(ext.createIV(new LiteralImpl(c.name(), ColorsEnumExtension.COLOR)));
    }

    final IV<?, ?>[] e = a.toArray(new IV[0]);

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /** Unit test for round-trip of xsd:dateTime values. */
  public void test_encodeDecodeDateTime() throws Exception {

    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance("test");

    final DatatypeFactory df = DatatypeFactory.newInstance();

    final DateTimeExtension<EmbergraphValue> ext =
        new DateTimeExtension<EmbergraphValue>(
            new IDatatypeURIResolver() {
              public EmbergraphURI resolve(URI uri) {
                final EmbergraphURI buri = vf.createURI(uri.stringValue());
                buri.setIV(newTermId(VTE.URI));
                return buri;
              }
            },
            TimeZone.getDefault());

    final EmbergraphLiteral[] dt = {
      vf.createLiteral(df.newXMLGregorianCalendar("2001-10-26T21:32:52")),
      vf.createLiteral(df.newXMLGregorianCalendar("2001-10-26T21:32:52+02:00")),
      vf.createLiteral(df.newXMLGregorianCalendar("2001-10-26T19:32:52Z")),
      vf.createLiteral(df.newXMLGregorianCalendar("2001-10-26T19:32:52+00:00")),
      vf.createLiteral(df.newXMLGregorianCalendar("-2001-10-26T21:32:52")),
      vf.createLiteral(df.newXMLGregorianCalendar("2001-10-26T21:32:52.12679")),
      vf.createLiteral(df.newXMLGregorianCalendar("1901-10-26T21:32:52")),
    };

    final IV<?, ?>[] e = new IV[dt.length];

    for (int i = 0; i < dt.length; i++) {

      e[i] = ext.createIV(dt[i]);
    }

    final IV<?, ?>[] a = doEncodeDecodeTest(e);

    if (log.isInfoEnabled()) {
      for (int i = 0; i < e.length; i++) {
        log.info("original: " + dt[i]);
        log.info("asValue : " + ext.asValue((LiteralExtensionIV<?>) e[i], vf));
        log.info("decoded : " + ext.asValue((LiteralExtensionIV<?>) a[i], vf));
        log.info("");
      }
      //          log.info(svf.createLiteral(
      //                df.newXMLGregorianCalendar("2001-10-26T21:32:52.12679")));
    }

    doComparatorTest(e);
  }

  /**
   * Unit test verifies that the inline xsd:dateTime representation preserves the milliseconds
   * units. However, precision beyond milliseconds is NOT preserved by the inline representation,
   * which is based on milliseconds since the epoch.
   *
   * @throws DatatypeConfigurationException
   */
  public void test_dateTime_preservesMillis() throws DatatypeConfigurationException {

    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance("test");

    final DatatypeFactory df = DatatypeFactory.newInstance();

    final DateTimeExtension<EmbergraphValue> ext =
        new DateTimeExtension<EmbergraphValue>(
            new IDatatypeURIResolver() {
              public EmbergraphURI resolve(URI uri) {
                final EmbergraphURI buri = vf.createURI(uri.stringValue());
                buri.setIV(newTermId(VTE.URI));
                return buri;
              }
            },
            TimeZone.getTimeZone("GMT"));

    /*
     * The string representation of the dateTime w/ milliseconds+ precision.
     * This is assumed to be a time in the time zone specified to the date
     * time extension.
     */
    final String givenStr = "2001-10-26T21:32:52.12679";

    /*
     * The string representation w/ only milliseconds precision. This will
     * be a time in the time zone given to the date time extension. The
     * canonical form of a GMT time zone is "Z", indicating "Zulu", which is
     * why that is part of the expected representation here.
     */
    final String expectedStr = "2001-10-26T21:32:52.126Z";

    /*
     * A embergraph literal w/o inlining from the *givenStr*. This
     * representation has greater milliseconds+ precision.
     */
    final EmbergraphLiteral lit = vf.createLiteral(df.newXMLGregorianCalendar(givenStr));

    // Verify the representation is exact.
    assertEquals(givenStr, lit.stringValue());

    /*
     * The IV representation of the dateTime. This will convert the date
     * time into the time zone given to the extension and will also truncate
     * the precision to no more than milliseconds.
     */
    final LiteralExtensionIV<?> iv = ext.createIV(lit);

    // Convert the IV back into a embergraph literal.
    final EmbergraphLiteral lit2 = (EmbergraphLiteral) ext.asValue(iv, vf);

    // Verify that millisecond precision was retained.
    assertEquals(expectedStr, lit2.stringValue());
  }

  /** Unit test for round-trip of derived numeric values. */
  public void test_encodeDecodeDerivedNumerics() throws Exception {

    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance("test");

    final DerivedNumericsExtension<EmbergraphValue> ext =
        new DerivedNumericsExtension<EmbergraphValue>(
            new IDatatypeURIResolver() {
              public EmbergraphURI resolve(URI uri) {
                final EmbergraphURI buri = vf.createURI(uri.stringValue());
                buri.setIV(newTermId(VTE.URI));
                return buri;
              }
            });

    final EmbergraphLiteral[] dt = {
      vf.createLiteral("1", XSD.POSITIVE_INTEGER),
      vf.createLiteral("-1", XSD.NEGATIVE_INTEGER),
      vf.createLiteral("-1", XSD.NON_POSITIVE_INTEGER),
      vf.createLiteral("1", XSD.NON_NEGATIVE_INTEGER),
      vf.createLiteral("0", XSD.NON_POSITIVE_INTEGER),
      vf.createLiteral("0", XSD.NON_NEGATIVE_INTEGER),
    };

    final IV<?, ?>[] e = new IV[dt.length];

    for (int i = 0; i < dt.length; i++) {

      e[i] = ext.createIV(dt[i]);
    }

    final IV<?, ?>[] a = doEncodeDecodeTest(e);

    if (log.isInfoEnabled()) {
      for (int i = 0; i < e.length; i++) {
        log.info("original: " + dt[i]);
        log.info("asValue : " + ext.asValue((LiteralExtensionIV<?>) e[i], vf));
        log.info("decoded : " + ext.asValue((LiteralExtensionIV<?>) a[i], vf));
        log.info("");
      }
      //          log.info(svf.createLiteral(
      //                df.newXMLGregorianCalendar("2001-10-26T21:32:52.12679")));
    }

    doComparatorTest(e);
  }

  /** Unit test for round-trip of GeoSpatial literals */
  public void test_encodeDecodeGeoSpatialLiterals01() throws Exception {

    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance("test");

    /** Initialize geo spatial config with default */
    final List<String> datatypeConfigs = new ArrayList<String>();
    datatypeConfigs.add(GEO_SPATIAL_DATATYPE_CONFIG);
    final GeoSpatialConfig conf =
        new GeoSpatialConfig(datatypeConfigs, GEO_SPATIAL_DATATYPE /* default */);
    final GeoSpatialDatatypeConfiguration datatypeConfig = conf.getDatatypeConfigs().get(0);
    final GeoSpatialLiteralExtension<EmbergraphValue> ext =
        new GeoSpatialLiteralExtension<EmbergraphValue>(
            new IDatatypeURIResolver() {
              public EmbergraphURI resolve(URI uri) {
                final EmbergraphURI buri = vf.createURI(uri.stringValue());
                buri.setIV(newTermId(VTE.URI));
                return buri;
              }
            },
            datatypeConfig);

    final EmbergraphLiteral[] dt = {
      vf.createLiteral("2#2#1", GEO_SPATIAL_DATATYPE_URI),
      vf.createLiteral("3#3#1", GEO_SPATIAL_DATATYPE_URI),
      vf.createLiteral("4#4#1", GEO_SPATIAL_DATATYPE_URI),
      vf.createLiteral("5#5#1", GEO_SPATIAL_DATATYPE_URI),
      vf.createLiteral("6#6#1", GEO_SPATIAL_DATATYPE_URI),
      vf.createLiteral("7#7#1", GEO_SPATIAL_DATATYPE_URI),
    };

    final IV<?, ?>[] e = new IV[dt.length];

    for (int i = 0; i < dt.length; i++) {

      e[i] = ext.createIV(dt[i]);
    }

    final IV<?, ?>[] a = doEncodeDecodeTest(e);

    if (log.isInfoEnabled()) {
      for (int i = 0; i < e.length; i++) {
        log.info("original: " + dt[i]);
        log.info("asValue : " + ext.asValue((LiteralExtensionIV<?>) e[i], vf));
        log.info("decoded : " + ext.asValue((LiteralExtensionIV<?>) a[i], vf));
        log.info("");
      }
      //          log.info(svf.createLiteral(
      //                df.newXMLGregorianCalendar("2001-10-26T21:32:52.12679")));
    }

    doComparatorTest(e);
  }

  /** Unit test for round-trip of GeoSpatial literals */
  public void test_encodeDecodeGeoSpatialLiterals02() throws Exception {

    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance("test");

    /** Initialize geo spatial config with default */
    final List<String> datatypeConfigs = new ArrayList<String>();
    datatypeConfigs.add(GEO_SPATIAL_DATATYPE_CONFIG);
    final GeoSpatialConfig conf =
        new GeoSpatialConfig(datatypeConfigs, GEO_SPATIAL_DATATYPE /* default */);
    final GeoSpatialDatatypeConfiguration datatypeConfig = conf.getDatatypeConfigs().get(0);
    final GeoSpatialLiteralExtension<EmbergraphValue> ext =
        new GeoSpatialLiteralExtension<EmbergraphValue>(
            new IDatatypeURIResolver() {
              public EmbergraphURI resolve(URI uri) {
                final EmbergraphURI buri = vf.createURI(uri.stringValue());
                buri.setIV(newTermId(VTE.URI));
                return buri;
              }
            },
            datatypeConfig);

    final EmbergraphLiteral[] dt = {vf.createLiteral("8#8#1", GEO_SPATIAL_DATATYPE_URI)};

    final IV<?, ?>[] e = new IV[dt.length];

    for (int i = 0; i < dt.length; i++) {

      e[i] = ext.createIV(dt[i]);
    }

    final IV<?, ?>[] a = doEncodeDecodeTest(e);

    if (log.isInfoEnabled()) {
      for (int i = 0; i < e.length; i++) {
        log.info("original: " + dt[i]);
        log.info("asValue : " + ext.asValue((LiteralExtensionIV<?>) e[i], vf));
        log.info("decoded : " + ext.asValue((LiteralExtensionIV<?>) a[i], vf));
        log.info("");
      }
      //          log.info(svf.createLiteral(
      //                df.newXMLGregorianCalendar("2001-10-26T21:32:52.12679")));
    }

    doComparatorTest(e);
  }

  /** Unit test for {@link SidIV}. */
  public void test_encodeDecode_sids() {

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
    //        spo1.setStatementIdentifier(true);
    //        spo2.setStatementIdentifier(true);
    //        spo3.setStatementIdentifier(true);
    //        spo6.setStatementIdentifier(true);
    final SPO spo13 = new SPO(spo1.getStatementIdentifier(), p1, o1, StatementEnum.Explicit);
    final SPO spo14 = new SPO(spo2.getStatementIdentifier(), p2, o2, StatementEnum.Explicit);
    final SPO spo15 = new SPO(s1, p1, spo3.getStatementIdentifier(), StatementEnum.Explicit);
    //        spo15.setStatementIdentifier(true);
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

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /**
   * Unit test for a fully inlined representation of a URI based on a <code>byte</code> code. The
   * flags byte looks like: <code>VTE=URI, inline=true, extension=false,
   * DTE=XSDByte</code>. It is followed by a <code>unsigned byte</code> value which is the index of
   * the URI in the {@link Vocabulary} class for the triple store.
   */
  public void test_encodeDecode_URIByteIV() {

    final IV<?, ?>[] e = {
      new VocabURIByteIV<EmbergraphURI>(Byte.MIN_VALUE),
      new VocabURIByteIV<EmbergraphURI>((byte) -1),
      new VocabURIByteIV<EmbergraphURI>((byte) 0),
      new VocabURIByteIV<EmbergraphURI>((byte) 1),
      new VocabURIByteIV<EmbergraphURI>(Byte.MAX_VALUE),
    };

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }

  /**
   * Unit test for a fully inlined representation of a URI based on a <code>short</code> code. The
   * flags byte looks like: <code>VTE=URI, inline=true, extension=false,
   * DTE=XSDShort</code>. It is followed by an <code>unsigned short</code> value which is the index
   * of the URI in the {@link Vocabulary} class for the triple store.
   */
  public void test_encodeDecode_URIShortIV() {

    final IV<?, ?>[] e = {
      new VocabURIShortIV<EmbergraphURI>(Short.MIN_VALUE),
      new VocabURIShortIV<EmbergraphURI>((short) -1),
      new VocabURIShortIV<EmbergraphURI>((short) 0),
      new VocabURIShortIV<EmbergraphURI>((short) 1),
      new VocabURIShortIV<EmbergraphURI>(Short.MAX_VALUE),
    };

    doEncodeDecodeTest(e);

    doComparatorTest(e);
  }
}
