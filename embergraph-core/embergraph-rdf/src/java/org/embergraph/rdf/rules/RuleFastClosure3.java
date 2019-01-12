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
 * Created on Oct 25, 2007
 */

package org.embergraph.rdf.rules;

import java.util.Set;
import org.embergraph.bop.IConstant;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.accesspath.IBuffer;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.eval.IJoinNexus;
import org.embergraph.relation.rule.eval.IRuleTaskFactory;
import org.embergraph.relation.rule.eval.ISolution;
import org.embergraph.relation.rule.eval.IStepTask;
import org.openrdf.model.vocabulary.RDFS;

public class RuleFastClosure3 extends AbstractRuleFastClosure_3_5_6_7_9 {

  /** */
  private static final long serialVersionUID = 8276555097415122677L;

  /*
   * @param vocab
   * @param P
   */
  public RuleFastClosure3(final String database, final String focusStore, final Vocabulary vocab) {
    // , Set<Long> P) {

    super(
        "fastClosure3",
        database,
        vocab.getConstant(RDFS.SUBPROPERTYOF),
        vocab.getConstant(RDFS.SUBPROPERTYOF),
        new FastClosure_3_RuleTaskFactory(database, focusStore, vocab));
  }

  /*
   * Custom rule executor factory.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  private static class FastClosure_3_RuleTaskFactory implements IRuleTaskFactory {

    /** */
    private static final long serialVersionUID = -7577223026737453989L;

    private final String database;

    private final String focusStore;

    private final IConstant<IV> rdfsSubPropertyOf;

    public FastClosure_3_RuleTaskFactory(
        final String database, final String focusStore, final Vocabulary vocab) {

      this.database = database;

      this.focusStore = focusStore;

      rdfsSubPropertyOf = vocab.getConstant(RDFS.SUBPROPERTYOF);
    }

    public IStepTask newTask(IRule rule, IJoinNexus joinNexus, IBuffer<ISolution[]> buffer) {

      return new FastClosureRuleTask(
          database,
          focusStore,
          rule,
          joinNexus,
          buffer, /* P, */
          rdfsSubPropertyOf,
          rdfsSubPropertyOf) {

        /** Note: This is the set {P} in the fast closure program. */
        public Set<IV> getSet() {

          return getSubProperties();
        }
      };
    }
  }
}
