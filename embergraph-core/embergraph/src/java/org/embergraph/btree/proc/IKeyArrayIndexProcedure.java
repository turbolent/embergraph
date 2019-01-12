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
package org.embergraph.btree.proc;

import org.embergraph.btree.raba.IRaba;

/*
 * Interface for procedures that are mapped across one or more index partitions based on an array of
 * keys. The keys are interpreted as variable length unsigned byte[]s and MUST be in sorted order.
 * The {@link ClientIndexView} will transparently break down the procedure into one procedure per
 * index partition based on the index partitions spanned by the keys.
 *
 * <p>Note: Implementations of this interface MUST declare an {@link
 * AbstractKeyArrayIndexProcedureConstructor} that will be used to create the instances of the
 * procedure mapped onto the index partitions.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IKeyArrayIndexProcedure<T> extends IIndexProcedure<T> {

  /*
   * The keys.
   *
   * @return The keys and never <code>null</code>.
   */
  IRaba getKeys();

  /*
   * The values.
   *
   * @return The values -or- <code>null</code> if no values were associated with the {@link
   *     IIndexProcedure}.
   */
  IRaba getValues();
}
