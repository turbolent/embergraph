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
 * Created on Jun 10, 2011
 */

package org.embergraph.rdf.lexicon;

import junit.framework.TestCase2;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.bop.Var;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.test.MockTermIdFactory;

/*
 * Test suite for {@link LexAccessPatternEnum}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestLexAccessPatternEnum extends TestCase2 {

  /** */
  public TestLexAccessPatternEnum() {}

  /** @param name */
  public TestLexAccessPatternEnum(String name) {
    super(name);
  }

  public void test_IVBound_pattern() {

    final MockTermIdFactory f = new MockTermIdFactory();

    final IConstant<?> iv = new Constant<IV>(f.newTermId());

    final IVariableOrConstant<?>[] args =
        new IVariableOrConstant[] {
          Var.var(), // value
          iv, // iv
        };

    final LexPredicate p = new LexPredicate(args);

    assertEquals(LexAccessPatternEnum.IVBound, LexAccessPatternEnum.valueOf(p));
  }

  public void test_ValueBound_pattern() {

    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance(getName());

    final IConstant<?> value =
        new Constant<>(vf.createURI("http://www.embergraph.org"));

    final IVariableOrConstant<?>[] args =
        new IVariableOrConstant[] {
          value, // value
          Var.var(), // iv
        };

    final LexPredicate p = new LexPredicate(args);

    assertEquals(LexAccessPatternEnum.ValueBound, LexAccessPatternEnum.valueOf(p));
  }

  public void test_FullyBound_pattern() {

    final MockTermIdFactory f = new MockTermIdFactory();

    final IConstant<?> iv = new Constant<IV>(f.newTermId());

    final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance(getName());

    final IConstant<?> value =
        new Constant<>(vf.createURI("http://www.embergraph.org"));

    final IVariableOrConstant<?>[] args =
        new IVariableOrConstant[] {
          value, // value
          iv, // iv
        };

    final LexPredicate p = new LexPredicate(args);

    assertEquals(LexAccessPatternEnum.FullyBound, LexAccessPatternEnum.valueOf(p));
  }

  public void test_NoneBound_pattern() {

    final IVariableOrConstant<?>[] args =
        new IVariableOrConstant[] {
          Var.var(), // value
          Var.var(), // iv
        };

    final LexPredicate p = new LexPredicate(args);

    assertEquals(LexAccessPatternEnum.NoneBound, LexAccessPatternEnum.valueOf(p));
  }
}
