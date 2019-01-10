package com.bigdata.rdf.internal;

import java.util.Comparator;

import com.bigdata.rdf.model.BigdataValue;

/**
 * Places {@link BigdataValue}s into an ordering determined by their assigned
 * {@link BigdataValue#getIV() IVs} (internal values).
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 * @see BigdataValue#getIV()
 */
public class TermIVComparator implements Comparator<BigdataValue> {

    public static final transient Comparator<BigdataValue> INSTANCE =
        new TermIVComparator();

    /**
     * Note: comparison avoids possible overflow of <code>long</code> by
     * not computing the difference directly.
     */
    public int compare(final BigdataValue term1, final BigdataValue term2) {

        final IV<?,?> iv1 = term1.getIV();
        final IV<?,?> iv2 = term2.getIV();
        
        if (iv1 == null && iv2 == null)
            return 0;
        if (iv1 == null)
            return -1;
        if (iv2 == null)
            return 1;
        
        return iv1.compareTo(iv2);

    }

}