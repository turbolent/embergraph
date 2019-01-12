/*
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
 * Created on Jul 5, 2008
 */

package org.embergraph.rdf.model;

import java.util.Comparator;
import org.embergraph.rdf.internal.IV;

/*
* Places {@link EmbergraphValue}s into an ordering determined by their assigned {@link
 * EmbergraphValue#getIV() term identifiers}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class EmbergraphValueIdComparator implements Comparator<EmbergraphValue> {

  public static final transient Comparator<EmbergraphValue> INSTANCE =
      new EmbergraphValueIdComparator();

  /*
   * Note: comparison avoids possible overflow of <code>long</code> by not computing the difference
   * directly.
   */
  public int compare(EmbergraphValue term1, EmbergraphValue term2) {

    final IV iv1 = term1.getIV();
    final IV iv2 = term2.getIV();

    return iv1.compareTo(iv2);
  }
}
