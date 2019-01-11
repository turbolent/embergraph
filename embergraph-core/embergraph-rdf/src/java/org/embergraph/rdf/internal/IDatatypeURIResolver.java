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

package org.embergraph.rdf.internal;

import org.openrdf.model.URI;

import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.BigdataURI;
import org.embergraph.rdf.vocab.Vocabulary;

/**
 * Specialized interface for resolving (and creating if necessary) datatype
 * URIs. This interface requires access to a mutable view of the database since
 * unknown URIs will be registered.
 * 
 * @author mrpersonick
 * 
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/456">
 *      IExtension implementations do point lookups on lexicon</a>
 */
public interface IDatatypeURIResolver {

    /**
     * Returns a fully resolved datatype URI with the {@link IV} set.
     * <p>
     * {@link IExtension}s handle encoding and decoding of inline literals for
     * custom datatypes, however to do so they need the {@link IV} for the
     * custom datatype. By passing an instance of this interface to the
     * {@link IExtension}, it will be able to resolve its datatype URI(s) and
     * cache them for future use.
     * <p>
     * The URIs used by {@link IExtension}s MUST be pre-declared by the
     * {@link Vocabulary}.
     * <p>
     * This interface is implemented by the {@link LexiconRelation}.
     * 
     * @param uri
     *            the term to resolve
     * 
     * @return The fully resolved term
     * 
     * @throws IllegalArgumentException
     *             if the argument is <code>null</code>.
     * @throws NoSuchVocabularyItem
     *             if the argument is not part of the pre-declared
     *             {@link Vocabulary}.
     */
    BigdataURI resolve(final URI datatypeURI);
    
}
