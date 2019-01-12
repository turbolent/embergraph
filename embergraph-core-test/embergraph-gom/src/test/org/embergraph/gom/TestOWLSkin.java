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
 * Created on Mar 19, 2012
 */
package org.embergraph.gom;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.embergraph.gom.alchemy.owl.OWLClassSkin;
import org.embergraph.gom.alchemy.owl.OWLOntologySkin;
import org.embergraph.gom.alchemy.owl.OWLPropertySkin;
import org.embergraph.gom.alchemy.owl.OWLSkins;
import org.embergraph.gom.gpo.IGPO;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

public class TestOWLSkin extends LocalGOMTestCase {
  //	EmbergraphSailRepository m_repo = null;
  //	ObjectManager m_om = null;
  //	ValueFactory m_vf = null;

  private static final Logger log = Logger.getLogger(TestOwlGOM.class);

  public void testOWLwithProperties() throws RDFParseException, RepositoryException, IOException {
    showOntology("testowl2.xml");
  }

  public void testSimpleOWL() throws RDFParseException, RepositoryException, IOException {
    showOntology("testowl.xml");
  }

  public void testUnivBenchOWL() throws RDFParseException, RepositoryException, IOException {
    showOntology("univ-bench.owl");
  }

  // TODO NOT TESTING ANYTHING.
  private void showOntology(String owlfile)
      throws RDFParseException, RepositoryException, IOException {
    doLoad(owlfile);
    try {
      final OWLOntologySkin onto = OWLOntologySkin.getOntology(om);
      Iterator<OWLClassSkin> classes = onto.getClasses();
      while (classes.hasNext()) {
        final OWLClassSkin clss = classes.next();
        System.out.println(clss.getName());
        Iterator<OWLClassSkin> subclasses = clss.getSubclasses();
        if (subclasses.hasNext()) {
          System.out.print("Subclasses: ");
          while (subclasses.hasNext()) {
            System.out.print(subclasses.next().getName());
            if (subclasses.hasNext()) {
              System.out.print(", ");
            }
          }
          System.out.println();
        }
        final Iterator<OWLPropertySkin> props = clss.getProperties();
        while (props.hasNext()) {
          final OWLPropertySkin prop = props.next();
          final IGPO typ = prop.getType();
          System.out.println(
              "- " + prop.getName() + " : " + (typ == null ? null : typ.getId().stringValue()));
        }
      }
    } finally {
      om.close();
    }
  }

  private void doLoad(final String owlFile)
      throws RDFParseException, RepositoryException, IOException {
    final URL xml = TestGOM.class.getResource(owlFile);

    load(xml, RDFFormat.RDFXML);
  }

  protected void setUp() throws Exception {
    super.setUp();
    //		Properties properties = new Properties();
    //
    //		// create a backing file for the database
    //		File journal = File.createTempFile("embergraph", ".jnl");
    //		properties.setProperty(EmbergraphSail.Options.FILE, journal
    //				.getAbsolutePath());
    //		properties
    //				.setProperty("org.embergraph.rdf.sail.truthMaintenance", "false");
    //
    //		properties.setProperty(Options.BUFFER_MODE, BufferMode.DiskRW
    //				.toString());
    //		properties.setProperty(AbstractTripleStore.Options.TEXT_INDEX, "false");
    //		properties.setProperty(
    //				IndexMetadata.Options.WRITE_RETENTION_QUEUE_CAPACITY, "200");
    //		properties
    //				.setProperty(
    //						"org.embergraph.namespace.kb.spo.SPO.org.embergraph.btree.BTree.branchingFactor",
    //						"200");
    //		properties
    //				.setProperty(
    //						"org.embergraph.namespace.kb.spo.POS.org.embergraph.btree.BTree.branchingFactor",
    //						"200");
    //		properties
    //				.setProperty(
    //						"org.embergraph.namespace.kb.spo.OSP.org.embergraph.btree.BTree.branchingFactor",
    //						"200");
    //		properties
    //				.setProperty(
    //						"org.embergraph.namespace.kb.spo.BLOBS.org.embergraph.btree.BTree.branchingFactor",
    //						"200");
    //		properties
    //				.setProperty(
    //						"org.embergraph.namespace.kb.lex.TERM2ID.org.embergraph.btree.BTree.branchingFactor",
    //						"200");
    //		properties
    //				.setProperty(
    //						"org.embergraph.namespace.kb.lex.ID2TERM.org.embergraph.btree.BTree.branchingFactor",
    //						"200");
    //
    //		// instantiate a sail and a Sesame repository
    //		EmbergraphSail sail = new EmbergraphSail(properties);
    //		m_repo = new EmbergraphSailRepository(sail);
    //		m_repo.initialize();
    //
    //		m_om = new ObjectManager(m_repo);
    //		m_vf = m_om.getValueFactory();

    OWLSkins.register();
  }

  //	    /**
  //     * Utility to load n3 statements from a resource
  //     */
  //    private void load(final URL data, final RDFFormat format)
  //            throws IOException, RDFParseException, RepositoryException {
  //
  //        final InputStream in = data.openConnection().getInputStream();
  //
  //        final Reader reader = new InputStreamReader(in);
  //
  //        m_repo.getConnection().add(reader, "", format);
  //
  //    }
}
