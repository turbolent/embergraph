package com.bigdata.btree.data;

import java.io.OutputStream;

import com.bigdata.btree.raba.IRaba;

/**
 * Abstract base class for mock node and leaf data implementations for unit
 * tests.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract class AbstractMockNodeData implements IAbstractNodeData {

    // mutable.
    final private IRaba keys;

    final public int getKeyCount() {

        return keys.size();

    }

    final public IRaba getKeys() {

        return keys;

    }

    public final byte[] getKey(final int index) {

        return keys.get(index);

    }

    final public void copyKey(final int index, final OutputStream os) {

        keys.copy(index, os);

    }

    protected AbstractMockNodeData(final IRaba keys) {

        if (keys == null)
            throw new IllegalArgumentException();

//        Note: The HTree IRaba for keys does NOT report true for isKeys() since
//        it does not obey any of the contract for the B+Tree keys (unordered,
//        sparse, allows duplicates and nulls, not searchable).
//        
//        if (!keys.isKeys())
//            throw new IllegalArgumentException();

        this.keys = keys;

    }

}
