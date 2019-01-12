/**
 * The Notice below must appear in each file of the Source Code of any copy you distribute of the
 * Licensed Product. Contributors to any Modifications may add their own copyright notices to
 * identify their own contributions.
 *
 * <p>License:
 *
 * <p>The contents of this file are subject to the CognitiveWeb Open Source License Version 1.1 (the
 * License). You may not copy or use this file, in either source code or executable form, except in
 * compliance with the License. You may obtain a copy of the License from
 *
 * <p>http://www.CognitiveWeb.org/legal/license/
 *
 * <p>Software distributed under the License is distributed on an AS IS basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyrights:
 *
 * <p>Portions created by or assigned to CognitiveWeb are Copyright (c) 2003-2003 CognitiveWeb. All
 * Rights Reserved. Contact information for CognitiveWeb is available at
 *
 * <p>http://www.CognitiveWeb.org
 *
 * <p>Portions Copyright (c) 2002-2003 Bryan Thompson.
 *
 * <p>Acknowledgements:
 *
 * <p>Special thanks to the developers of the Jabber Open Source License 1.0 (JOSL), from which this
 * License was derived. This License contains terms that differ from JOSL.
 *
 * <p>Special thanks to the CognitiveWeb Open Source Contributors for their suggestions and support
 * of the Cognitive Web.
 *
 * <p>Modifications:
 */
/*
 * Created on Apr 16, 2008
 */

package org.embergraph.rdf.model;

import org.embergraph.bop.IElement;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.store.IRawTripleStore;
import org.embergraph.rdf.store.TempTripleStore;
import org.openrdf.model.Value;

/**
 * An interface which exposes the internal 64-bit long integer identifiers for {@link Value}s stored
 * within a {@link IRawTripleStore}. Values may also be stored inline inside the statement indices
 * rather than referencing the lexicon. See {@link IV}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface EmbergraphValue extends Value, IElement { // , Comparable<EmbergraphValue> {

  /**
   * Return the factory which produced this object. This is guaranteed to be a singleton (there will
   * only be one {@link EmbergraphValueFactory} instance for on a given JVM for all {@link
   * EmbergraphValue}s associated with a given lexicon relation namespace).
   */
  EmbergraphValueFactory getValueFactory();

  /**
   * Return the internal value for this value. May be a term identifier or an inline value. The term
   * identifier uniquely identifies a {@link Value} for a database. Sometimes a {@link
   * TempTripleStore} will be used that shares the lexicon with a given database, in which case the
   * same term identifiers will be value for that {@link TempTripleStore}.
   */
  IV getIV();

  /**
   * Set the internal value for this value.
   *
   * <p>Note: Both {@link IV} and {@link EmbergraphValue} can cache one another. The pattern for
   * caching is that you <em>always</em> cache the {@link IV} on the {@link EmbergraphValue} using
   * {@link EmbergraphValue#setIV(IV)}. However, the {@link EmbergraphValue} is normally NOT cached
   * on the {@link IV}. The exception is when the {@link EmbergraphValue} has been materialized from
   * the {@link IV} by joining against the lexicon. The query plan is responsible for deciding when
   * to materialize the {@link EmbergraphValue} from the {@link IV}.
   *
   * @param iv The internal value.
   * @throws IllegalArgumentException if <i>iv</i> is null.
   * @throws IllegalStateException if the internal value is already set to a different non-null
   *     value.
   */
  void setIV(IV iv);

  /**
   * Return <code>true</code> if the {@link IV} is either is set to a "real" IV. Return <code>false
   * </code> if the {@link IV} is either not set or is set to a "mock" or "dummy" {@link IV}.
   */
  boolean isRealIV();

  /** Clears the internal value to null. */
  void clearInternalValue();
}
