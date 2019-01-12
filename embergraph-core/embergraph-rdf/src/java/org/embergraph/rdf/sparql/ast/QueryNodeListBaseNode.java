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
 * Created on Aug 17, 2011
 */

package org.embergraph.rdf.sparql.ast;

import java.util.Iterator;
import java.util.Map;
import org.embergraph.bop.BOp;

/**
 * Base class for AST nodes which model an ordered list of children.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class QueryNodeListBaseNode<E extends IQueryNode> extends QueryNodeBase
    implements Iterable<E> {

  /** */
  private static final long serialVersionUID = 1L;

  public QueryNodeListBaseNode() {
    super();
  }

  /** Deep copy constructor. */
  public QueryNodeListBaseNode(final QueryNodeListBaseNode<E> op) {

    super(op);
  }

  /** Shallow copy constructor. */
  public QueryNodeListBaseNode(final BOp[] args, final Map<String, Object> anns) {

    super(args, anns);
  }

  public void add(final E e) {

    if (e == null) throw new IllegalArgumentException();

    addArg(e);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Iterator<E> iterator() {

    return (Iterator) argIterator();
  }

  public int size() {

    return arity();
  }

  public boolean isEmpty() {

    return size() == 0;
  }

  public String toString(final int indent) {

    final StringBuilder sb = new StringBuilder();

    for (IQueryNode node : this) {

      sb.append(node.toString(indent + 1));
    }

    return sb.toString();
  }
}
