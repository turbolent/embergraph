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
 * Created on Jul 1, 2008
 */

package org.embergraph.relation.rule.eval;

import java.io.Serializable;
import org.embergraph.relation.accesspath.IBuffer;
import org.embergraph.relation.rule.IRule;

/*
 * A factory for objects that handle the execution of an {@link IRule}. This interface is {@link
 * Serializable} since instances of the interface must travel with the {@link IRule} to which they
 * are attached.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IRuleTaskFactory extends Serializable {

  /*
   * The object will be used to evaluate the rule for the {@link IRule}.
   *
   * @param rule The rule (MAY have been specialized since it was declared).
   * @param joinNexus Encapsulates various important information required for join operations.
   * @param buffer The buffer onto which chunks of computed {@link ISolution}s for the {@link IRule}
   *     must be written.
   * @return <code>null</code> unless custom evaluation is desired.
   */
  IStepTask newTask(IRule rule, IJoinNexus joinNexus, IBuffer<ISolution[]> buffer);
}