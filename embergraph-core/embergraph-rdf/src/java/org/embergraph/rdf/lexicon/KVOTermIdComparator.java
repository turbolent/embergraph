package org.embergraph.rdf.lexicon;

import java.util.Comparator;

import org.embergraph.btree.keys.KVO;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.BigdataValue;

/**
 * Places {@link KVO}s containing {@link BigdataValue} references into an
 * ordering determined by the assigned term identifiers}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 * @see BigdataValue#getIV()
 */
public class KVOTermIdComparator implements Comparator<KVO<BigdataValue>> {

    public static final transient Comparator<KVO<BigdataValue>> INSTANCE = new KVOTermIdComparator();

    /**
     * Note: defers to natural ordering for {@link IV} objects.
     */
    public int compare(final KVO<BigdataValue> term1,
            final KVO<BigdataValue> term2) {

        final IV<?,?> iv1 = term1.obj.getIV();
        final IV<?,?> iv2 = term2.obj.getIV();

        return iv1.compareTo(iv2);

    }

}
