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
package org.embergraph.blueprints;

import java.io.File;
import java.util.Properties;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.remote.EmbergraphSailFactory;
import org.embergraph.rdf.sail.remote.EmbergraphSailFactory.Option;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;

/*
 * Class to test EmbergraphGraphEmbedded creation using a SailRepository for client test suite
 * coverage.
 *
 * @author beebs
 */
public class TestEmbergraphGraphEmbeddedRepository extends AbstractTestEmbergraphGraphFactory {

  public void setProperties() {

    Properties props = System.getProperties();

    // no inference
    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");

    // no text index
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    // triples mode
    props.setProperty(EmbergraphSail.Options.QUADS, "false");
    props.setProperty(EmbergraphSail.Options.STATEMENT_IDENTIFIERS, "false");
  }

  public EmbergraphSail getOrCreateRepository(String journalFile) {

    final java.util.Properties props = new java.util.Properties();
    SailRepository repo = null;

    /*
     * Lax edges allows us to use non-unique edge identifiers
     */
    props.setProperty(EmbergraphGraph.Options.LAX_EDGES, "true");

    /*
     * SPARQL bottom up evaluation semantics can have performance impact.
     */
    props.setProperty(AbstractTripleStore.Options.BOTTOM_UP_EVALUATION, "false");

    if (journalFile == null || !new File(journalFile).exists()) {

      /*
       * No journal specified or journal does not exist yet at specified
       * location. Create a new store. (If journal== null an in-memory
       * store will be created.
       */
      repo =
          EmbergraphSailFactory.createRepository(
              props, journalFile, Option.TextIndex); // , Option.RDR);

    } else {

      /*
       * Journal already exists at specified location. Open existing
       * store.
       */
      repo = EmbergraphSailFactory.openRepository(journalFile);
    }

    try {
      repo.initialize();
    } catch (RepositoryException e) {
      e.printStackTrace();
      testPrint(e.toString());
    }

    return (EmbergraphSail) repo.getSail();
  }

  @Override
  protected EmbergraphGraph getNewGraph(String file) throws Exception {

    return loadGraph(file);
  }

  @Override
  protected EmbergraphGraph loadGraph(String file) throws Exception {

    return new EmbergraphGraphEmbedded(getOrCreateRepository(file));
  }
}
