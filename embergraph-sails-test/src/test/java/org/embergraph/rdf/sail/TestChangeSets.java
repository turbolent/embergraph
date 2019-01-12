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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.axioms.OwlAxioms;
import org.embergraph.rdf.changesets.ChangeAction;
import org.embergraph.rdf.changesets.ChangeRecord;
import org.embergraph.rdf.changesets.IChangeLog;
import org.embergraph.rdf.changesets.IChangeRecord;
import org.embergraph.rdf.changesets.InMemChangeLog;
import org.embergraph.rdf.changesets.InferenceChangeLogReporter;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.spo.ModifiedEnum;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.BD;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.embergraph.rdf.vocab.RDFSVocabulary;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

/*
* Test suite for the {@link IChangeLog} feature.
 *
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class TestChangeSets extends ProxyEmbergraphSailTestCase {

  private static final Logger log = Logger.getLogger(TestChangeSets.class);

  public Properties getTriplesNoInference() {

    Properties props = super.getProperties();

    // triples with sids
    props.setProperty(EmbergraphSail.Options.QUADS, "false");
    props.setProperty(EmbergraphSail.Options.STATEMENT_IDENTIFIERS, "false");

    // no inference
    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
    props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    return props;
  }

  public Properties getTriplesWithInference() {

    Properties props = super.getProperties();

    // triples with sids
    props.setProperty(EmbergraphSail.Options.QUADS, "false");
    props.setProperty(EmbergraphSail.Options.STATEMENT_IDENTIFIERS, "false");

    // no inference
    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "true");
    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, OwlAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, RDFSVocabulary.class.getName());
    props.setProperty(EmbergraphSail.Options.JUSTIFY, "true");
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    return props;
  }

  /** */
  public TestChangeSets() {}

  /** @param arg0 */
  public TestChangeSets(String arg0) {
    super(arg0);
  }

  public void testSimpleAdd() throws Exception {

    EmbergraphSailRepositoryConnection cxn = null;

    final EmbergraphSail sail = getSail(getTriplesNoInference());

    try {

      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = repo.getConnection();
      cxn.setAutoCommit(false);
      final AbstractTripleStore tripleStore = cxn.getTripleStore();

      final InMemChangeLog changeLog = new InMemChangeLog();
      cxn.addChangeLog(changeLog);

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();

      final String ns = BD.NAMESPACE;

      final URI a = vf.createURI(ns + "A");
      final URI b = vf.createURI(ns + "B");
      final URI c = vf.createURI(ns + "C");

      final EmbergraphStatement[] stmts =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDFS.SUBCLASSOF, b), vf.createStatement(b, RDFS.SUBCLASSOF, c),
          };

      final EmbergraphStatement[] stmts2 =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDFS.SUBCLASSOF, c),
          };

      /**/
      cxn.setNamespace("ns", ns);

      // add the stmts[]

      for (EmbergraphStatement stmt : stmts) {
        cxn.add(stmt);
      }

      cxn.commit();

      { // should see all of the stmts[] added
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        for (EmbergraphStatement stmt : stmts) {
          expected.add(new ChangeRecord(stmt, ChangeAction.INSERTED));
        }

        compare(expected, changeLog.getLastCommit(tripleStore));
      }

      // add the stmts[] again

      for (EmbergraphStatement stmt : stmts) {
        cxn.add(stmt);
      }

      cxn.commit();

      { // shouldn't see any change records
        compare(new LinkedList<IChangeRecord>(), changeLog.getLastCommit(tripleStore));
      }

      // add the stmts2[]

      for (EmbergraphStatement stmt : stmts2) {
        cxn.add(stmt);
      }

      cxn.commit();

      { // should see all of the stmts2[] added
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        for (EmbergraphStatement stmt : stmts2) {
          expected.add(new ChangeRecord(stmt, ChangeAction.INSERTED));
        }

        compare(expected, changeLog.getLastCommit(tripleStore));
      }

      if (log.isDebugEnabled()) {
        log.debug("\n" + tripleStore.dumpStore(true, true, false));
      }

    } finally {
      if (cxn != null) cxn.close();
      sail.__tearDownUnitTest();
    }
  }

  /*
   * Unit test with a full read/write transaction.
   *
   * @throws Exception
   */
  public void testSimpleTxAdd() throws Exception {

    EmbergraphSailRepositoryConnection cxn = null;

    final Properties properties = getTriplesNoInference();

    properties.setProperty(EmbergraphSail.Options.ISOLATABLE_INDICES, "true");

    final EmbergraphSail sail = getSail(properties);

    try {

      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = repo.getConnection();
      cxn.setAutoCommit(false);
      final AbstractTripleStore tripleStore = cxn.getTripleStore();

      final InMemChangeLog changeLog = new InMemChangeLog();
      cxn.addChangeLog(changeLog);

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();

      final String ns = BD.NAMESPACE;

      final URI a = vf.createURI(ns + "A");
      final URI b = vf.createURI(ns + "B");
      final URI c = vf.createURI(ns + "C");

      final EmbergraphStatement[] stmts =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDFS.SUBCLASSOF, b), vf.createStatement(b, RDFS.SUBCLASSOF, c),
          };

      final EmbergraphStatement[] stmts2 =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDFS.SUBCLASSOF, c),
          };

      /**/
      cxn.setNamespace("ns", ns);

      // add the stmts[]

      for (EmbergraphStatement stmt : stmts) {
        cxn.add(stmt);
      }

      cxn.commit();

      { // should see all of the stmts[] added
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        for (EmbergraphStatement stmt : stmts) {
          expected.add(new ChangeRecord(stmt, ChangeAction.INSERTED));
        }

        compare(expected, changeLog.getLastCommit(tripleStore));
      }

      // add the stmts[] again

      for (EmbergraphStatement stmt : stmts) {
        cxn.add(stmt);
      }

      cxn.commit();

      { // shouldn't see any change records
        compare(new LinkedList<IChangeRecord>(), changeLog.getLastCommit(tripleStore));
      }

      // add the stmts2[]

      for (EmbergraphStatement stmt : stmts2) {
        cxn.add(stmt);
      }

      cxn.commit();

      { // should see all of the stmts2[] added
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        for (EmbergraphStatement stmt : stmts2) {
          expected.add(new ChangeRecord(stmt, ChangeAction.INSERTED));
        }

        compare(expected, changeLog.getLastCommit(tripleStore));
      }

      if (log.isDebugEnabled()) {
        log.debug("\n" + tripleStore.dumpStore(true, true, false));
      }

    } finally {
      if (cxn != null) cxn.close();
      sail.__tearDownUnitTest();
    }
  }

  public void testSimpleRemove() throws Exception {

    EmbergraphSailRepositoryConnection cxn = null;
    final EmbergraphSail sail = getSail(getTriplesNoInference());
    try {
      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = repo.getConnection();
      cxn.setAutoCommit(false);
      final AbstractTripleStore tripleStore = cxn.getTripleStore();

      final InMemChangeLog changeLog = new InMemChangeLog();
      cxn.addChangeLog(changeLog);

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();

      final String ns = BD.NAMESPACE;

      final URI a = vf.createURI(ns + "A");
      final URI b = vf.createURI(ns + "B");
      final URI c = vf.createURI(ns + "C");

      final EmbergraphStatement[] stmts =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDFS.SUBCLASSOF, b), vf.createStatement(b, RDFS.SUBCLASSOF, c),
          };

      /**/
      cxn.setNamespace("ns", ns);

      // add the stmts[]

      for (EmbergraphStatement stmt : stmts) {
        cxn.add(stmt);
      }

      cxn.commit();

      // remove the stmts[]

      for (EmbergraphStatement stmt : stmts) {
        stmt.setModified(ModifiedEnum.NONE);
        cxn.remove(stmt);
      }

      cxn.commit();

      if (log.isDebugEnabled()) {
        log.debug("\ndump store:\n" + tripleStore.dumpStore(true, true, false));
      }

      { // should see all of the stmts[] removed
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        for (EmbergraphStatement stmt : stmts) {
          expected.add(new ChangeRecord(stmt, ChangeAction.REMOVED));
        }

        compare(expected, changeLog.getLastCommit(tripleStore));
      }

      // remove the stmts[] again

      for (EmbergraphStatement stmt : stmts) {
        cxn.remove(stmt);
      }

      cxn.commit();

      { // shouldn't see any change records
        compare(new LinkedList<IChangeRecord>(), changeLog.getLastCommit(tripleStore));
      }

    } finally {
      if (cxn != null) cxn.close();
      sail.__tearDownUnitTest();
    }
  }

  public void testSids() throws Exception {

    if (!Boolean.valueOf(
            getProperties()
                .getProperty(
                    EmbergraphSail.Options.STATEMENT_IDENTIFIERS,
                    EmbergraphSail.Options.DEFAULT_STATEMENT_IDENTIFIERS))
        .booleanValue()) {
      log.warn("cannot run this test without sids enabled");
      return;
    }

    EmbergraphSailRepositoryConnection cxn = null;
    final EmbergraphSail sail = getSail(getTriplesNoInference());
    try {
      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = repo.getConnection();
      cxn.setAutoCommit(false);

      final InMemChangeLog changeLog = new InMemChangeLog();
      cxn.addChangeLog(changeLog);

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();

      final String ns = BD.NAMESPACE;

      final URI a = vf.createURI(ns + "A");
      final URI b = vf.createURI(ns + "B");
      final URI c = vf.createURI(ns + "C");
      final URI d = vf.createURI(ns + "D");
      final URI x = vf.createURI(ns + "X");
      final URI y = vf.createURI(ns + "Y");
      final URI z = vf.createURI(ns + "Z");
      //            final BNode sid1 = vf.createBNode();
      //            final BNode sid2 = vf.createBNode();

      final EmbergraphStatement axb = vf.createStatement(a, x, b);
      final EmbergraphBNode sid1 = vf.createBNode(axb);

      final EmbergraphStatement[] add =
          new EmbergraphStatement[] {
            axb, vf.createStatement(sid1, y, c), vf.createStatement(d, z, sid1),
          };

      final EmbergraphStatement[] explicitRemove =
          new EmbergraphStatement[] {
            axb,
          };

      final EmbergraphStatement[] inferredRemove =
          new EmbergraphStatement[] {
            vf.createStatement(sid1, y, c), vf.createStatement(d, z, sid1),
          };

      /**/
      cxn.setNamespace("ns", ns);

      for (EmbergraphStatement stmt : add) {
        cxn.add(stmt);
      }

      cxn.commit();

      final AbstractTripleStore tripleStore = cxn.getTripleStore();

      // resolve bnodes (sids)
      for (int i = 0; i < add.length; i++) {
        add[i] = tripleStore.getStatement(add[i]);
      }
      for (int i = 0; i < explicitRemove.length; i++) {
        explicitRemove[i] = tripleStore.getStatement(explicitRemove[i]);
      }
      for (int i = 0; i < inferredRemove.length; i++) {
        inferredRemove[i] = tripleStore.getStatement(inferredRemove[i]);
      }

      {
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        for (EmbergraphStatement stmt : add) {
          expected.add(new ChangeRecord(stmt, ChangeAction.INSERTED));
        }

        compare(expected, changeLog.getLastCommit(tripleStore));
      }

      for (EmbergraphStatement stmt : explicitRemove) {
        cxn.remove(stmt);
      }

      cxn.commit();

      {
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        for (EmbergraphStatement stmt : explicitRemove) {
          expected.add(new ChangeRecord(stmt, ChangeAction.REMOVED));
        }
        for (EmbergraphStatement stmt : inferredRemove) {
          expected.add(new ChangeRecord(stmt, ChangeAction.REMOVED));
        }

        compare(expected, changeLog.getLastCommit(tripleStore));
      }

      if (log.isDebugEnabled()) {
        log.debug("\n" + tripleStore.dumpStore(true, true, false));
      }

    } finally {
      if (cxn != null) cxn.close();
      sail.__tearDownUnitTest();
    }
  }

  public void testTMAdd() throws Exception {

    if (!Boolean.valueOf(
            getProperties()
                .getProperty(
                    EmbergraphSail.Options.TRUTH_MAINTENANCE,
                    EmbergraphSail.Options.DEFAULT_TRUTH_MAINTENANCE))
        .booleanValue()) {
      log.warn("cannot run this test without TM enabled");
      return;
    }

    EmbergraphSailRepositoryConnection cxn = null;
    final EmbergraphSail sail = getSail(getTriplesWithInference());
    try {

      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = repo.getConnection();
      cxn.setAutoCommit(false);
      final AbstractTripleStore tripleStore = cxn.getTripleStore();

      final InMemChangeLog changeLog = new InMemChangeLog();
      cxn.addChangeLog(changeLog);

      final InferenceChangeLogReporter changeLog2 = new InferenceChangeLogReporter(tripleStore);
      cxn.addChangeLog(changeLog2);

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();

      final String ns = BD.NAMESPACE;

      final URI a = vf.createURI(ns + "A");
      final URI b = vf.createURI(ns + "B");
      final URI c = vf.createURI(ns + "C");

      final EmbergraphStatement[] explicit =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDFS.SUBCLASSOF, b), vf.createStatement(b, RDFS.SUBCLASSOF, c),
          };

      final EmbergraphStatement[] inferred =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDF.TYPE, RDFS.CLASS),
            vf.createStatement(a, RDFS.SUBCLASSOF, RDFS.RESOURCE),
            vf.createStatement(a, RDFS.SUBCLASSOF, a),
            vf.createStatement(a, RDFS.SUBCLASSOF, c),
            vf.createStatement(b, RDF.TYPE, RDFS.CLASS),
            vf.createStatement(b, RDFS.SUBCLASSOF, RDFS.RESOURCE),
            vf.createStatement(b, RDFS.SUBCLASSOF, b),
            vf.createStatement(c, RDF.TYPE, RDFS.CLASS),
            vf.createStatement(c, RDFS.SUBCLASSOF, RDFS.RESOURCE),
            vf.createStatement(c, RDFS.SUBCLASSOF, c),
          };

      final EmbergraphStatement[] upgrades =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDFS.SUBCLASSOF, c),
          };

      /**/
      cxn.setNamespace("ns", ns);

      for (EmbergraphStatement stmt : explicit) {
        cxn.add(stmt);
      }

      cxn.commit();

      if (log.isDebugEnabled()) {
        log.debug("\n" + tripleStore.dumpStore(true, true, false));
      }

      {
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        for (EmbergraphStatement stmt : explicit) {
          expected.add(new ChangeRecord(stmt, ChangeAction.INSERTED));
        }
        for (EmbergraphStatement stmt : inferred) {
          expected.add(new ChangeRecord(stmt, ChangeAction.INSERTED));
        }

        compare(expected, changeLog.getLastCommit(tripleStore));
        assertSameIteratorAnyOrder(inferred, changeLog2.addedIterator());
        assertSameIteratorAnyOrder(new EmbergraphStatement[] {}, changeLog2.removedIterator());
      }

      for (EmbergraphStatement stmt : upgrades) {
        cxn.add(stmt);
      }

      cxn.commit();

      if (log.isDebugEnabled()) {
        log.debug("\n" + tripleStore.dumpStore(true, true, false));
      }

      {
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        for (EmbergraphStatement stmt : upgrades) {
          expected.add(new ChangeRecord(stmt, ChangeAction.UPDATED));
        }

        compare(expected, changeLog.getLastCommit(tripleStore));
      }

    } finally {
      if (cxn != null) cxn.close();
      sail.__tearDownUnitTest();
    }
  }

  public void testTMRetract() throws Exception {

    if (!Boolean.valueOf(
            getProperties()
                .getProperty(
                    EmbergraphSail.Options.TRUTH_MAINTENANCE,
                    EmbergraphSail.Options.DEFAULT_TRUTH_MAINTENANCE))
        .booleanValue()) {
      log.warn("cannot run this test without TM enabled");
      return;
    }

    EmbergraphSailRepositoryConnection cxn = null;
    final EmbergraphSail sail = getSail(getTriplesWithInference());
    try {
      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = repo.getConnection();
      cxn.setAutoCommit(false);
      final AbstractTripleStore tripleStore = cxn.getTripleStore();

      final InMemChangeLog changeLog = new InMemChangeLog();
      cxn.addChangeLog(changeLog);

      final InferenceChangeLogReporter changeLog2 = new InferenceChangeLogReporter(tripleStore);
      cxn.addChangeLog(changeLog2);

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();

      final String ns = BD.NAMESPACE;

      final URI a = vf.createURI(ns + "A");
      final URI b = vf.createURI(ns + "B");
      final URI c = vf.createURI(ns + "C");

      final EmbergraphStatement[] explicitAdd =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDFS.SUBCLASSOF, b), vf.createStatement(b, RDFS.SUBCLASSOF, c),
          };

      final EmbergraphStatement[] inferredAdd =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDF.TYPE, RDFS.CLASS),
            vf.createStatement(a, RDFS.SUBCLASSOF, RDFS.RESOURCE),
            vf.createStatement(a, RDFS.SUBCLASSOF, a),
            vf.createStatement(a, RDFS.SUBCLASSOF, c),
            vf.createStatement(b, RDF.TYPE, RDFS.CLASS),
            vf.createStatement(b, RDFS.SUBCLASSOF, RDFS.RESOURCE),
            vf.createStatement(b, RDFS.SUBCLASSOF, b),
            vf.createStatement(c, RDF.TYPE, RDFS.CLASS),
            vf.createStatement(c, RDFS.SUBCLASSOF, RDFS.RESOURCE),
            vf.createStatement(c, RDFS.SUBCLASSOF, c),
          };

      final EmbergraphStatement[] explicitRemove =
          new EmbergraphStatement[] {
            vf.createStatement(b, RDFS.SUBCLASSOF, c),
          };

      final EmbergraphStatement[] inferredRemove =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDFS.SUBCLASSOF, c),
            vf.createStatement(c, RDF.TYPE, RDFS.CLASS),
            vf.createStatement(c, RDFS.SUBCLASSOF, RDFS.RESOURCE),
            vf.createStatement(c, RDFS.SUBCLASSOF, c),
          };

      /**/
      cxn.setNamespace("ns", ns);

      for (EmbergraphStatement stmt : explicitAdd) {
        cxn.add(stmt);
      }

      cxn.commit();

      {
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        for (EmbergraphStatement stmt : explicitAdd) {
          expected.add(new ChangeRecord(stmt, ChangeAction.INSERTED));
        }
        for (EmbergraphStatement stmt : inferredAdd) {
          expected.add(new ChangeRecord(stmt, ChangeAction.INSERTED));
        }

        compare(expected, changeLog.getLastCommit(tripleStore));
        assertSameIteratorAnyOrder(inferredAdd, changeLog2.addedIterator());
        assertSameIteratorAnyOrder(new EmbergraphStatement[] {}, changeLog2.removedIterator());
      }

      // reset
      changeLog2.clear();

      for (EmbergraphStatement stmt : explicitRemove) {
        cxn.remove(stmt);
      }

      cxn.commit();

      {
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        for (EmbergraphStatement stmt : explicitRemove) {
          expected.add(new ChangeRecord(stmt, ChangeAction.REMOVED));
        }
        for (EmbergraphStatement stmt : inferredRemove) {
          expected.add(new ChangeRecord(stmt, ChangeAction.REMOVED));
        }

        compare(expected, changeLog.getLastCommit(tripleStore));
        assertSameIteratorAnyOrder(new EmbergraphStatement[] {}, changeLog2.addedIterator());
        assertSameIteratorAnyOrder(inferredRemove, changeLog2.removedIterator());
      }

      if (log.isDebugEnabled()) {
        log.debug("\n" + tripleStore.dumpStore(true, true, false));
      }

    } finally {
      if (cxn != null) cxn.close();
      sail.__tearDownUnitTest();
    }
  }

  public void testTMUpdate() throws Exception {

    if (!Boolean.valueOf(
            getProperties()
                .getProperty(
                    EmbergraphSail.Options.TRUTH_MAINTENANCE,
                    EmbergraphSail.Options.DEFAULT_TRUTH_MAINTENANCE))
        .booleanValue()) {
      log.warn("cannot run this test without TM enabled");
      return;
    }

    EmbergraphSailRepositoryConnection cxn = null;
    final EmbergraphSail sail = getSail(getTriplesWithInference());

    try {

      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = repo.getConnection();
      cxn.setAutoCommit(false);
      final AbstractTripleStore tripleStore = cxn.getTripleStore();

      final InMemChangeLog changeLog = new InMemChangeLog();
      cxn.addChangeLog(changeLog);

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();

      final String ns = BD.NAMESPACE;

      final URI a = vf.createURI(ns + "A");
      final URI b = vf.createURI(ns + "B");
      final URI c = vf.createURI(ns + "C");

      final EmbergraphStatement[] explicit =
          new EmbergraphStatement[] {
            vf.createStatement(a, RDFS.SUBCLASSOF, b), vf.createStatement(b, RDFS.SUBCLASSOF, c),
          };

      //            final EmbergraphStatement[] inferred = new EmbergraphStatement[] {
      //                vf.createStatement(a, RDF.TYPE, RDFS.CLASS),
      //                vf.createStatement(a, RDFS.SUBCLASSOF, RDFS.RESOURCE),
      //                vf.createStatement(a, RDFS.SUBCLASSOF, a),
      //                vf.createStatement(a, RDFS.SUBCLASSOF, c),
      //                vf.createStatement(b, RDF.TYPE, RDFS.CLASS),
      //                vf.createStatement(b, RDFS.SUBCLASSOF, RDFS.RESOURCE),
      //                vf.createStatement(b, RDFS.SUBCLASSOF, b),
      //                vf.createStatement(c, RDF.TYPE, RDFS.CLASS),
      //                vf.createStatement(c, RDFS.SUBCLASSOF, RDFS.RESOURCE),
      //                vf.createStatement(c, RDFS.SUBCLASSOF, c),
      //            };

      //            final EmbergraphStatement[] updates = new EmbergraphStatement[] {
      EmbergraphStatement update = vf.createStatement(a, RDFS.SUBCLASSOF, c);
      //            };

      /**/
      cxn.setNamespace("ns", ns);

      for (EmbergraphStatement stmt : explicit) {
        cxn.add(stmt);
      }

      cxn.commit();

      if (log.isDebugEnabled()) {
        log.debug("\n" + tripleStore.dumpStore(true, true, false));
      }

      // test adding a statement that is already an inference - should
      // be upgraded to Explicit from Inferred
      cxn.add(update);

      cxn.commit();

      if (log.isDebugEnabled()) {
        log.debug("\n" + tripleStore.dumpStore(true, true, false));
      }

      {
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        expected.add(new ChangeRecord(update, ChangeAction.UPDATED));

        compare(expected, changeLog.getLastCommit(tripleStore));

        // get the latest statement type from the db
        update = tripleStore.getStatement(update);

        assertTrue("wrong type", update.isExplicit());
      }

      // test removing a statement that is still provable as an
      // inference - should be downgraded from Explicit to Inferred
      cxn.remove(update);

      cxn.commit();

      if (log.isDebugEnabled()) {
        log.debug("\n" + tripleStore.dumpStore(true, true, false));
      }

      {
        final Collection<IChangeRecord> expected = new LinkedList<IChangeRecord>();
        expected.add(new ChangeRecord(update, ChangeAction.UPDATED));

        compare(expected, changeLog.getLastCommit(tripleStore));

        // get the latest statement type from the db
        update = tripleStore.getStatement(update);

        assertTrue("wrong type", update.isInferred());
      }

    } finally {
      if (cxn != null) cxn.close();
      sail.__tearDownUnitTest();
    }
  }

  private void compare(
      final Collection<IChangeRecord> expected, final Collection<IChangeRecord> actual) {

    final Collection<IChangeRecord> extra = new LinkedList<IChangeRecord>();
    Collection<IChangeRecord> missing = new LinkedList<IChangeRecord>();

    //        int resultCount = 0;
    int nmatched = 0;
    for (IChangeRecord rec : actual) {
      //            resultCount++;
      boolean match = false;
      if (log.isInfoEnabled()) log.info(rec);
      Iterator<IChangeRecord> it = expected.iterator();
      while (it.hasNext()) {
        if (it.next().equals(rec)) {
          it.remove();
          match = true;
          nmatched++;
          break;
        }
      }
      if (match == false) {
        extra.add(rec);
      }
    }
    missing = expected;

    for (IChangeRecord rec : extra) {
      if (log.isInfoEnabled()) {
        log.info("extra result: " + rec);
      }
    }

    for (IChangeRecord rec : missing) {
      if (log.isInfoEnabled()) {
        log.info("missing result: " + rec);
      }
    }

    if (!extra.isEmpty() || !missing.isEmpty()) {
      fail(
          "matchedResults="
              + nmatched
              + ", extraResults="
              + extra.size()
              + ", missingResults="
              + missing.size());
    }
  }
}
