package org.embergraph.rdf.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.embergraph.rdf.model.EmbergraphValue;

/**
 * Simple {@link IExtensionFactory} implementation that creates two {@link IExtension}s - the {@link
 * EpochExtension} and the {@link ColorsEnumExtension}.
 */
public class SampleExtensionFactory implements IExtensionFactory {

  private final List<IExtension<? extends EmbergraphValue>> extensions;

  public SampleExtensionFactory() {

    extensions = new LinkedList<IExtension<? extends EmbergraphValue>>();
  }

  @Override
  public void init(
      final IDatatypeURIResolver resolver, final ILexiconConfiguration<EmbergraphValue> config) {

    //       	if (lex.isInlineDateTimes())
    //    		extensions.add(new DateTimeExtension(
    //    				lex, lex.getInlineDateTimesTimeZone()));
    extensions.add(new EpochExtension(resolver));
    extensions.add(new ColorsEnumExtension(resolver));
    //		extensionsArray = extensions.toArray(new IExtension[2]);

  }

  @Override
  public Iterator<IExtension<? extends EmbergraphValue>> getExtensions() {
    return Collections.unmodifiableList(extensions).iterator();
  }
}
