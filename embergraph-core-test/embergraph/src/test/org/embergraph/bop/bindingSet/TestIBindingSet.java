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
 * Created on Jun 19, 2008
 */

package org.embergraph.bop.bindingSet;

import java.util.Iterator;
import java.util.Map;
import junit.framework.TestCase2;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.io.SerializerUtil;

/**
 * Unit tests for {@link IBindingSet}.
 *
 * <p>Note:
 *
 * <ul>
 *   <li>a) these tests assume that the values held for a given key are not cloned, i.e. comparison
 *       is done by '==' and not '.equals' (this is true except for the Serializatoin tests, where
 *       the {@link Var} references will be preserved but the {@link IConstant}s will be distinct).
 *   <li>b) keys with the same 'name' are a unique object.
 * </ul>
 *
 * @author <a href="mailto:dmacgbr@users.sourceforge.net">David MacMillan</a>
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class TestIBindingSet extends TestCase2 {

  /** */
  public TestIBindingSet() {}

  /** @param name */
  public TestIBindingSet(String name) {
    super(name);
  }

  /** Unit test for {@link IBindingSet#isBound(IVariable)} */
  public void testIsBound() {
    Var<?> a = Var.var("a");
    Var<?> b = Var.var("b");
    Var<?> c = Var.var("a");

    IBindingSet bs = newBindingSet(new IVariable[] {a}, new IConstant[] {new Constant<Integer>(1)});

    assertTrue("bound expected, same variable", bs.isBound(a));
    assertFalse("not bound expected", bs.isBound(b));
    assertTrue("bound expected, equivalent variable", bs.isBound(c));
  }

  /** Unit test for {@link IBindingSet#set(IVariable,IConstant)} */
  public void testSet() {
    Var<?> var = Var.var("a");
    Constant<Integer> val1 = new Constant<Integer>(1);
    Constant<Integer> val2 = new Constant<Integer>(2);

    IBindingSet bs = newBindingSet(2);

    try {
      bs.set(null, val1);
      fail("IllegalArgumentException expected, var was null");
    } catch (IllegalArgumentException e) {
    }

    try {
      bs.set(var, null);
      fail("IllegalArgumentException expected, val was null");
    } catch (IllegalArgumentException e) {
    }

    bs.set(var, val1);
    assertTrue(val1 == bs.get(var));

    bs.set(var, val2);
    assertTrue(val2 == bs.get(var));
  }

  /** Unit test for {@link IBindingSet#get(IVariable)} */
  public void testGet() {
    Var<?> var1 = Var.var("a");
    Var<?> var2 = Var.var("b");
    Constant<Integer> val1 = new Constant<Integer>(1);

    IBindingSet bs = newBindingSet(new IVariable[] {var1}, new IConstant[] {val1});

    try {
      bs.get(null);
      fail("IllegalArgumentException expected, var was null");
    } catch (IllegalArgumentException e) {
    }

    assertTrue(val1 == bs.get(var1));
    assertTrue(null == bs.get(var2));
  }

  /** Unit test for {@link IBindingSet#clear(IVariable)} */
  public void testClear() {
    Var<?> var1 = Var.var("a");
    Var<?> var2 = Var.var("b");
    Constant<Integer> val1 = new Constant<Integer>(1);
    Constant<Integer> val2 = new Constant<Integer>(2);

    IBindingSet bs = newBindingSet(new IVariable[] {var1, var2}, new IConstant[] {val1, val2});

    try {
      bs.clear(null);
      fail("IllegalArgumentException expected, var was null");
    } catch (IllegalArgumentException e) {
    }

    bs.clear(var1);
    assertTrue(null == bs.get(var1));
    assertTrue(val2 == bs.get(var2));

    bs.clear(var2);
    assertTrue(null == bs.get(var2));
    assertTrue(0 == bs.size());
  }

  /** Unit test for {@link IBindingSet#clearAll()} */
  public void testClearAll() {
    Var<?> var1 = Var.var("a");
    Var<?> var2 = Var.var("b");
    Constant<Integer> val1 = new Constant<Integer>(1);
    Constant<Integer> val2 = new Constant<Integer>(2);

    IBindingSet bs = newBindingSet(new IVariable[] {var1, var2}, new IConstant[] {val1, val2});

    bs.clearAll();
    assertTrue(null == bs.get(var1));
    assertTrue(null == bs.get(var2));
    assertTrue(0 == bs.size());
  }

  /** Unit test for {@link IBindingSet#size()} */
  public void testSize() {
    Var<?> var1 = Var.var("a");
    Var<?> var2 = Var.var("b");
    Constant<Integer> val1 = new Constant<Integer>(1);
    Constant<Integer> val2 = new Constant<Integer>(2);

    IBindingSet bs = newBindingSet(2);

    assertTrue(0 == bs.size());

    bs.set(var1, val1);
    bs.set(var2, val2);
    assertTrue(2 == bs.size());

    bs.clear(var2);
    assertTrue(1 == bs.size());
  }

  /** Unit test for {@link IBindingSet#iterator()} */
  public void testIterator() {
    final Var<?> var1 = Var.var("a");
    final Var<?> var2 = Var.var("b");
    final Constant<Integer> val1 = new Constant<Integer>(1);
    final Constant<Integer> val2 = new Constant<Integer>(2);

    final IBindingSet bs =
        newBindingSet(new IVariable[] {var1, var2}, new IConstant[] {val1, val2});

    int nvisited = 0;
    for (Iterator<Map.Entry<IVariable, IConstant>> i = bs.iterator(); i.hasNext(); ) {

      final Map.Entry<IVariable, IConstant> e = i.next();
      final IVariable<?> var = e.getKey();

      if (var1 == var) assertTrue("wrong value", val1 == e.getValue());
      else if (var2 == var) assertTrue("wrong value", val2 == e.getValue());
      else fail("unexpected variable: " + var);

      //            try {
      //                i.remove();
      //                fail("UnsupportedOperationException expected, iterator remove");
      //            } catch (UnsupportedOperationException ex) {
      //            }
      i.remove();
      nvisited++;
    }
    assertEquals("size", 2, nvisited);

    // Should be empty afterwards.
    assertEquals(newBindingSet(new IVariable[] {}, new IConstant[] {}), bs);
  }

  /** Unit test for {@link IBindingSet#vars()} */
  public void testVars() {
    Var<?> var1 = Var.var("a");
    Var<?> var2 = Var.var("b");
    Constant<Integer> val1 = new Constant<Integer>(1);
    Constant<Integer> val2 = new Constant<Integer>(2);

    IBindingSet bs = newBindingSet(new IVariable[] {var1, var2}, new IConstant[] {val1, val2});

    int n = 0;
    for (Iterator<IVariable> i = bs.vars(); i.hasNext(); ) {
      IVariable<?> var = i.next();

      if (var1 != var && var2 != var) fail("unexpected variable: " + var);

      try {
        i.remove();
        fail("UnsupportedOperationException expected, iterator remove");
      } catch (UnsupportedOperationException e) {
      }
      n++;
    }
    assertTrue("wrong count", 2 == n);
  }

  /** Unit test for {@link IBindingSet#copy(IVariable[])} */
  public void testCopy() {
    Var<?> var1 = Var.var("a");
    Var<?> var2 = Var.var("b");
    Var<?> var3 = Var.var("c");
    Var<?> var4 = Var.var("d");
    Var<?> var5 = Var.var("e");
    Constant<Integer> val1 = new Constant<Integer>(1);
    Constant<Integer> val2 = new Constant<Integer>(2);
    Constant<Integer> val3 = new Constant<Integer>(3);
    Constant<Integer> val4 = new Constant<Integer>(4);
    Constant<Integer> val5 = new Constant<Integer>(5);

    IBindingSet bs =
        newBindingSet(
            new IVariable[] {var1, var2, var3, var4, var5},
            new IConstant[] {val1, val2, val3, val4, val5});

    assertEqual(
        bs.copy(null /* variablesToKeep */),
        new IVariable[] {var1, var2, var3, var4, var5},
        new IConstant[] {val1, val2, val3, val4, val5});

    IBindingSet bs2 = bs.copy(new IVariable[] {var1, var3, var5});

    assertTrue(3 == bs2.size());
    for (IVariable<?> v : new IVariable[] {var1, var3, var5})
      assertTrue(bs2.get(v).equals(bs.get(v)));
  }

  /** Unit test for {@link IBindingSet#equals(Object)} */
  public void testEquals() {
    Var<?> var1 = Var.var("a");
    Var<?> var2 = Var.var("b");
    Var<?> var3 = Var.var("c");
    Constant<Integer> val1 = new Constant<Integer>(1);
    Constant<Integer> val2 = new Constant<Integer>(2);
    Constant<Integer> val3 = new Constant<Integer>(3);

    IBindingSet bs1 = newBindingSet(new IVariable[] {var1, var2}, new IConstant[] {val1, val2});
    IBindingSet bs2 = newBindingSet(new IVariable[] {var1, var2}, new IConstant[] {val1, val2});
    IBindingSet bs3 = newBindingSet(new IVariable[] {var2, var1}, new IConstant[] {val2, val1});
    IBindingSet bs4 = newBindingSet(new IVariable[] {var1, var2}, new IConstant[] {val1, val3});
    IBindingSet bs5 = newBindingSet(new IVariable[] {var1, var3}, new IConstant[] {val1, val3});
    IBindingSet bs6 =
        newBindingSet(new IVariable[] {var1, var2, var3}, new IConstant[] {val1, val2, val3});
    IBindingSet bs7 = newBindingSet(new IVariable[] {var1}, new IConstant[] {val1});

    assertTrue("expected equal: same bindings, same order", bs1.equals(bs2));
    assertTrue("expected equal: same bindings, different order", bs1.equals(bs3));
    assertTrue("expected not equal: different value", !bs1.equals(bs4));
    assertTrue("expected not equal: different variable", !bs1.equals(bs5));
    assertTrue("expected not equal: subsetOf ( this, that )", !bs1.equals(bs6));
    assertTrue("expected not equal: subsetOf ( that, this )", !bs1.equals(bs7));
  }

  /** Unit test for {@link IBindingSet#hashCode()} */
  public void testHashCode() {
    Var<?> var1 = Var.var("a");
    Var<?> var2 = Var.var("b");
    Constant<Integer> val1 = new Constant<Integer>(1);
    Constant<Integer> val2 = new Constant<Integer>(2);

    IBindingSet bs1 = newBindingSet(new IVariable[] {var1, var2}, new IConstant[] {val1, val2});
    IBindingSet bs2 = newBindingSet(new IVariable[] {var1, var2}, new IConstant[] {val1, val2});
    IBindingSet bs3 = newBindingSet(new IVariable[] {var2, var1}, new IConstant[] {val2, val1});
    IBindingSet bs4 = newBindingSet(new IVariable[] {var2}, new IConstant[] {val2});

    assertTrue("expected equal: same bindings, same order", bs1.hashCode() == bs2.hashCode());
    assertTrue("expected equal: same bindings, different order", bs1.hashCode() == bs3.hashCode());

    // After mutation. Not sure that this really proves anything, although in most cases I guess
    // that
    // the original value of bs1.hasCode () will not equal the subsequent value or that of
    // bs4.hashCode ()

    bs1.clear(var1);
    assertTrue("expected equal: same bindings after mutation", bs1.hashCode() == bs4.hashCode());
  }

  //    /*
  //     * push()/pop() tests.
  //     *
  //     * In addition to testing push() and pop(), we have to test that copy() and
  //     * clone() operate correctly in the presence of nested symbol tables, and
  //     * that the visitation patterns for the bindings operate correctly when
  //     * there are nested symbol tables. For example, if there "y" is bound at
  //     * level zero, a push() is executed, and then "x" is bound at level one. The
  //     * visitation pattern must visit both "x" and "y".
  //     */
  //
  //    public void test_pushPop1() {
  //
  //        final Var<?> var1 = Var.var("var1"); // visible and bound in query and subquery.
  //        final Var<?> var2 = Var.var("var2"); // visible only in subquery and not projected.
  //        final Var<?> var3 = Var.var("var3"); // visible in query and subquery; bound in
  // subquery.
  //        final Var<?> var4 = Var.var("var4"); // visible and bound only in query
  //
  //        final Constant<Integer> val1 = new Constant<Integer>(1);
  //        final Constant<Integer> val2 = new Constant<Integer>(2);
  //        final Constant<Integer> val3 = new Constant<Integer>(3);
  //        final Constant<Integer> val4 = new Constant<Integer>(4);
  //        final Constant<Integer> val4a = new Constant<Integer>(40);
  //
  //        // the "projected" variables.
  //        final IVariable<?>[] vars = new IVariable[] { var1, var3 };
  //
  //        final IBindingSet bs1 = newBindingSet(4/* size */);
  //
  //        /*
  //         * bind var1 in the base symbol table.
  //         */
  //        bs1.set(var1, val1);
  //        bs1.set(var4, val4);
  //        assertEqual(bs1, new IVariable[] { var1, var4 }, new IConstant[] { val1, val4 });
  //
  //        /*
  //         * push a symbol table onto the stack
  //         */
  //        bs1.push(vars);
  //
  //        // verify initial symbol table for subquery.
  //        assertEqual(bs1, new IVariable[] { var1 }, new IConstant[] { val1 });
  //
  //        // bind an unbound variables.
  //        bs1.set(var2, val2);
  //        bs1.set(var3, val3);
  //        bs1.set(var4, val4a); // note: variable and binding are distinct in this scope.
  //        assertEqual(bs1, new IVariable[] { var1, var2, var3, var4 }, new IConstant[] {
  //                val1, val2, val3, val4a });
  //
  //        // verify clone shows the same symbol table.
  //        final IBindingSet copy = bs1.clone();
  //        assertEqual(copy, new IVariable[] { var1, var2, var3, var4 }, new IConstant[] {
  //                val1, val2, val3, val4a });
  //        assertEquals(bs1, copy);
  //
  //        // pop the stack.
  //        bs1.pop(vars);
  //
  //        // verify projected variables were copied and local vars discarded.
  //        assertEqual(bs1, new IVariable[] { var1, var3, var4 }, new IConstant[] {
  //                val1, val3, val4 });
  //
  //        // verify copy was not effected.
  //        assertEqual(copy, new IVariable[] { var1, var2, var3, var4 }, new IConstant[] {
  //                val1, val2, val3, val4a });
  //
  //        copy.pop(vars);
  //
  //        // verify projected variables were copied and local vars discarded.
  //        assertEqual(bs1, new IVariable[] { var1, var3, var4 }, new IConstant[] {
  //                val1, val3, val4 });
  //
  //        // copy and original should now be equal again.
  //        assertEquals(bs1, copy);
  //
  //        /*
  //         * Verify that we can not pop() off the last symbol table in the stack.
  //         */
  //        try {
  //            bs1.pop(vars);
  //            fail("Expecting: " + IllegalStateException.class);
  //        } catch (IllegalStateException ex) {
  //            if (log.isInfoEnabled())
  //                log.info("Ignoring expected exception: " + ex);
  //        }
  //
  //    }

  public void test_serialization() {

    final Var<?> var1 = Var.var("a");
    final Var<?> var2 = Var.var("b");
    final Constant<Integer> val1 = new Constant<Integer>(1);
    final Constant<Integer> val2 = new Constant<Integer>(2);

    final IBindingSet bs1 = newBindingSet(2 /* size */);

    bs1.set(var1, val1);

    bs1.set(var2, val2);

    assertEqual(bs1, new IVariable[] {var1, var2}, new IConstant[] {val1, val2});

    final IBindingSet bs2 = (IBindingSet) SerializerUtil.deserialize(SerializerUtil.serialize(bs1));

    assertEquals(bs1, bs2);
  }

  /*
   * Hooks for testing specific implementations.
   */

  protected abstract IBindingSet newBindingSet(IVariable<?>[] vars, IConstant<?>[] vals);

  protected abstract IBindingSet newBindingSet(int size);

  /**
   * Compare actual and expected, where the latter is expressed using (vars,vals).
   *
   * <p>Note: This does not follow the junit pattern for asserts, which puts the expected data
   * first.
   *
   * @param actual
   * @param vars
   * @param vals
   */
  protected void assertEqual(
      final IBindingSet actual, final IVariable<?>[] vars, final IConstant<?>[] vals) {

    assertEquals("size", actual.size(), vars.length);

    for (int i = 0; i < vars.length; i++) {

      if (vals[i] != actual.get(vars[i])) {

        assertEquals(vars[i].toString(), vals[i], actual.get(vars[i]));
      }
    }
  }

  protected void assertEquals(IBindingSet expected, IBindingSet actual) {

    // expected variables in some order.
    final Iterator<IVariable> evars = expected.vars();

    // actual variables in some order (the order MAY be different).
    final Iterator<IVariable> avars = actual.vars();

    while (evars.hasNext()) {

      // Some variable for which we expect a binding.
      final IVariable evar = evars.next();

      if (!avars.hasNext()) {

        fail("actual does not bind enough variables.");
      }

      // consume and ignore actual variable since the order may differ
      avars.next();

      // The expected binding for some variable.
      final IConstant eval = expected.get(evar);

      // The actual binding for the same variable.
      final IConstant aval = actual.get(evar);

      // Verify that the bound values compare as equals.
      assertEquals(evar.getName(), eval, aval);
    }

    if (avars.hasNext()) {

      fail("actual binds more variables than expected.");
    }
  }
}
