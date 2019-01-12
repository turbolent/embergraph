package org.embergraph.rdf.lexicon;

import java.util.Comparator;
import org.embergraph.btree.keys.KVO;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphValue;

/*
* Places {@link KVO}s containing {@link EmbergraphValue} references into an ordering determined by
 * the assigned term identifiers}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @see EmbergraphValue#getIV()
 */
public class KVOTermIdComparator implements Comparator<KVO<EmbergraphValue>> {

  public static final transient Comparator<KVO<EmbergraphValue>> INSTANCE =
      new KVOTermIdComparator();

  /** Note: defers to natural ordering for {@link IV} objects. */
  public int compare(final KVO<EmbergraphValue> term1, final KVO<EmbergraphValue> term2) {

    final IV<?, ?> iv1 = term1.obj.getIV();
    final IV<?, ?> iv2 = term2.obj.getIV();

    return iv1.compareTo(iv2);
  }
}
