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
 * Created on Aug 29, 2011
 */
/* Portions of this file are:
 *
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */

package org.embergraph.rdf.sparql.ast;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOpContextBase;
import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.bop.engine.AbstractQueryEngineTestCase;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.spo.SPORelation;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.LocalTripleStore;
import org.openrdf.model.Value;

/*
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class AbstractASTEvaluationTestCase extends AbstractQueryEngineTestCase {

  private static final Logger log = Logger.getLogger(AbstractASTEvaluationTestCase.class);

  /** */
  public AbstractASTEvaluationTestCase() {}

  public AbstractASTEvaluationTestCase(String name) {
    super(name);
  }

  protected AbstractTripleStore store = null;

  protected EmbergraphValueFactory valueFactory = null;

  protected String baseURI = null;

  private BOpContextBase context = null;

  /*
   * Return the context for evaluation of {@link IValueExpression}s during query optimization.
   *
   * @return The context that can be used to resolve the {@link ILexiconConfiguration} and {@link
   *     LexiconRelation} for evaluation if {@link IValueExpression}s during query optimization.
   *     (During query evaluation this information is passed into the pipeline operators by the
   *     {@link ContextBindingSet}.)
   * @see BLZG-1372
   */
  public BOpContextBase getBOpContext() {

    return context;
  }

  @Override
  protected void setUp() throws Exception {

    super.setUp();

    store = getStore(getProperties());

    context = new BOpContextBase(null /* fed */, store.getIndexManager());

    valueFactory = store.getValueFactory();

    /*
     * Note: This needs to be an absolute URI.
     */

    baseURI = "http://www.embergraph.org";
  }

  @Override
  protected void tearDown() throws Exception {

    if (store != null) {

      store.__tearDownUnitTest();

      store = null;
    }

    context = null;

    valueFactory = null;

    baseURI = null;

    super.tearDown();
  }

  protected void enableDeleteMarkersInIndes() {
    final SPORelation rel = store.getSPORelation();
    rel.getPrimaryIndex().getIndexMetadata().setDeleteMarkers(true);
  }

  @Override
  public Properties getProperties() {

    // Note: clone to avoid modifying!!!
    final Properties properties = (Properties) super.getProperties().clone();

    // turn on quads.
    properties.setProperty(AbstractTripleStore.Options.QUADS, "true");

    // TM not available with quads.
    properties.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");

    //        // override the default vocabulary.
    //        properties.setProperty(AbstractTripleStore.Options.VOCABULARY_CLASS,
    //                NoVocabulary.class.getName());

    // turn off axioms.
    properties.setProperty(AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // no persistence.
    properties.setProperty(
        org.embergraph.journal.Options.BUFFER_MODE, BufferMode.Transient.toString());

    return properties;
  }

  protected AbstractTripleStore getStore(final Properties properties) {

    final String namespace = "kb";

    // create/re-open journal.
    final Journal journal = new Journal(properties);

    final LocalTripleStore lts =
        new LocalTripleStore(journal, namespace, ITx.UNISOLATED, properties);

    lts.create();

    return lts;
  }

  protected static void assertSameAST(final IQueryNode expected, final IQueryNode actual) {

    if (!expected.equals(actual)) {

      log.error("expected: " + BOpUtility.toString(expected));
      log.error("actual  : " + BOpUtility.toString(actual));

      AbstractQueryEngineTestCase.diff(expected, actual);

      // No difference was detected?
      throw new AssertionError();
      //            fail("expected:\n" + BOpUtility.toString((BOp) expected)
      //                    + "\nactual:\n" + BOpUtility.toString((BOp) actual));

    } else if (log.isInfoEnabled()) {

      log.info(BOpUtility.toString(expected));
    }
  }

  protected static Set<IVariable<?>> asSet(final String[] vars) {

    final Set<IVariable<?>> set = new LinkedHashSet<>();

    for (String s : vars) {

      set.add(Var.var(s));
    }

    return set;
  }

  protected static Set<IVariable<?>> asSet(final IVariable<?>[] vars) {

    final Set<IVariable<?>> set = new LinkedHashSet<>();

    set.addAll(Arrays.asList(vars));

    return set;
  }

  protected static Set<VarNode> asSet(final VarNode[] a) {

    return new LinkedHashSet<>(Arrays.asList(a));
  }

  protected static Set<FilterNode> asSet(final FilterNode[] a) {

    return new LinkedHashSet<>(Arrays.asList(a));
  }

  protected static Set<Integer> asSet(final Integer[] a) {

    return new LinkedHashSet<>(Arrays.asList(a));
  }

  //    /*
  //     * Return a mock IV for the value.
  //     */
  //    @SuppressWarnings("unchecked")
  //    protected IV<EmbergraphValue, ?> mockIV(final EmbergraphValue value) {
  //
  //        IV iv = store.getLexiconRelation().getInlineIV(value);
  //
  //        if (iv == null) {
  //
  //            iv = (IV<EmbergraphValue, ?>) TermId.mockIV(VTE.valueOf(value));
  //
  //            iv.setValue(value);
  //
  //        }
  //
  //        return iv;
  //
  //    }

  /*
   * Return a (Mock) IV for a Value.
   *
   * @param v The value.
   * @return The Mock IV.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  protected IV makeIV(final Value v) {
    final EmbergraphValue bv = store.getValueFactory().asValue(v);
    final IV iv = TermId.mockIV(VTE.valueOf(v));
    iv.setValue(bv);
    return iv;
  }
}
