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

package org.embergraph.rdf.vocab;

import org.embergraph.rdf.internal.InlineIPv4URIHandler;
import org.embergraph.rdf.internal.InlineUUIDURIHandler;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.decls.VoidVocabularyDecl;

/**
 * Extended vocabulary to include some new declarations. 
 * Note: Do not modify this class.  Create an entirely new vocabulary and edit
 * {@link AbstractTripleStore.Options#DEFAULT_VOCABULARY_CLASS}.
 * 
 * NOTE: Default is a terrible name for this class.  The core vocabulary
 * will naturally evolve over time.  This version of this class was the default
 * vocabulary for journals created prior to 11/6/2015.  -MP
 */
public class DefaultBigdataVocabulary extends RDFSVocabulary {

    /**
     * De-serialization ctor.
     */
    public DefaultBigdataVocabulary() {
        
        super();
        
    }
    
    /**
     * Used by {@link AbstractTripleStore#create()}.
     * 
     * @param namespace
     *            The namespace of the KB instance.
     */
    public DefaultBigdataVocabulary(final String namespace) {

        super(namespace);
        
    }

    @Override
    protected void addValues() {

        super.addValues();
        
        addDecl(new VoidVocabularyDecl());

        /*
         * Some new URIs for inline URI handling.
         */
        addDecl(new BaseVocabularyDecl(
                XSD.IPV4,
                InlineIPv4URIHandler.NAMESPACE,
                XSD.UUID,
                InlineUUIDURIHandler.NAMESPACE
                ));

    }

}
