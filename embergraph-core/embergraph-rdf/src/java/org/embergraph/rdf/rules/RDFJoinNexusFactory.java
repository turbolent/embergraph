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
 * Created on Jul 9, 2008
 */

package org.embergraph.rdf.rules;

import java.util.Properties;

import org.apache.log4j.Logger;

import org.embergraph.bop.joinGraph.IEvaluationPlanFactory;
import org.embergraph.journal.IIndexManager;
import org.embergraph.rdf.axioms.Axioms;
import org.embergraph.rdf.spo.SPORelation;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.relation.accesspath.IElementFilter;
import org.embergraph.relation.rule.eval.AbstractJoinNexusFactory;
import org.embergraph.relation.rule.eval.ActionEnum;
import org.embergraph.relation.rule.eval.IJoinNexus;
import org.embergraph.relation.rule.eval.IRuleTaskFactory;

/**
 * Factory for {@link RDFJoinNexus} objects.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RDFJoinNexusFactory extends AbstractJoinNexusFactory {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected static final transient Logger log = Logger.getLogger(RDFJoinNexusFactory.class);

    final RuleContextEnum ruleContext;
    final boolean justify;
    final boolean backchain;
    final boolean isOwlSameAsUsed;

    @Override
    protected void toString(final StringBuilder sb) {

        sb.append("{ ruleContext=" + ruleContext);

        sb.append(", justify=" + justify);

        sb.append(", backchain=" + backchain);

        sb.append(", isOwlSameAsUsed=" + isOwlSameAsUsed);

    }

	/**
     * {@inheritDoc}
     * 
     * @param justify
     *            if justifications are required.
     * @param backchain
     *            Normally <code>true</code> for high level query and
     *            <code>false</code> for database-at-once-closure and Truth
     *            Maintenance. When <code>true</code>, query time inferences
     *            are included when reading on an {@link IAccessPath} for the
     *            {@link SPORelation} using the {@link InferenceEngine} to
     *            "backchain" any necessary entailments.
     * @param isOwlSameAsUsed
     *            <code>true</code> iff {@link Axioms#isOwlSameAs()} AND
     *            <code>(x owl:sameAs y)</code> is not empty in the data.
     */
	public RDFJoinNexusFactory(
	        final ActionEnum action,
            final long writeTimestamp,
            final long readTimestamp,
            final Properties properties,
            final int solutionFlags,
            final IElementFilter<?> filter,
            final IEvaluationPlanFactory planFactory,
            final IRuleTaskFactory defaultRuleTaskFactory,
            // RDF specific parameters.
            final RuleContextEnum ruleContext,
            final boolean justify,
            final boolean backchain,
            final boolean isOwlSameAsUsed// 
            ) {

        super(action, writeTimestamp, readTimestamp, properties, solutionFlags,
                filter, planFactory, defaultRuleTaskFactory);
	    
       if (ruleContext == null)
            throw new IllegalArgumentException();

        this.ruleContext = ruleContext;

        this.justify = justify;

        this.backchain = backchain;

        this.isOwlSameAsUsed = isOwlSameAsUsed;

    }

    @Override
    protected IJoinNexus newJoinNexus(final IIndexManager indexManager) {

        return new RDFJoinNexus(this, indexManager);

    }

}
