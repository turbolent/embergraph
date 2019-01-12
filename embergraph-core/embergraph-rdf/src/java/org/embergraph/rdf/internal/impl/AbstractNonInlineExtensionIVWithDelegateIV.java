package org.embergraph.rdf.internal.impl;

import org.embergraph.rdf.internal.DTE;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;

/**
 * Class <strong>always</strong> has the <em>extension</em> bit set but is NOT
 * 100% "inline". An instance of this class bundles together an value of some
 * primitive data type declared by {@link DTE} with the "extension" {@link IV}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 * @param <V>
 * @param <T>
 */
abstract public class AbstractNonInlineExtensionIVWithDelegateIV<V extends EmbergraphValue, T>
        extends AbstractNonInlineExtensionIV<V, T> {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
	 * The inline literal delegate.
	 */
    private final AbstractLiteralIV<EmbergraphLiteral, ?> delegate;

    /**
     * The non-inline extension IV.
     */
    private final IV<?, ?> extensionIV;
    
    protected AbstractNonInlineExtensionIVWithDelegateIV(final VTE vte,
            final AbstractLiteralIV<EmbergraphLiteral, ?> delegate,
            final IV<?, ?> extensionIv) {

        super(vte, delegate.getDTE());
        
        if (extensionIv == null)
            throw new IllegalArgumentException();
        
        this.delegate = delegate;
        
        this.extensionIV = extensionIv;
        
    }
    
    public AbstractLiteralIV<EmbergraphLiteral, ?> getDelegate() {

        return delegate;
        
    }
    
    @Override
    public IV<?, ?> getExtensionIV() {

        return extensionIV;
        
    }
    
    final public int hashCode() {
        return delegate.hashCode();
    }

    final public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o instanceof AbstractNonInlineExtensionIVWithDelegateIV<?, ?>) {
            return this.delegate
                    .equals(((AbstractNonInlineExtensionIVWithDelegateIV<?, ?>) o).delegate)
                    && this.extensionIV
                            .equals(((AbstractNonInlineExtensionIVWithDelegateIV<?, ?>) o).extensionIV);
        }
        return false;
    }

    final public int _compareTo(final IV o) {

//        int ret = extensionIv._compareTo(((AbstractExtensionIV) o).extensionIv);

        final int ret = extensionIV
                .compareTo(((AbstractNonInlineExtensionIVWithDelegateIV) o).extensionIV);

        if (ret != 0)
            return ret;

        return delegate
                ._compareTo(((AbstractNonInlineExtensionIVWithDelegateIV) o).delegate);

    }

	/**
	 * Return one (for the flags) plus the length of the extension IV plus the
	 * length of the delegate IV.
	 */
    final public int byteLength() {

        return 1/* flags */+ 1/* extensionByte */+ extensionIV.byteLength()
                + delegate.byteLength();

    }

}
