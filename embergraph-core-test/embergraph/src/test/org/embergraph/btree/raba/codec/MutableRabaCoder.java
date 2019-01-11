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
 * Created on Sep 7, 2009
 */

package org.embergraph.btree.raba.codec;

import org.embergraph.btree.Leaf;
import org.embergraph.btree.Node;
import org.embergraph.btree.raba.IRaba;
import org.embergraph.btree.raba.MutableKeyBuffer;
import org.embergraph.btree.raba.MutableValueBuffer;
import org.embergraph.io.AbstractFixedByteArrayBuffer;
import org.embergraph.io.DataOutputBuffer;

/**
 * This "codes" a raba as a {@link MutableKeyBuffer} or
 * {@link MutableValueBuffer} depending on whether it represents B+Tree keys or
 * values. This class is used by some unit tests as a convenience for
 * establishing a baseline for the performance of {@link ICodedRaba}s against
 * the core mutable {@link IRaba} implementations actually used by {@link Node}
 * and {@link Leaf}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class MutableRabaCoder implements IRabaCoder {

    /**
     * 
     */
    private static final long serialVersionUID = -7123556255775810548L;

    @Override
    public ICodedRaba decode(final AbstractFixedByteArrayBuffer data) {
        
        // Note: an alternative class is used to encode/decode.
        final IRaba raba = SimpleRabaCoder.INSTANCE.decode(data);
        
        if (raba.isKeys()) {
        
            return new KeysRabaImpl(raba, data);
            
        } else {
            
            return new ValuesRabaImpl(raba, data);
            
        }
        
    }

    @Override
    public AbstractFixedByteArrayBuffer encode(final IRaba raba,
            final DataOutputBuffer buf) {

        return encodeLive(raba, buf).data();

    }

    @Override
    public ICodedRaba encodeLive(final IRaba raba, final DataOutputBuffer buf) {

        // Note: an alternative class is used to encode/decode.
        final AbstractFixedByteArrayBuffer data = SimpleRabaCoder.INSTANCE
                .encode(raba, buf);
        
        if (raba.isKeys()) {
        
            return new KeysRabaImpl(raba, data);
            
        } else {
            
            return new ValuesRabaImpl(raba, data);
            
        }
        
    }

    /**
     * Yes.
     */
    @Override
    final public boolean isKeyCoder() {
        
        return true;
        
    }

    /**
     * Yes.
     */
    @Override
    final public boolean isValueCoder() {
        
        return true;
        
    }

    @Override
    public boolean isDuplicateKeys() {

        throw new UnsupportedOperationException();
        
    }
    
    /**
     * {@link MutableKeyBuffer} with mock implementation of {@link ICodedRaba}
     * methods.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    private static class KeysRabaImpl extends MutableKeyBuffer implements ICodedRaba {

        private final AbstractFixedByteArrayBuffer data;

        public KeysRabaImpl(final IRaba raba,
                final AbstractFixedByteArrayBuffer data) {

            super(raba.capacity(), raba);

            this.data = data;
            
        }

        public AbstractFixedByteArrayBuffer data() {
            
            return data;
            
        }
        
    }
    
    /**
     * {@link MutableValueBuffer} with mock implementation of {@link ICodedRaba}
     * methods.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan
     *         Thompson</a>
     */
    private static class ValuesRabaImpl extends MutableValueBuffer implements
            ICodedRaba {

        private final AbstractFixedByteArrayBuffer data;

        public ValuesRabaImpl(final IRaba raba,
                final AbstractFixedByteArrayBuffer data) {

            super(raba.capacity(), raba);
        
            this.data = data;
            
        }

        @Override
        public AbstractFixedByteArrayBuffer data() {
            
            return data;
            
        }
        
    }

}
