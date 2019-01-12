package org.embergraph.btree.filter;

import cutthecrap.utils.striterators.FilterBase;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleCursor;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.StrengthEnum;
import org.embergraph.btree.keys.SuccessorUtil;
import org.embergraph.util.BytesUtil;

/**
 * Filter visits all {@link ITuple}s whose keys begin with any of the specified prefix(s). The filer
 * accepts a key or an array of keys that define the key prefix(s) whose completions will be
 * visited. It efficiently forms the successor of each key prefix, performs a key-range scan of the
 * key prefix, and (if more than one key prefix is given), seeks to the start of the next key-range
 * scan.
 *
 * <h4>WARNING</h4>
 *
 * <p><strong>The prefix keys MUST be formed with {@link StrengthEnum#Primary}. This is necessary in
 * order to match all keys in the index since it causes the secondary characteristics to NOT be
 * included in the prefix key even if they are present in the keys in the index.</strong> Using
 * other {@link StrengthEnum}s will result in secondary characteristics being encoded by additional
 * bytes appended to the key. This will result in scan matching ONLY the given prefix key(s) and
 * matching nothing if those prefix keys are not actually present in the index.
 *
 * <p>For example, the Unicode text "Bryan" is encoded as the <em>unsigned</em> byte[]
 *
 * <pre>
 * [43, 75, 89, 41, 67]
 * </pre>
 *
 * <p>at PRIMARY strength but as the <em>unsigned</em> byte[]
 *
 * <pre>
 * [43, 75, 89, 41, 67, 1, 9, 1, 143, 8]
 * </pre>
 *
 * <p>at IDENTICAL strength. The additional bytes for the IDENTICAL strength reflect the Locale
 * specific Unicode sort key encoding of secondary characteristics such as case. The successor of
 * the IDENTICAL strength byte[] is
 *
 * <pre>
 * [43, 75, 89, 41, 67, 1, 9, 1, 143, 9]
 * </pre>
 *
 * <p>(one was added to the last byte) which spans all keys of interest. However the successor of
 * the PRIMARY strength byte[] would
 *
 * <pre>
 * [43, 75, 89, 41, 68]
 * </pre>
 *
 * <p>and would ONLY span the single tuple whose key was "Bryan".
 *
 * <p>You can form an appropriate {@link IKeyBuilder} for the prefix keys using
 *
 * <pre>
 * Properties properties = new Properties();
 *
 * properties.setProperty(KeyBuilder.Options.STRENGTH,
 *         StrengthEnum.Primary.toString());
 *
 * prefixKeyBuilder = KeyBuilder.newUnicodeInstance(properties);
 * </pre>
 *
 * <p>Note: It is NOT trivial to define filter that may be used to accept only keys that extend the
 * prefix on a caller-defined boundary (e.g., corresponding to the encoding of a whitespace or word
 * break). There are two issues: (1) the keys are encoded so the filter needs to recognize the
 * byte(s) in the Unicode sort key that correspond to, e.g., the work boundary. (2) the keys may
 * have been encoded with secondary characteristics, in which case the boundary will not begin
 * immediately after the prefix.
 *
 * @todo Only pass the relevant elements of keyPrefix to any given index partition. It is possible
 *     that an element spans the end of an index partition, in which case the scan must resume with
 *     the next partition. There is no real way to know this without testing the next partition....
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @see <a href="http://trac.blazegraph.com/ticket/974" > Name2Addr.indexNameScan(prefix) uses scan
 *     + filter </a>
 */
public class PrefixFilter<E> extends FilterBase implements ITupleFilter<E> {

  protected static final transient Logger log = Logger.getLogger(PrefixFilter.class);

  private static final long serialVersionUID = 1828228416774862469L;

  /** The array of key prefixes to be scanned. */
  private final byte[][] keyPrefix;

  /**
   * Completion scan with a single prefix. The iterator will visit all tuples having the given key
   * prefix.
   *
   * @param keyPrefix An unsigned byte[] containing a key prefix.
   */
  public PrefixFilter(byte[] keyPrefix) {

    this(new byte[][] {keyPrefix});
  }

  /**
   * Completion scan with an array of key prefixes. The iterator will visit all tuples having the
   * first key prefix, then all tuples having the next key prefix, etc. until all key prefixes have
   * been evaluated.
   *
   * @param keyPrefix An array of unsigned byte prefixes (the elements of the array MUST be
   *     presented in sorted order and <code>null</code>s are not permitted).
   */
  public PrefixFilter(byte[][] keyPrefix) {

    if (keyPrefix == null) throw new IllegalArgumentException();

    if (keyPrefix.length == 0) throw new IllegalArgumentException();

    for (int i = 0; i < keyPrefix.length; i++) {

      if (keyPrefix[i] == null) throw new IllegalArgumentException();
    }

    this.keyPrefix = keyPrefix;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ITupleIterator<E> filterOnce(Iterator src, Object context) {

    return new PrefixFilterator<E>((ITupleCursor<E>) src, context, this);
  }

  private static class PrefixFilterator<E> implements ITupleIterator<E> {

    /**
     * The source iterator. The lower bound for the source iterator should be the first key prefix.
     * The upper bound should be the fixed length successor of the last key prefix (formed by adding
     * one bit, not by appending a <code>nul</code> byte).
     */
    private final ITupleCursor<E> src;

    private final Object context;

    private final PrefixFilter<E> filter;

    /**
     * The index of the key prefix that is currently being scanned. The entire scan is complete when
     * index == keyPrefix.length.
     */
    private int index = 0;

    /** The exclusive upper bound. This is updated each time we begin to scan another key prefix. */
    protected byte[] toKey;

    /** The current tuple. */
    private ITuple<E> current = null;

    /**
     * Completion scan.
     *
     * @param src The source iterator.
     * @param filter The filter to be applied.
     */
    public PrefixFilterator(
        final ITupleCursor<E> src, final Object context, final PrefixFilter<E> filter) {

      if (src == null) throw new IllegalArgumentException();

      if (filter == null) throw new IllegalArgumentException();

      this.src = src;

      this.context = context;

      this.filter = filter;

      this.index = 0;

      nextPrefix();
    }

    public boolean hasNext() {

      if (current != null) return true;

      /*
       * Find the next tuple having the same prefix.
       */
      while (src.hasNext()) {

        final ITuple<E> tuple = src.next();

        final byte[] key = tuple.getKey();

        if (BytesUtil.compareBytes(key, toKey) >= 0) {

          if (log.isInfoEnabled())
            log.info(
                "Scanned beyond prefix: toKey=" + BytesUtil.toString(toKey) + ", tuple=" + tuple);

          if (index + 1 < filter.keyPrefix.length) {

            // next prefix.
            index++;

            nextPrefix();

            if (current != null) {

              // found an exact prefix match.
              return true;
            }

            continue;
          }

          if (log.isInfoEnabled()) log.info("No more prefixes.");

          return false;
        }

        current = tuple;

        // found another tuple that is a completion of the current prefix.
        return true;
      }

      // no more tuples (at least in this index partition).

      if (log.isInfoEnabled()) log.info("No more tuples.");

      return false;
    }

    /** Start a sub-scan of the key prefix at the current {@link #index}. */
    protected void nextPrefix() {

      final byte[] prefix = filter.keyPrefix[index];

      // make a note of the exclusive upper bound for that prefix.
      toKey = SuccessorUtil.successor(prefix.clone());

      /*
       * Seek to the inclusive lower bound for that key prefix.
       *
       * Note: if we seek to a key that has a visitable tuple then that
       * will be the next tuple to be returned.
       */
      current = src.seek(prefix);
      //            current = src.tuple();

      if (log.isInfoEnabled()) {

        log.info(
            "index=" + index + ", prefix=" + BytesUtil.toString(prefix) + ", current=" + current);
      }
    }

    public ITuple<E> next() {

      if (!hasNext()) {

        throw new NoSuchElementException();
      }

      assert current != null;

      final ITuple<E> t = current;

      current = null;

      return t;
    }

    public void remove() {

      src.remove();
    }
  }
}
