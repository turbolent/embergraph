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
import org.openrdf.model.vocabulary.RDF;

/**
 * Vocabulary and namespace for RDF.
 *
 * @see http://www.w3.org/1999/02/22-rdf-syntax-ns#
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RDFVocabularyDecl implements VocabularyDecl {

  private static final URI[] uris =
      new URI[] {
        new URIImpl(RDF.NAMESPACE),
        RDF.ALT,
        RDF.BAG,
        RDF.FIRST,
        RDF.LI,
        RDF.LIST,
        RDF.NIL,
        RDF.OBJECT,
        RDF.PREDICATE,
        RDF.PROPERTY,
        RDF.REST,
        RDF.SEQ,
        RDF.STATEMENT,
        RDF.SUBJECT,
        RDF.TYPE,
        RDF.VALUE,
        RDF.XMLLITERAL,
      };

  public RDFVocabularyDecl() {}

  public Iterator<URI> values() {

    return Collections.unmodifiableList(Arrays.asList(uris)).iterator();
  }
}
