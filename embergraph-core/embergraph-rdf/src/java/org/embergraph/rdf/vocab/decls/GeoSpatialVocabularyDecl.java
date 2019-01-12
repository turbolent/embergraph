/*
Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on July 30, 2015
 */

package org.embergraph.rdf.vocab.decls;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.embergraph.rdf.vocab.VocabularyDecl;
import org.embergraph.service.geospatial.GeoSpatial;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/*
 * Vocabulary and namespace for GeoSpatial extensions.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
@Deprecated
public class GeoSpatialVocabularyDecl implements VocabularyDecl {

  // TODO: proper registration of datatypes passed in via config
  private static final URI[] uris =
      new URI[] {
        new URIImpl(GeoSpatial.NAMESPACE), GeoSpatial.DEFAULT_DATATYPE,
      };

  public GeoSpatialVocabularyDecl() {}

  public Iterator<URI> values() {

    return Collections.unmodifiableList(Arrays.asList(uris)).iterator();
  }
}
