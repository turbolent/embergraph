/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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
 * Created on Jun 3, 2010
 */

package org.embergraph.rdf.lexicon;

import java.util.Iterator;
import java.util.Locale;

import org.openrdf.model.Value;

import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.model.BigdataValue;
import com.bigdata.rdf.store.AbstractTripleStore;
import com.bigdata.search.FullTextIndex;
import com.bigdata.search.IHit;

/**
 * Abstraction for the text indexer for RDF {@link Value}s allowing either the
 * built-in bigdata {@link FullTextIndex} or support for Lucene, etc.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: ITextIndexer.java 4585 2011-06-01 13:42:56Z thompsonbry $
 * 
 * @see AbstractTripleStore.Options#TEXT_INDEXER_CLASS
 */
public interface ISubjectCentricTextIndexer<A extends IHit> extends ITextIndexer<A> {
   
    /**
     * <p>
     * Add the terms to the full text index so that we can do fast lookup of the
     * corresponding term identifiers. Only literals are tokenized. Literals
     * that have a language code property are parsed using a tokenizer
     * appropriate for the specified language family. Other literals and URIs
     * are tokenized using the default {@link Locale}.
     * </p>
     * In the subject-centric text index, these tokenized literals are rolled
     * up by subject rather than using the IV for the literals as the docId in
     * the text index.
     * 
     * @param subject
     *            The subject to which these values belong.
     * @param itr
     *            Iterator visiting the terms to be indexed.
     * 
     * @todo allow registeration of datatype specific tokenizers (we already
     *       have language family based lookup).
     */
    public void index(IV<?,?> subject, Iterator<BigdataValue> valuesIterator);

}
