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
* Unit tests for {@link EQConstant}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestEQConstant extends TestCase2 {

  /** */
  public TestEQConstant() {}

  /** @param name */
  public TestEQConstant(String name) {
    super(name);
  }

  /** Unit test for {@link EQConstant#EQConstant(IVariable,IConstant)} */
  public void testConstructor() {
    try {
      assertTrue(null != new EQConstant(null, new Constant<String>("1")));
      fail("IllegalArgumentException expected, lhs was null");
    } catch (IllegalArgumentException e) {
    }

    try {
      assertTrue(null != new EQConstant(Var.var("x"), null));
      fail("IllegalArgumentException expected, rhs was null");
    } catch (IllegalArgumentException e) {
    }

    assertTrue(null != new EQConstant(Var.var("x"), new Constant<String>("1")));
  }

  /** Unit test for {@link EQConstant#accept(IBindingSet)} */
  public void testAccept() {
    Var<?> var = Var.var("x");
    Constant<String> val1 = new Constant<String>("1");
    Constant<String> val2 = new Constant<String>("2");
    Constant<Integer> val3 = new Constant<Integer>(1);

    EQConstant op = new EQConstant(var, val1);

    IBindingSet eq = new ListBindingSet(new IVariable<?>[] {var}, new IConstant[] {val1});
    IBindingSet ne1 = new ListBindingSet(new IVariable<?>[] {var}, new IConstant[] {val2});
    IBindingSet ne2 = new ListBindingSet(new IVariable<?>[] {var}, new IConstant[] {val3});
    IBindingSet nb = new ListBindingSet(new IVariable<?>[] {}, new IConstant[] {});

    assertTrue(op.get(eq));
    assertFalse(op.get(ne1));
    assertFalse(op.get(ne2));
    assertTrue(op.get(nb));
  }
}
