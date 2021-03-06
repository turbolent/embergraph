package org.embergraph.rdf.lexicon;

import java.io.Serializable;
import org.embergraph.rdf.internal.IV;

/*
 * Interface for filtering internal values.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface ITermIVFilter extends Serializable {

  /*
   * Return <code>true</code> iff the term {@link IV} should be visited.
   *
   * @param iv The internal value
   */
  boolean isValid(IV iv);
}
