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

import java.math.BigInteger;
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
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.internal.constraints.MathBOp;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.internal.impl.literal.XSDIntegerIV;
import org.embergraph.rdf.internal.impl.literal.XSDNumericIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.util.InnerCause;

/*
 * Unit tests for {@link COUNT}.
 *
 * @author thompsonbry
 */
public class TestCOUNT extends TestCase2 {

  public TestCOUNT() {}

  public TestCOUNT(String name) {
    super(name);
  }

  protected void setUp() throws Exception {

    super.setUp();
  }

  protected void tearDown() throws Exception {

    super.tearDown();
  }

  public void test_count() {

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

    final COUNT op = new COUNT(false /* distinct */, lprice);
    assertFalse(op.isDistinct());
    assertFalse(op.isWildcard());

    op.reset();
    for (IBindingSet bs : data) {
      op.get(bs);
    }
    assertEquals(new XSDIntegerIV(new BigInteger(Long.toString((long) data.length))), op.done());
  }

  public void test_count_with_complex_inner_value_expression() {

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

      // COUNT(lprice+1)
      final COUNT op =
          new COUNT(
              false /* distinct */,
              new MathBOp(
                  lprice, new Constant<IV>(new XSDNumericIV(1)), MathBOp.MathOp.PLUS, globals));
      assertFalse(op.isDistinct());
      assertFalse(op.isWildcard());

      op.reset();
      for (IBindingSet bs : data) {
        op.get(bs);
      }
      assertEquals(new XSDIntegerIV(new BigInteger(Long.toString((long) data.length))), op.done());
    } finally {
      kb.getIndexManager().destroy();
    }
  }

  public void test_count_with_null() {

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
     * org1  auth1  book3  NULL
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
              new IVariable<?>[] {
                org, auth, book,
              },
              new IConstant[] {
                org1, auth1, book2,
              }),
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org1, auth2, book3, price7}),
          new ListBindingSet(
              new IVariable<?>[] {org, auth, book, lprice},
              new IConstant[] {org2, auth3, book4, price7})
        };

    final COUNT op = new COUNT(false /* distinct */, lprice);
    assertFalse(op.isDistinct());
    assertFalse(op.isWildcard());

    op.reset();
    for (IBindingSet bs : data) {
      op.get(bs);
    }
    assertEquals(new XSDIntegerIV(new BigInteger(Long.toString(3L))), op.done());
  }

  public void test_count_with_errors() {

    AbstractTripleStore kb = TestMockUtility.mockTripleStore(getName());
    try {
      final BOpContext<IBindingSet> context = TestMockUtility.mockContext(kb);
      final String lexiconNamespace = kb.getLexiconRelation().getNamespace();

      GlobalAnnotations globals = new GlobalAnnotations(lexiconNamespace, ITx.READ_COMMITTED);

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
                    new IConstant[] {org1, auth2, book3, auth2})),
            new ContextBindingSet(
                context,
                new ListBindingSet(
                    new IVariable<?>[] {org, auth, book, lprice},
                    new IConstant[] {org2, auth3, book4, price7}))
          };

      /*
       * Setup a materialized IV for ZERO (0).
       */
      final IV ZERO = new XSDNumericIV(0);
      ZERO.setValue(f.createLiteral("0", XSD.INT));

      // Note: Formula will produce an error for non-numeric data.
      final COUNT op =
          new COUNT(
              false /* distinct */,
              new MathBOp(lprice, new Constant<IV>(ZERO), MathBOp.MathOp.PLUS, globals));
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
    } finally {
      kb.getIndexManager().destroy();
    }
  }
}
