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
 * Created on Jun 27, 2008
 */

package org.embergraph.relation.rule.eval;

import java.util.concurrent.Callable;
import org.embergraph.striterator.IChunkedOrderedIterator;

/**
 * Interface for a task that executes a (complex) program (vs a single rule).
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IProgramTask extends Callable<Object> {

  /**
   * The return will be either an {@link IChunkedOrderedIterator} (for {@link ActionEnum#Query}) or
   * a {@link Long} element mutation count (for {@link ActionEnum#Insert} or {@link
   * ActionEnum#Delete}).
   *
   * @return
   * @throws Exception
   */
  public Object call() throws Exception;
}
