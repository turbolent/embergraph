package org.embergraph.btree.raba.codec;

/*
 * Interface for {@link IRaba} data generators. A generator produces a <code>byte[][]</code>
 * representing keys or values.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IRabaGenerator {

  /** If the generator can generate B+Tree keys. */
  boolean isKeysGenerator();

  /** If the generator can generate B+Tree values. */
  boolean isValuesGenerator();

  /*
   * Generate B+Tree keys (no <code>null</code>, no duplicates, data are fully ordered based on
   * <code>unsigned byte[]</code> comparison).
   *
   * @param size The #of keys.
   * @return The keys.
   * @throws UnsupportedOperationException if the generator can not generate B+Tree keys.
   */
  byte[][] generateKeys(int size);

  /*
   * Generate B+Tree values (may include <code>null</code>s).
   *
   * @param size The #of values.
   * @return The values.
   * @throws UnsupportedOperationException if the generator can not generate B+Tree values.
   */
  byte[][] generateValues(int size);
}
