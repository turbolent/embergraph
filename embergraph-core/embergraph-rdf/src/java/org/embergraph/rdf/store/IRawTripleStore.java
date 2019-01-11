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
 * Created on Oct 20, 2007
 */

package org.embergraph.rdf.store;

import org.openrdf.model.Value;

import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.BigdataValue;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPO;
import org.embergraph.rdf.spo.SPOKeyOrder;
import org.embergraph.rdf.spo.SPORelation;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.relation.accesspath.IElementFilter;
import org.embergraph.striterator.IChunkedOrderedIterator;
import org.embergraph.striterator.IKeyOrder;

/**
 * Low-level API directly using long term identifiers rather than an RDF Value
 * object model.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 * @todo consider refactor making this the primary interface so that we can
 *       realize non-Sesame integrations by layering an appropriate interface
 *       over this one.
 */
public interface IRawTripleStore extends ITripleStore {
    
    /**
     * The constant <code>NULL</code>.
     */
    String NULLSTR = "NULL";

    /**
     * Add a term into the term:id index and the id:term index, returning the
     * assigned term identifier (non-batch API).
     * <p>
     * Note: This method delegates to the batch API, but it is extremely
     * inefficient for scale-out as it does one RMI per request!
     * 
     * @param value
     *            The term.
     * 
     * @return The assigned internal value.
     */
    public IV addTerm(Value value);

    /**
     * Batch insert of terms into the database. The internal values are set on
     * the terms as a side-effect.
     * 
     * @param terms
     *            An array to be inserted.
     * 
     * @see LexiconRelation#addTerms(BigdataValue[], int, boolean)
     */
    public void addTerms(BigdataValue[] terms);

    /**
     * Return the RDF {@link Value} given an internal value (non-batch api).
     * 
     * @return the RDF value or <code>null</code> if there is no term with
     *         that internal value in the index.
     */
    public Value getTerm(IV iv);

    /**
     * Return the pre-assigned internal value for the value (non-batch API).
     * 
     * @param value
     *            Any {@link Value} reference (MAY be <code>null</code>).
     * 
     * @return The pre-assigned internal value -or- null iff the term is not
     *         known to the database.
     * 
     * @deprecated This is only used by the unit tests. It is not efficient for
     *             scale-out.
     */
    public IV getIV(Value value);

    /**
     * Chooses and returns the best {@link IAccessPath} for the given triple
     * pattern.
     * 
     * @param s
     *            The internal value for the subject -or- null.
     * @param p
     *            The internal value for the predicate -or- null.
     * @param o
     *            The internal value for the object -or- null.
     * 
     * @deprecated by {@link SPORelation#getAccessPath(IV, IV, IV)}
     */
    public IAccessPath<ISPO> getAccessPath(IV s, IV p, IV o);

    /**
     * Return the {@link IAccessPath} for the specified {@link IKeyOrder} and a
     * fully unbound triple pattern. This is generally used only when you want
     * to perform a {@link IAccessPath#distinctTermScan()}.
     * 
     * @deprecated by
     *             {@link SPORelation#getAccessPath(SPOKeyOrder, org.embergraph.relation.rule.IPredicate)}
     */
    public IAccessPath<ISPO> getAccessPath(IKeyOrder<ISPO> keyOrder);

    /**
     * Return the statement from the database (fully bound s:p:o only).
     * <p>
     * Note: This may be used to examine the {@link StatementEnum}.
     * 
     * @param s
     *            The internal value ({@link IV}) for the subject.
     * @param p
     *            The internal value ({@link IV}) for the predicate.
     * @param o
     *            The internal value ({@link IV}) for the object.
     * @param c
     *            The internal value ({@link IV}) for the context (required for 
     *            quads and ignored for triples).
     * 
     * @return The {@link SPO} for that statement, including its
     *         {@link StatementEnum} -or- <code>null</code> iff the statement is
     *         not in the database.
     * 
     * @exception IllegalArgumentException
     *                if the s, p, or o is null.
     * @exception IllegalArgumentException
     *                if the c is null and {@link #isQuads()} would
     *                return <code>true</code>.
     */
    public ISPO getStatement(IV s, IV p, IV o, IV c);
    
    /** @deprecated does not support quads. */
    public ISPO getStatement(IV s, IV p, IV o);
    
    /**
     * Writes the statements onto the statements indices (batch, parallel, NO
     * truth maintenance).
     * 
     * @param stmts
     *            The statements (sorted into {@link IKeyOrder#SPO} order as a
     *            side-effect).
     * 
     * @param numStmts
     *            The #of entries in <i>stmts</i> that are valid.
     * 
     * @return The #of statements that were written on the indices (a statement
     *         that was previously an axiom or inferred and that is converted to
     *         an explicit statement by this method will be reported in this
     *         count as well as any statement that was not pre-existing in the
     *         database).
     */
    public long addStatements(ISPO[] stmts, int numStmts );

    /**
     * Writes the statements onto the statement indices (batch, parallel, NO
     * truth maintenance).
     * 
     * @param stmts
     *            The statements.
     * 
     * @param numStmts
     *            The #of entries in <i>stmts</i> that are valid.
     * 
     * @param filter
     *            Optional statement filter. Statements matching the filter are
     *            NOT added to the database.
     * 
     * @return The #of statements that were written on the indices (a statement
     *         that was previously an axiom or inferred and that is converted to
     *         an explicit statement by this method will be reported in this
     *         count as well as any statement that was not pre-existing in the
     *         database).
     */
    public long addStatements(ISPO[] stmts, int numStmts,
            IElementFilter<ISPO> filter);

    /**
     * Writes the statements onto the statement indices (batch, parallel, NO
     * truth maintenance).
     * 
     * @param itr
     *            An iterator visiting the statements to be added.
     * @param filter
     *            Optional statement filter. Statements matching the filter are
     *            NOT added to the database. The iterator is closed by this
     *            operation.
     * 
     * @return The #of statements that were written on the indices (a statement
     *         that was previously an axiom or inferred and that is converted to
     *         an explicit statement by this method will be reported in this
     *         count as well as any statement that was not pre-existing in the
     *         database).
     * 
     * @deprecated by {@link SPORelation#insert(IChunkedOrderedIterator)}
     */
    public long addStatements(IChunkedOrderedIterator<ISPO> itr,
            IElementFilter<ISPO> filter);

    /**
     * Removes the statements from the statement indices (batch, parallel, NO
     * truth maintenance).
     * <p>
     * Note: The {@link StatementEnum} on the {@link SPO}s is ignored by this
     * method. It will delete all statements having the same bindings regardless
     * of whether they are inferred, explicit, or axioms.
     * 
     * @param itr
     *            The iterator
     * 
     * @return The #of statements that were removed from the indices.
     */
    public long removeStatements(ISPO[] stmts, int numStmts);

    /**
     * Removes the statements from the statement indices (batch, parallel, NO
     * truth maintenance).
     * <p>
     * Note: The {@link StatementEnum} on the {@link SPO}s is ignored by this
     * method. It will delete all statements having the same bindings regardless
     * of whether they are inferred, explicit, or axioms.
     * 
     * @param itr
     *            The iterator
     * 
     * @return The #of statements that were removed from the indices.
     */
    public long removeStatements(IChunkedOrderedIterator<ISPO> itr);

    /**
     * Filter the supplied set of {@link ISPO} objects for whether they are
     * "present" or "not present" in the database, depending on the value of the
     * supplied boolean variable (batch API).
     * 
     * @param stmts
     *            the statements to test
     * @param numStmts
     *            the number of statements to test
     * @param present
     *            if true, filter for statements that exist in the db, otherwise
     *            filter for statements that do not exist
     * 
     * @return an iteration over the filtered set of statements
     */
    public IChunkedOrderedIterator<ISPO> bulkFilterStatements(ISPO[] stmts,
            int numStmts, boolean present);

    /**
     * Efficiently filter the supplied set of {@link SPO} objects for whether
     * they are "present" or "not present" in the database, depending on the
     * value of the supplied boolean variable (batch api).
     * 
     * @param itr
     *            an iterator over the set of statements to test
     * @param present
     *            if true, filter for statements that exist in the db, otherwise
     *            filter for statements that do not exist
     * 
     * @return an iteration over the filtered set of statements
     */
    public IChunkedOrderedIterator<ISPO> bulkFilterStatements(
            IChunkedOrderedIterator<ISPO> itr, boolean present);

    /**
     * This method fills out the statement metadata (type and sid) for
     * {@link ISPO}s that are present in the database. {@link ISPO}s not present
     * in the database are left as-is.
     * 
     * @return An iterator visiting the completed {@link ISPO}s. Any
     *         {@link ISPO}s that were not found will be present but their
     *         statement metadata (type and sid) will be unchanged.
     */
    public IChunkedOrderedIterator<ISPO> bulkCompleteStatements(
            final IChunkedOrderedIterator<ISPO> itr);

    /**
     * Externalizes a quad or a triple with a statement identifier using an
     * abbreviated syntax.
     */
    public String toString(IV s, IV p, IV o, IV c);

    /**
     * Externalizes a triple using an abbreviated syntax.
     * 
     * @deprecated by {@link #toString(IV, IV, IV, IV)}
     */
    public String toString(IV s, IV p, IV o);

    /**
     * Externalizes a term using an abbreviated syntax.
     * 
     * @param iv
     *            The term identifier.
     * 
     * @return A representation of the term.
     */
    public String toString(IV iv);

}
