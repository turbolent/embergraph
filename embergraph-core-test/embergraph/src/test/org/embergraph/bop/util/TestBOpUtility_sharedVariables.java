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
 * Created on Feb 21, 2011
 */

package org.embergraph.bop.util;

import java.util.Map;
import java.util.Set;
import junit.framework.TestCase2;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpBase;
import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.Var;
import org.embergraph.bop.ap.Predicate;

/*
 * Unit tests for {@link BOpUtility#getSharedVars(BOp, BOp)}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestBOpUtility_sharedVariables extends TestCase2 {

  /** */
  public TestBOpUtility_sharedVariables() {}

  /** @param name */
  public TestBOpUtility_sharedVariables(String name) {
    super(name);
  }

  /*
   * Unit test for correct rejection of illegal arguments.
   *
   * @see BOpUtility#getSharedVars(BOp, BOp)
   */
  public void test_getSharedVariables_correctRejection() {

    // correct rejection w/ null arg.
    try {
      BOpUtility.getSharedVars(Var.var("x"), null);
      fail("Expecting: " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // correct rejection w/ null arg.
    try {
      BOpUtility.getSharedVars(null, Var.var("x"));
      fail("Expecting: " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
  }

  /*
   * Unit test for correct identification of cases in which there are no shared variables.
   *
   * @see BOpUtility#getSharedVars(BOp, BOp)
   */
  @SuppressWarnings("unchecked")
  public void test_getSharedVariables_nothingShared() {

    // nothing shared because no variables for one operand.
    assertTrue(BOpUtility.getSharedVars(new Constant<Integer>(12), Var.var("y")).isEmpty());

    // nothing shared because no variables for the other operand.
    assertTrue(BOpUtility.getSharedVars(Var.var("y"), new Constant<Integer>(12)).isEmpty());

    // nothing shared.
    assertTrue(BOpUtility.getSharedVars(Var.var("x"), Var.var("y")).isEmpty());

    // nothing shared.
    assertTrue(BOpUtility.getSharedVars(Var.var("x"), new Constant<String>("x")).isEmpty());

    // nothing shared.
    assertTrue(
        BOpUtility.getSharedVars(
                Var.var("x"),
                new Predicate(new BOp[] {Var.var("y"), Var.var("z")}, (Map) null /* annotations */))
            .isEmpty());

    // nothing shared.
    assertTrue(
        BOpUtility.getSharedVars(
                Var.var("x"),
                new Predicate(new BOp[] {Var.var("y"), Var.var("z")}, new NV("name", "value")))
            .isEmpty());
  }

  /*
   * Unit test for correct identification of cases in which there are shared variables.
   *
   * @see BOpUtility#getSharedVars(BOp, BOp)
   */
  @SuppressWarnings("unchecked")
  public void test_getSharedVariables_somethingShared() {

    // two variables
    assertSameVariables(
        new IVariable[] {Var.var("x")}, BOpUtility.getSharedVars(Var.var("x"), Var.var("x")));

    // variable and expression.
    assertSameVariables(
        new IVariable[] {Var.var("x")},
        BOpUtility.getSharedVars(
            Var.var("x"),
            new BOpBase(
                new BOp[] {new Constant<String>("x"), Var.var("x")}, null // annotations
                )));

    // expression and variable.
    assertSameVariables(
        new IVariable[] {Var.var("x")},
        BOpUtility.getSharedVars(
            new BOpBase(
                new BOp[] {new Constant<String>("x"), Var.var("x")}, null // annotations
                ),
            Var.var("x")));

    // variable and predicate w/o annotations.
    assertSameVariables(
        new IVariable[] {Var.var("x")},
        BOpUtility.getSharedVars(
            Var.var("x"),
            new Predicate(new BOp[] {Var.var("y"), Var.var("x")}, (Map) null /* annotations */)));

    // predicate w/o annotations and variable.
    assertSameVariables(
        new IVariable[] {Var.var("x")},
        BOpUtility.getSharedVars(
            new Predicate(new BOp[] {Var.var("y"), Var.var("x")}, (Map) null /* annotations */),
            Var.var("x")));

    // variable and predicate w/ annotations (w/o var).
    assertSameVariables(
        new IVariable[] {Var.var("x")},
        BOpUtility.getSharedVars(
            Var.var("x"),
            new Predicate(new BOp[] {Var.var("x"), Var.var("z")}, new NV("name", "value"))));

    // variable and predicate w/ annotations (w/ same var).
    assertSameVariables(
        new IVariable[] {Var.var("x")},
        BOpUtility.getSharedVars(
            Var.var("x"),
            new Predicate(new BOp[] {Var.var("y"), Var.var("z")}, new NV("name", Var.var("x")))));

    // variable and predicate w/ annotations (w/ another var).
    assertSameVariables(
        new IVariable[] {
          /*Var.var("x")*/
        },
        BOpUtility.getSharedVars(
            Var.var("x"),
            new Predicate(new BOp[] {Var.var("y"), Var.var("z")}, new NV("name", Var.var("z")))));

    // two predicates
    assertSameVariables(
        new IVariable[] {Var.var("y"), Var.var("z")},
        BOpUtility.getSharedVars(
            new Predicate(new BOp[] {Var.var("y"), Var.var("z")}, new NV("name", Var.var("z"))),
            new Predicate(new BOp[] {Var.var("y"), Var.var("z")}, new NV("name", Var.var("x")))));

    // two predicates
    assertSameVariables(
        new IVariable[] {Var.var("x"), Var.var("y"), Var.var("z")},
        BOpUtility.getSharedVars(
            new Predicate(new BOp[] {Var.var("y"), Var.var("x")}, new NV("name", Var.var("z"))),
            new Predicate(new BOp[] {Var.var("y"), Var.var("z")}, new NV("name", Var.var("x")))));
  }

  /*
   * Test helper.
   *
   * @param expected The expected variables in any order.
   * @param actual A set of variables actually reported.
   */
  private static void assertSameVariables(
      final IVariable<?>[] expected, final Set<IVariable<?>> actual) {

    for (IVariable<?> var : expected) {

      if (!actual.contains(var)) {

        fail("Expecting: " + var);
      }
    }

    assertEquals("size", expected.length, actual.size());
  }
}
