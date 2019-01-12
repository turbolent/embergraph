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
package org.embergraph.rdf.internal.impl.bnode;

import org.embergraph.rdf.internal.DTE;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.AbstractInlineIV;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.model.BNode;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;

/*
* Class for inline RDF blank nodes.
 *
 * <p>{@inheritDoc}
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: TestEncodeDecodeKeys.java 2753 2010-05-01 16:36:59Z thompsonbry $
 * @see AbstractTripleStore.Options
 */
public abstract class AbstractBNodeIV<V extends EmbergraphBNode, T> extends AbstractInlineIV<V, T>
    implements BNode {

  /** */
  private static final long serialVersionUID = -4560216387427028030L;

  public AbstractBNodeIV(final DTE dte) {

    super(VTE.BNODE, dte);
  }

  public V asValue(final LexiconRelation lex) {

    V bnode = getValueCache();

    if (bnode == null) {

      final ValueFactory f = lex.getValueFactory();

      bnode = (V) f.createBNode(stringValue());

      bnode.setIV(this);

      setValue(bnode);
    }

    return bnode;
  }

  /** Implements {@link Value#stringValue()}. */
  @Override
  public String stringValue() {

    return getID();
  }
}
