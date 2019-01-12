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
package org.embergraph.ha;

import org.embergraph.journal.IIndexManager;

@SuppressWarnings("serial")
public abstract class IndexManagerCallable<T> implements
      IIndexManagerCallable<T> {

   private transient IIndexManager indexManager;

   public IndexManagerCallable() {

   }

   @Override
   public void setIndexManager(final IIndexManager indexManager) {
      this.indexManager = indexManager;
   }

   @Override
   public IIndexManager getIndexManager() {
      final IIndexManager tmp = this.indexManager;
      if (tmp == null)
         throw new IllegalStateException();

      return tmp;
   }
}
