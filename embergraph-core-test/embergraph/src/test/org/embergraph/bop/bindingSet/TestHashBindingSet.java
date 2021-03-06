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

import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;

/*
 * Unit tests for {@link HashBindingSet}.
 *
 * <p>Note: a) these tests assume that the values held for a given key are not cloned, i.e.
 * comparison is done by '==' and not '.equals' b) keys with the same 'name' are a unique object.
 *
 * @author <a href="mailto:dmacgbr@users.sourceforge.net">David MacMillan</a>
 * @version $Id$
 */
public class TestHashBindingSet extends TestIBindingSet {

  /** */
  public TestHashBindingSet() {}

  /** @param name */
  public TestHashBindingSet(String name) {
    super(name);
  }

  /** Unit test for {@link HashBindingSet#HashBindingSet()} */
  public void testConstructorHashBindingSet() {
    assertTrue(null != new HashBindingSet());
  }

  /** Unit test for {@link HashBindingSet#HashBindingSet(HashBindingSet)} */
  public void testConstructorHashBindingSetHashBindingSet() {
    // TODO what is our philosophy on argument validation?
    //        try { assertTrue ( null != new HashBindingSet ( null ) ) ; fail (
    // "IllegalArgumentException expected, copy from was null" ) ; }
    //        catch ( IllegalArgumentException e ) {}

    Var<?> var1 = Var.var("a");
    Var<?> var2 = Var.var("b");
    Constant<Integer> val1 = new Constant<>(1);
    Constant<Integer> val2 = new Constant<>(2);
    IVariable<?>[] vars = new IVariable[] {var1, var2};
    IConstant<?>[] vals = new IConstant[] {val1, val2};

    assertEqual(new HashBindingSet(new HashBindingSet(vars, vals)), vars, vals);
  }

  /** Unit test for {@link HashBindingSet#HashBindingSet(IBindingSet)} */
  public void testConstructorHashBindingSetIBindingSet() {
    // TODO what is our philosophy on argument validation?
    //        try { assertTrue ( null != new HashBindingSet ( null ) ) ; fail (
    // "IllegalArgumentException expected, copy from was null" ) ; }
    //        catch ( IllegalArgumentException e ) {}

    Var<?> var1 = Var.var("a");
    Var<?> var2 = Var.var("b");
    Constant<Integer> val1 = new Constant<>(1);
    Constant<Integer> val2 = new Constant<>(2);
    IVariable<?>[] vars = new IVariable[] {var1, var2};
    IConstant<?>[] vals = new IConstant[] {val1, val2};

    assertEqual(new HashBindingSet(new ListBindingSet(vars, vals)), vars, vals);
  }

  /** Unit test for {@link HashBindingSet#HashBindingSet(IVariable[],IConstant[])} */
  public void testConstructorVariablesConstants() {
    Var<?> var1 = Var.var("a");
    Var<?> var2 = Var.var("b");
    Constant<Integer> val1 = new Constant<>(1);
    Constant<Integer> val2 = new Constant<>(2);
    IVariable<?>[] vars = new IVariable[] {var1, var2};
    IConstant<?>[] vals = new IConstant[] {val1, val2};

    try {
      assertTrue(null != new HashBindingSet(null, vals));
      fail("IllegalArgumentException expected, vars was null");
    } catch (IllegalArgumentException e) {
    }

    try {
      assertTrue(null != new HashBindingSet(vars, null));
      fail("IllegalArgumentException expected, vals was null");
    } catch (IllegalArgumentException e) {
    }

    try {
      assertTrue(null != new HashBindingSet(vars, new IConstant[] {val1}));
      fail("IllegalArgumentException expected, vars and vals were different sizes");
    } catch (IllegalArgumentException e) {
    }

    assertEqual(new HashBindingSet(vars, vals), vars, vals);
  }

  @Override
  protected IBindingSet newBindingSet(IVariable<?>[] vars, IConstant<?>[] vals) {
    return new HashBindingSet(vars, vals);
  }

  @Override
  protected IBindingSet newBindingSet(int size) {
    return new HashBindingSet();
  }
}
