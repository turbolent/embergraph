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
 * Created on Apr 2, 2008
 */

package org.embergraph.btree.keys;

/**
 * A key-value pair. Comparison places the {@link KV} tuples into an order based
 * on the interpretation of their {@link #key}s as unsigned byte[]s. This may
 * be used to perform a correlated sort of keys and values.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class KV extends KVO<Void> { //implements Comparable<KV> {

//    final public byte[] key;
//
//    final public byte[] val;

    /**
     * @param key
     *            The unsigned byte[] key (required).
     * @param val
     *            The serialized representation of the value (optional).
     */
    public KV(final byte[] key, final byte[] val) {

        super(key, val, null/* obj */);
        
//        this.key = key;
//
//        this.val = val;

    }

//    public int compareTo(final KV arg0) {
//
//        return BytesUtil.compareBytes(key, arg0.key);
//
//    }

}
