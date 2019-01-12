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
 * Created on Apr 11, 2008
 */

package org.embergraph.rdf.model;

import org.embergraph.rdf.spo.ISPO;
import org.openrdf.model.Statement;

/**
 * Also reports whether the statement is explicit, inferred or an axiom.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface EmbergraphStatement extends Statement, ISPO {

  /** Specialized return type. */
  EmbergraphResource getSubject();

  /** Specialized return type. */
  EmbergraphURI getPredicate();

  /** Specialized return type. */
  EmbergraphValue getObject();

  /** Specialized return type. */
  EmbergraphResource getContext();

  /**
   * <code>true</code> if the statement is an axiom that is not present as an explicit assertion.
   */
  boolean isAxiom();

  /**
   * <code>true</code> if the statement is an inference that is not present as an explicit assertion
   * or an axiom.
   */
  boolean isInferred();

  /** <code>true</code> if the statement is an explicit assertion. */
  boolean isExplicit();
}
