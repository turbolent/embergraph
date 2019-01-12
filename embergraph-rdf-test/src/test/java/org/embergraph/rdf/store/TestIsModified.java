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
 * Created on Feb 4, 2010
 */

package org.embergraph.rdf.store;

import java.util.Properties;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPORelation;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

/*
* Test suite for {@link ISPO#isModified()}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestIsModified extends AbstractTripleStoreTestCase {

  /** */
  public TestIsModified() {}

  /** @param name */
  public TestIsModified(String name) {
    super(name);
  }

  /*
   * Unit test for {@link ISPO#isModified()}. The test explores the correct reporting of statement
   * modification as statements are asserted and retracted on the database using the low-level
   * {@link SPORelation} API.
   */
  public void test_reportMutation() {

    final Properties properties = super.getProperties();

    // override the default vocabulary.
    properties.setProperty(
        AbstractTripleStore.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    // override the default axiom model.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS,
        NoAxioms.class.getName());

    final AbstractTripleStore store = getStore(properties);

    try {

      final EmbergraphValueFactory valueFactory = store.getValueFactory();

      final EmbergraphURI x = valueFactory.createURI("http://www.foo.org/x");
      final EmbergraphURI y = valueFactory.createURI("http://www.foo.org/y");
      final EmbergraphURI z = valueFactory.createURI("http://www.foo.org/z");

      final EmbergraphURI A = valueFactory.createURI("http://www.foo.org/A");
      final EmbergraphURI B = valueFactory.createURI("http://www.foo.org/B");
      final EmbergraphURI C = valueFactory.createURI("http://www.foo.org/C");

      final EmbergraphURI rdfType = valueFactory.createURI(RDF.TYPE.stringValue());

      final EmbergraphURI rdfsSubClassOf = valueFactory.createURI(RDFS.SUBCLASSOF.stringValue());

      // resolve term identifiers.
      store.addTerms(new EmbergraphValue[] {x, y, z, A, B, C, rdfType, rdfsSubClassOf});

      // Add a bunch of statements.
      {
        final EmbergraphStatement s1 =
            valueFactory.createStatement(x, rdfType, C, null /* c */, StatementEnum.Explicit);

        final EmbergraphStatement s2 =
            valueFactory.createStatement(y, rdfType, B, null /* c */, StatementEnum.Explicit);

        final EmbergraphStatement s3 =
            valueFactory.createStatement(z, rdfType, A, null /* c */, StatementEnum.Explicit);

        final EmbergraphStatement s4 =
            valueFactory.createStatement(
                B, rdfsSubClassOf, A, null /* c */, StatementEnum.Explicit);

        final EmbergraphStatement s5 =
            valueFactory.createStatement(
                C, rdfsSubClassOf, B, null /* c */, StatementEnum.Explicit);

        assertFalse(s1.isModified());
        assertFalse(s2.isModified());
        assertFalse(s3.isModified());
        assertFalse(s4.isModified());
        assertFalse(s5.isModified());

        store.getSPORelation().insert(new ISPO[] {s1, s2, s3, s4, s5}, 5, null /* filter */);

        assertTrue(s1.isModified());
        assertTrue(s2.isModified());
        assertTrue(s3.isModified());
        assertTrue(s4.isModified());
        assertTrue(s5.isModified());
      }

      // Delete two statements, only one of which exists.
      {

        // statement exists.
        final EmbergraphStatement s1 =
            valueFactory.createStatement(x, rdfType, C, null /* c */, StatementEnum.Explicit);

        // statement does not exist.
        final EmbergraphStatement s2 =
            valueFactory.createStatement(x, x, x, null /* c */, StatementEnum.Explicit);

        assertFalse(store.hasStatement(s2.s(), s2.p(), s2.o(), s2.c()));

        assertFalse(s1.isModified());
        assertFalse(s2.isModified());

        assertEquals(1, store.getSPORelation().delete(new ISPO[] {s1, s2}, 2));

        assertTrue(s1.isModified());
        assertFalse(s2.isModified());
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }
}
