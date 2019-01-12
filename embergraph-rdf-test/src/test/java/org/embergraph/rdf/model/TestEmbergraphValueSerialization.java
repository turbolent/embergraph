package org.embergraph.rdf.model;

import java.util.UUID;

import junit.framework.TestCase2;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;

import org.embergraph.io.SerializerUtil;

/**
 * Test suite for {@link EmbergraphValueImpl} serialization semantics, including
 * the correct recovery of the {@link EmbergraphValueFactoryImpl} reference when
 * deserialized.
 * 
 * @see EmbergraphValueSerializer
 */
public class TestEmbergraphValueSerialization extends TestCase2 {

	public TestEmbergraphValueSerialization() {
	}

	public TestEmbergraphValueSerialization(String name) {
		super(name);
	}

    /**
     * Fixture under test.
     */
    private EmbergraphValueSerializer<Value> fixture = null;
    
    protected void setUp() throws Exception {
        
        super.setUp();
        
        fixture = new EmbergraphValueSerializer<Value>(
                ValueFactoryImpl.getInstance());
        
    }
    
    protected void tearDown() throws Exception {

        fixture = null;
        
        super.tearDown();
        
    }
    
    /**
     * Performs round trip (de-)serialization using
     * {@link EmbergraphValueSerializer#serialize()} and
     * {@link EmbergraphValueSerializer#deserialize(byte[])}.
     * 
     * @param o
     *            The {@link Value}
     * 
     * @return The de-serialized {@link Value}.
     */
    private Value roundTrip_tuned(final Value o) {
        
        return fixture.deserialize(fixture.serialize(o));
        
    }
    
    /**
     * Test round trip of some URIs.
     */
    public void test_URIs() {

        final URI a = new URIImpl("http://www.embergraph.org");
        
        assertEquals(a, roundTrip_tuned(a));
        
    }
    
    /**
     * Test round trip of some plain literals.
     */
    public void test_plainLiterals() {

        final Literal a = new LiteralImpl("embergraph");
        
        assertEquals(a, roundTrip_tuned(a));
        
    }
    
    /**
     * Test round trip of some language code literals.
     */
    public void test_langCodeLiterals() {

        final Literal a = new LiteralImpl("embergraph","en");
        
        assertEquals(a, roundTrip_tuned(a));
        
    }

    /**
     * Test round trip of some datatype literals.
     */
    public void test_dataTypeLiterals() {

        final Literal a = new LiteralImpl("embergraph", XMLSchema.INT);
        
        assertEquals(a, roundTrip_tuned(a));
        
    }

    /*
     * Note: BNode serialization has been disabled since we never write
     * them on the database.
     */
    /**
     * Test round trip of some bnodes.
     */
    public void test_bnodes() {

        final BNode a = new BNodeImpl(UUID.randomUUID().toString());
        
        assertEquals(a, roundTrip_tuned(a));
        
    }

	public void test_roundTrip_URI() {

		doRoundTripTest(new URIImpl("http://www.embergraph.org"));
		
	}

	public void test_roundTrip_BNode() {

        doRoundTripTest(new BNodeImpl("12"));

        doRoundTripTest(new BNodeImpl(UUID.randomUUID().toString()));
		
	}

	public void test_roundTrip_plainLiteral() {

		doRoundTripTest(new LiteralImpl("embergraph"));
		
	}

    public void test_roundTrip_langCodeLiterals() {

        doRoundTripTest(new LiteralImpl("embergraph", "en"));

    }
	
	public void test_roundTrip_xsd_string() {

		doRoundTripTest(new LiteralImpl("embergraph", XMLSchema.STRING));

	}

	public void test_roundTrip_xsd_int() {

		doRoundTripTest(new LiteralImpl("12", XMLSchema.INT));

	}

    public void test_roundTrip_veryLargeLiteral() {

        final int len = 1024000;

        final StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++) {

            sb.append(Character.toChars('A' + (i % 26)));

        }

        final String s = sb.toString();

        if (log.isInfoEnabled())
            log.info("length(s)=" + s.length());
        
	    doRoundTripTest(new LiteralImpl(s));
	    
	}
	
	private void doRoundTripTest(final Value v) {
		
		final String namespace = getName();
		
		final EmbergraphValueFactory f = EmbergraphValueFactoryImpl.getInstance(namespace);

		// same reference (singleton pattern).
    assertSame(f, EmbergraphValueFactoryImpl.getInstance(namespace));

		// Coerce into a EmbergraphValue.
		final EmbergraphValue expected = f.asValue(v);

    assertSame(f, expected.getValueFactory());

		// test default java serialization.
        final EmbergraphValue actual1 = doDefaultJavaSerializationTest(expected);

        // same value factory reference on the deserialized term.
        assertSame(f, actual1.getValueFactory());

        // test EmbergraphValueSerializer
        final EmbergraphValue actual2 = doEmbergraphValueSerializationTest(expected);

        // same value factory reference on the deserialized term.
        assertSame(f, actual2.getValueFactory());

    }

    /**
     * Test of default Java Serialization (on an ObjectOutputStream).
     */
    private EmbergraphValue doDefaultJavaSerializationTest(
            final EmbergraphValue expected) {

        // serialize
        final byte[] data = SerializerUtil.serialize(expected);

        // deserialize
        final EmbergraphValue actual = (EmbergraphValue) SerializerUtil
                .deserialize(data);

        // Values compare as equal.
        assertEquals(expected, actual);

        return actual;

    }

    /**
     * Test of {@link EmbergraphValueSerializer}.
     */
    private EmbergraphValue doEmbergraphValueSerializationTest(
            final EmbergraphValue expected) {

        final EmbergraphValueSerializer<EmbergraphValue> ser = expected
                .getValueFactory().getValueSerializer();
        
        // serialize
        final byte[] data = ser.serialize(expected);

        // deserialize
        final EmbergraphValue actual = ser.deserialize(data);

        // Values compare as equal.
        assertEquals(expected, actual);

        return actual;

    }

}
