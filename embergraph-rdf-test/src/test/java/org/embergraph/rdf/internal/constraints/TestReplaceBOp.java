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
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sparql.ast.DummyConstantNode;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.ProxyTestCase;

/*
* Test suite for {@link ReplaceBOp}.
 *
 * @author <a href="mailto:mpersonick@users.sourceforge.net">Mike Personick</a>
 */
public class TestReplaceBOp extends ProxyTestCase {

  //	private static final Logger log = Logger.getLogger(TestSubstrBOp.class);

  /** */
  public TestReplaceBOp() {
    super();
  }

  /** @param name */
  public TestReplaceBOp(String name) {
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

      // replace("abcd", "b", "Z") -> "aZcd"
      {
        final IV expected = DummyConstantNode.toDummyIV(vf.createLiteral("aZcd"));

        final IV var = DummyConstantNode.toDummyIV(vf.createLiteral("abcd"));

        final IV pattern = DummyConstantNode.toDummyIV(vf.createLiteral("b"));

        final IV replacement = DummyConstantNode.toDummyIV(vf.createLiteral("Z"));

        final IV actual =
            new ReplaceBOp(
                    new Constant<IV>(var),
                    new Constant<IV>(pattern),
                    new Constant<IV>(replacement),
                    new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED))
                .get(emptyBindingSet);

        assertEquals(expected, actual);
      }

      // replace("abab", "B", "Z","i") -> "aZaZ"
      {
        final IV expected = DummyConstantNode.toDummyIV(vf.createLiteral("aZaZ"));

        final IV var = DummyConstantNode.toDummyIV(vf.createLiteral("abab"));

        final IV pattern = DummyConstantNode.toDummyIV(vf.createLiteral("B"));

        final IV replacement = DummyConstantNode.toDummyIV(vf.createLiteral("Z"));

        final IV flags = DummyConstantNode.toDummyIV(vf.createLiteral("i"));

        final IV actual =
            new ReplaceBOp(
                    new Constant<IV>(var),
                    new Constant<IV>(pattern),
                    new Constant<IV>(replacement),
                    new Constant<IV>(flags),
                    new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED))
                .get(emptyBindingSet);

        assertEquals(expected, actual);
      }

      // replace("abab", "B.", "Z","i") -> "aZb"
      {
        final IV expected = DummyConstantNode.toDummyIV(vf.createLiteral("aZb"));

        final IV var = DummyConstantNode.toDummyIV(vf.createLiteral("abab"));

        final IV pattern = DummyConstantNode.toDummyIV(vf.createLiteral("B."));

        final IV replacement = DummyConstantNode.toDummyIV(vf.createLiteral("Z"));

        final IV flags = DummyConstantNode.toDummyIV(vf.createLiteral("i"));

        final IV actual =
            new ReplaceBOp(
                    new Constant<IV>(var),
                    new Constant<IV>(pattern),
                    new Constant<IV>(replacement),
                    new Constant<IV>(flags),
                    new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED))
                .get(emptyBindingSet);

        assertEquals(expected, actual);
      }

    } finally {

      db.__tearDownUnitTest();
    }
  }
}
