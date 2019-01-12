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
 * Created on Jun 17, 2011
 */

package org.embergraph.rdf.internal;

import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;

import org.embergraph.rdf.internal.impl.bnode.FullyInlineUnicodeBNodeIV;
import org.embergraph.rdf.internal.impl.extensions.XSDStringExtension;
import org.embergraph.rdf.internal.impl.literal.FullyInlineTypedLiteralIV;
import org.embergraph.rdf.internal.impl.literal.PartlyInlineTypedLiteralIV;
import org.embergraph.rdf.internal.impl.uri.FullyInlineURIIV;
import org.embergraph.rdf.internal.impl.uri.PartlyInlineURIIV;
import org.embergraph.rdf.internal.impl.uri.URIExtensionIV;
import org.embergraph.rdf.internal.impl.uri.VocabURIByteIV;
import org.embergraph.rdf.internal.impl.uri.VocabURIShortIV;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.vocab.Vocabulary;

/**
 * Unit tests for {@link IV}s which inline Unicode data.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestEncodeDecodeUnicodeIVs extends
        AbstractEncodeDecodeKeysTestCase {

    /**
     * 
     */
    public TestEncodeDecodeUnicodeIVs() {
    }

    /**
     * @param name
     */
    public TestEncodeDecodeUnicodeIVs(String name) {
        super(name);
    }

    /**
     * Unit test for the {@link XSDStringExtension} support for inlining
     * <code>xsd:string</code>. This approach is more efficient since the
     * datatypeURI is implicit in the {@link IExtension} handler than being
     * explicitly represented in the inline data.
     */
    public void test_encodeDecode_extension_xsdString() {
        
        final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance("test");
        
        final int maxInlineStringLength = 128;
        
        final XSDStringExtension<EmbergraphValue> ext =
            new XSDStringExtension<EmbergraphValue>(
                new IDatatypeURIResolver() {
                    public EmbergraphURI resolve(final URI uri) {
                        final EmbergraphURI buri = vf.createURI(uri.stringValue());
                        buri.setIV(newTermId(VTE.URI));
                        return buri;
                    }
                }, maxInlineStringLength);
        
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

        doEncodeDecodeTest(e);
        
        doComparatorTest(e);
        
    }

    /**
     * Unit test for inlining an entire URI using {@link FullyInlineURIIV}. The URI
     * is inlined as a Unicode component using {@link DTE#XSDString}. The
     * extension bit is NOT set since we are not factoring out the namespace
     * component of the URI.
     */
    public void test_encodeDecode_Inline_URI() {
        
        final IV<?, ?>[] e = {
                new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org")),
                new FullyInlineURIIV<EmbergraphURI>(RDF.TYPE),
                new FullyInlineURIIV<EmbergraphURI>(RDF.SUBJECT),
                new FullyInlineURIIV<EmbergraphURI>(RDF.BAG),
                new FullyInlineURIIV<EmbergraphURI>(RDF.OBJECT),
        };

        doEncodeDecodeTest(e);
        
        doComparatorTest(e);
        
    }

    /**
     * Unit test for inlining an entire URI using {@link URIExtensionIV}. The
     * URI is inlined as a combination of a {@link Vocabulary} item and a
     * Unicode component using {@link DTE#XSDString}. The extension bit is set
     * since we are factoring out the namespace component of the URI.
     */
    public void test_encodeDecode_Inline_Extension_URI() {

        final IV<?, ?>[] e = {
                new URIExtensionIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                                "http://www.example.com/"),
                        new VocabURIByteIV<EmbergraphURI>((byte) 1)),
                new URIExtensionIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                                "http://www.example.com/foo"),
                        new VocabURIByteIV<EmbergraphURI>((byte) 1)),
                new URIExtensionIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>(
                                "http://www.example.com/foobar"),
                        new VocabURIByteIV<EmbergraphURI>((byte) 1)),
        };

        doEncodeDecodeTest(e);
        
        doComparatorTest(e);
        
    }

    /**
     * Unit test for inlining blank nodes having a Unicode <code>ID</code>.
     */
    public void test_encodeDecode_Inline_BNode_UnicodeID() {

        final IV<?, ?>[] e = {
                new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("FOO"),
                new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("_bar"),
                new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("bar"),
                new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("baz"),
                new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("12"),
                new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("1298"),
                new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("asassdao"),
                new FullyInlineUnicodeBNodeIV<EmbergraphBNode>("1"),
        };

        doEncodeDecodeTest(e);

        doComparatorTest(e);

    }

    /**
     * Unit test for {@link FullyInlineTypedLiteralIV}. That class provides inlining of
     * any kind of {@link Literal}. However, while that class is willing to
     * inline <code>xsd:string</code> it is more efficient to handle inlining
     * for <code>xsd:string</code> using the {@link XSDStringExtension}.
     * <p>
     * This tests the inlining of plain literals.
     */
    public void test_encodeDecode_Inline_Literal_plainLiteral() {
        
        final IV<?, ?>[] e = {
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", null/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", null/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", null/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123", null/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23", null/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3", null/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("", null/* language */,
                        null/* datatype */),
        };

        doEncodeDecodeTest(e);
        
        doComparatorTest(e);

    }

    /**
     * Unit test for {@link FullyInlineTypedLiteralIV}. That class provides inlining of
     * any kind of {@link Literal}. However, while that class is willing to
     * inline <code>xsd:string</code> it is more efficient to handle inlining
     * for <code>xsd:string</code> using the {@link XSDStringExtension}.
     * <p>
     * This tests inlining of language code literals.
     */
    public void test_encodeDecode_Inline_Literal_languageCodeLiteral() {

        final IV<?, ?>[] e = {
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("goo", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("goo", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("2", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("2", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3", "de"/* language */,
                        null/* datatype */),
        };

        doEncodeDecodeTest(e);

        doComparatorTest(e);

    }

    /**
     * Unit test for {@link FullyInlineTypedLiteralIV}. That class provides inlining of
     * any kind of {@link Literal}. However, while that class is willing to
     * inline <code>xsd:string</code> it is more efficient to handle inlining
     * for <code>xsd:string</code> using the {@link XSDStringExtension}.
     * <p>
     * This tests inlining of datatype literals which DO NOT correspond to
     * registered extension types as the datatypeIV plus the inline Unicode
     * value of the label.
     */
    public void test_encodeDecode_Inline_Literal_datatypeLiteral() {
        
        final URI dt1 = new URIImpl("http://www.embergraph.org/mock-datatype-1");
        final URI dt2 = new URIImpl("http://www.embergraph.org/mock-datatype-2");
        
        final IV<?, ?>[] e = {
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", null/* language */,
                        dt1),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", null/* language */,
                        dt1),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", null/* language */,
                        dt1),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("goo", null/* language */,
                        dt1),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", null/* language */,
                        dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", null/* language */,
                        dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", null/* language */,
                        dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("goo", null/* language */,
                        dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("", null/* language */, dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("", null/* language */, dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1", null/* language */, dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1", null/* language */, dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12", null/* language */, dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12", null/* language */, dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123", null/* language */, dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123", null/* language */, dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23", null/* language */, dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23", null/* language */, dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3", null/* language */, dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3", null/* language */, dt2),
        };

        doEncodeDecodeTest(e);
        
        doComparatorTest(e);

    }

    /**
     * A unit test for {@link FullyInlineTypedLiteralIV} in which we mix plain literals,
     * language code literals, and datatype literals. This verifies that they
     * encode and decode correctly but also that the unsigned byte[] ordering of
     * the encoded keys is preserved across the different types of literals.
     */
    public void test_encodeDecode_Inline_Literals_All_Types() {
        
        final URI dt1 = new URIImpl("http://www.embergraph.org/mock-datatype-1");
        final URI dt2 = new URIImpl("http://www.embergraph.org/mock-datatype-2");
        
        final IV<?, ?>[] e = {
                /*
                 * Plain literals.
                 */
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", null/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", null/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", null/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123", null/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23", null/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3", null/* language */,
                        null/* datatype */),
                /*
                 * Language code literals.
                 */
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("goo", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", "en"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("goo", "de"/* language */,
                        null/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", "de"/* language */,
                        null/* datatype */),
                /*
                 * Datatype literals.
                 */
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", null/* language */,
                        dt1),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", null/* language */,
                        dt1),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", null/* language */,
                        dt1),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("goo", null/* language */,
                        dt1),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", null/* language */,
                        dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", null/* language */,
                        dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", null/* language */,
                        dt2),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("goo", null/* language */,
                        dt2),
        };
        
        doEncodeDecodeTest(e);

        doComparatorTest(e);

    }
    
    /**
     * Unit test for {@link FullyInlineTypedLiteralIV}. That class provides inlining of
     * any kind of {@link Literal}. However, while that class is willing to
     * inline <code>xsd:string</code> it is more efficient to handle inlining
     * for <code>xsd:string</code> using the {@link XSDStringExtension}.
     * <p>
     * This tests for possible conflicting interpretations of an xsd:string
     * value. The interpretation as a fully inline literal should be distinct
     * from other possible interpretations so this is testing for unexpected
     * errors.
     */
    public void test_encodeDecode_Inline_Literal_XSDString_DeconflictionTest() {
    
        final IV<?, ?>[] e = {
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("foo", null/* language */,
                        XSD.STRING/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar", null/* language */,
                        XSD.STRING/* datatype */),
                new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz", null/* language */,
                        XSD.STRING/* datatype */),
        };

        doEncodeDecodeTest(e);

        doComparatorTest(e);

    }

    /**
     * Test for a URI broken down into namespace and local name components. The
     * namespace component is coded by setting the extension bit and placing the
     * IV of the namespace into the extension IV field. The local name is
     * inlined as a Unicode component using {@link DTE#XSDString}.
     */
    public void test_encodeDecode_NonInline_URI_with_NamespaceIV() {

        final IV<?,?> namespaceIV = newTermId(VTE.URI);
        
        final IV<?, ?>[] e = {
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), namespaceIV),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), namespaceIV),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), namespaceIV),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), namespaceIV),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), namespaceIV),
        };

        doEncodeDecodeTest(e);
        
        doComparatorTest(e);
        
    }

    /**
     * Test for a literal broken down into datatype IV and an inline label. The
     * datatype IV is coded by setting the extension bit and placing the IV of
     * the namespace into the extension IV field. The local name is inlined as a
     * Unicode component using {@link DTE#XSDString}.
     */
    public void test_encodeDecode_NonInline_Literal_with_DatatypeIV() {

        final IV<?,?> datatypeIV = newTermId(VTE.URI);
        final IV<?,?> datatypeIV2 = newTermId(VTE.URI);

        final IV<?, ?>[] e = {
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>(""), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>(" "), datatypeIV2),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1"), datatypeIV2),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12"), datatypeIV2),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), datatypeIV2),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), datatypeIV2),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), datatypeIV2),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), datatypeIV2),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), datatypeIV2),
        };

        doEncodeDecodeTest(e);

        doComparatorTest(e);

    }

    /**
     * Unit test for a fully inline representation of a URI based on a
     * namespaceIV represented by a {@link VocabURIShortIV} and a Unicode localName.
     */
    public void test_encodeDecode_URINamespaceIV() {

        final IV<?, ?>[] e = {
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"),// localName
                        new VocabURIShortIV<EmbergraphURI>((short) 1) // namespace
                ),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"),// localName
                        new VocabURIShortIV<EmbergraphURI>((short) 1) // namespace
                ),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"),// localName
                        new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                ),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"),// localName
                        new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                ),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"),// localName
                        new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                ),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"),// localName
                        new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                ),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"),// localName
                        new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                ),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"),// localName
                        new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                ),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"),// localName
                        new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                ),
                new PartlyInlineURIIV<EmbergraphURI>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"),// localName
                        new VocabURIShortIV<EmbergraphURI>((short) 2) // namespace
                ),
        };

        doEncodeDecodeTest(e);
     
        doComparatorTest(e);

    }

    /**
     * Unit test for a fully inline representation of a datatype Literal based
     * on a datatypeIV represented by a {@link VocabURIShortIV} and a Unicode
     * localName.
     */
    public void test_encodeDecode_LiteralNamespaceIV() {

        final IV<?,?> datatypeIV = new VocabURIShortIV<EmbergraphURI>((short) 1);
        final IV<?,?> datatypeIV2 = new VocabURIShortIV<EmbergraphURI>((short) 2);

        final IV<?, ?>[] e = {
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("bar"), datatypeIV2),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("baz"), datatypeIV2),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123"), datatypeIV2),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("23"), datatypeIV2),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), datatypeIV),
                new PartlyInlineTypedLiteralIV<EmbergraphLiteral>(
                        new FullyInlineTypedLiteralIV<EmbergraphLiteral>("3"), datatypeIV2),
        };

        doEncodeDecodeTest(e);

        doComparatorTest(e);

    }

}
