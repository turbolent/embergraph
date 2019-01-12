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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;

/**
 * Unit tests for {@link INBinarySearch}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestInBinarySearch extends TestINConstraint {

  /** */
  public TestInBinarySearch() {}

  /** @param name */
  public TestInBinarySearch(String name) {
    super(name);
  }

  /** Unit test for {@link INConstraint#accept(IBindingSet)} */
  public void testAccept() {
    super.testAccept();

    // specific test because the implementation does a sort, etc...

    Var<?> x = Var.var("x");

    IConstant[] vals = new IConstant[100];
    for (int i = 0; i < vals.length; i++) vals[i] = new Constant<Integer>(i);
    List<IConstant> list = Arrays.asList(vals);
    Collections.shuffle(list);
    vals = list.toArray(vals);

    INConstraint op = new INBinarySearch(x, vals);

    assertTrue(
        op.get(
            new ListBindingSet(
                new IVariable<?>[] {x}, new IConstant[] {new Constant<Integer>(21)})));
    assertTrue(
        op.get(
            new ListBindingSet(
                new IVariable<?>[] {x}, new IConstant[] {new Constant<Integer>(37)})));
    assertTrue(
        op.get(
            new ListBindingSet(
                new IVariable<?>[] {x}, new IConstant[] {new Constant<Integer>(75)})));
    assertFalse(
        op.get(
            new ListBindingSet(
                new IVariable<?>[] {x}, new IConstant[] {new Constant<Integer>(101)})));
  }

  @Override
  protected INConstraint newINConstraint(IVariable<?> var, IConstant<?>[] vals) {
    return new INBinarySearch(var, vals);
  }
}
