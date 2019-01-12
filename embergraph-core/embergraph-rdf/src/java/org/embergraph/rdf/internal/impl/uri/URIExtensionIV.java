package org.embergraph.rdf.internal.impl.uri;

import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

import org.embergraph.rdf.internal.ILexiconConfiguration;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.AbstractInlineExtensionIV;
import org.embergraph.rdf.internal.impl.AbstractInlineIV;
import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.vocab.Vocabulary;

/**
 * Class provides support for fully inline {@link URI}s for which a
 * {@link Vocabulary} item was registered for the {@link URI} <em>namespace</em>
 * . An {@link URIExtensionIV} <strong>always</strong> has the <em>inline</em>
 * and <em>extension</em> bits set. {@link URIExtensionIV} are fully inline
 * since the <code>namespace</code> can be materialized from the
 * {@link Vocabulary} and the <code>localName</code> is directly inline.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @param <V>
 */
public class URIExtensionIV<V extends EmbergraphURI>
    	extends AbstractInlineExtensionIV<V, Object> 
		implements URI { 

    /**
     * 
     */
    private static final long serialVersionUID = 8267554196603121194L;

    /**
     * The namespace.
     */
    private final AbstractInlineIV<EmbergraphURI, ?> namespaceIV;

    /**
     * The localName.
     */
    private final AbstractLiteralIV<EmbergraphLiteral, ?> delegateIV;

    /**
     * {@inheritDoc}
     * <p>
     * Note: The extensionIV and delegateIV are NOT cloned. The rationale is
     * that we are only cloning to break the hard reference from the {@link IV}
     * to to cached value. If that needs to be done for the extensionIV and
     * delegateIV, then it will be done separately for those objects when they
     * are inserted into the termsCache.
     */
    @Override
    public IV<V, Object> clone(final boolean clearCache) {

        final URIExtensionIV<V> tmp = new URIExtensionIV<V>(delegateIV,
                namespaceIV);

        if (!clearCache) {

            tmp.setValue(getValueCache());
            
        }
        
        return tmp;

    }
    
    /**
     * 
     * @param delegateIV
     *            The {@link IV} which represents the localName.
     * @param namespaceIV
     *            The {@link IV} which represents the namespace. This MUST be a
     *            fully inline {@link IV} declared by the {@link Vocabulary}.
     */
    @SuppressWarnings("unchecked")
    public URIExtensionIV(
    		final AbstractLiteralIV<EmbergraphLiteral, ?> delegateIV,
    		final IV<?,?> namespaceIV) {
        
        super(VTE.URI, true/* extension */, delegateIV.getDTE());

        if (namespaceIV == null)
            throw new IllegalArgumentException();

        if (!namespaceIV.isInline()) // must be fully inline.
            throw new IllegalArgumentException();

        if (!delegateIV.isInline()) // must be fully inline. 
            throw new IllegalArgumentException();

        this.delegateIV = delegateIV;

        this.namespaceIV = (AbstractInlineIV<EmbergraphURI, ?>) namespaceIV;

    }
    
    /**
     * The namespace IV does need materialization, although it will not need
     * to go to the index to get the value (it just needs access to the lexicon's
     * vocabulary).
     */
    @Override
    public boolean needsMaterialization() {
    	return delegateIV.needsMaterialization() 
    	            || namespaceIV.needsMaterialization();
    }
    
    public AbstractLiteralIV<EmbergraphLiteral, ?> getLocalNameIV() {
        return delegateIV;
    }
    
    @Override
    public Object getInlineValue() { 
        return new URIImpl(stringValue());
    }
    
    /**
     * Extension IV is the <code>namespace</code> for the {@link URI}.
     */
    @Override
    public IV<EmbergraphURI, ?> getExtensionIV() {
        return namespaceIV;
    }
    
    /**
     * 
     */
    public int hashCode() {// TODO Inspect distribution.
        return namespaceIV.hashCode() ^ delegateIV.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o instanceof URIExtensionIV<?>) {
            return this.delegateIV.equals(((URIExtensionIV<?>) o).delegateIV)
                    && this.namespaceIV.equals(((URIExtensionIV<?>) o).namespaceIV);
        }
        return false;
    }

    // See BLZG-1591
    @Override
    public String toString() {
    	if (this.namespaceIV != null && this.delegateIV != null )
    		return this.namespaceIV.toString() + ":" + this.delegateIV.toString();
    	else 
    		return getValue().stringValue();
    }
    
    //////////////////////
    // OpenRDF URI methods
    //////////////////////
    
    @Override
    public String stringValue() {
        return toString(); // See BLZG-1591
    }

    @Override
    public String getNamespace() {// See BLZG-1591
        if(this.namespaceIV != null)
            return namespaceIV.getValue().stringValue();
        return getValue().getNamespace();
    }

    @Override
    public String getLocalName() {// See BLZG-1591
        if(this.delegateIV!=null)
            return delegateIV.getInlineValue().toString();
        return getValue().getLocalName();
    }
    

    // End of OpenRDF URI methods.

    
    @Override
    @SuppressWarnings("rawtypes")
    public int _compareTo(final IV o) {

        int ret = namespaceIV.compareTo(((URIExtensionIV<?>) o).namespaceIV);

        if (ret != 0)
            return ret;

        return delegateIV._compareTo(((URIExtensionIV<?>) o).delegateIV);

    }

    /**
     * Return the length of the namespace IV plus the length of the localName
     * IV.
     */
    @Override
    public int byteLength() {

        return 1/* flags */+ namespaceIV.byteLength() + delegateIV.byteLength();
        
    }

	/**
	 * Defer to the {@link ILexiconConfiguration} which has specific knowledge
	 * of how to generate an RDF value from this general purpose extension IV.
	 * <p>
	 * {@inheritDoc}
	 */
    @Override
	@SuppressWarnings( "unchecked" )
	public V asValue(final LexiconRelation lex) {

		V v = getValueCache();
		
		if (v == null) {
			
			final EmbergraphValueFactory f = lex.getValueFactory();
			
//			final ILexiconConfiguration config = lex.getLexiconConfiguration();
//
//            v = setValue((V) config.asValueFromVocab(this));

			final URI namespace = namespaceIV.asValue(lex);
			
			final String localName = lex.getLexiconConfiguration()
					.getInlineURILocalNameFromDelegate(namespace, delegateIV);
			
			v = setValue((V) f.createURI(namespace.stringValue(), localName));
			
			v.setIV(this);

		}

		return v;
		
    }
    
}
