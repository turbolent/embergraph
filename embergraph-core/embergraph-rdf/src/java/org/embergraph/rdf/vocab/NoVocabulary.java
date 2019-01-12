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
 * Created on Aug 26, 2008
 */

package org.embergraph.rdf.vocab;

/*
* An empty {@link Vocabulary}.
 *
 * <p>Note: The use of this class is no longer recommended. It was used historically when the
 * lexicon should be empty (no pre-declared terms). However, the {@link Vocabulary} now provides a
 * mechanism for fast and compact inlining of URIs into the statement indices. Using an empty {@link
 * Vocabulary} therefore simply robs you of the opportunity to have more compact encodings of those
 * URIs.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class NoVocabulary extends BaseVocabulary {

  /** */
  private static final long serialVersionUID = -5023634139839648847L;

  /** De-serialization ctor. */
  public NoVocabulary() {}

  /** @param Namespace */
  public NoVocabulary(final String namespace) {

    super(namespace);
  }

  /** NOP. */
  @Override
  protected void addValues() {}
}
