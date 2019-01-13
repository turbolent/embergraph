package org.embergraph.bop.fed.shards;

import java.util.Arrays;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IPredicate;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.striterator.IKeyOrder;
import org.embergraph.util.BytesUtil;

/*
 * Helper class used to place the binding sets into order based on the {@link #fromKey} associated
 * with the {@link #asBound} predicate.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
class Bundle<F> implements Comparable<Bundle<F>> {

  /** The binding set. */
  final IBindingSet bindingSet;

  /** The asBound predicate. */
  final IPredicate<F> asBound;

  final IKeyOrder<F> keyOrder;

  /** The fromKey generated from that asBound predicate. */
  final byte[] fromKey;

  /** The toKey generated from that asBound predicate. */
  final byte[] toKey;

  public Bundle(
      final IKeyBuilder keyBuilder,
      final IPredicate<F> asBound,
      final IKeyOrder<F> keyOrder,
      final IBindingSet bindingSet) {

    this.bindingSet = bindingSet;

    this.asBound = asBound;

    this.keyOrder = keyOrder;

    this.fromKey = keyOrder.getFromKey(keyBuilder, asBound);

    this.toKey = keyOrder.getToKey(keyBuilder, asBound);
  }

  /*
   * Orders {@link Bundle}s first by their {@link IKeyOrder} and then imposes an <code>
   * unsigned byte[]</code> order on the {@link #fromKey}. This groups {@link Bundle}s for the same
   * scale-out index together which allows us to make more efficient requests against the MDS and
   * makes it more likely that we can reuse the last {@link PartitionLocator} for the next as-bound
   * predicate.
   *
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/457">"No such index" on cluster
   *     under concurrent query workload </a>
   */
  public int compareTo(final Bundle<F> o) {

    int ret = keyOrder.getIndexName().compareTo(o.keyOrder.getIndexName());

    if (ret == 0) {

      ret = BytesUtil.compareBytes(this.fromKey, o.fromKey);
    }

    return ret;
  }

  /** Implemented to shut up findbugs, but not used. */
  @SuppressWarnings("unchecked")
  public boolean equals(final Object o) {

    if (this == o) return true;

    if (!(o instanceof Bundle)) return false;

    final Bundle<F> t = (Bundle<F>) o;

    if (keyOrder == t.keyOrder) return false;

    if (compareTo(t) != 0) return false;

    if (!bindingSet.equals(t.bindingSet)) return false;

    return asBound.equals(t.asBound);
  }

  /** Implemented to shut up find bugs. */
  public int hashCode() {

    if (hash == 0) {

      hash = Arrays.hashCode(fromKey);
    }

    return hash;
  }

  private int hash = 0;

  public String toString() {
    String sb = super.toString() + "{bindingSet=" + bindingSet
        + ",asBound=" + asBound
        + ",keyOrder=" + keyOrder
        + ",fromKey=" + BytesUtil.toString(fromKey)
        + ",toKey=" + BytesUtil.toString(toKey)
        + "}";
    return sb;
  }
}
