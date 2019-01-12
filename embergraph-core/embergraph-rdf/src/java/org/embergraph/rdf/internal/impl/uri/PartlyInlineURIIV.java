package org.embergraph.rdf.internal.impl.uri;

import org.embergraph.rdf.internal.INonInlineExtensionCodes;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.AbstractNonInlineExtensionIVWithDelegateIV;
import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.openrdf.model.URI;

/**
 * A {@link URI} modeled as a namespace {@link IV} plus an inline Unicode <code>localName</code>.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <V>
 */
public class PartlyInlineURIIV<V extends EmbergraphURI>
    extends AbstractNonInlineExtensionIVWithDelegateIV<V, URI> {

  /** */
  private static final long serialVersionUID = -4548354704407887640L;

  /**
   * {@inheritDoc}
   *
   * <p>Note: The extensionIV and delegateIV are NOT cloned. The rationale is that we are only
   * cloning to break the hard reference from the {@link IV} to to cached value. If that needs to be
   * done for the extensionIV and delegateIV, then it will be done separately for those objects when
   * they are inserted into the termsCache.
   */
  public IV<V, URI> clone(final boolean clearCache) {

    final PartlyInlineURIIV<V> tmp = new PartlyInlineURIIV<V>(getDelegate(), getExtensionIV());

    if (!clearCache) {

      tmp.setValue(getValueCache());
    }

    return tmp;
  }

  public PartlyInlineURIIV(
      final AbstractLiteralIV<EmbergraphLiteral, ?> delegate, final IV<?, ?> namespace) {

    super(VTE.URI, delegate, namespace);
  }

  /**
   * Human readable representation includes the namespace {@link IV} and the <code>localName</code>.
   */
  public String toString() {

    return "URI(namespaceIV="
        + getExtensionIV()
        + getVTE().getCharCode()
        + ", localName="
        + getDelegate()
        + ")";
  }

  @Override
  public final byte getExtensionByte() {

    return INonInlineExtensionCodes.URINamespaceIV;
  }

  /**
   * Implements {@link URI#getLocalName()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public String getLocalName() {
    return getDelegate().stringValue();
  }
}
