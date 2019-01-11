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

import org.openrdf.Sesame;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;

import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.decls.DCAllVocabularyDecl;
import org.embergraph.rdf.vocab.decls.FOAFVocabularyDecl;
import org.embergraph.rdf.vocab.decls.OWLVocabularyDecl;
import org.embergraph.rdf.vocab.decls.RDFSVocabularyDecl;
import org.embergraph.rdf.vocab.decls.RDFVocabularyDecl;
import org.embergraph.rdf.vocab.decls.SKOSVocabularyDecl;
import org.embergraph.rdf.vocab.decls.SesameVocabularyDecl;
import org.embergraph.rdf.vocab.decls.VoidVocabularyDecl;
import org.embergraph.rdf.vocab.decls.XMLSchemaVocabularyDecl;

/**
 * A {@link Vocabulary} including well-known {@link Value}s for {@link RDF},
 * {@link RDFS}, {@link OWL}, {@link DCAllVocabularyDecl Dublin Core},
 * {@link SKOSVocabularyDecl SKOS}, {@link FOAFVocabularyDecl FOAF},
 * {@link XMLSchema}, and {@link Sesame}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RDFSVocabulary extends BaseVocabulary {

    /**
     * De-serialization ctor.
     */
    public RDFSVocabulary() {
        
        super();
        
    }
    
    /**
     * Used by {@link AbstractTripleStore#create()}.
     * 
     * @param namespace
     *            The namespace of the KB instance.
     */
    public RDFSVocabulary(final String namespace) {

        super( namespace );
        
    }

    @Override
    protected void addValues() {

        addDecl(new RDFVocabularyDecl());
   
        addDecl(new RDFSVocabularyDecl());
        
        addDecl(new OWLVocabularyDecl());
        
        addDecl(new FOAFVocabularyDecl());
        
        addDecl(new SKOSVocabularyDecl());
        
        addDecl(new DCAllVocabularyDecl());
        
        addDecl(new XMLSchemaVocabularyDecl());
        
        addDecl(new SesameVocabularyDecl());

    }

}
