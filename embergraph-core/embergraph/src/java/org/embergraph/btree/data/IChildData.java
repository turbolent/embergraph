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
 * Created on May 2nd, 2011
 */
package org.embergraph.btree.data;

/**
 * Interface for data access to children of an index node.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IChildData {

  /**
   * The #of children of this node. Either all children will be nodes or all children will be
   * leaves. The #of children of a node MUST be <code>{@link IAbstractNodeData#getKeyCount()}+1
   * </code>
   *
   * @return The #of children of this node.
   */
  int getChildCount();

  /**
   * Return the persistent addresses of the specified child node.
   *
   * @param index The index of the child in [0:nkeys].
   * @return The persistent child address -or- zero(0L) if the child is not persistent.
   */
  long getChildAddr(int index);
}
