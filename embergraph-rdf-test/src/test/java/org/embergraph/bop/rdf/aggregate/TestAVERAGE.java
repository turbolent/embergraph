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
package org.embergraph.bop.rdf.aggregate;

import java.math.BigDecimal;
import junit.framework.TestCase2;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.Constant;
import org.embergraph.bop.ContextBindingSet;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.TestMockUtility;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.journal.ITx;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.constraints.MathBOp;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.internal.impl.literal.XSDDecimalIV;
import org.embergraph.rdf.internal.impl.literal.XSDNumericIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.util.InnerCause;

/*
 * Unit tests for {@link AVERAGE}.
 *
 * @author thompsonbry
 */
public class TestAVERAGE extends TestCase2 {

  public TestAVERAGE() {}

  public TestAVERAGE(String name) {
    super(name);
  }

  protected void setUp() throws Exception {

    super.setUp();
  }

  protected void tearDown() throws Exception {

    super.tearDown();
  }

  public void test_average() {

    final IVariable<IV> org = Var.var("org");
    final IVariable<IV> auth = Var.var("auth");
    final IVariable<IV> book = Var.var("book");
    final IVariable<IV> lprice = Var.var("lprice");

    final IConstant<String> org1 = new Constant<String>("org1");
    final IConstant<String> org2 = new Constant<String>("org2");
    final IConstant<String> auth1 = new Constant<String>("auth1");
    final IConstant<String> auth2 = new Constant<String>("auth2");
    final IConstant<String> auth3 = new Constant<String>("auth3");
    final IConstant<String> book1 = new Constant<String>("book1");
    final IConstant<String> book2 = new Constant<String>("book2");
    final IConstant<String> book3 = new Constant<String>("book3");
    final IConstant<String> book4 = new Constant<String>("book4");
    final IConstant<XSDNumericIV<EmbergraphLiteral>> price5 =
        new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(5));
    final IConstant<XSDNumericIV<EmbergraphLiteral>> price7 =
        new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(7));
    final IConstant<XSDNumericIV<EmbergraphLiteral>> price9 =
        new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(9));

    /*
     * The test data:
     *
     * <pre>
     * ?org  ?auth  ?book  ?lprice
     * org1  auth1  book1  9
     * org1  auth1  book3  5
     * org1  auth2  book3  7
     * org2  auth3  book4  7
     * </pre>
     */
    final IBindingSet[] data =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org1, auth1, book1, price9}),
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org1, auth1, book2, price5}),
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org1, auth2, book3, price7}),
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org2, auth3, book4, price7})
        };

    final AVERAGE op = new AVERAGE(false /* distinct */, lprice);
    assertFalse(op.isDistinct());
    assertFalse(op.isWildcard());

    op.reset();
    for (IBindingSet bs : data) {
      op.get(bs);
    }
    assertEquals(new XSDDecimalIV(new BigDecimal((9 + 5 + 7 + 7) / 4.)), op.done());
  }

  public void test_average_with_complex_inner_value_expression() {

    AbstractTripleStore kb = TestMockUtility.mockTripleStore(getName());
    try {
      final BOpContext<IBindingSet> context = TestMockUtility.mockContext(kb);
      final String lexiconNamespace = kb.getLexiconRelation().getNamespace();

      GlobalAnnotations globals = new GlobalAnnotations(lexiconNamespace, ITx.READ_COMMITTED);

      final IVariable<IV> org = Var.var("org");
      final IVariable<IV> auth = Var.var("auth");
      final IVariable<IV> book = Var.var("book");
      final IVariable<IV> lprice = Var.var("lprice");

      final IConstant<String> org1 = new Constant<String>("org1");
      final IConstant<String> org2 = new Constant<String>("org2");
      final IConstant<String> auth1 = new Constant<String>("auth1");
      final IConstant<String> auth2 = new Constant<String>("auth2");
      final IConstant<String> auth3 = new Constant<String>("auth3");
      final IConstant<String> book1 = new Constant<String>("book1");
      final IConstant<String> book2 = new Constant<String>("book2");
      final IConstant<String> book3 = new Constant<String>("book3");
      final IConstant<String> book4 = new Constant<String>("book4");
      final IConstant<XSDNumericIV<EmbergraphLiteral>> price5 =
          new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(5));
      final IConstant<XSDNumericIV<EmbergraphLiteral>> price7 =
          new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(7));
      final IConstant<XSDNumericIV<EmbergraphLiteral>> price9 =
          new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(9));

      /*
       * The test data:
       *
       * <pre>
       * ?org  ?auth  ?book  ?lprice
       * org1  auth1  book1  9
       * org1  auth1  book3  5
       * org1  auth2  book3  7
       * org2  auth3  book4  7
       * </pre>
       */
      final IBindingSet[] data =
          new IBindingSet[] {
            new ContextBindingSet(
                context,
                new ListBindingSet(
                    new IVariable<?>[] {org, auth, book, lprice},
                    new IConstant[] {org1, auth1, book1, price9})),
            new ContextBindingSet(
                context,
                new ListBindingSet(
                    new IVariable<?>[] {org, auth, book, lprice},
                    new IConstant[] {org1, auth1, book2, price5})),
            new ContextBindingSet(
                context,
                new ListBindingSet(
                    new IVariable<?>[] {org, auth, book, lprice},
                    new IConstant[] {org1, auth2, book3, price7})),
            new ContextBindingSet(
                context,
                new ListBindingSet(
                    new IVariable<?>[] {org, auth, book, lprice},
                    new IConstant[] {org2, auth3, book4, price7}))
          };

      // AVERAGE(lprice*2)
      final AVERAGE op =
          new AVERAGE(
              false /* distinct */,
              new MathBOp(
                  lprice, new Constant<IV>(new XSDNumericIV(2)), MathBOp.MathOp.MULTIPLY, globals));
      assertFalse(op.isDistinct());
      assertFalse(op.isWildcard());

      op.reset();
      for (IBindingSet bs : data) {
        op.get(bs);
      }
      assertEquals(
          new XSDDecimalIV(BigDecimal.valueOf((9 * 2 + 5 * 2 + 7 * 2 + 7 * 2) / 4.)), op.done());
    } finally {
      kb.getIndexManager().destroy();
    }
  }

  public void test_average_with_null() {

    final IVariable<IV> org = Var.var("org");
    final IVariable<IV> auth = Var.var("auth");
    final IVariable<IV> book = Var.var("book");
    final IVariable<IV> lprice = Var.var("lprice");

    final IConstant<String> org1 = new Constant<String>("org1");
    final IConstant<String> org2 = new Constant<String>("org2");
    final IConstant<String> auth1 = new Constant<String>("auth1");
    final IConstant<String> auth2 = new Constant<String>("auth2");
    final IConstant<String> auth3 = new Constant<String>("auth3");
    final IConstant<String> book1 = new Constant<String>("book1");
    final IConstant<String> book2 = new Constant<String>("book2");
    final IConstant<String> book3 = new Constant<String>("book3");
    final IConstant<String> book4 = new Constant<String>("book4");
    final IConstant<XSDNumericIV<EmbergraphLiteral>> price5 =
        new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(5));
    final IConstant<XSDNumericIV<EmbergraphLiteral>> price7 =
        new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(7));
    final IConstant<XSDNumericIV<EmbergraphLiteral>> price9 =
        new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(9));

    /*
     * The test data:
     *
     * <pre>
     * ?org  ?auth  ?book  ?lprice
     * org1  auth1  book1  9
     * org1  auth1  book3  5
     * org1  auth2  book3  7
     * org2  auth3  book4  7
     * </pre>
     */
    final IBindingSet[] data =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org1, auth1, book1, price9}),
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org1, auth1, book2, price5}),
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book}, new IConstant[] {org1, auth2, book3}),
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org2, auth3, book4, price7})
        };

    final AVERAGE op = new AVERAGE(false /* distinct */, lprice);
    assertFalse(op.isDistinct());
    assertFalse(op.isWildcard());

    op.reset();
    for (IBindingSet bs : data) {
      op.get(bs);
    }

    /*
     * Note: This assert is actually a bit tricky. If you setup the problem
     * as (9+7+7)/3 then you get 23/3 which has a non-terminating decimal
     * and would require you to setup the appropriate rounding mode and make
     * sure that the rounding mode is consistent with IVUtility's numerical
     * math routines. Therefore I've setup the problem as 21/3, which is
     * exactly 7 to make life a bit easier. Tests of the numerical math
     * routines really belong somewhere else.
     */
    assertEquals(
        new XSDDecimalIV(new BigDecimal(9 + 5 + /*7 +*/ 7).divide(new BigDecimal(3.))), op.done());
  }

  public void test_average_with_errors() {

    final EmbergraphValueFactory f = EmbergraphValueFactoryImpl.getInstance(getName());

    final IVariable<IV> org = Var.var("org");
    final IVariable<IV> auth = Var.var("auth");
    final IVariable<IV> book = Var.var("book");
    final IVariable<IV> lprice = Var.var("lprice");

    final IConstant<String> org1 = new Constant<String>("org1");
    final IConstant<String> org2 = new Constant<String>("org2");
    final IConstant<String> auth1 = new Constant<String>("auth1");
    final TermId tid1 = new TermId<EmbergraphValue>(VTE.LITERAL, 1);
    tid1.setValue(f.createLiteral("auth2"));
    final IConstant<IV> auth2 = new Constant<IV>(tid1);
    final IConstant<String> auth3 = new Constant<String>("auth3");
    final IConstant<String> book1 = new Constant<String>("book1");
    final IConstant<String> book2 = new Constant<String>("book2");
    final IConstant<String> book3 = new Constant<String>("book3");
    final IConstant<String> book4 = new Constant<String>("book4");
    final IConstant<XSDNumericIV<EmbergraphLiteral>> price5 =
        new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(5));
    final IConstant<XSDNumericIV<EmbergraphLiteral>> price7 =
        new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(7));
    final IConstant<XSDNumericIV<EmbergraphLiteral>> price9 =
        new Constant<XSDNumericIV<EmbergraphLiteral>>(new XSDNumericIV<EmbergraphLiteral>(9));

    /*
     * The test data:
     *
     * <pre>
     * ?org  ?auth  ?book  ?lprice
     * org1  auth1  book1  9
     * org1  auth1  book3  5
     * org1  auth2  book3  7
     * org2  auth3  book4  7
     * </pre>
     */
    final IBindingSet[] data =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org1, auth1, book1, price9}),
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org1, auth1, book2, price5}),
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org1, auth2, book3, auth2}),
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org2, auth3, book4, price7})
        };

    final AVERAGE op = new AVERAGE(false /* distinct */, lprice);
    assertFalse(op.isDistinct());
    assertFalse(op.isWildcard());

    try {
      op.reset();
      for (IBindingSet bs : data) {
        op.get(bs);
      }
      fail("Expecting: " + SparqlTypeErrorException.class);
    } catch (RuntimeException ex) {
      if (InnerCause.isInnerCause(ex, SparqlTypeErrorException.class)) {
        if (log.isInfoEnabled()) {
          log.info("Ignoring expected exception: " + ex);
        }
      } else {
        fail("Expecting: " + SparqlTypeErrorException.class, ex);
      }
    }

    /*
     * Now verify that the error is sticky.
     */
    try {
      op.done();
      fail("Expecting: " + SparqlTypeErrorException.class);
    } catch (RuntimeException ex) {
      if (InnerCause.isInnerCause(ex, SparqlTypeErrorException.class)) {
        if (log.isInfoEnabled()) {
          log.info("Ignoring expected exception: " + ex);
        }
      } else {
        fail("Expecting: " + SparqlTypeErrorException.class, ex);
      }
    }

    /*
     * Now verify that reset() clears the error.
     */
    op.reset();
    op.done();
  }
}
