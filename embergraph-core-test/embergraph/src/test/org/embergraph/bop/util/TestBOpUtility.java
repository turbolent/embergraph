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
/*
 * Created on Aug 27, 2010
 */

package org.embergraph.bop.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.FutureTask;
import junit.framework.TestCase2;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpBase;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.BadBOpIdTypeException;
import org.embergraph.bop.Constant;
import org.embergraph.bop.DuplicateBOpIdException;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.NotPipelineOpException;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.Var;

/**
 * Unit tests for {@link BOpUtility}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestBOpUtility extends TestCase2 {

  /** */
  public TestBOpUtility() {}

  /** @param name */
  public TestBOpUtility(String name) {
    super(name);
  }

  /**
   * Unit test for {@link BOpUtility#getArgumentVariables(BOp)} and {@link
   * BOpUtility#getArgumentVariableCount(BOp)}.
   *
   * @todo unit test for iterator over arguments first, then over annotations which are bops, then
   *     over bop with annotations and finally over depth first recursion of (bop) and
   *     (bop+annotation bops). Everything else should just build on that. Declare what order is
   *     obeyed by those iterators, e.g., visitation order is pre-order or post-order for the
   *     argument hierarchy.
   * @todo test methods which do recursion to verify annotations handled if null or never null.
   */
  public void test_getArgumentVariables() {

    {
      final BOp op1 = new BOpBase(new BOp[] {Var.var("y")}, null /* annotations */);

      assertEquals(1, op1.arity());

      assertSameIterator(new Object[] {Var.var("y")}, op1.argIterator());

      assertSameIterator(new Object[] {Var.var("y")}, BOpUtility.getArgumentVariables(op1));

      assertEquals(1, BOpUtility.getArgumentVariableCount(op1));
    }

    {
      final BOp op2 = new BOpBase(new BOp[] {Var.var("x"), Var.var("y")}, null /* annotations */);

      assertEquals(2, op2.arity());

      assertSameIterator(new Object[] {Var.var("x"), Var.var("y")}, op2.argIterator());

      assertSameIterator(
          new Object[] {Var.var("x"), Var.var("y")}, BOpUtility.getArgumentVariables(op2));

      assertEquals(2, BOpUtility.getArgumentVariableCount(op2));
    }

    {
      final BOp op3 =
          new BOpBase(new BOp[] {new Constant<String>("x"), Var.var("y")}, null /* annotations */);

      assertSameIterator(new Object[] {new Constant<String>("x"), Var.var("y")}, op3.argIterator());

      assertSameIterator(new Object[] {Var.var("y")}, BOpUtility.getArgumentVariables(op3));

      assertEquals(1, BOpUtility.getArgumentVariableCount(op3));
    }
  }

  /** Unit test for {@link BOpUtility#preOrderIterator(BOp)}. */
  public void test_preOrderIterator() {

    //    	final Stack<INodeOrAttribute> context = new Stack<INodeOrAttribute>();

    final BOp op2 = new BOpBase(new BOp[] {Var.var("x")}, null /* annotations */);

    // root
    final BOp root =
        new BOpBase(
            new BOp[] { // root args[]
              new Constant<String>("x"), Var.var("y"), op2
            },
            null /* annotations */);

    final Object[] expected =
        new Object[] {
          root, new Constant<String>("x"), Var.var("y"), op2, Var.var("x"),
        };
    int i = 0;
    final Iterator<BOp> itr = BOpUtility.preOrderIterator(root);
    while (itr.hasNext()) {
      final BOp t = itr.next();
      if (log.isInfoEnabled()) log.info(i + " : " + t);
      assertTrue(
          "index=" + i + ", expected=" + expected[i] + ", actual=" + t, expected[i].equals(t));
      i++;
    }
    assertEquals(i, expected.length);

    assertSameIterator(expected, BOpUtility.preOrderIterator(root));
  }

  /** Unit test for {@link BOpUtility#postOrderIterator(BOp)}. */
  public void test_postOrderIterator() {

    final BOp op2 = new BOpBase(new BOp[] {Var.var("x")}, null /* annotations */);

    // root
    final BOp root =
        new BOpBase(
            new BOp[] { // root args[]
              new Constant<String>("x"), Var.var("y"), op2
            },
            null /* annotations */);

    final Object[] expected =
        new Object[] {
          new Constant<String>("x"), Var.var("y"), Var.var("x"), op2, root,
        };
    int i = 0;
    final Iterator<BOp> itr = BOpUtility.postOrderIterator(root);
    while (itr.hasNext()) {
      final BOp t = itr.next();
      if (log.isInfoEnabled()) log.info(i + " : " + t);
      assertTrue(
          "index=" + i + ", expected=" + expected[i] + ", actual=" + t, expected[i].equals(t));
      i++;
    }
    assertEquals(i, expected.length);

    assertSameIterator(expected, BOpUtility.postOrderIterator(root));
  }

  /**
   * Unit test for {@link BOpUtility#annotationOpIterator(BOp)}.
   *
   * <p>Note: This test depends on the LinkedHashMap imposing the ordering in which the annotations
   * are declared.
   */
  public void test_annotationOpIterator() {

    /*
     * Verify that we get an empty iterator for an operator without any
     * annotations.
     */
    {
      final BOp a1 = new BOpBase(new BOp[] {Var.var("a")}, null /* annotations */);

      assertFalse(BOpUtility.annotationOpIterator(a1).hasNext());
    }

    /*
     * Verify that we get an empty iterator for an operator without any
     * annotations which are themselves operators.
     */
    {
      final BOp a1 =
          new BOpBase(
              new BOp[] {Var.var("a")}, // annotations
              NV.asMap(
                  new NV[] {
                    new NV("baz", "3"),
                  }));

      assertFalse(BOpUtility.annotationOpIterator(a1).hasNext());
    }

    /*
     * Verify that we get we visit the annotations which are themselves
     * operators, but not non-operator annotations.
     */
    {
      final BOp op =
          new BOpBase(
              // children
              new BOp[] {},
              // annotations
              NV.asMap(
                  new NV[] {
                    new NV("foo", Var.var("x")),
                    new NV("bar", new Constant<String>("2")),
                    new NV("baz", "3"),
                  }));

      final BOp[] expected =
          new BOp[] {
            Var.var("x"), new Constant<String>("2"),
          };
      int i = 0;
      final Iterator<BOp> itr = BOpUtility.annotationOpIterator(op);
      while (itr.hasNext()) {
        final BOp t = itr.next();
        if (log.isInfoEnabled()) log.info(i + " : " + t);
        assertTrue(
            "index=" + i + ", expected=" + expected[i] + ", actual=" + t, expected[i].equals(t));
        i++;
      }
      assertEquals(i, expected.length);

      assertSameIterator(expected, BOpUtility.annotationOpIterator(op));
    }
  }

  //    /**
  //     * Unit test for {@link BOpUtility#annotationOpPreOrderIterator(BOp)}
  //     * (pre-order traversal of the operator annotations of the given operator
  //     * without recursion through the children of the given operator)).
  //     * <p>
  //     * Note: This test depends on the LinkedHashMap imposing the ordering in
  //     * which the annotations are declared.
  //     */
  //    public void test_annotationOpPreOrderIterator() {
  //
  //        final BOp a1 = new BOpBase(new BOp[]{Var.var("a")},null/*annotations*/);
  //        final BOp a2 = new BOpBase(new BOp[]{Var.var("b")},null/*annotations*/);
  //        // Note: [a3] tests recursion (annotations of annotations).
  //        final BOp a3 = new BOpBase(new BOp[] { Var.var("z") }, NV
  //                .asMap(
  //                        new NV[] {
  //                                new NV("baz", a2),
  //                                new NV("baz2", "skip")
  //                                }
  //                        ));
  //
  //        final BOp op2 = new BOpBase(new BOp[] { Var.var("x") }, NV.asMap(new NV[]{
  //                new NV("foo1",a1),
  //                new NV("foo2",a3),
  //                new NV("foo3", "skip"),
  //        }));
  //
  //        final Object[] expected = new Object[]{
  //                op2,
  //                a1,
  //                Var.var("a"),
  //                a3,
  //                a2,
  //                Var.var("b"),
  //                Var.var("z"),
  //                Var.var("x"),
  //        };
  //        int i = 0;
  //        final Iterator<BOp> itr = BOpUtility
  //                .annotationOpPreOrderIterator(op2);
  //        while (itr.hasNext()) {
  //            final BOp t = itr.next();
  ////            System.out.println(i + " : " + t);
  ////            assertTrue("index=" + i + ", expected=" + expected[i] + ", actual="
  ////                    + t, expected[i].equals(t));
  //            i++;
  //        }
  //        assertEquals(i, expected.length);
  //
  //        assertSameIterator(expected, BOpUtility
  //                .annotationOpPreOrderIterator(op2));
  //
  //    }

  /**
   * Unit test for {@link BOpUtility#preOrderIteratorWithAnnotations(BOp)}.
   *
   * <p>Note: This test depends on the LinkedHashMap imposing the ordering in which the annotations
   * are declared.
   */
  public void test_preOrderIteratorWithAnnotations() {

    final BOp a1 = new BOpBase(new BOp[] {Var.var("a")}, null /*annotations*/);
    final BOp a2 = new BOpBase(new BOp[] {Var.var("b")}, null /*annotations*/);
    // Note: [a3] tests recursion (annotations of annotations).
    final BOp a3 =
        new BOpBase(
            new BOp[] {Var.var("z")},
            NV.asMap(new NV[] {new NV("baz", a2), new NV("baz2", "skip")}));

    final BOp op2 =
        new BOpBase(
            new BOp[] {Var.var("x")},
            NV.asMap(
                new NV[] {
                  new NV("foo1", a1), new NV("foo2", a3), new NV("foo3", "skip"),
                }));

    // root
    final BOp root =
        new BOpBase(
            new BOp[] { // root args[]
              new Constant<String>("12"), Var.var("y"), op2
            },
            null /* annotations */);

    final Object[] expected =
        new Object[] {
          root,
          new Constant<String>("12"),
          Var.var("y"),
          op2,
          a1,
          Var.var("a"),
          a3,
          a2,
          Var.var("b"),
          Var.var("z"),
          Var.var("x"),
        };
    int i = 0;
    final Iterator<BOp> itr = BOpUtility.preOrderIteratorWithAnnotations(root);
    while (itr.hasNext()) {
      final BOp t = itr.next();
      if (log.isInfoEnabled()) log.info(i + " : " + t);
      assertTrue(
          "index=" + i + ", expected=" + expected[i] + ", actual=" + t, expected[i].equals(t));
      i++;
    }
    assertEquals(i, expected.length);

    assertSameIterator(expected, BOpUtility.preOrderIteratorWithAnnotations(root));
  }

  /**
   * Unit test for {@link BOpUtility#postOrderIteratorWithAnnotations(BOp)}.
   *
   * <p>Note: This test depends on the LinkedHashMap imposing the ordering in which the annotations
   * are declared.
   *
   * @see <a href="http://trac.bigdata.com/ticket/1210" >
   *     BOpUtility.postOrderIteratorWithAnnotations() is has wrong visitation order. </a>
   */
  public void test_postOrderIteratorWithAnnotations() {

    final BOp a1 = new BOpBase(new BOp[] {Var.var("a")}, null /*annotations*/);
    final BOp a2 = new BOpBase(new BOp[] {Var.var("b")}, null /*annotations*/);
    // Note: [a3] tests recursion (annotations of annotations).
    final BOp a3 =
        new BOpBase(
            new BOp[] {Var.var("z")},
            NV.asMap(new NV[] {new NV("baz", a2), new NV("baz2", "skip")}));

    final BOp op2 =
        new BOpBase(
            new BOp[] {Var.var("x")},
            NV.asMap(
                new NV[] {
                  new NV("foo1", a1), new NV("foo2", a3), new NV("foo3", "skip"),
                }));

    // root
    final BOp root =
        new BOpBase(
            new BOp[] { // root args[]
              new Constant<String>("12"), Var.var("y"), op2
            },
            null /* annotations */);

    //		final BOp[] expectedMGC = new BOp[] {// MGC - Arguments THEN Annotation
    //
    //				// root annotations {}
    //
    //				// root arguments {"12", ?y, op2}
    //				new Constant<String>("12"),			// root[0]
    //				Var.var("y"),						// root[1]
    //
    //					// op2 annotations {a1, a3}
    //					// a1 children
    //					Var.var("x"),					// op2[0]
    //							Var.var("a"),				// a1[0]
    //						a1,							// op2 anno
    //
    //						// a3 arguments {?z}
    //							Var.var("z"),			// a3[0]
    //								Var.var("b"),		// a2[0]
    //							a2,						// a3 anno
    //						a3,							// op2 anno
    //
    //					op2,							// root[2]
    //
    //				root,
    //		};

    final BOp[] expected =
        new BOp[] { // Annotations THEN Arguments

          // root annotations {}

          // root arguments {"12", ?y, op2}
          new Constant<String>("12"),
          Var.var("y"),

          // op2 annotations {a1, a3}
          // a1 children
          Var.var("a"),
          a1,

          // a3 annotations {a2}
          // a2 children {b}
          Var.var("b"),
          a2,
          // a3 arguments {?z}
          Var.var("z"),
          a3,

          // op2 arguments.
          Var.var("x"),
          op2,
          root,
        };

    final Object[] actual = unwrap(BOpUtility.postOrderIteratorWithAnnotations(root));
    assertTrue(actual.length == expected.length);
    for (int i = 0; i < expected.length; i++) {
      assertTrue(
          "index=" + i + ", expected=" + expected[i] + ", actual=" + actual[i],
          expected[i].equals(actual[i]));
    }
  }

  private Object[] unwrap(Iterator<BOp> iter) {
    final ArrayList<BOp> array = new ArrayList<BOp>();
    while (iter.hasNext()) {
      array.add(iter.next());
    }

    return array.toArray();
  }

  /** Unit test for {@link BOpUtility#getSpannedVariables(BOp)}. */
  public void test_getSpannedVariables() {

    final BOp a1 = new BOpBase(new BOp[] {Var.var("a")}, null /*annotations*/);
    final BOp a2 = new BOpBase(new BOp[] {Var.var("b")}, null /*annotations*/);
    // Note: [a3] tests recursion (annotations of annotations).
    final BOp a3 =
        new BOpBase(
            new BOp[] {Var.var("z")},
            NV.asMap(new NV[] {new NV("baz", a2), new NV("baz2", "skip")}));

    final BOp op2 =
        new BOpBase(
            new BOp[] {Var.var("x")},
            NV.asMap(
                new NV[] {
                  new NV("foo1", a1), new NV("foo2", a3), new NV("foo3", "skip"),
                }));

    // root
    final BOp root =
        new BOpBase(
            new BOp[] { // root args[]
              new Constant<String>("12"), Var.var("y"), op2
            },
            null /* annotations */);

    final Object[] expected =
        new Object[] {
          Var.var("y"), Var.var("a"), Var.var("b"), Var.var("z"), Var.var("x"),
        };
    int i = 0;
    final Iterator<IVariable<?>> itr = BOpUtility.getSpannedVariables(root);
    while (itr.hasNext()) {
      final BOp t = itr.next();
      if (log.isInfoEnabled()) log.info(i + " : " + t);
      assertTrue(
          "index=" + i + ", expected=" + expected[i] + ", actual=" + t, expected[i].equals(t));
      i++;
    }
    assertEquals(i, expected.length);

    assertSameIterator(expected, BOpUtility.getSpannedVariables(root));
  }

  /** Unit test for {@link BOpUtility#getIndex(BOp)} using valid inputs. */
  public void test_getIndex() {

    final BOp op1 =
        new MockPipelineOp(
            new BOp[] {},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 1),
                }));

    final BOp op2 =
        new MockPipelineOp(
            new BOp[] {op1},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 2),
                }));

    final BOp op3 =
        new MockPipelineOp(
            new BOp[] {op2},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 3),
                }));

    // root
    final BOp root =
        new MockPipelineOp(
            new BOp[] { // root args[]
              op3
            },
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 4),
                }));

    // index the operator tree.
    final Map<Integer, BOp> map = BOpUtility.getIndex(root);

    assertTrue(op1 == map.get(1));
    assertTrue(op2 == map.get(2));
    assertTrue(op3 == map.get(3));
    assertTrue(root == map.get(4));
    assertNull(map.get(5));
    assertEquals(4, map.size());

    // verify map is immutable.
    try {
      map.put(Integer.valueOf(1), op1);
      fail("Expecting: " + UnsupportedOperationException.class);
    } catch (UnsupportedOperationException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
  }

  /** Unit test verifies that annotations are not indexed (they are outside of the pipeline). */
  public void test_getIndex_doesNotIndexAnnotations() {

    // Should not be indexed since only appears as annotation.
    final BOp a2 =
        new MockPipelineOp(
            new BOp[] {},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 1),
                }));

    // References [a2] as an annotation.
    final BOp a3 =
        new MockPipelineOp(
            new BOp[] {},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 2), new NV("baz", a2), new NV("baz2", "skip")
                }));

    // index the operator tree (should succeed).
    final Map<Integer, BOp> map = BOpUtility.getIndex(a3);

    assertNull(map.get(1)); // annotation was not indexed.
    assertEquals(a3, map.get(2)); // other operators were indexed.
    assertEquals(1, map.size()); // map contained just the indexed ops.
  }

  /**
   * Unit test for {@link BOpUtility#getIndex(BOp)} in which we verify that it rejects operator
   * trees having duplicate operator ids.
   */
  public void test_getIndex_rejectsDuplicateBOpIds() {

    final BOp op2 =
        new MockPipelineOp(
            new BOp[] {},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 4),
                }));

    // root
    final BOp root =
        new MockPipelineOp(
            new BOp[] { // root args[]
              op2
            },
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 4),
                }));

    try {
      BOpUtility.getIndex(root);
      fail("Expecting: " + DuplicateBOpIdException.class);
    } catch (DuplicateBOpIdException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
  }

  /**
   * Unit test for {@link BOpUtility#getIndex(BOp)} in which we verify that it rejects operator
   * trees with operator ids which are not {@link Integer}s.
   */
  public void test_getIndex_rejectsNonIntegerIds() {

    // root
    final BOp root =
        new MockPipelineOp(
            new BOp[] {},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, "4"),
                }));

    try {
      BOpUtility.getIndex(root);
      fail("Expecting: " + BadBOpIdTypeException.class);
    } catch (BadBOpIdTypeException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
  }

  /*
   * Note: Since annotations are not indexed it is no longer possible to
   * encounter the same operator more than once in the pipeline.
   */
  //    /**
  //     * Unit test for {@link BOpUtility#getIndex(BOp)} in which we verify that it
  //     * rejects operator trees in which the same {@link BOp} reference appears
  //     * more than once but allows duplicate {@link IVariable}s and
  //     * {@link IConstant}s.
  //     */
  //    public void test_getIndex_rejectsDuplicateBOps() {
  //
  //        final IConstant<Long> c1 = new Constant<Long>(12L);
  //        final IVariable<?> v1 = Var.var("y");
  //
  //        /*
  //         * Operator tree with duplicate variable and duplicate constant refs.
  //         */
  //        {
  //            // root
  //            final BOp root = new BOpBase(new BOp[] { // root args[]
  //                    c1, v1 }, NV.asMap(new NV[] {
  //                            new NV(BOp.Annotations.BOP_ID, 4),
  //                                    new NV("foo", v1), // duplicate variable.
  //                                    new NV("bar", c1) // duplicate variable.
  //                            }));
  //
  //            // should be Ok.
  //            final Map<Integer, BOp> map = BOpUtility.getIndex(root);
  //
  //            assertTrue(root == map.get(4));
  //
  //        }
  //
  ////        /*
  ////         * Operator tree with duplicate bop which is neither a var nor or a
  ////         * constant.
  ////         */
  ////        {
  //
  ////            /*
  ////             * bop w/o bopId is used to verify correct detection of duplicate
  ////             * references (it has to be a PipelineOp).
  ////             */
  ////            final BOp op2 = new PipelineOp(new BOp[]{}, null/*annotations*/) {
  //
  ////                private static final long serialVersionUID = 1L;
  //
  ////                @Override
  ////                public FutureTask<Void> eval(BOpContext<IBindingSet> context) {
  ////                    throw new UnsupportedOperationException();
  ////                }
  ////
  ////            };
  ////
  ////            // root
  ////            final BOp root = new BOpBase(new BOp[] { // root args[]
  ////                    op2, op2 }, NV.asMap(new NV[] {
  ////                            new NV(BOp.Annotations.BOP_ID, 4),
  ////                            }));
  //
  ////            try {
  ////                BOpUtility.getIndex(root);
  ////                fail("Expecting: " + DuplicateBOpException.class);
  ////            } catch (DuplicateBOpException ex) {
  ////                if (log.isInfoEnabled())
  ////                    log.info("Ignoring expected exception: " + ex);
  ////            }
  ////        }
  //
  //    }

  /** Unit test verifies that non-pipeline operators are rejected. */
  public void test_getIndex_rejectsNonPipelineOps() {

    final BOp a1 =
        new BOpBase(
            new BOp[] {Var.var("a")},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 1),
                }));

    final BOp a2 =
        new MockPipelineOp(
            new BOp[] {a1},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 2),
                }));

    final BOp a3 =
        new MockPipelineOp(
            new BOp[] {Var.var("a")},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 1),
                }));

    final BOp a4 =
        new MockPipelineOp(
            new BOp[] {new Constant<String>("a")},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 1),
                }));

    // non-pipeline op.
    try {
      BOpUtility.getIndex(a1);
      fail("Expecting: " + NotPipelineOpException.class);
    } catch (NotPipelineOpException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // pipeline op with non-pipeline child.
    try {
      BOpUtility.getIndex(a2);
      fail("Expecting: " + NotPipelineOpException.class);
    } catch (NotPipelineOpException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // pipeline op with non-pipeline child which is a variable.
    try {
      BOpUtility.getIndex(a3);
      fail("Expecting: " + NotPipelineOpException.class);
    } catch (NotPipelineOpException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // pipeline op with non-pipeline child which is a constant.
    try {
      BOpUtility.getIndex(a4);
      fail("Expecting: " + NotPipelineOpException.class);
    } catch (NotPipelineOpException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
  }

  private static class MockPipelineOp extends PipelineOp {

    private static final long serialVersionUID = 1L;

    public MockPipelineOp(BOp[] args, Map<String, Object> annotations) {
      super(args, annotations);
    }

    public MockPipelineOp(MockPipelineOp op) {
      super(op);
    }

    @Override
    public FutureTask<Void> eval(BOpContext<IBindingSet> context) {
      throw new UnsupportedOperationException();
    }
  }

  //    /**
  //     * A conditional join group:
  //     *
  //     * <pre>
  //     * (a b)
  //     * optional {
  //     *   (b c)
  //     *   (c d)
  //     * }
  //     * </pre>
  //     *
  //     * where the groupId for the optional join group is ONE (1). The test should
  //     * locate the first {@link PipelineJoin} in that join group, which is the
  //     * one reading on the <code>(b c)</code> access path.
  //     */
  //    public void test_getFirstBOpIdForConditionalGroup() {
  //
  //        final String namespace = "kb";
  //
  //        final int startId = 1; //
  //        final int joinId1 = 2; //         : base join group.
  //        final int predId1 = 3; // (a b)
  //        final int joinId2 = 4; //         : joinGroup1
  //        final int predId2 = 5; // (b c)
  //        final int joinId3 = 6; //         : joinGroup1
  //        final int predId3 = 7; // (c d)
  //        final int sliceId = 8; //
  //
  //        final IVariable<?> a = Var.var("a");
  //        final IVariable<?> b = Var.var("b");
  //        final IVariable<?> c = Var.var("c");
  //        final IVariable<?> d = Var.var("d");
  //
  //        final Integer joinGroup1 = Integer.valueOf(1);
  //
  //        final PipelineOp startOp = new StartOp(new BOp[] {},
  //                NV.asMap(new NV[] {
  //                        new NV(Predicate.Annotations.BOP_ID, startId),
  //                        new NV(SliceOp.Annotations.EVALUATION_CONTEXT,
  //                                BOpEvaluationContext.CONTROLLER),
  //                        }));
  //
  //        final Predicate<?> pred1Op = new Predicate<E>(
  //                new IVariableOrConstant[] { a, b }, NV
  //                .asMap(new NV[] {
  //                        new NV(Predicate.Annotations.RELATION_NAME,
  //                                new String[] { namespace }),
  //                        new NV(Predicate.Annotations.BOP_ID, predId1),
  //                        new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED),
  //                }));
  //
  //        final Predicate<?> pred2Op = new Predicate<E>(
  //                new IVariableOrConstant[] { b, c }, NV
  //                .asMap(new NV[] {
  //                        new NV(Predicate.Annotations.RELATION_NAME,
  //                                new String[] { namespace }),
  //                        new NV(Predicate.Annotations.BOP_ID, predId2),
  //                        new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED),
  //                }));
  //
  //        final Predicate<?> pred3Op = new Predicate<E>(
  //                new IVariableOrConstant[] { c, d }, NV
  //                .asMap(new NV[] {
  //                        new NV(Predicate.Annotations.RELATION_NAME,
  //                                new String[] { namespace }),
  //                        new NV(Predicate.Annotations.BOP_ID, predId3),
  //                        new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED),
  //                }));
  //
  //        final PipelineOp join1Op = new PipelineJoin<E>(
  //                new BOp[]{startOp},//
  //                        new NV(Predicate.Annotations.BOP_ID, joinId1),
  //                        new NV(PipelineJoin.Annotations.PREDICATE,pred1Op));
  //
  //        final PipelineOp join2Op = new PipelineJoin<E>(
  //                new BOp[] { join1Op },
  //                new NV(Predicate.Annotations.BOP_ID, joinId2),
  //                new NV(PipelineOp.Annotations.CONDITIONAL_GROUP, joinGroup1),
  //                new NV(PipelineJoin.Annotations.PREDICATE, pred2Op),
  //                // join is optional.
  //                new NV(PipelineJoin.Annotations.OPTIONAL, true),
  //                // optional target is the same as the default target.
  //                new NV(PipelineOp.Annotations.ALT_SINK_REF, sliceId));
  //
  //        final PipelineOp join3Op = new PipelineJoin<E>(
  //                new BOp[] { join2Op },
  //                new NV(Predicate.Annotations.BOP_ID, joinId3),
  //                new NV(PipelineOp.Annotations.CONDITIONAL_GROUP, joinGroup1),
  //                new NV(PipelineJoin.Annotations.PREDICATE, pred3Op),
  //                // join is optional.
  //                new NV(PipelineJoin.Annotations.OPTIONAL, true),
  //                // optional target is the same as the default target.
  //                new NV(PipelineOp.Annotations.ALT_SINK_REF, sliceId));
  //
  //        final PipelineOp sliceOp = new SliceOp(
  //                new BOp[]{join3Op},
  //                NV.asMap(new NV[] {
  //                        new NV(BOp.Annotations.BOP_ID, sliceId),
  //                        new NV(BOp.Annotations.EVALUATION_CONTEXT,
  //                                BOpEvaluationContext.CONTROLLER),
  //                        }));
  //
  //        final PipelineOp query = sliceOp;
  //
  //        // verify found.
  //        assertEquals(Integer.valueOf(joinId2), BOpUtility
  //                .getFirstBOpIdForConditionalGroup(query, joinGroup1));
  //
  //        // verify not-found.
  //        assertEquals(null, BOpUtility.getFirstBOpIdForConditionalGroup(query,
  //                Integer.valueOf(2)/* groupId */));
  //
  //    }

  /** Unit test for {@link BOpUtility#getParent(BOp, BOp)}. */
  public void test_getParent() {

    final BOp a1 =
        new BOpBase(
            new BOp[] {Var.var("a")},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 1),
                }));
    final BOp a2 =
        new BOpBase(
            new BOp[] {Var.var("b")},
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 2),
                }));
    // Note: [a3] tests recursion (annotations of annotations).
    final BOp a3 =
        new BOpBase(
            new BOp[] {Var.var("z"), a1},
            NV.asMap(new NV[] {new NV("baz", a2), new NV("baz2", "skip")}));

    final BOp op2 =
        new BOpBase(
            new BOp[] {Var.var("x"), a3},
            NV.asMap(
                new NV[] {
                  new NV("foo1", a1), new NV("foo3", "skip"), new NV(BOp.Annotations.BOP_ID, 3),
                }));

    // root
    final BOp root =
        new BOpBase(
            new BOp[] { // root args[]
              new Constant<String>("12"), Var.var("y"), op2
            },
            NV.asMap(
                new NV[] {
                  new NV(BOp.Annotations.BOP_ID, 4),
                }));

    assertTrue(root == BOpUtility.getParent(root, op2));

    assertTrue(op2 == BOpUtility.getParent(root, Var.var("x")));

    assertTrue(op2 == BOpUtility.getParent(root, a3));

    assertTrue(a3 == BOpUtility.getParent(root, a1));

    try {
      BOpUtility.getParent(null /* root */, op2);
      fail("Expecting: " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    try {
      BOpUtility.getParent(root, null /* op */);
      fail("Expecting: " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
  }

  /** Unit test for locating the left-deep child at which pipeline evaluation should begin. */
  public void test_getPipelineStart() {

    final int aid = 0;
    final int bid = 1;
    final int cid = 2;

    final BOp a =
        new BOpBase(new BOp[] {}, NV.asMap(new NV[] {new NV(BOp.Annotations.BOP_ID, aid)}));

    final BOp b =
        new BOpBase(new BOp[] {}, NV.asMap(new NV[] {new NV(BOp.Annotations.BOP_ID, bid)}));

    final BOp c =
        new BOpBase(new BOp[] {a, b}, NV.asMap(new NV[] {new NV(BOp.Annotations.BOP_ID, cid)}));

    assertEquals(a, BOpUtility.getPipelineStart(a));

    assertEquals(b, BOpUtility.getPipelineStart(b));

    assertEquals(a, BOpUtility.getPipelineStart(c));
  }

  /**
   * Unit tests for extracting the left-deep evaluation order for the query pipeline.
   *
   * <p>- test when the 1st operator is a control operator.
   *
   * <p>- test when there is an embedded control operator (subquery).
   *
   * <p>Note: this is not testing with left/right branches in the query plan. That sort of plan is
   * not currently supported by pipeline evaluation.
   */
  public void test_getEvaluationOrder() {

    final BOp op2 =
        new MyPipelineOp(
            new BOp[] {},
            NV.asMap(
                new NV(BOp.Annotations.BOP_ID, 1)
                //    			new NV(BOp.Annotations.CONTROLLER,false)
                ));
    final BOp op1 =
        new MyPipelineOp(
            new BOp[] {op2},
            NV.asMap(
                new NV(BOp.Annotations.BOP_ID, 2)
                //    			new NV(BOp.Annotations.CONTROLLER,false)
                ));
    final BOp op3 =
        new MyPipelineOp(
            new BOp[] {op1},
            NV.asMap(new NV(BOp.Annotations.BOP_ID, 3), new NV(BOp.Annotations.CONTROLLER, true)));

    assertEquals(new Integer[] {1, 2, 3}, BOpUtility.getEvaluationOrder(op3));
  }

  private static class MyPipelineOp extends PipelineOp {

    private static final long serialVersionUID = 1L;

    /** Deep copy constructor. */
    protected MyPipelineOp(MyPipelineOp op) {
      super(op);
    }

    /** Shallow copy constructor. */
    protected MyPipelineOp(BOp[] args, Map<String, Object> annotations) {
      super(args, annotations);
    }

    @Override
    public FutureTask<Void> eval(BOpContext<IBindingSet> context) {
      return null;
    }
  }
}
