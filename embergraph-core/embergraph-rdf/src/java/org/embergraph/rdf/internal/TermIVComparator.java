package org.embergraph.rdf.internal;

import java.util.Comparator;
import org.embergraph.rdf.model.EmbergraphValue;

/*
 * Places {@link EmbergraphValue}s into an ordering determined by their assigned {@link
 * EmbergraphValue#getIV() IVs} (internal values).
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @see EmbergraphValue#getIV()
 */
public class TermIVComparator implements Comparator<EmbergraphValue> {

  public static final transient Comparator<EmbergraphValue> INSTANCE = new TermIVComparator();

  /*
   * Note: comparison avoids possible overflow of <code>long</code> by not computing the difference
   * directly.
   */
  public int compare(final EmbergraphValue term1, final EmbergraphValue term2) {

    final IV<?, ?> iv1 = term1.getIV();
    final IV<?, ?> iv2 = term2.getIV();

    if (iv1 == null && iv2 == null) return 0;
    if (iv1 == null) return -1;
    if (iv2 == null) return 1;

    return iv1.compareTo(iv2);
  }
}
