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
 * Created on Apr 12, 2012
 */

package org.embergraph.rdf.sparql.ast.eval;

import java.util.Set;

import org.embergraph.bop.BOpContextBase;
import org.embergraph.bop.ContextBindingSet;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.engine.StaticAnalysisStats;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.sparql.ast.ISolutionSetStats;
import org.embergraph.rdf.sparql.ast.QueryHints;
import org.embergraph.rdf.sparql.ast.cache.IDescribeCache;
import org.embergraph.rdf.sparql.ast.optimizers.IASTOptimizer;
import org.embergraph.rdf.sparql.ast.ssets.ISolutionSetManager;
import org.embergraph.rdf.spo.SPORelation;
import org.embergraph.rdf.store.AbstractTripleStore;

/**
 * Interface providing access to various things of interest when preparing and
 * evaluating a query or update operation.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IEvaluationContext {

    /**
     * Some summary statistics about the exogenous solution sets. These are
     * computed by {@link AST2BOpUtility#convert(AST2BOpContext, IBindingSet[])}
     * before it begins to run the {@link IASTOptimizer}s.
     */
    ISolutionSetStats getSolutionSetStats();

    /**
     * Summary statistics for the static analysis phase.
     */
    StaticAnalysisStats getStaticAnalysisStats();
    
    /**
     * The timestamp or transaction identifier associated with the view.
     */
    long getTimestamp();

    /**
     * Return <code>true</code> if we are running on a cluster.
     */
    boolean isCluster();

    /**
     * Return <code>true</code> iff the target {@link AbstractTripleStore} is in
     * quads mode.
     */
    boolean isQuads();

    /**
     * Return <code>true</code> iff the target {@link AbstractTripleStore} is in
     * SIDS mode.
     */
    boolean isSIDs();

    /**
     * Return <code>true</code> iff the target {@link AbstractTripleStore} is in
     * triples mode.
     */
    boolean isTriples();

    /**
     * Return the namespace of the {@link AbstractTripleStore}.
     */
    String getNamespace();

    /**
     * Return the namespace of the {@link SPORelation}.
     */
    String getSPONamespace();

    /**
     * Return the namespace of the {@link LexiconRelation}.
     */
    String getLexiconNamespace();

    /**
     * Return the context for evaluation of {@link IValueExpression}s during
     * query optimization.
     * 
     * @return The context that can be used to resolve the
     *         {@link ILexiconConfiguration} and {@link LexiconRelation} for
     *         evaluation if {@link IValueExpression}s during query
     *         optimization. (During query evaluation this information is passed
     *         into the pipeline operators by the {@link ContextBindingSet}.)
     * 
     * @see BLZG-1372
     */
    BOpContextBase getBOpContext();
    
    /**
     * Return the timestamp which will be used to read on the lexicon.
     * <p>
     * Note: This uses the timestamp of the triple store view unless this is a
     * read/write transaction, in which case we need to use the last commit
     * point in order to see any writes which it may have performed (lexicon
     * writes are always unisolated).
     */
    long getLexiconReadTimestamp();
    
    /**
     * Return the database.
     */
    AbstractTripleStore getAbstractTripleStore();
    
    /**
	 * Return the manager for named solution sets (experimental feature).
	 * 
	 * @return The manager -or- <code>null</code>.
	 */
    ISolutionSetManager getSolutionSetManager();

    /**
     * Return the cache for described resources (experimental feature).
     * 
     * @return The cache -or- <code>null</code>.
     * 
     * @see QueryHints#DESCRIBE_CACHE
     */
    IDescribeCache getDescribeCache();

    /**
     * Resolve the pre-existing named solution set returning its
     * {@link ISolutionSetStats}.
     * 
     * @param localName
     *            The local name of the named solution set.
     * 
     * @return The {@link ISolutionSetStats}
     * 
     * @throws RuntimeException
     *             if the named solution set can not be found.
     */
    ISolutionSetStats getSolutionSetStats(String name);
 
//    /**
//     * Resolve a pre-existing named solution set.
//     * 
//     * @param localName
//     *            The local name of the named solution set.
//     *            
//     * @return The {@link ISolutionSet}
//     * 
//     * @throws RuntimeException
//     *             if the named solution set can not be found.
//     */
//    ICloseableIterator<IBindingSet[]> getSolutionSet(String name);
    
    /**
     * Returns all the variables with a global scope. This basically serves
     * the purpose of identifying variables that are injected through Sesame's
     * Operation.setBinding() interface. Guaranteed to be not null.
     * 
     * @return
     */
    public Set<IVariable<?>> getGloballyScopedVariables();

    /**
     * Sets the variables with global scope. This basically serves the purpose
     * of identifying 
     * @param globallyScopedVariables
     */
    public void setGloballyScopedVariables(
       final Set<IVariable<?>> globallyScopedVariables);
    
}
