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
package org.embergraph.bop;

import java.util.Properties;
import java.util.UUID;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.bop.engine.BlockingBufferWithStats;
import org.embergraph.bop.engine.IRunningQuery;
import org.embergraph.bop.engine.MockRunningQuery;
import org.embergraph.bop.solutions.MockQuery;
import org.embergraph.bop.solutions.MockQueryContext;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.LocalTripleStore;
import org.embergraph.relation.accesspath.IAsynchronousIterator;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.accesspath.ThickAsynchronousIterator;

/*
* Mock utility for test cases.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public class TestMockUtility {

  /*
   * Creates a mocked local triple (memory) store with the given namespace, with unisolated
   * transactions.
   *
   * @param namespace
   * @return
   */
  public static AbstractTripleStore mockTripleStore(final String namespace) {

    final Properties properties = new Properties();
    properties.setProperty(org.embergraph.journal.Options.BUFFER_MODE, BufferMode.MemStore.name());

    final Journal store = new Journal(properties);

    final AbstractTripleStore kb =
        new LocalTripleStore(store, namespace, ITx.UNISOLATED, properties);

    kb.create();
    store.commit();

    return kb;
  }

  /*
   * Creates a mocked context associated with the given abstract triple store, with index manager
   * properly initialized.
   *
   * @param kb
   * @return
   */
  public static BOpContext<IBindingSet> mockContext(final AbstractTripleStore kb) {

    final UUID queryId = UUID.randomUUID();
    final IQueryContext queryContext = new MockQueryContext(queryId);
    final IRunningQuery runningQuery =
        new MockRunningQuery(null /* fed */, kb.getIndexManager() /* indexManager */, queryContext);

    final BOpStats stats = new BOpStats();
    final PipelineOp mockQuery = new MockQuery();
    final IAsynchronousIterator<IBindingSet[]> source =
        new ThickAsynchronousIterator<IBindingSet[]>(new IBindingSet[][] {});
    final IBlockingBuffer<IBindingSet[]> sink =
        new BlockingBufferWithStats<IBindingSet[]>(mockQuery, stats);
    final BOpContext<IBindingSet> context =
        new BOpContext<IBindingSet>(
            runningQuery,
            -1 /* partitionId */,
            stats,
            mockQuery /* op */,
            true /* lastInvocation */,
            source,
            sink,
            null /* sink2 */);

    return context;
  }
}
