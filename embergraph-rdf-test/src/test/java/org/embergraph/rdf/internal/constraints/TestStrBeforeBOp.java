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
 * Created on March 11, 2008
 */

package org.embergraph.rdf.internal.constraints;

import org.embergraph.bop.Constant;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.journal.ITx;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sparql.ast.DummyConstantNode;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.ProxyTestCase;

/*
 * Test suite for {@link StrBeforeBOp}.
 *
 * @author <a href="mailto:mpersonick@users.sourceforge.net">Mike Personick</a>
 */
public class TestStrBeforeBOp extends ProxyTestCase {

  //	private static final Logger log = Logger.getLogger(TestSubstrBOp.class);

  /** */
  public TestStrBeforeBOp() {
    super();
  }

  /** @param name */
  public TestStrBeforeBOp(String name) {
    super(name);
  }

  //    @Override
  //    public Properties getProperties() {
  //    	final Properties props = super.getProperties();
  //    	props.setProperty(EmbergraphSail.Options.INLINE_DATE_TIMES, "true");
  //    	return props;
  //    }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void test_bop() {

    final AbstractTripleStore db = getStore();

    try {

      final EmbergraphValueFactory vf = db.getValueFactory();

      final ListBindingSet emptyBindingSet = new ListBindingSet();

      // strbefore("abc","b") -> "a"
      {
        final IV expected = DummyConstantNode.toDummyIV(vf.createLiteral("a"));

        final IV arg1 = DummyConstantNode.toDummyIV(vf.createLiteral("abc"));

        final IV arg2 = DummyConstantNode.toDummyIV(vf.createLiteral("b"));

        final IV actual =
            new StrBeforeBOp(
                new Constant<>(arg1),
                new Constant<>(arg2),
                    new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED))
                .get(emptyBindingSet);

        assertEquals(expected, actual);
      }

      // strbefore("abc"@en,"bc") -> "a"@en
      {
        final IV expected = DummyConstantNode.toDummyIV(vf.createLiteral("a", "en"));

        final IV arg1 = DummyConstantNode.toDummyIV(vf.createLiteral("abc", "en"));

        final IV arg2 = DummyConstantNode.toDummyIV(vf.createLiteral("bc"));

        final IV actual =
            new StrBeforeBOp(
                new Constant<>(arg1),
                new Constant<>(arg2),
                    new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED))
                .get(emptyBindingSet);

        assertEquals(expected, actual);
      }

      // strbefore("abc"@en,"b"@cy) -> error
      {
        final IV arg1 = DummyConstantNode.toDummyIV(vf.createLiteral("abc", "en"));

        final IV arg2 = DummyConstantNode.toDummyIV(vf.createLiteral("b", "cy"));

        try {
          final IV actual =
              new StrBeforeBOp(
                  new Constant<>(arg1),
                  new Constant<>(arg2),
                      new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED))
                  .get(emptyBindingSet);

          fail("should be a type error");
        } catch (SparqlTypeErrorException ex) {
        }
      }

      // strbefore("abc"^^xsd:string,"") -> ""^^xsd:string
      {
        final IV expected = DummyConstantNode.toDummyIV(vf.createLiteral("", XSD.STRING));

        final IV arg1 = DummyConstantNode.toDummyIV(vf.createLiteral("abc", XSD.STRING));

        final IV arg2 = DummyConstantNode.toDummyIV(vf.createLiteral(""));

        final IV actual =
            new StrBeforeBOp(
                new Constant<>(arg1),
                new Constant<>(arg2),
                    new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED))
                .get(emptyBindingSet);

        assertEquals(expected, actual);
      }

      // strbefore("abc","xyz") -> ""
      {
        final IV expected = DummyConstantNode.toDummyIV(vf.createLiteral(""));

        final IV arg1 = DummyConstantNode.toDummyIV(vf.createLiteral("abc"));

        final IV arg2 = DummyConstantNode.toDummyIV(vf.createLiteral("xyz"));

        final IV actual =
            new StrBeforeBOp(
                new Constant<>(arg1),
                new Constant<>(arg2),
                    new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED))
                .get(emptyBindingSet);

        assertEquals(expected, actual);
      }

      // strbefore("abc","ab") -> ""
      {
        final IV expected = DummyConstantNode.toDummyIV(vf.createLiteral(""));

        final IV arg1 = DummyConstantNode.toDummyIV(vf.createLiteral("abc"));

        final IV arg2 = DummyConstantNode.toDummyIV(vf.createLiteral("ab"));

        final IV actual =
            new StrBeforeBOp(
                new Constant<>(arg1),
                new Constant<>(arg2),
                    new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED))
                .get(emptyBindingSet);

        assertEquals(expected, actual);
      }

    } finally {

      db.__tearDownUnitTest();
    }
  }
}
