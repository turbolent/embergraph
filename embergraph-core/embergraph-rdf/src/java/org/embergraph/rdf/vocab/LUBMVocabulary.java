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
 * Created on Oct 28, 2007
 */

package org.embergraph.rdf.vocab;

import org.embergraph.rdf.internal.impl.extensions.DerivedNumericsExtension;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.decls.LUBMVocabularyDecl;
import org.embergraph.rdf.vocab.decls.OWLVocabularyDecl;
import org.embergraph.rdf.vocab.decls.RDFSVocabularyDecl;
import org.embergraph.rdf.vocab.decls.RDFVocabularyDecl;
import org.embergraph.rdf.vocab.decls.XMLSchemaVocabularyDecl;

/**
 * A {@link Vocabulary} covering the ontologies used by LUBM.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class LUBMVocabulary extends BaseVocabulary {

    /**
     * De-serialization ctor.
     */
    public LUBMVocabulary() {
        
        super();
        
    }
    
    /**
     * Used by {@link AbstractTripleStore#create()}.
     * 
     * @param namespace
     *            The namespace of the KB instance.
     */
    public LUBMVocabulary(final String namespace) {

        super( namespace );
        
    }

    /**
     * Note: The current revision of this class declares vocabulary items which
     * are required by the {@link DerivedNumericsExtension}. This version of the
     * class is NOT compatible with the previous version of the class. This
     * should not be a problem since the LUBM data is just for benchmark
     * purposes.
     * 
     * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/463">
     *      NoSuchVocabularyItem with LUBMVocabulary for
     *      DerivedNumericsExtension </a>
     * 
     * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/456">
     *      IExtension implementations do point lookups on lexicon </a>
     */
    @Override
    protected void addValues() {

        addDecl(new RDFVocabularyDecl());
        addDecl(new RDFSVocabularyDecl());
        addDecl(new OWLVocabularyDecl());
        addDecl(new LUBMVocabularyDecl());

        // Note: Required by DerivedNumericsExtension.
        addDecl(new XMLSchemaVocabularyDecl());

    }

}
