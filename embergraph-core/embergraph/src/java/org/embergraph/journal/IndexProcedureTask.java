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
 * Created on Jan 10, 2008
 */
package org.embergraph.journal;

import org.embergraph.btree.proc.IIndexProcedure;

/*
* Class provides an adaptor allowing a {@link IIndexProcedure} to be executed on an {@link
 * IConcurrencyManager}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class IndexProcedureTask<T> extends AbstractTask<T> {

  private final IIndexProcedure<T> proc;

  public IndexProcedureTask(
      final ConcurrencyManager concurrencyManager,
      final long startTime,
      final String name,
      final IIndexProcedure<T> proc) {

    super(concurrencyManager, startTime, name);

    if (proc == null) throw new IllegalArgumentException();

    this.proc = proc;
  }

  @Override
  public final T doTask() throws Exception {

    return proc.apply(getIndex(getOnlyResource()));
  }

  /** Returns the name of the {@link IIndexProcedure} that is being executed. */
  @Override
  protected final String getTaskName() {

    return proc.getClass().getName();
  }
}
