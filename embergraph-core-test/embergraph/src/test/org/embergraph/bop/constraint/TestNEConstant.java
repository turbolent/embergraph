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
 * Created on Sep 2, 2010
 */

package org.embergraph.bop.constraint;

import junit.framework.TestCase2;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;

/*
 * Unit tests for {@link NEConstant}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestNEConstant extends TestCase2 {

  /** */
  public TestNEConstant() {}

  /** @param name */
  public TestNEConstant(String name) {
    super(name);
  }

  /** Unit test for {@link NEConstant#NEConstant(IVariable,IConstant)} */
  public void testConstructor() {
    try {
      assertTrue(null != new NEConstant(null, new Constant<>("1")));
      fail("IllegalArgumentException expected, lhs was null");
    } catch (IllegalArgumentException e) {
    }

    try {
      assertTrue(null != new NEConstant(Var.var("x"), null));
      fail("IllegalArgumentException expected, rhs was null");
    } catch (IllegalArgumentException e) {
    }

    assertTrue(null != new NEConstant(Var.var("x"), new Constant<>("1")));
  }

  /** Unit test for {@link NEConstant#get(IBindingSet)} */
  public void testAccept() {
    Var<?> var = Var.var("x");
    Constant<String> val1 = new Constant<>("1");
    Constant<String> val2 = new Constant<>("2");
    Constant<Integer> val3 = new Constant<>(1);

    NEConstant op = new NEConstant(var, val1);

    IBindingSet eq = new ListBindingSet(new IVariable<?>[] {var}, new IConstant[] {val1});
    IBindingSet ne1 = new ListBindingSet(new IVariable<?>[] {var}, new IConstant[] {val2});
    IBindingSet ne2 = new ListBindingSet(new IVariable<?>[] {var}, new IConstant[] {val3});
    IBindingSet nb = new ListBindingSet(new IVariable<?>[] {}, new IConstant[] {});

    assertFalse(op.get(eq));
    assertTrue(op.get(ne1));
    assertTrue(op.get(ne2));
    assertTrue(op.get(nb));
  }
}
