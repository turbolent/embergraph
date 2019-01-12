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
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.BindingImpl;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

/*
* @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class TestMultiGraphs extends ProxyEmbergraphSailTestCase {

  protected static final Logger log = Logger.getLogger(TestMultiGraphs.class);

  @Override
  public Properties getProperties() {

    final Properties props = super.getProperties();

    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
    props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    return props;
  }

  /** */
  public TestMultiGraphs() {}

  /** @param arg0 */
  public TestMultiGraphs(String arg0) {
    super(arg0);
  }

  public void testMultiGraphs() throws Exception {

    final Sail sail;
    final SailRepository repo;
    final SailRepositoryConnection cxn;

    /*
     * You'll get the same answer whether you run with Embergraph or Sesame.
     */
    if (true) {

      final EmbergraphSail bdSail = getSail();
      sail = bdSail;

      //            if (bdSail.getDatabase().isQuads() == false) {
      //                bdSail.__tearDownUnitTest();
      //                return;
      //            }

      repo = new EmbergraphSailRepository(bdSail);

    } else {
      sail = new MemoryStore();
      repo = new SailRepository(sail);
    }

    repo.initialize();
    cxn = repo.getConnection();

    try {

      if (cxn instanceof EmbergraphSailRepositoryConnection
          && ((EmbergraphSailRepositoryConnection) cxn).getTripleStore().isQuads() == false) {
        ((EmbergraphSail) sail).__tearDownUnitTest();
        return;
      }

      cxn.setAutoCommit(false);

      final ValueFactory vf = sail.getValueFactory();

      final String ns = BD.NAMESPACE;

      final URI mike = vf.createURI(ns + "Mike");
      //            URI bryan = vf.createURI(ns+"Bryan");
      final URI person = vf.createURI(ns + "Person");
      final URI likes = vf.createURI(ns + "likes");
      final URI rdf = vf.createURI(ns + "RDF");
      final Literal l1 = vf.createLiteral("Mike");
      //            Literal l2 = vf.createLiteral("Bryan");
      final URI g1 = vf.createURI(ns + "graph1");
      final URI g2 = vf.createURI(ns + "graph2");
      /**/
      cxn.setNamespace("ns", ns);

      cxn.add(mike, RDF.TYPE, person, g1, g2);
      cxn.add(mike, likes, rdf, g1, g2);
      cxn.add(mike, RDFS.LABEL, l1, g1, g2);
      //            cxn.add(bryan, RDF.TYPE, person, g1, g2);
      //            cxn.add(bryan, likes, rdf, g1, g2);
      //            cxn.add(bryan, RDFS.LABEL, l2, g1, g2);

      /*
       * Note: The either flush() or commit() is required to flush the
       * statement buffers to the database before executing any operations
       * that go around the sail.
       */
      cxn.commit();

      if (log.isInfoEnabled()) {
        if (sail instanceof EmbergraphSail)
          log.info("\n" + ((EmbergraphSailRepositoryConnection) cxn).getTripleStore().dumpStore());
      }

      {
        String query =
            "PREFIX rdf: <"
                + RDF.NAMESPACE
                + "> "
                + "PREFIX rdfs: <"
                + RDFS.NAMESPACE
                + "> "
                + "PREFIX ns: <"
                + ns
                + "> "
                + "select distinct ?p ?o "
                + "WHERE { "
                +
                //                    "  ?s rdf:type ns:Person . " +
                "  ns:Mike ?p ?o . "
                + "}";

        final TupleQuery tupleQuery = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        final TupleQueryResult result = tupleQuery.evaluate();

        //                while (result.hasNext()) {
        //                    System.err.println(result.next());
        //                }

        final Collection<BindingSet> solution = new LinkedList<BindingSet>();
        solution.add(
            createBindingSet(
                new BindingImpl("p", RDF.TYPE), new BindingImpl("o", person)));
        solution.add(
            createBindingSet(
                new BindingImpl("p", likes), new BindingImpl("o", rdf)));
        solution.add(
            createBindingSet(
                new BindingImpl("p", RDFS.LABEL), new BindingImpl("o", l1)));

        compare(result, solution);
      }

      {
        final String query =
            "PREFIX rdf: <"
                + RDF.NAMESPACE
                + "> "
                + "PREFIX rdfs: <"
                + RDFS.NAMESPACE
                + "> "
                + "PREFIX ns: <"
                + ns
                + "> "
                + "select ?p ?o "
                + "from <"
                + g1
                + ">"
                + "from <"
                + g2
                + ">"
                + "WHERE { "
                + "  ns:Mike ?p ?o . "
                + "}";

        final TupleQuery tupleQuery = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        final TupleQueryResult result = tupleQuery.evaluate();

        //                while (result.hasNext()) {
        //                    System.err.println(result.next());
        //                }

        final Collection<BindingSet> solution = new LinkedList<BindingSet>();
        solution.add(
            createBindingSet(
                new BindingImpl("p", RDF.TYPE), new BindingImpl("o", person)));
        solution.add(
            createBindingSet(
                new BindingImpl("p", likes), new BindingImpl("o", rdf)));
        solution.add(
            createBindingSet(
                new BindingImpl("p", RDFS.LABEL), new BindingImpl("o", l1)));

        compare(result, solution);
      }

    } finally {
      cxn.close();
      if (sail instanceof EmbergraphSail) ((EmbergraphSail) sail).__tearDownUnitTest();
    }
  }
}
