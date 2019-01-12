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
package org.embergraph.btree;

/*
 * Stress tests for basic tree operations (insert, lookup, and remove) without causing node or leaf
 * evictions (IO is disabled).
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestBTree extends AbstractBTreeTestCase {

  /** */
  public TestBTree() {}

  /** @param arg0 */
  public TestBTree(String arg0) {

    super(arg0);
  }

  /*
   * test helpers.
   */

  /*
   * Test structure modification.
   */

  /** Stress test for building up a tree and then removing all keys in a random order. */
  public void test_stress_removeStructure() {

    int nkeys = 500;

    doRemoveStructureStressTest(3, nkeys);

    doRemoveStructureStressTest(4, nkeys);

    doRemoveStructureStressTest(5, nkeys);
  }

  /*
   * Stress test of insert, removal and lookup of keys in the tree (allows splitting of the root
   * leaf).
   *
   * <p>Note: The #of inserts is limited by the size of the leaf hard reference queue since
   * evictions are disabled for the tests in this file. We can not know in advance how many touches
   * will result and when leaf evictions will begin, so ntrials is set heuristically.
   */
  public void test_insertLookupRemoveKeyTreeStressTest() {

    int ntrials = 1000;

    doInsertLookupRemoveStressTest(3, 1000, ntrials);

    doInsertLookupRemoveStressTest(4, 1000, ntrials);

    doInsertLookupRemoveStressTest(5, 1000, ntrials);

    doInsertLookupRemoveStressTest(16, 10000, ntrials);
  }

  /*
   * Note: This error was actually a fence post in {@link Node#dump(java.io.PrintStream, int,
   * boolean))}. That method was incorrectly reporting an error when nkeys was zero after a split of
   * a node.
   */
  public void test_errorSequence001() {

    final int m = 3;

    final int[] order = new int[] {0, 1, 6, 3, 7, 2, 4, 5, 8};

    doKnownKeySequenceTest(getBTree(m), order, 3);
  }

  /*
   * A stress test for sequential key insertion that runs with a variety of branching factors and
   * #of keys to insert.
   */
  public void test_splitRootLeaf_increasingKeySequence() {

    int[] branchingFactors = new int[] {3, 4, 5}; // 6,7,8,20,55,79,256,512,1024,4097};

    for (int i = 0; i < branchingFactors.length; i++) {

      int m = branchingFactors[i];

      doSplitWithIncreasingKeySequence(getBTree(m), m, m);

      doSplitWithIncreasingKeySequence(getBTree(m), m, m * m);

      doSplitWithIncreasingKeySequence(getBTree(m), m, m * m * m);

      doSplitWithIncreasingKeySequence(getBTree(m), m, m * m * m * m);
    }
  }

  /*
   * A stress test for sequential decreasing key insertions that runs with a variety of branching
   * factors and #of keys to insert.
   */
  public void test_splitRootLeaf_decreasingKeySequence() {

    int[] branchingFactors = new int[] {3, 4, 5}; // 6,7,8,20,55,79,256,512,1024,4097};

    for (int i = 0; i < branchingFactors.length; i++) {

      int m = branchingFactors[i];

      doSplitWithDecreasingKeySequence(getBTree(m), m, m);

      doSplitWithDecreasingKeySequence(getBTree(m), m, m * m);

      doSplitWithDecreasingKeySequence(getBTree(m), m, m * m * m);

      doSplitWithDecreasingKeySequence(getBTree(m), m, m * m * m * m);
    }
  }

  /*
   * Stress test inserts random permutations of keys into btrees of order m for several different
   * btrees, #of keys to be inserted, and permutations of keys.
   */
  public void test_stress_split() {

    doSplitTest(3, 0);

    doSplitTest(4, 0);

    doSplitTest(5, 0);
  }

  /*
   * A stress test for random key insertion using a that runs with a variety of branching factors
   * and #of keys to insert.
   */
  public void test_splitRootLeaf_randomKeySequence() {

    int[] branchingFactors = new int[] {3, 4, 5}; // 6,7,8,20,55,79,256,512,1024,4097};

    for (int i = 0; i < branchingFactors.length; i++) {

      int m = branchingFactors[i];

      doSplitWithRandomDenseKeySequence(getBTree(m), m, m);

      doSplitWithRandomDenseKeySequence(getBTree(m), m, m * m);

      doSplitWithRandomDenseKeySequence(getBTree(m), m, m * m * m);

      doSplitWithRandomDenseKeySequence(getBTree(m), m, m * m * m * m);
    }
  }

  //    /*
  //     * The branching factors that will be used in the stress tests. The larger
  //     * the branching factor, the longer the run for these tests. The very small
  //     * branching factors (3, 4) test the btree code more fully since they will
  //     * exercise the fence posts on the invariants for nodes and leaves on pretty
  //     * much each mutation.
  //     */
  //    int[] branchingFactors = new int[]{3,4};//,5,10,20,64};//,128};//,512};
  //
  //    /*
  //     * A stress test for sequential key insertion that runs with a variety of
  //     * branching factors and #of keys to insert.
  //     */
  //    public void test_splitRootLeaf_increasingKeySequence() {
  //
  //        for(int i=0; i<branchingFactors.length; i++) {
  //
  //            int m = branchingFactors[i];
  //
  //            {
  //
  //                BTree btree = getBTree(m);
  //
  //                try {
  //
  //                    doSplitWithIncreasingKeySequence( btree, m, m );
  //
  //                } finally {
  //
  //                    try {btree.getStore().closeAndDelete();}
  //                    catch(Throwable t) {
  //                        log.warn(t);
  //                    }
  //
  //                }
  //
  //            }
  //
  //            {
  //
  //                BTree btree = getBTree(m);
  //
  //                try {
  //
  //                    doSplitWithIncreasingKeySequence( btree, m, m*m );
  //
  //                } finally {
  //
  //                    try {btree.getStore().closeAndDelete();}
  //                    catch(Throwable t) {
  //                        log.warn(t);
  //                    }
  //
  //                }
  //
  //            }
  //
  //            {
  //
  //                BTree btree = getBTree(m);
  //
  //                try {
  //
  //                    doSplitWithIncreasingKeySequence( btree, m, m*m*m );
  //
  //                } finally {
  //
  //                    try {btree.getStore().closeAndDelete();}
  //                    catch(Throwable t) {
  //                        log.warn(t);
  //                    }
  //
  //                }
  //
  //            }
  //
  //            {
  //
  //                BTree btree = getBTree(m);
  //
  //                try {
  //
  //                    doSplitWithIncreasingKeySequence( btree, m, m*m*m*m );
  //
  //                } finally {
  //
  //                    try {btree.getStore().closeAndDelete();}
  //                    catch(Throwable t) {
  //                        log.warn(t);
  //                    }
  //
  //                }
  //
  //            }
  //
  //        }
  //
  //    }
  //
  //    /*
  //     * A stress test for sequential decreasing key insertions that runs with a
  //     * variety of branching factors and #of keys to insert.
  //     */
  //    public void test_splitRootLeaf_decreasingKeySequence() {
  //
  //        for(int i=0; i<branchingFactors.length; i++) {
  //
  //            int m = branchingFactors[i];
  //
  //            {
  //
  //                BTree btree = getBTree(m);
  //
  //                try {
  //
  //                    doSplitWithDecreasingKeySequence( btree, m, m );
  //
  //                } finally {
  //
  //                    try {btree.getStore().closeAndDelete();}
  //                    catch(Throwable t) {
  //                        log.warn(t);
  //                    }
  //
  //                }
  //
  //            }
  //
  //            {
  //
  //                BTree btree = getBTree(m);
  //
  //                try {
  //
  //                    doSplitWithDecreasingKeySequence( btree, m, m*m );
  //
  //                } finally {
  //
  //                    try {btree.getStore().closeAndDelete();}
  //                    catch(Throwable t) {
  //                        log.warn(t);
  //                    }
  //
  //                }
  //
  //            }
  //
  //            {
  //
  //                BTree btree = getBTree(m);
  //
  //                try {
  //
  //                    doSplitWithDecreasingKeySequence( btree, m, m*m*m );
  //
  //                } finally {
  //
  //                    try {btree.getStore().closeAndDelete();}
  //                    catch(Throwable t) {
  //                        log.warn(t);
  //                    }
  //
  //                }
  //
  //            }
  //
  //            {
  //
  //                BTree btree = getBTree(m);
  //
  //                try {
  //
  //                    doSplitWithDecreasingKeySequence( btree, m, m*m*m*m );
  //
  //                } finally {
  //
  //                    try {btree.getStore().closeAndDelete();}
  //                    catch(Throwable t) {
  //                        log.warn(t);
  //                    }
  //
  //                }
  //
  //            }
  //
  //        }
  //
  //    }
  //
  //    /*
  //     * A stress test for random key insertion that runs with a variety of
  //     * branching factors and #of keys to insert.
  //     */
  //    public void test_splitRootLeaf_randomKeySequence() {
  //
  //        for(int i=0; i<branchingFactors.length; i++) {
  //
  //            int m = branchingFactors[i];
  //
  //            {
  //
  //                BTree btree = getBTree(m);
  //
  //                try {
  //
  //                    doSplitWithRandomDenseKeySequence( btree, m, m );
  //
  //                } finally {
  //
  //                    try {
  //                        btree.getStore().closeAndDelete();
  //                    } catch (Throwable t) {
  //                        log.warn(t);
  //                    }
  //
  //                }
  //
  //            }
  //
  //            {
  //
  //                BTree btree = getBTree(m);
  //
  //                try {
  //
  //                    doSplitWithRandomDenseKeySequence(btree, m, m * m);
  //
  //                } finally {
  //
  //                    try {
  //                        btree.getStore().closeAndDelete();
  //                    } catch (Throwable t) {
  //                        log.warn(t);
  //                    }
  //
  //                }
  //
  //            }
  //
  //            {
  //
  //                BTree btree = getBTree(m);
  //
  //                try {
  //
  //                    doSplitWithRandomDenseKeySequence( btree, m, m * m * m);
  //
  //                } finally {
  //
  //                    try {
  //                        btree.getStore().closeAndDelete();
  //                    } catch (Throwable t) {
  //                        log.warn(t);
  //                    }
  //
  //                }
  //
  //            }
  //
  //         }
  //
  //    }
  //
  //    /*
  //     * Stress test inserts random permutations of keys into btrees of order m
  //     * for several different btrees, #of keys to be inserted, and permutations
  //     * of keys.
  //     */
  //    public void test_stress_split() {
  //
  //        for(int i=0; i<branchingFactors.length; i++) {
  //
  //            int m = branchingFactors[i];
  //
  //            doSplitTest( m, 0 );
  //
  //        }
  //
  //    }
  //
  //    /*
  //     * Stress test of insert, removal and lookup of keys in the tree (allows
  //     * splitting of the root leaf).
  //     */
  //    public void test_insertLookupRemoveKeyTreeStressTest() {
  //
  //        int nkeys = 2000;
  //
  //        int ntrials = 25000;
  //
  //        for(int i=0; i<branchingFactors.length; i++) {
  //
  //            int m = branchingFactors[i];
  //
  //            doInsertLookupRemoveStressTest(m, nkeys, ntrials);
  //
  //        }
  //
  //    }
  //
  //    /*
  //     * Stress test for building up a tree and then removing all keys in a random
  //     * order.
  //     */
  //    public void test_stress_removeStructure() {
  //
  //        int nkeys = 5000;
  //
  //        for(int i=0; i<branchingFactors.length; i++) {
  //
  //            int m = branchingFactors[i];
  //
  //            doRemoveStructureStressTest(m,nkeys);
  //
  //        }
  //
  //    }

}
