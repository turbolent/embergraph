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
 * Created on Sep 16, 2009
 */

package org.embergraph.rdf.sail;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.store.BD;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.BindingImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailTupleQuery;

public class TestSesameFilters extends ProxyEmbergraphSailTestCase {

  protected static final Logger log = Logger.getLogger(TestSesameFilters.class);

  protected static final boolean INFO = log.isInfoEnabled();

  @Override
  public Properties getProperties() {

    Properties props = super.getProperties();

    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    return props;
  }

  /** */
  public TestSesameFilters() {}

  /** @param arg0 */
  public TestSesameFilters(String arg0) {
    super(arg0);
  }

  public void testRegex() throws Exception {

    //        final Sail sail = new MemoryStore();
    //        sail.initialize();
    //        final Repository repo = new SailRepository(sail);

    final EmbergraphSail sail = getSail();
    sail.initialize();
    final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);

    final RepositoryConnection cxn = repo.getConnection();
    cxn.setAutoCommit(false);

    try {

      final ValueFactory vf = sail.getValueFactory();

      /*
       * Create some terms.
       */
      final URI mike = vf.createURI(BD.NAMESPACE + "mike");
      final URI bryan = vf.createURI(BD.NAMESPACE + "bryan");
      final URI person = vf.createURI(BD.NAMESPACE + "Person");
      final Literal l1 = vf.createLiteral("mike personick");
      final Literal l2 = vf.createLiteral("bryan thompson");

      /*
       * Create some statements.
       */
      cxn.add(mike, RDF.TYPE, person);
      cxn.add(mike, RDFS.LABEL, l1);
      cxn.add(bryan, RDF.TYPE, person);
      cxn.add(bryan, RDFS.LABEL, l2);

      /*
       * Note: The either flush() or commit() is required to flush the
       * statement buffers to the database before executing any operations
       * that go around the sail.
       */
      cxn.commit();

      {
        String query =
            "prefix bd: <"
                + BD.NAMESPACE
                + "> "
                + "prefix rdf: <"
                + RDF.NAMESPACE
                + "> "
                + "prefix rdfs: <"
                + RDFS.NAMESPACE
                + "> "
                + "select * "
                + "where { "
                + "  ?s rdf:type bd:Person . "
                + "  ?s rdfs:label ?label . "
                + "  FILTER regex(?label, \"mike\") . "
                + "}";

        final SailTupleQuery tupleQuery =
            (SailTupleQuery) cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        tupleQuery.setIncludeInferred(false /* includeInferred */);

        final Collection<BindingSet> answer = new LinkedList<>();
        answer.add(createBindingSet(new BindingImpl("s", mike), new BindingImpl("label", l1)));

        final TupleQueryResult result = tupleQuery.evaluate();
        compare(result, answer);
      }

    } finally {
      cxn.close();
      sail.shutDown();
    }
  }
}
