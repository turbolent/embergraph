/*

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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
 * Created on Aug 7, 2009
 */

package org.embergraph.btree.raba;

import java.io.DataInput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import com.bigdata.btree.ResultSet;
import com.bigdata.btree.proc.IIndexProcedure;
import com.bigdata.io.ByteArrayBuffer;

/**
 * Interface for random access to a logical byte[][]s. This is primarily used
 * for B+Tree keys and values, but is also used when serializing keys and values
 * for {@link IIndexProcedure}s. The interface defines optional operations for
 * mutation. If mutation is supported, then {@link #isReadOnly()} will return
 * <code>false</code>. An {@link IRaba} instance either stores B+Tree keys and
 * supports {@link #search(byte[])} -or- stores B+Tree values and allows
 * <code>null</code>s but does not support search. for storing <code>null</code>
 * s and search are mutually exclusive. Some {@link IRaba} implementations may
 * be used for either purpose, but the {@link IRaba} instance is always either
 * B+Tree keys or B+Tree values. In both cases, the {@link IRaba} stores
 * variable length byte[]s. However, when an {@link IRaba} stores B+Tree keys,
 * the byte[]s interpreted as <code>unsigned byte[]</code>s for the purpose of
 * {@link #search(byte[])}.
 * 
 * <h3>B+Tree keys</h3>
 * 
 * When used for B+Tree keys, the interface provides operations on an ordered
 * set of variable length <code>unsigned byte[]</code> keys and
 * <code>null</code>s ARE NOT permitted.
 * 
 * <h3>B+Tree values</h3>
 * 
 * When used for B+Tree values, the interface provides operations on an
 * unordered collection of variable length <code>byte[]</code>s, does NOT
 * support {@link #search(byte[])} and <code>null</code>s are allowed.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * 
 * @todo consider discarding all of the {@link #add(byte[])} methods. It is
 *       typically easier and more convenient to directly manage the existing
 *       {@link MutableKeyBuffer} and {@link MutableValueBuffer}. However, there
 *       are also {@link MutableKeysRaba} and {@link MutableValueRaba}
 *       implementations which are, I believe, only used by the
 *       {@link ResultSet}.
 */
public interface IRaba extends Iterable<byte[]> {

    /**
     * Return <code>true</code> if this implementation is read-only.
     */
    boolean isReadOnly();

    /**
     * When <code>true</code> the {@link IRaba} supports search and elements are
     * interpreted as <code>unsigned byte[]</code>s (B+Tree keys). For this case
     * the application MUST ensure that the elements are maintained in
     * <code>unsigned byte[]</code> order and that duplicates byte[]s are not
     * stored.
     * <p>
     * When <code>false</code>, the {@link IRaba} allows <code>null</code>s and
     * the elements are interpreted as <code>signed byte[]</code> (B+Tree
     * values).
     * 
     * @return <code>true</code> if the {@link IRaba} represents B+Tree keys and
     *         <code>false</code> if it represents B+Tree values.
     */
    boolean isKeys();

    /*
     * TODO This could be added to differentiate between IRaba implementations
     * that do / do not support duplicate keys.  The ones used with the HTree do.
     * The rest do not.
     */
//    /**
//     * When <code>true</code>, then {@link IRaba} supports duplicate keys.
//     * 
//     * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/763" >
//     *      Stochastic Results With Analytic Query Mode </a>
//     */
//    boolean isDuplicateKeys();
    
    /**
     * The capacity of the logical byte[][].
     * 
     * @todo Coded rabas generally impose <code>capacity == size</code> while
     *       only mutable rabas have a sense of capacity GTE size. Some of the
     *       unit tests assume that all rabas are dimensions based on the
     *       branching factor, which is no longer true. By moving this method
     *       onto the mutable IRaba implementations we would uncover those
     *       assumptions and clean things up a bit more.
     */
    public int capacity();

    /**
     * The #of entries in the logical byte[][].
     */
    public int size();

    /**
     * True iff the logical byte[][] is empty.
     */
    public boolean isEmpty();

    /**
     * True iff the logical byte[][] is full.
     */
    public boolean isFull();

    /**
     * Return <code>true</code> iff the byte[] at that index is
     * <code>null</code>. If {@link IRaba#isKeys()} would return
     * <code>true</code> then this method MUST return <code>false</code> since
     * <code>null</code>s are not permitted for B+Tree keys.
     * 
     * @param index
     *            The index in [0:{@link #size()}-1].
     * @throws IndexOutOfBoundsException
     *             unless index is in [0:{@link #size()}-1].
     */
    public boolean isNull(int index);

    /**
     * The length of the byte[] at that index.
     * 
     * @param index
     *            The index in [0:{@link #size()}-1].
     * 
     * @return The length of the byte[] at that index.
     * 
     * @throws NullPointerException
     *             if the key at that index is <code>null</code>.
     * @throws IndexOutOfBoundsException
     *             unless index is in [0:{@link #size()}-1].
     */
    public int length(int index);

    /**
     * Return the byte[] at the specified index. For greater efficiency,
     * implementations MAY return a reference to an internal the byte[].
     * 
     * @param index
     *            The index in [0:{@link #size()}-1].
     * 
     * @return The byte[] value at that index and <code>null</code> if a
     *         <code>null</code> value was stored at that index.
     * 
     * @throws IndexOutOfBoundsException
     *             unless index is in [0:{@link #size()}-1].
     */
    public byte[] get(int index);

    /**
     * Copy the value at the specified index onto the output stream. This is
     * often used with an {@link ByteArrayBuffer} so that the same backing
     * byte[] can be overwritten by each visited key.
     * 
     * @param index
     *            The index in [0:{@link #size()}-1].
     * @param out
     *            The output stream onto which the key will be copied.
     * 
     * @return The #of bytes copied.
     * 
     * @throws IndexOutOfBoundsException
     *             unless index is in [0:{@link #size()}-1].
     * @throws NullPointerException
     *             if the byte[] value at that index is <code>null</code>.
     * @throws RuntimeException
     *             if the {@link OutputStream} throws an {@link IOException}
     *             (generally the {@link OutputStream} is writing onto a byte[]
     *             so it is more convenient to masquerade this exception).
     */
    public int copy(int index, OutputStream os);

    /**
     * Iterator visits the byte[] elements in the view order. If an element is
     * <code>null</code>, then the iterator will report a <code>null</code> for
     * that element.
     */
    @Override
    public Iterator<byte[]> iterator();

    /*
     * Mutation operations (optional).
     */

    /**
     * Set the byte[] value at the specified index (optional operation).
     * 
     * @param index
     *            The index in [0:{@link #size()}-1].
     * @param a
     *            The byte[] value.
     * 
     * @throws IndexOutOfBoundsException
     *             unless index is in [0:{@link #size()}-1].
     * @throws IllegalArgumentException
     *             if the value is <code>null</code> and null values are not
     *             supported by this implementation.
     */
    public void set(int index, byte[] a);

    /**
     * Append a byte[] value to the end of the logical byte[][] (optional
     * operation).
     * 
     * @param a
     *            A value.
     * 
     * @return The #of values in the logical byte[][] (postcondition).
     * 
     * @throws IllegalArgumentException
     *             if the value is <code>null</code> and null values are not
     *             supported by this implementation.
     */
    public int add(byte[] a);

    /**
     * Append a byte[] value to the end of the logical byte[][] (optional
     * operation).
     * 
     * @param value
     *            A value
     * @param off
     *            The offset of the first byte to be copied.
     * @param len
     *            The #of bytes to be copied.
     * 
     * @return The #of values in the logical byte[][] (postcondition).
     * 
     * @throws IllegalArgumentException
     *             if the value is <code>null</code>.
     */
    public int add(byte[] value, int off, int len);

    /**
     * Append a byte[] value to the end of the logical byte[][] (optional
     * operation).
     * 
     * @param in
     *            The input stream from which the byte[] will be read.
     * @param len
     *            The #of bytes to be read.
     * 
     * @return The #of values in the logical byte[][] (postcondition).
     * 
     * @throws IllegalArgumentException
     *             if <i>in</i> is <code>null</code>.
     * 
     * @todo also define variant of {@link #set(int, byte[])} that copies bytes
     *       from an input stream?
     */
    public int add(DataInput in, int len) throws IOException;

    /*
     * Search (optional).
     */

    /**
     * Search for the given <i>searchKey</i> in the key buffer (optional
     * operation). Whether or not search is supported depends on whether the
     * logical byte[][] is <em>ordered</em>. However, the efficiency of search,
     * where supported, depends on the implementation. Some implementations
     * support binary search of the coded byte[] values. Others may require a
     * mixture of binary and linear search, etc.
     * <p>
     * 
     * <pre>
     * entryIndex = -entryIndex - 1
     * </pre>
     * 
     * or just
     * 
     * <pre>
     * entryIndex = -entryIndex
     * </pre>
     * 
     * if you are looking for the first key after the searchKey.
     * </p>
     * 
     * @param searchKey
     *            The search key.
     * 
     * @return index of the search key, if it is found; otherwise,
     *         <code>(-(insertion point) - 1)</code>. The insertion point is
     *         defined as the point at which the key would be inserted. Note
     *         that this guarantees that the return value will be >= 0 if and
     *         only if the key is found.
     * 
     * @exception IllegalArgumentException
     *                if the searchKey is null.
     * 
     * @throws UnsupportedOperationException
     *             if search is not supported.
     */
    int search(byte[] searchKey);

}
