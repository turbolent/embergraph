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
 * Created on Aug 14, 2012
 */
package org.embergraph.rdf.sparql.ast.cache;

import info.aduna.iteration.CloseableIteration;
import java.util.Set;
import org.apache.log4j.Logger;
import org.embergraph.bop.IVariable;
import org.embergraph.rdf.model.EmbergraphValue;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;

/*
 * Collects and reports the distinct bindings observed on some set of variables.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class DescribeBindingsCollector
    implements CloseableIteration<BindingSet, QueryEvaluationException> {

  private static final transient Logger log = Logger.getLogger(DescribeBindingsCollector.class);

  private final IVariable<?>[] originalVars;
  private final Set<EmbergraphValue> describedResources;
  private final CloseableIteration<BindingSet, QueryEvaluationException> src;
  private boolean open = true;

  /*
   * @param originalVars The set of variables whose distinct bound values will be reported.
   * @param describedResources The set of distinct bound values for those variables (a high
   *     concurrency, thread-safe set).
   * @param src The source iterator.
   */
  public DescribeBindingsCollector(
      final Set<IVariable<?>> originalVars,
      final Set<EmbergraphValue> describedResources,
      final CloseableIteration<BindingSet, QueryEvaluationException> src) {

    if (originalVars == null) throw new IllegalArgumentException();

    if (originalVars.isEmpty()) throw new IllegalArgumentException();

    if (describedResources == null) throw new IllegalArgumentException();

    if (src == null) throw new IllegalArgumentException();

    this.originalVars = originalVars.toArray(new IVariable[0]);

    this.describedResources = describedResources;

    this.src = src;
  }

  @Override
  public void close() {

    open = false;
  }

  @Override
  public boolean hasNext() throws QueryEvaluationException {

    if (!src.hasNext()) {

      close();

      return false;
    }

    return true;
  }

  @Override
  public BindingSet next() throws QueryEvaluationException {

    if (!open) throw new QueryEvaluationException("Closed");

    final BindingSet bs = src.next();

    for (IVariable<?> var : originalVars) {

      final Binding binding = bs.getBinding(var.getName());

      if (binding == null) continue;

      final EmbergraphValue boundValue = (EmbergraphValue) binding.getValue();

      if (boundValue != null) {

        if (describedResources.add(boundValue)) {

          if (log.isInfoEnabled()) {

            log.info("Will describe: var=" + var + ",boundValue=" + boundValue);
          }
        }
      }
    }

    return bs;
  }

  @Override
  public void remove() {

    throw new UnsupportedOperationException();
  }
}
