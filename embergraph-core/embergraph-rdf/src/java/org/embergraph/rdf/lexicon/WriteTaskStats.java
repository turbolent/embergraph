package org.embergraph.rdf.lexicon;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.embergraph.counters.CAT;

/*
 * Class for reporting various timings for writes on the lexicon indices.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class WriteTaskStats {

  /*
   * The #of distinct terms lacking a pre-assigned term identifier. If writes were permitted, then
   * this is also the #of terms written onto the index.
   */
  final AtomicLong ndistinct = new AtomicLong();

  /** time to convert unicode terms to byte[] sort keys. */
  final CAT keyGenTime = new CAT();

  /** time to sort terms by assigned byte[] keys. */
  final CAT keySortTime = new CAT();

  /** time to insert terms into indices. */
  final AtomicLong indexTime = new AtomicLong();

  /** time on the forward index. */
  long forwardIndexTime;

  /** time on the reverse index. */
  long reverseIndexTime;

  /** time on the terms index. */
  long termsIndexTime;

  /** time to insert terms into the text indexer. */
  final AtomicLong fullTextIndexTime = new AtomicLong();

  /** The total size of all hash collisions buckets examined). */
  final CAT totalBucketSize = new CAT();

  /** The size of the largest hash collision bucket encountered. */
  final AtomicInteger maxBucketSize = new AtomicInteger();

  /** The #of terms that could not be resolved (iff readOnly == true). */
  final AtomicInteger nunknown = new AtomicInteger();

  public String toString() {
    String sb = getClass().getSimpleName()
        + "{ndistinct=" + ndistinct
        + ",keyGenTime=" + keyGenTime + "ms"
        + ",keySortTime=" + keySortTime + "ms"
        + ",indexTime=" + indexTime + "ms"
        + ",t2idIndexTime=" + forwardIndexTime + "ms"
        + ",id2tIndexTime=" + reverseIndexTime + "ms"
        + ",termsIndexTime=" + termsIndexTime + "ms"
        + ",fullTextIndexTime=" + fullTextIndexTime + "ms"
        + ",totalBucketSize=" + totalBucketSize
        + ",maxBucketSize=" + maxBucketSize
        + ",nunknown=" + nunknown
        + "}";
    return sb;
  }
}
