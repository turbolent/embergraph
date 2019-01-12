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

import cutthecrap.utils.striterators.EmptyIterator;
import cutthecrap.utils.striterators.Striterator;
import java.util.Iterator;
import org.embergraph.rdf.vocab.VocabularyDecl;
import org.openrdf.model.URI;

/**
 * Vocabulary and namespace for Dublin Core, including:
 *
 * <ul>
 *   <li>{@link DCTermsVocabularyDecl}
 *   <li>{@link DCElementsVocabularyDecl}
 * </ul>
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: SesameVocabularyDecl.java 4628 2011-06-04 22:01:57Z thompsonbry $
 * @see DCTermsVocabularyDecl
 * @see DCElementsVocabularyDecl
 */
public class DCAllVocabularyDecl implements VocabularyDecl {

  public DCAllVocabularyDecl() {}

  public Iterator<URI> values() {

    return new Striterator(new EmptyIterator())
        .append(new DCTermsVocabularyDecl().values())
        .append(new DCElementsVocabularyDecl().values());
  }
}
