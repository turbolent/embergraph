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

package org.embergraph.rdf.vocab.core;

import org.openrdf.model.impl.URIImpl;

import org.embergraph.rdf.sail.RDRHistory;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.BD;
import org.embergraph.rdf.vocab.BaseVocabularyDecl;
import org.embergraph.rdf.vocab.decls.GeoSpatialVocabularyDecl;

/**
 * Core Bigdata vocabulary.
 *  
 * Note: Do not modify this class.  Create an entirely new vocabulary that
 * extends this one and edit
 * {@link AbstractTripleStore.Options#DEFAULT_VOCABULARY_CLASS}.
 */
public class EmbergraphCoreVocabulary_v20151210 extends EmbergraphCoreVocabulary_v20151106 {

    /**
     * De-serialization ctor.
     */
    public EmbergraphCoreVocabulary_v20151210() {
        
        super();
        
    }
    
    /**
     * Used by {@link AbstractTripleStore#create()}.
     * 
     * @param namespace
     *            The namespace of the KB instance.
     */
    public EmbergraphCoreVocabulary_v20151210(final String namespace) {

        super(namespace);
        
    }

    @Override
    protected void addValues() {

        super.addValues();
        addDecl(new GeoSpatialVocabularyDecl());

        /*
         * Some new URIs for graph and RDR management.
         */
		addDecl(new BaseVocabularyDecl(
				new URIImpl(BD.NAMESPACE + "Vertex"), // BigdataRDFFactory.VERTEX,
				new URIImpl(BD.NAMESPACE + "Edge"), // BigdataRDFFactory.EDGE,
				new URIImpl("attr:/type"), // GPO.tid,
				BD.SID,
				BD.STATEMENT_TYPE,
				BD.NULL_GRAPH,
				BD.VIRTUAL_GRAPH,
				RDRHistory.Vocab.ADDED,
				RDRHistory.Vocab.REMOVED));

    }

}
