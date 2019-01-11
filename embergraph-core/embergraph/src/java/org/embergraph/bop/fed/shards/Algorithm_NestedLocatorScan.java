package org.embergraph.bop.fed.shards;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.embergraph.bop.IBindingSet;
import org.embergraph.mdi.IMetadataIndex;
import org.embergraph.mdi.PartitionLocator;
import org.embergraph.relation.accesspath.IBuffer;
import org.embergraph.striterator.IKeyOrder;
import org.embergraph.util.BytesUtil;

/**
 * This does a locatorScan for each binding set. This is a general purpose
 * technique, but it will issue one query to the {@link IMetadataIndex} per
 * source {@link IBindingSet}.
 * 
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/457">
 *      "No such index" on cluster under concurrent query workload </a>
 */
class Algorithm_NestedLocatorScan<E extends IBindingSet, F> implements
        IShardMapper<E, F> {

    static transient private final Logger log = Logger
            .getLogger(Algorithm_NestedLocatorScan.class);

    private final MapBindingSetsOverShardsBuffer<E, F> op;

    public Algorithm_NestedLocatorScan(
            final MapBindingSetsOverShardsBuffer<E, F> op) {

        this.op = op;

    }

    public void mapOverShards(final Bundle<F>[] bundles) {

		/*
		 * Sort the binding sets in the chunk by the fromKey associated with
		 * each asBound predicate.
		 */
		Arrays.sort(bundles);

		// The most recently discovered locator.
		PartitionLocator current = null;
		// The key order for [current]
		IKeyOrder<?> currentKeyOrder = null;
		
//		// The list of binding sets which are bound for the current locator.
//		List<IBindingSet> list = new LinkedList<IBindingSet>();
		
		final Iterator<Bundle<F>> bitr = Arrays.asList(bundles).iterator();

		while(bitr.hasNext()) {
			
			final Bundle<F> bundle = bitr.next();

			if (current != null
			        && currentKeyOrder == bundle.keyOrder // same s/o index
					&& BytesUtil.rangeCheck(bundle.fromKey, current
							.getLeftSeparatorKey(), current
							.getRightSeparatorKey())
					&& BytesUtil.rangeCheck(bundle.toKey, current
							.getLeftSeparatorKey(), current
							.getRightSeparatorKey())) {

                /*
                 * Optimization when the bundle fits inside of the last index
                 * partition scanned (this optimization is only possible when
                 * the asBound predicate will be mapped onto a single index
                 * partition, but this is a very common case since we try to
                 * choose selective indices for access paths).
                 * 
                 * Note: The bundle MUST be for the scale-out index associated
                 * with the last PartitionLocator. We enforce this constraint by
                 * tracking the IKeyOrder for the last PartitionLocator and
                 * verifying that the Bundle is associated with the same
                 * IKeyOrder.
                 * 
                 * Note: Bundle#compareTo() is written to group together the
                 * [Bundle]s first by their IKeyOrder and then by their fromKey.
                 * That provides the maximum possibility of reuse of the last
                 * PartitionLocator. It also provides ordered within scale-out
                 * index partition locator scans.
                 */
				
                final IBuffer<IBindingSet[]> sink = op.getBuffer(current);

                sink.add(new IBindingSet[] { bundle.bindingSet });

                continue;
                
			}
			
            /*
             * Locator scan for the index partitions for that predicate as
             * bound.
             */
            final Iterator<PartitionLocator> itr = op.locatorScan(
                    bundle.keyOrder, bundle.fromKey, bundle.toKey);

            // Clear the old partition locator.
            current = null;

            // Update key order for the partition that we are scanning.
            currentKeyOrder = bundle.keyOrder;
            
            // Scan locators.
            while (itr.hasNext()) {

                final PartitionLocator locator = current = itr.next();

                if (log.isTraceEnabled())
					log.trace("adding bindingSet to buffer" + ": asBound="
							+ bundle.asBound + ", partitionId="
							+ locator.getPartitionId() + ", dataService="
							+ locator.getDataServiceUUID() + ", bindingSet="
							+ bundle.bindingSet);

                final IBuffer<IBindingSet[]> sink = op.getBuffer(locator);

                sink.add(new IBindingSet[] { bundle.bindingSet });

            }

        }
        
    }

}
