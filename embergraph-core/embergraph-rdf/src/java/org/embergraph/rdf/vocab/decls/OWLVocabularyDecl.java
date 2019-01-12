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
 * Created on Jun 4, 2011
 */

package org.embergraph.rdf.vocab.decls;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.embergraph.rdf.vocab.VocabularyDecl;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;

/*
* Vocabulary and namespace for {@link OWL}.
 *
 * @see http://www.w3.org/2002/07/owl#
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class OWLVocabularyDecl implements VocabularyDecl {

  private static final URI[] uris =
      new URI[] {
        new URIImpl(OWL.NAMESPACE),
        OWL.ALLDIFFERENT,
        OWL.ALLVALUESFROM,
        OWL.ANNOTATIONPROPERTY,
        OWL.BACKWARDCOMPATIBLEWITH,
        OWL.CARDINALITY,
        OWL.CLASS,
        OWL.COMPLEMENTOF,
        OWL.DATATYPEPROPERTY,
        OWL.DEPRECATEDCLASS,
        OWL.DEPRECATEDPROPERTY,
        OWL.DIFFERENTFROM,
        OWL.DISJOINTWITH,
        OWL.DISTINCTMEMBERS,
        OWL.EQUIVALENTCLASS,
        OWL.EQUIVALENTPROPERTY,
        OWL.FUNCTIONALPROPERTY,
        OWL.HASVALUE,
        OWL.IMPORTS,
        OWL.INCOMPATIBLEWITH,
        OWL.INDIVIDUAL,
        OWL.INTERSECTIONOF,
        OWL.INVERSEFUNCTIONALPROPERTY,
        OWL.INVERSEOF,
        OWL.MAXCARDINALITY,
        OWL.MINCARDINALITY,
        OWL.OBJECTPROPERTY,
        OWL.ONEOF,
        OWL.ONPROPERTY,
        OWL.ONTOLOGY,
        OWL.ONTOLOGYPROPERTY,
        OWL.PRIORVERSION,
        OWL.RESTRICTION,
        OWL.SAMEAS,
        OWL.SOMEVALUESFROM,
        OWL.SYMMETRICPROPERTY,
        OWL.TRANSITIVEPROPERTY,
        OWL.UNIONOF,
        OWL.VERSIONINFO,
      };

  public OWLVocabularyDecl() {}

  public Iterator<URI> values() {

    return Collections.unmodifiableList(Arrays.asList(uris)).iterator();
  }
}
