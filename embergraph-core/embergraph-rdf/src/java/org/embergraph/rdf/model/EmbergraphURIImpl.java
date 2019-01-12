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
 * Created on Apr 16, 2008
 */

package org.embergraph.rdf.model;

import org.openrdf.model.URI;
import org.openrdf.model.util.URIUtil;

/*
* A URI. Use {@link EmbergraphValueFactory} to create instances of this class.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class EmbergraphURIImpl extends EmbergraphResourceImpl implements EmbergraphURI {

  /** */
  private static final long serialVersionUID = 3018590380571802474L;

  private final String uriString;

  /** lazily assigned. */
  private int indexOf = -1;

  //    public EmbergraphURIImpl(String uriString) {
  //
  //        this(null, uriString);
  //
  //    }

  /** Used by {@link EmbergraphValueFactoryImpl}. */
  EmbergraphURIImpl(final EmbergraphValueFactory valueFactory, final String uriString) {

    super(valueFactory, null);

    if (uriString == null) throw new IllegalArgumentException();

    if (uriString.indexOf(':') == -1) {

      throw new IllegalArgumentException(uriString);
    }

    this.uriString = uriString;
  }

  public String toString() {

    return uriString;
  }

  public String getNamespace() {

    if (indexOf == -1) {

      indexOf = URIUtil.getLocalNameIndex(uriString);
    }

    return uriString.substring(0, indexOf);
  }

  public int getNamespaceLength() {

    if (indexOf == -1) {

      indexOf = URIUtil.getLocalNameIndex(uriString);
    }

    return indexOf;
  }

  public int getLocalNameLength() {

    if (indexOf == -1) {

      indexOf = URIUtil.getLocalNameIndex(uriString);
    }

    return uriString.length() - (indexOf + 1);
  }

  public String getLocalName() {

    if (indexOf == -1) {

      indexOf = URIUtil.getLocalNameIndex(uriString);
    }

    return uriString.substring(indexOf);
  }

  public String stringValue() {

    return uriString;
  }

  public final boolean equals(final Object o) {

    if (!(o instanceof URI)) return false;

    return equals((URI) o);
  }

  public final boolean equals(final URI o) {

    if (this == o) return true;

    if (o == null) return false;

    if ((o instanceof EmbergraphValue)
        && isRealIV()
        && ((EmbergraphValue) o).isRealIV()
        && ((EmbergraphValue) o).getValueFactory() == getValueFactory()) {

      final boolean equals = getIV().equals(((EmbergraphValue) o).getIV());

      return equals;
    }

    return uriString.equals(o.stringValue());
  }

  public final int hashCode() {

    return uriString.hashCode();
  }
}
