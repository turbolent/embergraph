package org.embergraph.rdf.sail.webapp;

import junit.framework.TestCase;
import org.embergraph.rdf.sail.remote.EmbergraphSailRemoteRepository;
import org.embergraph.rdf.sail.webapp.client.RemoteRepositoryManager;
import org.embergraph.util.httpd.Config;
import org.openrdf.model.Resource;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;

/*
 * Helper class to debug the NSS by issuing commands that we can not issue from the index.html page
 * (HTTP DELETEs, etc).
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestHelper extends TestCase {

  /*
   * @param args
   * @throws Exception
   */
  public static void main(final String[] args) throws Exception {

    if (args.length != 1) {

      System.err.println("usage: SPARQL-Endpoint-URL");

      System.exit(1);
    }

    final String sparqlEndpointURL = args[0];

    final RemoteRepositoryManager mgr =
        new RemoteRepositoryManager("localhost:" + Config.HTTP_PORT /* serviceURLIsIngored */);

    try {

      final EmbergraphSailRemoteRepository repo =
          mgr.getRepositoryForURL(sparqlEndpointURL).getEmbergraphSailRemoteRepository();

      RepositoryConnection cxn = null;
      try {

        cxn = repo.getConnection();

        cxn.remove(null /* s */, RDF.TYPE, FOAF.PERSON, (Resource[]) null /* c */);

      } finally {

        if (cxn != null) {
          cxn.close();
          cxn = null;
        }

        repo.shutDown();
      }

    } finally {

      mgr.close();
    }
  }
}
