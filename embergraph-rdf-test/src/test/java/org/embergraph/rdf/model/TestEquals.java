package org.embergraph.rdf.model;

import junit.framework.TestCase;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;

import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.TermId;

/**
 * Test suite for equals() semantics for {@link EmbergraphValue} implementations.
 * Each test makes sure that two bigdata values are equals() if they have the
 * same data, regardless of whether they have the same value factory. Note that
 * two {@link EmbergraphValue}s for the same {@link ValueFactory} which have the
 * same {@link IV} are compared on the basis of that {@link IV} (unless it is a
 * "dummy" or "mock" IV). 
 */
public class TestEquals extends TestCase {

	public TestEquals() {
		
	}

	public TestEquals(String name) {
		super(name);
	}

	public void test_equalsURI() {
		
	    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance(getName());
	
	    final EmbergraphValueFactory vf2 = EmbergraphValueFactoryImpl.getInstance(getName()+"2");
		
	    final EmbergraphURI v1 = vf.createURI("http://www.embergraph.org");
	    
	    final EmbergraphURI v2 = vf.createURI("http://www.embergraph.org");
	    
	    final URI v3 = new URIImpl("http://www.embergraph.org");

	    final EmbergraphURI v4 = vf2.createURI("http://www.embergraph.org");

	    assertTrue( v1 != v2 );
	    
	    assertTrue(v1.equals(v2));
	    assertTrue(v2.equals(v1));

	    assertTrue(v3.equals(v1));
	    assertTrue(v3.equals(v2));
	    assertTrue(v1.equals(v3));
	    assertTrue(v2.equals(v3));

	    assertTrue(v1.equals(v4));
	    assertTrue(v4.equals(v1));
	    assertTrue(v2.equals(v4));
	    assertTrue(v4.equals(v2));

	    v2.setIV(TermId.mockIV(VTE.URI));
	    
	    assertTrue(v1.equals(v2));
	    assertTrue(v2.equals(v1));

	    assertTrue(v3.equals(v1));
	    assertTrue(v3.equals(v2));
	    assertTrue(v1.equals(v3));
	    assertTrue(v2.equals(v3));

	    assertTrue(v1.equals(v4));
	    assertTrue(v4.equals(v1));
	    assertTrue(v2.equals(v4));
	    assertTrue(v4.equals(v2));

	    v1.setIV(new TermId<EmbergraphURI>(VTE.URI, 1));

	    assertTrue(v1.equals(v2));
	    assertTrue(v2.equals(v1));

	    assertTrue(v3.equals(v1));
	    assertTrue(v3.equals(v2));
	    assertTrue(v1.equals(v3));
	    assertTrue(v2.equals(v3));

	    assertTrue(v1.equals(v4));
	    assertTrue(v4.equals(v1));
	    assertTrue(v2.equals(v4));
	    assertTrue(v4.equals(v2));

	}

	public void test_equalsLiteral() {

		doLiteralTest("embergraph", null/* datatype */, null/* languageCode */);

		doLiteralTest("embergraph", XMLSchema.STRING/* datatype */, null/* languageCode */);

		doLiteralTest("embergraph", null/* datatype */, "en"/* languageCode */);

	}

	private Literal createLiteral(ValueFactory f, final String label,
			final URI datatype, final String languageCode) {

		if (datatype == null && languageCode == null)
			return f.createLiteral(label);

		if (datatype == null)
			return f.createLiteral(label, languageCode);
		
		return f.createLiteral(label, datatype);

	}

	private void doLiteralTest(final String label, final URI datatype,
			final String languageCode) {

		final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl
				.getInstance(getName());

		final EmbergraphValueFactory vf2 = EmbergraphValueFactoryImpl
				.getInstance(getName() + "2");

		final EmbergraphLiteral v1 = (EmbergraphLiteral) createLiteral(vf, label,
				datatype, languageCode);

		final EmbergraphLiteral v2 = (EmbergraphLiteral) createLiteral(vf, label,
				datatype, languageCode);

		final Literal v3 = createLiteral(new ValueFactoryImpl(), label,
				datatype, languageCode);

		final EmbergraphLiteral v4 = (EmbergraphLiteral) createLiteral(vf2, label,
				datatype, languageCode);

	    assertTrue( v1 != v2 );
	    
	    assertTrue(v1.equals(v2));
	    assertTrue(v2.equals(v1));

	    assertTrue(v3.equals(v1));
	    assertTrue(v3.equals(v2));
	    assertTrue(v1.equals(v3));
	    assertTrue(v2.equals(v3));

	    assertTrue(v1.equals(v4));
	    assertTrue(v4.equals(v1));
	    assertTrue(v2.equals(v4));
	    assertTrue(v4.equals(v2));

	    v2.setIV(TermId.mockIV(VTE.LITERAL));
	    
	    assertTrue(v1.equals(v2));
	    assertTrue(v2.equals(v1));

	    assertTrue(v3.equals(v1));
	    assertTrue(v3.equals(v2));
	    assertTrue(v1.equals(v3));
	    assertTrue(v2.equals(v3));

	    assertTrue(v1.equals(v4));
	    assertTrue(v4.equals(v1));
	    assertTrue(v2.equals(v4));
	    assertTrue(v4.equals(v2));

	    v1.setIV(new TermId<EmbergraphLiteral>(VTE.LITERAL, 1));

	    assertTrue(v1.equals(v2));
	    assertTrue(v2.equals(v1));

	    assertTrue(v3.equals(v1));
	    assertTrue(v3.equals(v2));
	    assertTrue(v1.equals(v3));
	    assertTrue(v2.equals(v3));

	    assertTrue(v1.equals(v4));
	    assertTrue(v4.equals(v1));
	    assertTrue(v2.equals(v4));
	    assertTrue(v4.equals(v2));

	}
	
	public void test_equalsBNode() {

	    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance(getName());

	    final EmbergraphValueFactory vf2 = EmbergraphValueFactoryImpl.getInstance(getName()+"2");
		
	    final EmbergraphBNode v1 = vf.createBNode("embergraph");
	    
	    final EmbergraphBNode v2 = vf.createBNode("embergraph");

	    final BNode v3 = new BNodeImpl("embergraph");

	    final EmbergraphBNode v4 = vf2.createBNode("embergraph");

	    assertTrue( v1 != v2 );
	    
	    assertTrue(v1.equals(v2));
	    assertTrue(v2.equals(v1));

	    assertTrue(v3.equals(v1));
	    assertTrue(v3.equals(v2));
	    assertTrue(v1.equals(v3));
	    assertTrue(v2.equals(v3));

	    assertTrue(v1.equals(v4));
	    assertTrue(v4.equals(v1));
	    assertTrue(v2.equals(v4));
	    assertTrue(v4.equals(v2));

	    v2.setIV(TermId.mockIV(VTE.BNODE));
	    
	    assertTrue(v1.equals(v2));
	    assertTrue(v2.equals(v1));

	    assertTrue(v3.equals(v1));
	    assertTrue(v3.equals(v2));
	    assertTrue(v1.equals(v3));
	    assertTrue(v2.equals(v3));

	    assertTrue(v1.equals(v4));
	    assertTrue(v4.equals(v1));
	    assertTrue(v2.equals(v4));
	    assertTrue(v4.equals(v2));

	    v1.setIV(new TermId<EmbergraphBNode>(VTE.BNODE, 1));

	    assertTrue(v1.equals(v2));
	    assertTrue(v2.equals(v1));

	    assertTrue(v3.equals(v1));
	    assertTrue(v3.equals(v2));
	    assertTrue(v1.equals(v3));
	    assertTrue(v2.equals(v3));

	    assertTrue(v1.equals(v4));
	    assertTrue(v4.equals(v1));
	    assertTrue(v2.equals(v4));
	    assertTrue(v4.equals(v2));

	}

}
