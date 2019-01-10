/**
Copyright (C) SYSTAP, LLC DBA Blazegraph 2011.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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

package org.embergraph.rdf.sail;

import java.io.IOException;
import java.util.Properties;

import org.openrdf.model.ValueFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

import com.bigdata.rdf.axioms.NoAxioms;
import com.bigdata.rdf.vocab.NoVocabulary;

/**
 * Unit test template for use in submission of bugs.
 * <p>
 * This test case will delegate to an underlying backing store. You can specify
 * this store via a JVM property as follows:
 * <code>-DtestClass=com.bigdata.rdf.sail.TestBigdataSailWithQuads</code>
 * <p>
 * There are three possible configurations for the testClass:
 * <ul>
 * <li>com.bigdata.rdf.sail.TestBigdataSailWithQuads (quads mode)</li>
 * <li>com.bigdata.rdf.sail.TestBigdataSailWithoutSids (triples mode)</li>
 * <li>com.bigdata.rdf.sail.TestBigdataSailWithSids (SIDs mode)</li>
 * </ul>
 * <p>
 * The default for triples and SIDs mode is for inference with truth maintenance
 * to be on. If you would like to turn off inference, make sure to do so in
 * {@link #getProperties()}.
 * 
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id: TestTicket276.java 4613 2011-06-03 11:35:18Z thompsonbry $
 * 
 * @see https://sourceforge.net/apps/trac/bigdata/ticket/355
 * @see https://sourceforge.net/apps/trac/bigdata/ticket/356
 */
public class TestTicket353 extends QuadsTestCase {

	public TestTicket353() {
	}

	public TestTicket353(String arg0) {
		super(arg0);
	}

	/**
	 * Please set your database properties here, except for your journal file,
	 * please DO NOT SPECIFY A JOURNAL FILE.
	 */
	@Override
	public Properties getProperties() {

		final Properties props = super.getProperties();

		/*
		 * For example, here is a set of five properties that turns off
		 * inference, truth maintenance, and the free text index.
		 */
		props.setProperty(BigdataSail.Options.AXIOMS_CLASS,
				NoAxioms.class.getName());
		props.setProperty(BigdataSail.Options.VOCABULARY_CLASS,
				NoVocabulary.class.getName());
		props.setProperty(BigdataSail.Options.TRUTH_MAINTENANCE, "false");
		props.setProperty(BigdataSail.Options.JUSTIFY, "false");
		props.setProperty(BigdataSail.Options.INLINE_DATE_TIMES, "true");
		props.setProperty(BigdataSail.Options.ISOLATABLE_INDICES, "true");
		props.setProperty(BigdataSail.Options.EXACT_SIZE, "true");
//		props.setProperty(BigdataSail.Options.ALLOW_SESAME_QUERY_EVALUATION,
//				"false");
		props.setProperty(BigdataSail.Options.STATEMENT_IDENTIFIERS, "false");

		return props;

	}

	public void testBug() throws Exception {
		// try with Sesame MemoryStore:
		executeQuery(new SailRepository(new MemoryStore()));

		// try with Bigdata:
		final BigdataSail sail = getSail();
		try {
			executeQuery(new BigdataSailRepository(sail));
		} finally {
			sail.__tearDownUnitTest();
		}

	}

	private void executeQuery(final SailRepository repo)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException, RDFParseException, IOException,
			RDFHandlerException {
		try {
			repo.initialize();
			final RepositoryConnection conn = repo.getConnection();
			conn.setAutoCommit(false);
			try {
				final ValueFactory vf = conn.getValueFactory();
				conn.add(vf.createURI("os:subject"), vf.createURI("os:prop"), vf.createLiteral("value"));
				conn.commit();

				String query = 
					"SELECT ?b { {} union { ?a ?b ?c } }"
					;
				
				TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
				TupleQueryResult tqr = tq.evaluate();
				assertTrue(tqr.hasNext());
				
				while (tqr.hasNext()) {
					System.err.println(tqr.next());
				}
				
				tqr.close();
			} finally {
				conn.close();
			}
		} finally {
			repo.shutDown();
		}
	}
}
