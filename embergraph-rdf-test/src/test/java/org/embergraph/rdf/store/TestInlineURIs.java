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

package org.embergraph.rdf.store;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.internal.DTE;
import org.embergraph.rdf.internal.InlineLiteralIV;
import org.embergraph.rdf.internal.InlinePrefixedIntegerURIHandler;
import org.embergraph.rdf.internal.InlineSignedIntegerURIHandler;
import org.embergraph.rdf.internal.InlineSuffixedIntegerURIHandler;
import org.embergraph.rdf.internal.InlineURIFactory;
import org.embergraph.rdf.internal.InlineURIHandler;
import org.embergraph.rdf.internal.InlineUUIDURIHandler;
import org.embergraph.rdf.internal.InlineUnsignedIntegerURIHandler;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.internal.impl.literal.FullyInlineTypedLiteralIV;
import org.embergraph.rdf.internal.impl.literal.IPv4AddrIV;
import org.embergraph.rdf.internal.impl.literal.LiteralArrayIV;
import org.embergraph.rdf.internal.impl.literal.UUIDLiteralIV;
import org.embergraph.rdf.internal.impl.literal.XSDNumericIV;
import org.embergraph.rdf.internal.impl.uri.URIExtensionIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.rio.StatementBuffer;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.vocab.BaseVocabularyDecl;
import org.embergraph.rdf.vocab.core.EmbergraphCoreVocabulary_v20151106;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

/*
* Integration test suite for {@link InlineURIFactory} (the inline IVs are also tested in their own
 * package without the triple store integration).
 *
 * @author <a href="mailto:mpersonick@users.sourceforge.net">Mike Personick</a>
 */
public class TestInlineURIs extends AbstractTripleStoreTestCase {

  protected static final Logger log = Logger.getLogger(TestInlineURIs.class);

  /*
   * Please set your database properties here, except for your journal file, please DO NOT SPECIFY A
   * JOURNAL FILE.
   */
  @Override
  public Properties getProperties() {

    final Properties props = new Properties(super.getProperties());

    /*
     * Turn off inference.
     */
    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    return props;
  }

  public TestInlineURIs() {}

  public TestInlineURIs(final String arg0) {
    super(arg0);
  }

  public void testInlineUUIDs() throws Exception {

    /*
     * The embergraph store, backed by a temporary journal file.
     */
    final AbstractTripleStore store = getStore(getProperties());

    try {

      final EmbergraphValueFactory vf = store.getValueFactory();

      final EmbergraphURI uri1 = vf.createURI("urn:uuid:" + UUID.randomUUID().toString());
      final EmbergraphURI uri2 = vf.createURI("urn:uuid:" + UUID.randomUUID().toString());
      final EmbergraphURI uri3 = vf.createURI("urn:uuid:foo");

      final StatementBuffer<EmbergraphStatement> sb =
          new StatementBuffer<EmbergraphStatement>(store, 10 /* capacity */);

      sb.add(uri1, RDF.TYPE, XSD.UUID);
      sb.add(uri2, RDF.TYPE, XSD.UUID);
      sb.add(uri3, RDF.TYPE, XSD.UUID);
      sb.flush();
      store.commit();

      if (log.isDebugEnabled()) log.debug(store.dumpStore());

      assertTrue(uri1.getIV().isInline());
      assertTrue(uri2.getIV().isInline());
      assertFalse(uri3.getIV().isInline());

    } finally {
      store.__tearDownUnitTest();
    }
  }

  public void testInlineIPv4s() throws Exception {

    /*
     * The embergraph store, backed by a temporary journal file.
     */
    final AbstractTripleStore store = getStore(getProperties());

    try {

      final EmbergraphValueFactory vf = store.getValueFactory();

      final EmbergraphURI uri1 = vf.createURI("urn:ipv4:10.128.1.2");
      final EmbergraphURI uri2 = vf.createURI("urn:ipv4:10.128.1.2/24");
      final EmbergraphURI uri3 = vf.createURI("urn:ipv4:500.425.1.2");
      final EmbergraphURI uri4 = vf.createURI("urn:ipv4");

      final EmbergraphLiteral l = vf.createLiteral("10.128.1.2", XSD.IPV4);

      final StatementBuffer<EmbergraphStatement> sb =
          new StatementBuffer<EmbergraphStatement>(store, 10 /* capacity */);

      sb.add(uri1, RDF.TYPE, XSD.IPV4);
      sb.add(uri2, RDF.TYPE, XSD.IPV4);
      sb.add(uri3, RDF.TYPE, XSD.IPV4);
      sb.add(uri4, RDFS.LABEL, l);
      sb.flush();
      store.commit();

      if (log.isDebugEnabled()) log.debug("\n" + store.dumpStore());

      assertTrue(uri1.getIV().isInline());
      assertTrue(uri2.getIV().isInline());
      assertFalse(uri3.getIV().isInline());
      assertFalse(uri4.getIV().isInline());

    } finally {
      store.__tearDownUnitTest();
    }
  }

  public void testCustomUUIDNamespace() throws Exception {

    final Properties props = new Properties(getProperties());

    props.setProperty(AbstractTripleStore.Options.VOCABULARY_CLASS, CustomVocab.class.getName());
    props.setProperty(
        AbstractTripleStore.Options.INLINE_URI_FACTORY_CLASS,
        CustomInlineURIFactory.class.getName());

    /*
     * The embergraph store, backed by a temporary journal file.
     */
    final AbstractTripleStore store = getStore(props);

    try {

      final EmbergraphValueFactory vf = store.getValueFactory();

      final EmbergraphURI uri1 = vf.createURI(CUSTOM_NAMESPACE + UUID.randomUUID().toString());
      final EmbergraphURI uri2 = vf.createURI(CUSTOM_NAMESPACE + UUID.randomUUID().toString());
      final EmbergraphURI uri3 = vf.createURI(CUSTOM_NAMESPACE + "foo");

      final StatementBuffer<EmbergraphStatement> sb =
          new StatementBuffer<EmbergraphStatement>(store, 10 /* capacity */);

      sb.add(uri1, RDF.TYPE, XSD.UUID);
      sb.add(uri2, RDF.TYPE, XSD.UUID);
      sb.add(uri3, RDF.TYPE, XSD.UUID);
      sb.flush();
      store.commit();

      if (log.isDebugEnabled()) log.debug(store.dumpStore());

      assertTrue(uri1.getIV().isInline());
      assertTrue(uri2.getIV().isInline());
      assertFalse(uri3.getIV().isInline());

    } finally {
      store.__tearDownUnitTest();
    }
  }

  public void testSignedInteger() throws Exception {
    uriRoundtripTestCase(
        SIGNED_INT_NAMESPACE + "1", true,
        SIGNED_INT_NAMESPACE + "-123", true,
        SIGNED_INT_NAMESPACE + "-123342343214", true,
        SIGNED_INT_NAMESPACE + "123342343214", true,
        SIGNED_INT_NAMESPACE + Byte.MAX_VALUE, true,
        SIGNED_INT_NAMESPACE + Byte.MIN_VALUE, true,
        SIGNED_INT_NAMESPACE + Short.MAX_VALUE, true,
        SIGNED_INT_NAMESPACE + Short.MIN_VALUE, true,
        SIGNED_INT_NAMESPACE + Integer.MAX_VALUE, true,
        SIGNED_INT_NAMESPACE + Integer.MIN_VALUE, true,
        SIGNED_INT_NAMESPACE + Long.MAX_VALUE, true,
        SIGNED_INT_NAMESPACE + Long.MIN_VALUE, true,
        SIGNED_INT_NAMESPACE + "19223372036854775807", true,
        SIGNED_INT_NAMESPACE + "foo", false);
  }

  public void testUnsignedInteger() throws Exception {
    uriRoundtripTestCase(
        UNSIGNED_INT_NAMESPACE + "1",
        true,
        UNSIGNED_INT_NAMESPACE + "-123",
        false,
        UNSIGNED_INT_NAMESPACE + "-123342343214",
        false,
        UNSIGNED_INT_NAMESPACE + "123342343214",
        true,
        UNSIGNED_INT_NAMESPACE + Byte.MAX_VALUE,
        true,
        UNSIGNED_INT_NAMESPACE + Short.MAX_VALUE,
        true,
        UNSIGNED_INT_NAMESPACE + Integer.MAX_VALUE,
        true,
        UNSIGNED_INT_NAMESPACE + Long.MAX_VALUE,
        true,
        UNSIGNED_INT_NAMESPACE + "19223372036854775807",
        true,
        UNSIGNED_INT_NAMESPACE + "foo",
        false);
  }

  public void testSuffixedInteger() throws Exception {
    uriRoundtripTestCase(
        SUFFIXED_INT_NAMESPACE + "1-suffix",
        true,
        SUFFIXED_INT_NAMESPACE + "1",
        false,
        SUFFIXED_INT_NAMESPACE + "foo-suffix",
        false,
        SUFFIXED_INT_NAMESPACE + "-suffix",
        false,
        SUFFIXED_INT_NAMESPACE + "foo",
        false);
  }

  public void testPrefixedInteger() throws Exception {
    uriRoundtripTestCase(
        PREFIXED_INT_NAMESPACE + "prefix-1",
        true,
        PREFIXED_INT_NAMESPACE + "1",
        false,
        PREFIXED_INT_NAMESPACE + "prefix-foo",
        false,
        PREFIXED_INT_NAMESPACE + "prefix-",
        false,
        PREFIXED_INT_NAMESPACE + "foo",
        false);
  }

  private void uriRoundtripTestCase(final Object... options) throws Exception {

    final Properties props = new Properties(getProperties());
    props.setProperty(AbstractTripleStore.Options.VOCABULARY_CLASS, CustomVocab.class.getName());
    props.setProperty(
        AbstractTripleStore.Options.INLINE_URI_FACTORY_CLASS,
        CustomInlineURIFactory.class.getName());
    /*
     * The embergraph store, backed by a temporary journal file.
     */
    final AbstractTripleStore store = getStore(props);

    try {

      final EmbergraphValueFactory vf = store.getValueFactory();
      final ArrayList<EmbergraphURI> uris = new ArrayList<>();
      {
        final StatementBuffer<EmbergraphStatement> sb =
            new StatementBuffer<EmbergraphStatement>(store, 10 /* capacity */);
        for (int i = 0; i < options.length; i += 2) {
          final EmbergraphURI uri = vf.createURI((String) options[i]);
          uris.add(uri);
          sb.add(uri, RDF.TYPE, vf.createLiteral("doesn't matter"));
        }
        sb.flush();
        store.commit();

        if (log.isDebugEnabled()) log.debug(store.dumpStore());
      }

      for (int i = 0, j = 0; i < options.length; i += 2, j++) {

        final Object givenOption = options[i];

        final boolean isInline = (Boolean) options[i + 1];

        final EmbergraphURI uri = uris.get(j);

        assertEquals(
            "String representation different for:  " + givenOption, givenOption, uri.stringValue());

        assertEquals(
            "Inline expectation different for:  " + givenOption, isInline, uri.getIV().isInline());
      }

    } finally {
      store.__tearDownUnitTest();
    }
  }

  public void testMultipurposeIDNamespace() throws Exception {

    final Properties props = new Properties(getProperties());

    props.setProperty(AbstractTripleStore.Options.VOCABULARY_CLASS, CustomVocab.class.getName());
    props.setProperty(
        AbstractTripleStore.Options.INLINE_URI_FACTORY_CLASS,
        MultipurposeInlineIDFactory.class.getName());

    /*
     * The embergraph store, backed by a temporary journal file.
     */
    final AbstractTripleStore store = getStore(props);

    try {

      final EmbergraphValueFactory vf = store.getValueFactory();

      final EmbergraphURI uri1 = vf.createURI(CUSTOM_NAMESPACE + UUID.randomUUID().toString());
      final EmbergraphURI uri2 = vf.createURI(CUSTOM_NAMESPACE + "1");
      final EmbergraphURI uri3 = vf.createURI(CUSTOM_NAMESPACE + Short.MAX_VALUE);
      final EmbergraphURI uri4 = vf.createURI(CUSTOM_NAMESPACE + Integer.MAX_VALUE);
      final EmbergraphURI uri5 = vf.createURI(CUSTOM_NAMESPACE + Long.MAX_VALUE);
      final EmbergraphURI uri6 = vf.createURI(CUSTOM_NAMESPACE + "2.3");
      final EmbergraphURI uri7 = vf.createURI(CUSTOM_NAMESPACE + "foo");

      {
        final StatementBuffer<EmbergraphStatement> sb =
            new StatementBuffer<EmbergraphStatement>(store, 10 /* capacity */);
        sb.add(uri1, RDF.TYPE, RDFS.RESOURCE);
        sb.add(uri2, RDF.TYPE, RDFS.RESOURCE);
        sb.add(uri3, RDF.TYPE, RDFS.RESOURCE);
        sb.add(uri4, RDF.TYPE, RDFS.RESOURCE);
        sb.add(uri5, RDF.TYPE, RDFS.RESOURCE);
        sb.add(uri6, RDF.TYPE, RDFS.RESOURCE);
        sb.add(uri7, RDF.TYPE, RDFS.RESOURCE);
        sb.flush();
        store.commit();

        if (log.isDebugEnabled()) log.debug(store.dumpStore());
      }

      for (EmbergraphURI uri : new EmbergraphURI[] {uri1, uri2, uri3, uri4, uri5, uri6, uri7}) {

        assertTrue(uri.getIV().isInline());
      }

      assertEquals(DTE.UUID, uri1.getIV().getDTE());
      assertEquals(DTE.XSDByte, uri2.getIV().getDTE());
      assertEquals(DTE.XSDShort, uri3.getIV().getDTE());
      assertEquals(DTE.XSDInt, uri4.getIV().getDTE());
      assertEquals(DTE.XSDLong, uri5.getIV().getDTE());
      assertEquals(DTE.XSDDouble, uri6.getIV().getDTE());
      assertEquals(DTE.XSDString, uri7.getIV().getDTE());

    } finally {
      store.__tearDownUnitTest();
    }
  }

  public void testInlineArray() throws Exception {

    final Properties props = new Properties(getProperties());

    props.setProperty(AbstractTripleStore.Options.VOCABULARY_CLASS, CustomVocab.class.getName());
    props.setProperty(
        AbstractTripleStore.Options.INLINE_URI_FACTORY_CLASS, InlineArrayFactory.class.getName());

    /*
     * The embergraph store, backed by a temporary journal file.
     */
    final AbstractTripleStore store = getStore(props);

    try {

      final EmbergraphValueFactory vf = store.getValueFactory();

      final Object[] array =
          new Object[] {
            UUID.randomUUID(), "1", Short.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, "2.3", "foo"
          };

      final StringBuilder sb = new StringBuilder();
      sb.append(ARRAY);
      for (Object o : array) {
        sb.append(o);
        sb.append(':');
      }
      sb.setLength(sb.length() - 1);

      final EmbergraphURI uri1 = vf.createURI(sb.toString());

      {
        final StatementBuffer<EmbergraphStatement> buf =
            new StatementBuffer<EmbergraphStatement>(store, 10 /* capacity */);
        buf.add(uri1, RDF.TYPE, RDFS.RESOURCE);
        buf.flush();
        store.commit();

        if (log.isDebugEnabled()) log.debug(store.dumpStore());
      }

      for (EmbergraphURI uri : new EmbergraphURI[] {uri1}) {

        assertTrue(uri.getIV().isInline());
      }

      assertEquals(DTE.Extension, uri1.getIV().getDTE());

      @SuppressWarnings("rawtypes")
      final InlineLiteralIV[] ivs =
          ((LiteralArrayIV) ((URIExtensionIV) uri1.getIV()).getLocalNameIV()).getIVs();

      assertEquals(DTE.UUID, ivs[0].getDTE());
      assertEquals(DTE.XSDByte, ivs[1].getDTE());
      assertEquals(DTE.XSDShort, ivs[2].getDTE());
      assertEquals(DTE.XSDInt, ivs[3].getDTE());
      assertEquals(DTE.XSDLong, ivs[4].getDTE());
      assertEquals(DTE.XSDDouble, ivs[5].getDTE());
      assertEquals(DTE.XSDString, ivs[6].getDTE());

    } finally {
      store.__tearDownUnitTest();
    }
  }

  private static final String CUSTOM_NAMESPACE = "application:id:";
  private static final String SIGNED_INT_NAMESPACE = "http://example.com/int/";
  private static final String UNSIGNED_INT_NAMESPACE = "http://example.com/uint/";
  private static final String SUFFIXED_INT_NAMESPACE = "http://example.com/intsuf/";
  private static final String PREFIXED_INT_NAMESPACE = "http://example.com/intprefix/";
  private static final String PREFIX = "prefix-";
  private static final String SUFFIX = "-suffix";
  private static final String ARRAY = "myapp:array:";

  /** Note: Must be public for access patterns. */
  public static class CustomVocab extends EmbergraphCoreVocabulary_v20151106 {

    public CustomVocab() {
      super();
    }

    public CustomVocab(final String namespace) {
      super(namespace);
    }

    @Override
    protected void addValues() {
      super.addValues();

      addDecl(new BaseVocabularyDecl(CUSTOM_NAMESPACE));
      addDecl(new BaseVocabularyDecl(SIGNED_INT_NAMESPACE));
      addDecl(new BaseVocabularyDecl(UNSIGNED_INT_NAMESPACE));
      addDecl(new BaseVocabularyDecl(SUFFIXED_INT_NAMESPACE));
      addDecl(new BaseVocabularyDecl(PREFIXED_INT_NAMESPACE));
      addDecl(new BaseVocabularyDecl(ARRAY));
    }
  }

  public static class CustomInlineURIFactory extends InlineURIFactory {

    public CustomInlineURIFactory() {
      super();
      addHandler(new InlineUUIDURIHandler(CUSTOM_NAMESPACE));
      addHandler(new InlineSignedIntegerURIHandler(SIGNED_INT_NAMESPACE));
      addHandler(new InlineUnsignedIntegerURIHandler(UNSIGNED_INT_NAMESPACE));
      addHandler(new InlineSuffixedIntegerURIHandler(SUFFIXED_INT_NAMESPACE, SUFFIX));
      addHandler(new InlinePrefixedIntegerURIHandler(PREFIXED_INT_NAMESPACE, PREFIX));
    }
  }

  public static class MultipurposeInlineIDFactory extends InlineURIFactory {

    public MultipurposeInlineIDFactory() {
      super();
      addHandler(new MultipurposeInlineIDHandler(CUSTOM_NAMESPACE));
    }
  }

  @SuppressWarnings("rawtypes")
  public static class MultipurposeInlineIDHandler extends InlineURIHandler {

    public MultipurposeInlineIDHandler(final String namespace) {
      super(namespace);
    }

    @Override
    protected AbstractLiteralIV createInlineIV(final String localName) {

      try {
        return new IPv4AddrIV(localName);
      } catch (Exception ex) {
        // ok, not an ip address
      }

      try {
        return new UUIDLiteralIV<>(UUID.fromString(localName));
      } catch (Exception ex) {
        // ok, not a uuid
      }

      try {
        return new XSDNumericIV(Byte.parseByte(localName));
      } catch (Exception ex) {
        // ok, not a byte
      }

      try {
        return new XSDNumericIV(Short.parseShort(localName));
      } catch (Exception ex) {
        // ok, not a short
      }

      try {
        return new XSDNumericIV(Integer.parseInt(localName));
      } catch (Exception ex) {
        // ok, not a int
      }

      try {
        return new XSDNumericIV(Long.parseLong(localName));
      } catch (Exception ex) {
        // ok, not a long
      }

      try {
        return new XSDNumericIV(Double.parseDouble(localName));
      } catch (Exception ex) {
        // ok, not a double
      }

      // just use a UTF encoded string, this is expensive
      return new FullyInlineTypedLiteralIV<EmbergraphLiteral>(localName);
    }
  }

  public static class InlineArrayFactory extends InlineURIFactory {

    public InlineArrayFactory() {
      super();
      addHandler(new InlineArrayHandler(ARRAY));
    }
  }

  @SuppressWarnings("rawtypes")
  public static class InlineArrayHandler extends MultipurposeInlineIDHandler {

    public InlineArrayHandler(final String namespace) {
      super(namespace);
    }

    @Override
    protected AbstractLiteralIV createInlineIV(final String localName) {

      final List<AbstractLiteralIV> list = new LinkedList<>();

      final StringTokenizer st = new StringTokenizer(localName, ":");
      while (st.hasMoreTokens()) {
        list.add(super.createInlineIV(st.nextToken()));
      }

      if (list.isEmpty()) {
        throw new IllegalArgumentException();
      }

      return new LiteralArrayIV(list.toArray(new AbstractLiteralIV[list.size()]));
    }

    @Override
    public String getLocalNameFromDelegate(final AbstractLiteralIV<EmbergraphLiteral, ?> delegate) {

      final StringBuilder sb = new StringBuilder();

      final LiteralArrayIV array = (LiteralArrayIV) delegate;

      for (InlineLiteralIV iv : array.getIVs()) {
        sb.append(iv.getInlineValue());
        sb.append(':');
      }
      sb.setLength(sb.length() - 1);

      return sb.toString();
    }
  }
}
