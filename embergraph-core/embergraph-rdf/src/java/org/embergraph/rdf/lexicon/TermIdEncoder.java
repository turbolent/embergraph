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
package org.embergraph.rdf.lexicon;

import org.embergraph.btree.BTree;
import org.embergraph.btree.BTree.PartitionedCounter;

/*
* An encoder/decoder for long values formed from a partition identifier in the high word and a
 * local counter in the low word where the low N bits of the long value are reversed and rotated
 * into the high N bits of the long value.
 *
 * <p>The purpose of this encoding is to cause the N high bits to vary rapidly as the local counter
 * is driven up by writes on the index partition. This has the effect of scattering writes on
 * dependent indices (those using the resulting long value as the sole or leading component of their
 * key).
 *
 * <p>Given a source RDF/XML document with M "terms" distributed uniformly over K TERM2ID index
 * partitions, each term has a uniform likelihood of setting any of the low bits of the local
 * counter. After encoding, this means that the N high-bits of encoded term identifier are uniformly
 * distributed. Assuming that the separator keys for the ID2TERM index divide the key space into
 * equally sized key-ranges, then the reads and writes on the ID2TERM index partitions will be
 * uniformly distributed as well.
 *
 * <p>The next bits in the encoded values are derived from the partition identifier followed by the
 * term identifier and therefore have a strong bias for the index partition and the sequential
 * assignment of local counter values within an index partition respectively. This means that read /
 * write access within an index partition tends to have some locality, which improves B+Tree
 * performance through several mechanisms (mainly improved cache effects, reduced copy-on-write for
 * dirty leaves and nodes, and less IO costs).
 *
 * <p>When the #of ID2TERM index partitions GT <code>2^N</code>, only a subset of those index
 * partitions can be directly selected by the N high bits with their uniform distribution. The next
 * bias is the partition identifier, which begins at ZERO (0), is inflated to (0, [1:P]), where P is
 * the #of index partitions generated by a scatter split, and grows relatively slowly thereafter as
 * index partitions are fill up and are split or are moved to redistribute the load on the cluster.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TermIdEncoder {

  /*
   * The #of low bits from the local counter that will be reversed and written into the high-bits of
   * the encoded long value.
   */
  private final int N;

  /** A mask with the {@link #N} low bits turned on. */
  private final int mask;

  public String toString() {

    return getClass().getName() + "{N=" + N + ", mask=" + Integer.toBinaryString(mask) + "}";
  }

  /*
   * Return the #of low bits from the local counter that will be reversed and written into the
   * high-bits of the encoded long value.
   */
  public int getNBits() {

    return N;
  }

  /*
   * @param N The #of low bits from the local counter that will be reversed and written into the
   *     high-bits of the encoded long value.
   */
  public TermIdEncoder(final int N) {

    if (N < 0) throw new IllegalArgumentException();

    if (N > 31) throw new IllegalArgumentException();

    this.N = N;

    /*
     * Construct the bit mask - this will have zeros in the high bits
     * that correspond to the bits of the localCounter that WILL NOT be
     * reversed and ones in the low bits that correspond to bits of the
     * localCounter that WILL be reversed.
     */
    {
      int mask = 0;

      int bit;

      for (int i = 0; i < N; i++) {

        bit = (1 << i);

        mask |= bit;
      }

      this.mask = mask;
    }
  }

  /*
   * Encode a term identifier using the configured value of {@link #getNBits() NBits}.
   *
   * @param v A 64-bit long counter value as generated by an {@link BTree.PartitionedCounter}.
   * @return A permutation of that long value in which the low <i>N</i> bits have been reversed and
   *     rotated into the high <i>N</i> bits.
   */
  public long encode(final long v) {

    if (v == 0L) {
      // 0L is reserved for NULLs.
      throw new IllegalArgumentException();
    }

    // the partition identifier.
    final long pid = 0xFFFFFFFFL & getPartitionId(v);

    // the local counter.
    final long ctr = 0xFFFFFFFFL & getLocalCounter(v);

    // the output value.
    long u = 0L;

    /*
     * Move pid to high word.
     */
    u |= pid << (32 - N);

    /*
     * Right shift the counter over the bits that are being reversed,
     * extend to a long value.
     */
    u |= ctr >>> N;

    /*
     * Use the mask to isolate the low-N bits of the counter, which are
     * then reversed into the high-N bits.
     */
    final long rev = Integer.reverse(((int) ctr) & mask);

    /*
     * Overwrite the high N bits of the long value using the reversed
     * low N bits from the local counter.
     */
    u |= rev << 32;

    return u;
  }

  /*
   * Reverses the effect of {@link #encode(long)}.
   *
   * @param u An encoded long value.
   * @return The decode long value.
   */
  public long decode(final long u) {

    // reverse high word and mask to recover the low-N bits.
    final int fwd = Integer.reverse(((int) (u >>> 32))) & mask;

    /*
     * Left-shift to make room for the (un-)reversed bits and then combine
     * them back in.
     */
    final int ctr = ((int) (u << N) | fwd);

    /*
     * Bring the partition identifier back to an int by shifting it the
     * same number of bits in the other direction.
     */
    final int pid = ((int) (u >>> (32 - N)));

    // reconstruct the long counter value.
    return combine(pid, ctr);
  }

  /*
   * Return the partition identifier from the high word of a partitioned counter.
   *
   * @param v The partitioned counter.
   * @return The high word.
   */
  public static int getPartitionId(final long v) {

    return BTree.PartitionedCounter.getPartitionId(v);
    //        return (int) (v >>> 32);

  }

  /*
   * Return the local counter from the low word of a partitioned counter.
   *
   * @param v The partitioned counter.
   * @return The low word.
   */
  public static int getLocalCounter(final long v) {

    //        return (int) v;
    return BTree.PartitionedCounter.getLocalCounter(v);
  }

  /*
   * Combines the partition identifier and the local counter using the same logic as the {@link
   * PartitionedCounter}.
   *
   * @param pid The partition identifier.
   * @param ctr The local counter.
   * @return The long counter assembled from those values.
   * @see BTree.PartitionedCounter, which performs the same operation and MUST be consistent with
   *     this method.
   */
  public static long combine(final int pid, final int ctr) {

    //        return ((long) pid) << 32 | (0xFFFFFFFFL & (long) ctr);
    return BTree.PartitionedCounter.combine(pid, ctr);
  }

  /*
   * Alternative versions used for debugging.  Package private for the unit
   * tests.
   */

  long encode2(final long v1) {
    long v2 = v1 >>> N;

    for (int b = 0; b < N; b++) {
      if ((v1 & (1L << b)) != 0) {
        final long sv = 1L << (63 - b);
        v2 |= sv;
      }
    }

    return v2;
  }

  long decode2(final long v2) {
    long v1 = v2 << N;

    for (int b = 0; b < N; b++) {
      if ((v2 & (1L << (63 - b))) != 0) {
        v1 |= 1L << b;
      }
    }

    return v1;
  }
}
