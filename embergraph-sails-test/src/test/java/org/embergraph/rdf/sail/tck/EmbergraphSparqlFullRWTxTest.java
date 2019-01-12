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
 * Created on May 6, 2011
 */
package org.embergraph.rdf.sail.tck;

import java.util.Properties;

import org.openrdf.query.Dataset;

import org.embergraph.journal.Journal;
import org.embergraph.rdf.sail.EmbergraphSail.Options;

/**
 * Test harness for running the SPARQL test suites against a {@link Journal}
 * using full read/write transaction support.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class EmbergraphSparqlFullRWTxTest extends EmbergraphSparqlTest {

    public EmbergraphSparqlFullRWTxTest(String testURI, String name, String queryFileURL,
            String resultFileURL, Dataset dataSet, boolean laxCardinality,
            boolean checkOrder) {

        super(testURI, name, queryFileURL, resultFileURL, dataSet,
                laxCardinality, checkOrder);

    }
    
    @Override
    protected Properties getProperties() {

        final Properties properties = super.getProperties();
         
        // enable read/write transactions.
        properties.setProperty(Options.ISOLATABLE_INDICES, "true");

        return properties;
        
    }

//	/**
//	 * Overridden to use {@link EmbergraphSail#getConnection()} since we do not
//	 * have to workaround a deadlock in concurrent access to the unisolated
//	 * connection by the test harness when using full read-write transactions.
//	 */
//	@Override
//	protected EmbergraphSailRepositoryConnection getQueryConnection(Repository dataRep)
//			throws Exception {
//
//		return ((EmbergraphSailRepository) ((DatasetRepository) dataRep)
//				.getDelegate()).getConnection();
//
//	}

}
