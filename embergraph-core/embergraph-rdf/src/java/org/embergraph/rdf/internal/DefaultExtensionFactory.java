package org.embergraph.rdf.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.embergraph.rdf.internal.impl.extensions.DateTimeExtension;
import org.embergraph.rdf.internal.impl.extensions.DerivedNumericsExtension;
import org.embergraph.rdf.internal.impl.extensions.GeoSpatialLiteralExtension;
import org.embergraph.rdf.internal.impl.extensions.XSDStringExtension;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.service.geospatial.GeoSpatialDatatypeConfiguration;

/*
 * Default {@link IExtensionFactory}. The following extensions are supported:
 *
 * <dl>
 *   <dt>{@link DateTimeExtension}
 *   <dd>Inlining literals which represent <code>xsd:dateTime</code> values into the statement
 *       indices.
 *   <dt>{@link XSDStringExtension}
 *   <dd>Inlining <code>xsd:string</code> literals into the statement indices.
 *   <dt>{@link DerivedNumericsExtension}
 *   <dd>Inlining literals which represent derived numeric values into the statement indices.
 * </dl>
 */
public class DefaultExtensionFactory implements IExtensionFactory {

  private final List<IExtension<? extends EmbergraphValue>> extensions;

  public DefaultExtensionFactory() {

    extensions = new LinkedList<IExtension<? extends EmbergraphValue>>();
  }

  @Override
  public void init(
      final IDatatypeURIResolver resolver, final ILexiconConfiguration<EmbergraphValue> config) {

    /*
     * Always going to inline the derived numeric types.
     */
    extensions.add(new DerivedNumericsExtension<EmbergraphLiteral>(resolver));

    /*
     * Set up the configuration of the geospatial module
     */
    if (config.isGeoSpatial()) {

      // register the extensions, adding one extension per datatype config
      final List<GeoSpatialDatatypeConfiguration> datatypeConfigs =
          config.getGeoSpatialConfig().getDatatypeConfigs();
      for (int i = 0; i < datatypeConfigs.size(); i++) {
        extensions.add(
            new GeoSpatialLiteralExtension<EmbergraphLiteral>(resolver, datatypeConfigs.get(i)));
      }
    }

    if (config.isInlineDateTimes()) {

      extensions.add(
          new DateTimeExtension<EmbergraphLiteral>(resolver, config.getInlineDateTimesTimeZone()));
    }

    if (config.getMaxInlineStringLength() > 0) {
      /*
       * Note: This extension is used for both literals and URIs. It MUST
       * be enabled when MAX_INLINE_TEXT_LENGTH is GT ZERO (0). Otherwise
       * we will not be able to inline either the local names or the full
       * text of URIs.
       */
      extensions.add(
          new XSDStringExtension<EmbergraphLiteral>(resolver, config.getMaxInlineStringLength()));
    }

    _init(resolver, config, extensions);
  }

  /*
   * Give subclasses a chance to add extensions.
   *
   * @param resolver {@link IDatatypeURIResolver} from {@link #init(IDatatypeURIResolver,
   *     ILexiconConfiguration)}.
   * @param config The {@link ILexiconConfiguration} from {@link #init(IDatatypeURIResolver,
   *     ILexiconConfiguration)}.
   * @param extensions The extensions that have already been registered.
   * @see #init(IDatatypeURIResolver, ILexiconConfiguration)
   */
  protected void _init(
      final IDatatypeURIResolver resolver,
      final ILexiconConfiguration<EmbergraphValue> config,
      final Collection<IExtension<? extends EmbergraphValue>> extensions) {

    // noop

  }

  @Override
  public Iterator<IExtension<? extends EmbergraphValue>> getExtensions() {
    return Collections.unmodifiableList(extensions).iterator();
  }
}
