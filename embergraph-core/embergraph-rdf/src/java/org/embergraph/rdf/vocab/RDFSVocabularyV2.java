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
 * Created on Oct 28, 2007
 */

package org.embergraph.rdf.vocab;

import org.embergraph.rdf.vocab.decls.RDFVocabularyDecl;

/*
 * Extended vocabulary to include {@link VoidVocabularyDecl}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: RDFSVocabulary.java 4632 2011-06-06 15:11:53Z thompsonbry $
 */
public class RDFSVocabularyV2 extends RDFSVocabulary {

  /** De-serialization ctor. */
  public RDFSVocabularyV2() {

    super();
  }

  /*
   * Used by {@link AbstractTripleStore#create()}.
   *
   * @param namespace The namespace of the KB instance.
   */
  public RDFSVocabularyV2(final String namespace) {

    super(namespace);
  }

  @Override
  protected void addValues() {

    super.addValues();

    addDecl(new RDFVocabularyDecl());
  }
}
