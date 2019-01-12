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
 * Created on Sep 16, 2009
 */

package org.embergraph.rdf.sail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.BindingImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.SailException;

import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.vocab.NoVocabulary;

/**
 * A test suite developed for some bits and pieces of SPARQL 1.0 coverage.
 * 
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class DavidsTestBOps extends ProxyEmbergraphSailTestCase {

    private static final Logger log = Logger.getLogger(DavidsTestBOps.class);
    
    @Override
    public Properties getProperties() {
        
        Properties props = super.getProperties();
        
        props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
        props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
        props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
        props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");
        
        return props;
        
    }

    /**
     * 
     */
    public DavidsTestBOps() {
    }

    /**
     * @param arg0
     */
    public DavidsTestBOps(String arg0) {
        super(arg0);
    }
    
    public void testImplementationDefinedDefaultGraph ()
        throws Exception
    {
        final EmbergraphSail sail = getTheSail () ;
        final ValueFactory vf = sail.getValueFactory();
        final RepositoryConnection cxn = getRepositoryConnection ( sail ) ;
        try {
            final String ns = "http://xyz.com/test#" ;
            final String kb = String.format ( "<%ss> <%sp> <%so> .", ns, ns, ns ) ;
            final String qs = String.format ( "select ?p ?o where { <%ss> ?p ?o .}", ns ) ;
    
            final Resource graphs [] = new Resource [] { vf.createURI ( String.format ( "%sg1", ns ) ), vf.createURI ( String.format ( "%sg2", ns ) ) } ;
    
            final Collection<BindingSet> expected = getExpected ( createBindingSet ( new BindingImpl ( "p", new URIImpl ( String.format ( "%sp", ns ) ) )
                                                                                   , new BindingImpl ( "o", new URIImpl ( String.format ( "%so", ns ) ) )
                                                                                   )
                                                          ) ;
            run ( sail, cxn, kb, graphs, qs, expected ) ;
        } finally {
            cxn.close();
        }
    }

    public void testExplicitDefaultGraph ()
        throws Exception
    {
        final EmbergraphSail sail = getTheSail () ;
        final ValueFactory vf = sail.getValueFactory();
        final RepositoryConnection cxn = getRepositoryConnection ( sail ) ;
        try {
        String ns = "http://xyz.com/test#" ;
        String kb = String.format ( "<%ss> <%sp> <%so> .", ns, ns, ns ) ;
        String qs = String.format ( "select ?p ?o from <%sg1> from <%sg2> where { <%ss> ?p ?o .}", ns, ns, ns ) ;

        Resource graphs [] = new Resource [] { vf.createURI ( String.format ( "%sg1", ns ) ), vf.createURI ( String.format ( "%sg2", ns ) ) } ;

        Collection<BindingSet> expected = getExpected ( createBindingSet ( new BindingImpl ( "p", new URIImpl ( String.format ( "%sp", ns ) ) )
                                                                         , new BindingImpl ( "o", new URIImpl ( String.format ( "%so", ns ) ) )
                                                                         )
                                                      ) ;
        run ( sail, cxn, kb, graphs, qs, expected ) ;
        } finally {
            cxn.close();
        }
    }

    public void testNamedGraphNoGraphKeyword1 ()
        throws Exception
    {
        final EmbergraphSail sail = getTheSail () ;
        final ValueFactory vf = sail.getValueFactory();
        final RepositoryConnection cxn = getRepositoryConnection ( sail ) ;
        try {
        String ns = "http://xyz.com/test#" ;
        String kb = String.format ( "<%ss> <%sp> <%so> .", ns, ns, ns ) ;
        String qs = String.format ( "select ?s from named <%sg2> where { ?s ?p ?o .}", ns ) ;

        Resource graphs [] = new Resource [] { vf.createURI ( String.format ( "%sg1", ns ) ), vf.createURI ( String.format ( "%sg2", ns ) ) } ;

        Collection<BindingSet> expected = getExpected () ;

        run ( sail, cxn, kb, graphs, qs, expected ) ;
        } finally {
            cxn.close();
        }
    }

    public void testNamedGraphNoGraphKeyword2 ()
        throws Exception
    {
        final EmbergraphSail sail = getTheSail () ;
        final ValueFactory vf = sail.getValueFactory();
        final RepositoryConnection cxn = getRepositoryConnection ( sail ) ;
        try {
        String ns = "http://xyz.com/test#" ;
        String kb = String.format ( "<%ss> <%sp> <%so> .", ns, ns, ns ) ;
        String qs = String.format ( "select ?s from named <%sg1> from named <%sg2> where { ?s ?p ?o .}", ns, ns ) ;

        Resource graphs [] = new Resource [] { vf.createURI ( String.format ( "%sg1", ns ) ), vf.createURI ( String.format ( "%sg2", ns ) ) } ;

        Collection<BindingSet> expected = getExpected () ;

        run ( sail, cxn, kb, graphs, qs, expected ) ;
        } finally {
            cxn.close();
        }
    }

    public void testExplicitDefaultAndNamedGraphNoGraphKeyword ()
        throws Exception
    {
        final EmbergraphSail sail = getTheSail () ;
        final ValueFactory vf = sail.getValueFactory();
        final RepositoryConnection cxn = getRepositoryConnection ( sail ) ;
        try {
        String ns = "http://xyz.com/test#" ;
        String kb = String.format ( "<%ss> <%sp> <%so> .", ns, ns, ns ) ;
        String qs = String.format ( "select ?s from <%sg1> from named <%sg2> where { ?s ?p ?o .}", ns, ns ) ;

        Resource graphs [] = new Resource [] { vf.createURI ( String.format ( "%sg1", ns ) ), vf.createURI ( String.format ( "%sg2", ns ) ) } ;

        Collection<BindingSet> expected = getExpected ( createBindingSet ( new BindingImpl ( "s", new URIImpl ( String.format ( "%ss", ns ) ) ) ) ) ;

        run ( sail, cxn, kb, graphs, qs, expected ) ;
        } finally {
            cxn.close();
        }
    }

    private EmbergraphSail getTheSail ()
        throws SailException
    {
        EmbergraphSail sail = getSail () ;
        sail.initialize () ;
        return sail ;
    }

    private RepositoryConnection getRepositoryConnection ( final EmbergraphSail sail )
        throws RepositoryException
    {
        final EmbergraphSailRepository repo = new EmbergraphSailRepository( sail ) ;
        final EmbergraphSailRepositoryConnection cxn = (EmbergraphSailRepositoryConnection)repo.getConnection () ;
        cxn.setAutoCommit ( false ) ;
        return cxn ;
    }

    private void run ( final EmbergraphSail sail, final RepositoryConnection rc, final String kb, final Resource graphs [], final String qs, final Collection<BindingSet> expected )
    {
        if (!((EmbergraphSailRepositoryConnection) rc).getSailConnection().isQuads()) {
            log.info("This test requires quads.");
            return;
        }
        try
        {
            for ( Resource g : graphs )
                load ( rc, kb, g ) ;
            compare ( query ( rc, qs ), expected ) ;
        }
        catch ( Exception e )
        {
            e.printStackTrace () ;
        }
        finally
        {
            try { if ( null != rc ) rc.close () ; }
            catch ( Exception e ) {
                throw new RuntimeException(e);
            } finally {
                if ( null != sail ) sail.__tearDownUnitTest () ;
            }
        }
    }

    private void load ( RepositoryConnection rc, String kb, Resource g )
        throws RepositoryException, RDFParseException, IOException
    {
        rc.add ( new ByteArrayInputStream ( kb.toString ().getBytes ( "UTF-8" ) )
               , "http://xyz.com/test"
               , RDFFormat.TURTLE
               , g
               ) ;
        rc.commit () ;
    }

    private TupleQueryResult query ( RepositoryConnection rc, String qs )
        throws RepositoryException, MalformedQueryException, QueryEvaluationException
    {
        return rc.prepareTupleQuery ( QueryLanguage.SPARQL, qs ).evaluate () ;
    }

    private Collection<BindingSet> getExpected ( BindingSet... bindingSets )
    {
        Collection<BindingSet> expected = new LinkedList<BindingSet> () ;
        for ( BindingSet bs : bindingSets )
            expected.add ( bs ) ;
        return expected ;
    }
}
