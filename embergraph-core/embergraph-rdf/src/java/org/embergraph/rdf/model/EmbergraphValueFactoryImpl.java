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
 * Created on Apr 21, 2008
 */

package org.embergraph.rdf.model;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.impl.BooleanLiteralImpl;

import org.embergraph.cache.WeakValueCache;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.impl.extensions.DateTimeExtension;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedByteIV;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedIntIV;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedLongIV;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedShortIV;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.util.concurrent.CanonicalFactory;

/**
 * An implementation using {@link EmbergraphValue}s and {@link EmbergraphStatement}s.
 * Values constructed using this factory do NOT have term identifiers assigned.
 * Statements constructed using this factory do NOT have statement identifiers
 * assigned. Those metadata can be resolved against the various indices and then
 * set on the returned values and statements.
 * 
 * @todo Consider a {@link WeakValueCache} on this factory to avoid duplicate
 *       values.
 * 
 * @todo Consider a {@link WeakValueCache} to shortcut recently used statements?
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class EmbergraphValueFactoryImpl implements EmbergraphValueFactory {

	private final String namespace;
	
	@Override
	public String getNamespace() {
		
	    if (namespace != null) {
	        
	        return namespace;
	        
	    } else {
	        
	        throw new RuntimeException("Headless value factory should not be asked for its namespace");
	        
	    }
		
	}
	
    /**
     * WARNING: Use {@link #getInstance(String)} NOT this constructor.
     * <p>
     * WARNING: This constructor provides 'headless' (not associated with any
     * namespace) instance of the {@link EmbergraphValueFactory}, which is used for
     * query/update parsing. It SHOULD NOT be used in code working with
     * triple-store.
     * 
     * @see BLZG-1678 (remove "headless" EmbergraphValueFactory impl class)
     * @see BLZG-1176 (SPARQL Query/Update parser should not use db connection)
     */
    public EmbergraphValueFactoryImpl() {

        this(null);

    }

	/**
     * WARNING: Use {@link #getInstance(String)} NOT this constructor.
     */
    private EmbergraphValueFactoryImpl(final String namespace) {

        this.namespace = namespace;

        xsdMap = getXSDMap();
        
        // @see <a href="http://trac.blazegraph.com/ticket/983"> Concurrent insert data with boolean object causes IllegalArgumentException </a>
        // @see <a href="http://trac.blazegraph.com/ticket/980"> Object position query hint is not a Literal </a>
//        /**
//         * Cache the IV on the EmbergraphValue for these boolean constants.
//         * 
//         * @see <a href="http://trac.blazegraph.com/ticket/983"> Concurrent insert
//         *      data with boolean object causes IllegalArgumentException </a>
//         */
//        TRUE.setIV(XSDBooleanIV.TRUE);
//        FALSE.setIV(XSDBooleanIV.FALSE);
    }
    
	/**
	 * Canonicalizing mapping for {@link EmbergraphValueFactoryImpl}s based on the
	 * namespace of the {@link LexiconRelation}.
	 * <p>
	 * Note: The backing LRU should be small (and can be zero) since instances
	 * SHOULD be finalized quickly once they are no longer strongly reachable
	 * (which would imply that there was no {@link LexiconRelation} for that
	 * instance and that all {@link EmbergraphValueImpl}s for that instance had
	 * become weakly reachable or been swept).
	 */
    private static CanonicalFactory<String/* namespace */, EmbergraphValueFactoryImpl,String/*State*/> cache = new CanonicalFactory<String, EmbergraphValueFactoryImpl,String>(
            1/* capacity */) {
				@Override
				protected EmbergraphValueFactoryImpl newInstance(
						final String key, final String namespace) {
						return new EmbergraphValueFactoryImpl(namespace);
				}
    };
//    private static WeakValueCache<String/* namespace */, EmbergraphValueFactoryImpl> cache = new WeakValueCache<String, EmbergraphValueFactoryImpl>(
//            new LRUCache<String, EmbergraphValueFactoryImpl>(1/* capacity */));

    /**
     * Return the instance associated with the <i>namespace</i>.
     * <p>
     * Note: This canonicalizing mapping for {@link EmbergraphValueFactoryImpl}s is
     * based on the namespace of the {@link LexiconRelation}. This makes the
     * instances canonical within a JVM instance, which is all that we care
     * about. The actual assignments of term identifiers to {@link EmbergraphValue}
     * s is performed by the {@link LexiconRelation} itself and is globally
     * consistent for a given lexicon.
     * 
     * @param namespace
     *            The namespace of the {@link LexiconRelation}.
     * 
     *            TODO This method introduces the possibility that two journals
     *            in the same JVM would share the same
     *            {@link EmbergraphValueFactory} for a kb with the same namespace.
     *            This is doubtless not desired.  A workaround is to use the
     *            {@link UUID} of the Journal as part of the namespace of the
     *            KB, which would serve to make sure that all KB instances have
     *            distinct namespaces.
     */
	public static EmbergraphValueFactory/* Impl */getInstance(final String namespace) {

		return cache.getInstance(namespace, namespace/*state*/);
		
	}
	
//    /**
//     * Return the instance associated with the <i>namespace</i>.
//     * <p>
//     * Note: This canonicalizing mapping for {@link EmbergraphValueFactoryImpl}s is
//     * based on the namespace of the {@link LexiconRelation}. This makes the
//     * instances canonical within a JVM instance, which is all that we care
//     * about. The actual assignments of term identifiers to {@link EmbergraphValue}s
//     * is performed by the {@link LexiconRelation} itself and is globally
//     * consistent for a given lexicon.
//     * 
//     * @param namespace
//     *            The namespace of the {@link LexiconRelation}.
//     */
//    public static EmbergraphValueFactory/*Impl*/ getInstance(final String namespace) {
//        
//        if (namespace == null)
//            throw new IllegalArgumentException();
//        
//        synchronized(cache) {
//            
//            EmbergraphValueFactoryImpl a = cache.get(namespace);
//
//            if (a == null) {
//
//                a = new EmbergraphValueFactoryImpl();
//
//                cache.put(namespace, a, true/* dirty */);
//                
//            }
//            
//            return a;
//            
//        }
//        
//    }
    
    /**
     * Remove a {@link EmbergraphValueFactoryImpl} from the canonicalizing mapping.
     * <p>
     * Entries in this canonicalizing mapping for a {@link LexiconRelation} MUST
     * be {@link #remove(String)}ed if the {@link LexiconRelation} is destroyed
     * in case a distinct lexicon is subsequently creating with the same
     * namespace. There is no need to discard an entry during abort processing.
     * 
     */
//    * @param namespace
//    *            The namespace of the {@link LexiconRelation}.
    @Override
    public void remove(/*final String namespace*/) {
        
//        if (namespace == null)
//            throw new IllegalArgumentException();
//        
//        synchronized(cache) {
//        
//            cache.remove(namespace);
//            
//        }

    	cache.remove(namespace);
    	
    }

    @Override
    public String toString() {
        return super.toString()+"{namespace="+namespace+"}";
    }
    
    @Override
    public BNodeContextFactory newBNodeContext() {

        return new BNodeContextFactory(this);
        
    }
    
    /**
     * Returns a new blank node with a globally unique blank node ID based on a
     * {@link UUID}.
     * <p>
     * Note: Since the blank node IDs are random, they tend to be uniformly
     * distributed across the index partition(s). More efficient ordered writes
     * may be realized using {@link #newBNodeContext()} to obtain a derived
     * {@link EmbergraphValueFactory} instance that is specific to a document that
     * is being loaded into the RDF DB. 
     * 
     * @see #newBNodeContext()
     */
    @Override
    public EmbergraphBNodeImpl createBNode() {
        
        return createBNode(nextID());

    }

    /**
     * Returns a blank node identifier (ID) based on a random {@link UUID}.
     */
    protected String nextID() {

        return "u"+UUID.randomUUID();

    }
    
    @Override
    public EmbergraphBNodeImpl createBNode(final String id) {

        return new EmbergraphBNodeImpl(this, id);

    }

    @Override
    public EmbergraphBNodeImpl createBNode(final EmbergraphStatement stmt) {

    	// Subject, predicate, object and context should be processed to use the target value factory
    	// See https://jira.blazegraph.com/browse/BLZG-1875
    	final EmbergraphResource originalS = stmt.getSubject();
    	final EmbergraphURI originalP = stmt.getPredicate();
    	final EmbergraphValue originalO = stmt.getObject();
    	final EmbergraphResource originalC = stmt.getContext();
    	
    	final EmbergraphResource s = asValue(originalS);
    	final EmbergraphURI p = asValue(originalP);
    	final EmbergraphValue o = asValue(originalO);
    	final EmbergraphResource c = asValue(originalC);

    	final EmbergraphStatement effectiveStmt;
    	
		if (originalS != s || originalP != p || originalO != o || originalC != c) {

    		effectiveStmt = new EmbergraphStatementImpl(s, p, o, c, stmt.getStatementType(), stmt.getUserFlag());

    	} else {

    		effectiveStmt = stmt;
    		
    	}

		return new EmbergraphBNodeImpl(this, nextID(), effectiveStmt);
    }

    @Override
    public EmbergraphLiteralImpl createLiteral(final String label) {

        return new EmbergraphLiteralImpl(this, label, null, null);
        
    }

    /*
     * XSD support. 
     */
    public static final transient String NAMESPACE_XSD = "http://www.w3.org/2001/XMLSchema";
    
    public static final transient String xsd = NAMESPACE_XSD + "#";

    private final EmbergraphURIImpl xsd_string = new EmbergraphURIImpl(this, xsd
            + "string");

    private final EmbergraphURIImpl xsd_dateTime = new EmbergraphURIImpl(this,
            xsd + "dateTime");
    
    private final EmbergraphURIImpl xsd_date = new EmbergraphURIImpl(this,
            xsd + "date");
    
    private final EmbergraphURIImpl xsd_long = new EmbergraphURIImpl(this, xsd
            + "long");

    private final EmbergraphURIImpl xsd_int = new EmbergraphURIImpl(this,
            xsd + "int");

    private final EmbergraphURIImpl xsd_byte = new EmbergraphURIImpl(this, xsd
            + "byte");

    private final EmbergraphURIImpl xsd_short = new EmbergraphURIImpl(this, xsd
            + "short");

    private final EmbergraphURIImpl xsd_ulong = new EmbergraphURIImpl(this, xsd
            + "unsignedLong");

    private final EmbergraphURIImpl xsd_uint = new EmbergraphURIImpl(this,
            xsd + "unsignedInt");

    private final EmbergraphURIImpl xsd_ubyte = new EmbergraphURIImpl(this, xsd
            + "unsignedByte");

    private final EmbergraphURIImpl xsd_ushort = new EmbergraphURIImpl(this, xsd
            + "unsignedShort");

    private final EmbergraphURIImpl xsd_double = new EmbergraphURIImpl(this, xsd
            + "double");

    private final EmbergraphURIImpl xsd_float = new EmbergraphURIImpl(this, xsd
            + "float");

    private final EmbergraphURIImpl xsd_boolean = new EmbergraphURIImpl(this, xsd
            + "boolean");

//    private final EmbergraphLiteralImpl TRUE = new EmbergraphLiteralImpl(this, "true", null,
//            xsd_boolean);
//
//    private final EmbergraphLiteralImpl FALSE = new EmbergraphLiteralImpl(this, "false", null,
//            xsd_boolean);

	/**
	 * Map for fast resolution of XSD URIs. The keys are the string values of
	 * the URIs. The values are the URIs.
	 */
    private final Map<String, EmbergraphURIImpl> xsdMap;

    /**
     * Populate and return a map for fast resolution of XSD URIs.
     */
	private Map<String, EmbergraphURIImpl> getXSDMap() {

		final Map<String, EmbergraphURIImpl> map = new LinkedHashMap<String, EmbergraphURIImpl>();

		final EmbergraphURIImpl[] a = new EmbergraphURIImpl[] { xsd_string,
				xsd_dateTime, xsd_date, xsd_long, xsd_int, xsd_byte, xsd_short,
				xsd_double, xsd_float, xsd_boolean };

		for (EmbergraphURIImpl x : a) {

			// stringValue of URI => URI
			map.put(x.stringValue(), x);

		}

		return map;

    }
    
    /**
     * {@inheritDoc}
     * 
     * @see <a href="http://trac.blazegraph.com/ticket/983"> Concurrent insert data
     *      with boolean object causes IllegalArgumentException </a>
     * @see <a href="http://trac.blazegraph.com/ticket/980"> Object position of
     *      query hint is not a Literal </a>
     */
    @Override
    public EmbergraphLiteralImpl createLiteral(final boolean arg0) {

        return (arg0
                ? new EmbergraphLiteralImpl(this, "true", null, xsd_boolean)
                : new EmbergraphLiteralImpl(this, "false", null, xsd_boolean)
                );

    }

    @Override
    public EmbergraphLiteralImpl createLiteral(byte arg0) {

        return new EmbergraphLiteralImpl(this, "" + arg0, null, xsd_byte);

    }

    @Override
    public EmbergraphLiteralImpl createLiteral(byte arg0, final boolean unsigned) {

        return new EmbergraphLiteralImpl(this, "" + (unsigned ? XSDUnsignedByteIV.promote(arg0) : arg0), null, unsigned ? xsd_ubyte : xsd_byte);

    }

    @Override
    public EmbergraphLiteralImpl createLiteral(short arg0) {

        return new EmbergraphLiteralImpl(this, "" + arg0, null, xsd_short);

    }

    @Override
    public EmbergraphLiteralImpl createLiteral(short arg0, final boolean unsigned) {

        return new EmbergraphLiteralImpl(this, "" + (unsigned ? XSDUnsignedShortIV.promote(arg0) : arg0), null, unsigned ? xsd_ushort :xsd_short);

    }

    @Override
    public EmbergraphLiteralImpl createLiteral(int arg0) {

        return new EmbergraphLiteralImpl(this, "" + arg0, null, xsd_int);

    }

    @Override
    public EmbergraphLiteralImpl createLiteral(int arg0, final boolean unsigned) {

        return new EmbergraphLiteralImpl(this, "" +  (unsigned ? XSDUnsignedIntIV.promote(arg0) : arg0), null, unsigned ? xsd_uint :xsd_int);

    }

    @Override
    public EmbergraphLiteralImpl createLiteral(long arg0) {

        return new EmbergraphLiteralImpl(this, "" + arg0, null, xsd_long);

    }

    @Override
    public EmbergraphLiteralImpl createLiteral(long arg0, final boolean unsigned) {

        return new EmbergraphLiteralImpl(this, "" + (unsigned ? XSDUnsignedLongIV.promote(arg0) : arg0), null, unsigned ? xsd_ulong : xsd_long);

    }

    @Override
    public EmbergraphLiteralImpl createLiteral(float arg0) {

        return new EmbergraphLiteralImpl(this, "" + arg0, null, xsd_float);

    }

    @Override
    public EmbergraphLiteralImpl createLiteral(double arg0) {

        return new EmbergraphLiteralImpl(this, "" + arg0, null, xsd_double);

    }
    
    public EmbergraphLiteralImpl createLiteral(final Date date) {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(date);
        XMLGregorianCalendar xmlGC = 
                DateTimeExtension.datatypeFactorySingleton.newXMLGregorianCalendar(c);
        return createLiteral(xmlGC);
    }

    @Override
    public EmbergraphLiteralImpl createLiteral(final XMLGregorianCalendar arg0) {

		/*
		 * Note: QName#toString() does not produce the right representation,
		 * which is why we need to go through XMLDatatypeUtil.
		 * 
		 * @see https://sourceforge.net/apps/trac/bigdata/ticket/117
		 */
        return new EmbergraphLiteralImpl(this, arg0.toString(),
                null/* languageCode */, createURI(XMLDatatypeUtil.qnameToURI(
                        arg0.getXMLSchemaType()).stringValue()));
        
    }

    @Override
    public EmbergraphLiteralImpl createXSDDateTime(final long timestamp) {
        final TimeZone tz = TimeZone.getDefault()/*getTimeZone("GMT")*/;
        final GregorianCalendar c = new GregorianCalendar(tz);
        c.setGregorianChange(new Date(Long.MIN_VALUE));
        c.setTimeInMillis(timestamp);
        
        final XMLGregorianCalendar xmlGC = 
                DateTimeExtension.datatypeFactorySingleton.newXMLGregorianCalendar(c);
        return createLiteral(xmlGC);
    }


    @Override
    public EmbergraphLiteralImpl createLiteral(final String label, final String language) {

        return new EmbergraphLiteralImpl(this, label, language, null/* datatype */);

    }

    @Override
    public EmbergraphLiteralImpl createLiteral(final String label, URI datatype) {

        return createLiteral(label, datatype, null);
    }

    @Override
    public EmbergraphLiteralImpl createLiteral(String label, URI datatype, String language) {
        

        /*
         * Note: The datatype parameter may be null per the Sesame API.
         * 
         * See https://sourceforge.net/apps/trac/bigdata/ticket/226
         */
        if (datatype != null && !(datatype instanceof EmbergraphURIImpl)) {

            datatype = createURI(datatype.stringValue());
            
        }

        return new EmbergraphLiteralImpl(this, label, language,
                (EmbergraphURIImpl) datatype);
    }


    @Override
    public EmbergraphURIImpl createURI(final String uriString) {

		final String str = uriString;
		
//		if (str.startsWith(NAMESPACE_XSD)) {

			final EmbergraphURIImpl tmp = xsdMap.get(str);
			
			if(tmp != null) {

				// found in canonicalizing map.
				return tmp;
				
			}
    		
//    }
    
        return new EmbergraphURIImpl(this, uriString);

    }

    @Override
    public EmbergraphURIImpl createURI(final String namespace, final String localName) {

        return new EmbergraphURIImpl(this, namespace + localName);

    }

    @Override
    public EmbergraphStatementImpl createStatement(Resource s, URI p, Value o) {

        return createStatement(s, p, o, null/* c */, null/* type */);

    }

    @Override
    public EmbergraphStatementImpl createStatement(Resource s, URI p, Value o,
            Resource c) {

        return createStatement(s, p, o, c, null/* type */);

    }

    @Override
    public EmbergraphStatementImpl createStatement(Resource s, URI p, Value o,
            Resource c, StatementEnum type) {
        
        return createStatement(s, p, o, c, type, false/* userFlag */);
        
    }

    @Override
    public EmbergraphStatementImpl createStatement(Resource s, URI p, Value o,
            Resource c, StatementEnum type, final boolean userFlag) {
        
        return new EmbergraphStatementImpl(
                (EmbergraphResource) asValue(s),
                (EmbergraphURI)      asValue(p),
                (EmbergraphValue)    asValue(o),
                (EmbergraphResource) asValue(c),// optional
                type, // the statement type (optional).
                userFlag // the user flag (optional)
        );

    }

    @Override
    final public EmbergraphValue asValue(final Value v) {

        if (v == null)
            return null;

        if (v instanceof EmbergraphValueImpl
                && ((EmbergraphValueImpl) v).getValueFactory() == this) {

			final EmbergraphValueImpl v1 = (EmbergraphValueImpl) v;

			final IV<?, ?> iv = v1.getIV();

			if (iv == null || !iv.isNullIV()) {

				/*
				 * A value from the same value factory whose IV is either
				 * unknown or defined (but not a NullIV or DummyIV).
				 */

				return (EmbergraphValue) v;

			}

        }

        if (v instanceof BooleanLiteralImpl) {
        	
    		final BooleanLiteralImpl bl = (BooleanLiteralImpl) v;
    		
            return createLiteral(bl.booleanValue());

        } else if (v instanceof URI) {
        	
            return createURI(((URI) v).stringValue());
            
        } else if (v instanceof EmbergraphBNode && ((EmbergraphBNode)v).isStatementIdentifier()) {

       		return createBNode(((EmbergraphBNode) v).getStatement());

        } else if (v instanceof BNode) {

            return createBNode(((BNode) v).stringValue());

        } else if (v instanceof Literal) {

            final Literal tmp = ((Literal) v);

            final String label = tmp.getLabel();

            final String language = tmp.getLanguage();

            final URI datatype = tmp.getDatatype();

            return new EmbergraphLiteralImpl(
                    this,// Note: Passing in this factory!
                    label,
                    language,
                    (EmbergraphURI)asValue(datatype)
                    );

        } else {

            throw new AssertionError();

        }

    }
    
    /**
     * (De-)serializer paired with this {@link EmbergraphValueFactoryImpl}.
     */
    private final transient EmbergraphValueSerializer<EmbergraphValue> valueSer = new EmbergraphValueSerializer<EmbergraphValue>(
            this);

    @Override
    public EmbergraphValueSerializer<EmbergraphValue> getValueSerializer() {

        return valueSer;

    }

    @Override
    public EmbergraphResource asValue(Resource v) {

        return (EmbergraphResource) asValue((Value) v);
        
    }

    @Override
    public EmbergraphURI asValue(URI v) {
        
        return (EmbergraphURI)asValue((Value)v);
        
    }

    @Override
    public EmbergraphLiteral asValue(Literal v) {
        
        return (EmbergraphLiteral)asValue((Value)v);
        
    }

    @Override
    public EmbergraphBNode asValue(BNode v) {

        return (EmbergraphBNode)asValue((Value)v);
        
    }
    
}
