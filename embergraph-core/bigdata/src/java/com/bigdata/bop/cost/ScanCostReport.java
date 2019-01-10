package com.bigdata.bop.cost;

import java.io.Serializable;


/**
 * A report on the expected cost of an index key range scan.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan
 *         Thompson</a>
 * @version $Id$
 */
public class ScanCostReport implements Serializable {

	/**
	 * @todo should be either Externalizable and explicitly managed versioning
	 *       or Serializable with a public interface for versioning.
	 */
	private static final long serialVersionUID = 1L;

	/**
     * The fast range count.
     */
    public final long rangeCount;
    
    /**
     * The #of index partitions in the scan.
     */
    public final long shardCount;

    /**
     * The expected cost of the scan (milliseconds).
     */
    public final double cost;

    /**
     * 
     * @param rangeCount
     *            The fast range count.
     * @param cost
     *            The expected cost for the scan (milliseconds).
     */
    public ScanCostReport(final long rangeCount, final double cost) {

        if (rangeCount < 0)
            throw new IllegalArgumentException();

        if (cost < 0)
            throw new IllegalArgumentException();
        
        this.rangeCount = rangeCount;

        this.shardCount = 1;
        
        this.cost = cost;

    }

    /**
     * 
     * @param rangeCount
     *            The fast range count.
     * @param shardCount
     *            The #of index partitions in the scan.
     * @param cost
     *            The expected cost for the scan (milliseconds).
     */
    public ScanCostReport(final long rangeCount, final long shardCount,
            final double cost) {

        this.rangeCount = rangeCount;

        this.shardCount = shardCount;

        this.cost = cost;

    }

    /**
     * Human readable representation.
     */
    public String toString() {
        return super.toString() + //
                "{rangeCount=" + rangeCount + //
                ",shardCount=" + shardCount + //
                ",cost=" + cost + //
                "}";
    }

}
