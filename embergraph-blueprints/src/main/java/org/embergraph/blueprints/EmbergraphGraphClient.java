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
package org.embergraph.blueprints;

import java.util.Collection;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.model.RunningQuery;
import org.embergraph.rdf.sail.remote.EmbergraphSailFactory;
import org.embergraph.rdf.sail.remote.EmbergraphSailRemoteRepository;
import org.embergraph.rdf.sail.remote.EmbergraphSailRemoteRepositoryConnection;
import org.embergraph.rdf.sail.webapp.client.RemoteRepository;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.QueryType;
import com.tinkerpop.blueprints.Features;

/**
 * This is a thin-client implementation of a Blueprints wrapper around the
 * client library that interacts with the NanoSparqlServer. This is a functional
 * implementation suitable for writing POCs - it is not a high performance
 * implementation by any means (currently does not support caching or batched
 * update). Does have a single "bulk upload" operation that wraps a method on
 * RemoteRepository that will POST a graphml file to the blueprints layer of the
 * bigdata server.
 * 
 * @see {@link EmbergraphSailRemoteRepository}
 * @see {@link EmbergraphSailRemoteRepositoryConnection}
 * @see {@link RemoteRepository}
 * 
 * @author mikepersonick
 * 
 */
public class EmbergraphGraphClient extends EmbergraphGraph {
	
    private static final transient Logger log = Logger.getLogger(EmbergraphGraphClient.class);

    private static final Properties props = new Properties();

//    static {
//        /*
//         * We don't want the EmbergraphGraph to close our connection after every
//         * read.  The EmbergraphGraphClient represents a session with the server.
//         */
//        props.setProperty(EmbergraphGraph.Options.READ_FROM_WRITE_CONNECTION, "true");
//    }
    
	final EmbergraphSailRemoteRepository repo;
	
	transient EmbergraphSailRemoteRepositoryConnection cxn;

   /**
    * 
    * @param sparqlEndpointURL
    *           The URL of the SPARQL end point. This will be used to read and
    *           write on the graph using the blueprints API.
    */
   public EmbergraphGraphClient(final String sparqlEndpointURL) {
     
      this(sparqlEndpointURL, EmbergraphRDFFactory.INSTANCE);
      
   }

   /**
    * 
    * @param sparqlEndpointURL
    *           The URL of the SPARQL end point. This will be used to read and
    *           write on the graph using the blueprints API.
    * @param factory
    *           The {@link BlueprintsValueFactory}.
    */
    public EmbergraphGraphClient(final String sparqlEndpointURL,
            final BlueprintsValueFactory factory) {

       this(EmbergraphSailFactory.connect(sparqlEndpointURL), factory);
       
    }
	
	public EmbergraphGraphClient(final RemoteRepository repo) {
		this(repo, EmbergraphRDFFactory.INSTANCE);
	}
	
	public EmbergraphGraphClient(final RemoteRepository repo,
			final BlueprintsValueFactory factory) {
	    this(repo.getEmbergraphSailRemoteRepository(), factory);
	}
	
    public EmbergraphGraphClient(final EmbergraphSailRemoteRepository repo) {
        this(repo, EmbergraphRDFFactory.INSTANCE);
    }
    
   /**
    * Core implementation.
    * 
    * @param repo
    *           The {@link EmbergraphSailRemoteRepository} for the desired graph.
    * @param factory
    *           The {@link BlueprintsValueFactory}.
    */
   public EmbergraphGraphClient(final EmbergraphSailRemoteRepository repo,
         final BlueprintsValueFactory factory) {

      super(factory, props);

      if (repo == null)
         throw new IllegalArgumentException();

      this.repo = repo;

   }
    
    /**
     * Post a GraphML file to the remote server. (Bulk-upload operation.)
     */
    @Override
    public void loadGraphML(final String file) throws Exception {
        this.repo.getRemoteRepository().postGraphML(file);
    }
    
    /**
    * Get a {@link EmbergraphSailRemoteRepositoryConnection}.
    * 
    * TODO Review this now that we support read/write tx for
    * EmbergraphSailRemoteRepositoryConnection (if namespace uses
    * ISOLATABLE_INDICES).
    */
	@Override
	public EmbergraphSailRemoteRepositoryConnection cxn() throws Exception {
	    if (cxn == null) {
	        cxn = repo.getConnection();
	    }
	    return cxn;
	}
	
    /**
	 * Shutdown the connection and repository (client-side, not server-side).
	 */
	@Override
	public void shutdown() {
		try {
		    if (cxn != null) {
		        cxn.close();
		    }
			repo.shutDown();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
    protected static final Features FEATURES = new Features();

    @Override
    public Features getFeatures() {

        return FEATURES;
        
    }
    
    static {
        
        FEATURES.supportsSerializableObjectProperty = EmbergraphGraph.FEATURES.supportsSerializableObjectProperty;
        FEATURES.supportsBooleanProperty = EmbergraphGraph.FEATURES.supportsBooleanProperty;
        FEATURES.supportsDoubleProperty = EmbergraphGraph.FEATURES.supportsDoubleProperty;
        FEATURES.supportsFloatProperty = EmbergraphGraph.FEATURES.supportsFloatProperty;
        FEATURES.supportsIntegerProperty = EmbergraphGraph.FEATURES.supportsIntegerProperty;
        FEATURES.supportsPrimitiveArrayProperty = EmbergraphGraph.FEATURES.supportsPrimitiveArrayProperty;
        FEATURES.supportsUniformListProperty = EmbergraphGraph.FEATURES.supportsUniformListProperty;
        FEATURES.supportsMixedListProperty = EmbergraphGraph.FEATURES.supportsMixedListProperty;
        FEATURES.supportsLongProperty = EmbergraphGraph.FEATURES.supportsLongProperty;
        FEATURES.supportsMapProperty = EmbergraphGraph.FEATURES.supportsMapProperty;
        FEATURES.supportsStringProperty = EmbergraphGraph.FEATURES.supportsStringProperty;
        FEATURES.supportsDuplicateEdges = EmbergraphGraph.FEATURES.supportsDuplicateEdges;
        FEATURES.supportsSelfLoops = EmbergraphGraph.FEATURES.supportsSelfLoops;
        FEATURES.isPersistent = EmbergraphGraph.FEATURES.isPersistent;
        FEATURES.isWrapper = EmbergraphGraph.FEATURES.isWrapper;
        FEATURES.supportsVertexIteration = EmbergraphGraph.FEATURES.supportsVertexIteration;
        FEATURES.supportsEdgeIteration = EmbergraphGraph.FEATURES.supportsEdgeIteration;
        FEATURES.supportsVertexIndex = EmbergraphGraph.FEATURES.supportsVertexIndex;
        FEATURES.supportsEdgeIndex = EmbergraphGraph.FEATURES.supportsEdgeIndex;
        FEATURES.ignoresSuppliedIds = EmbergraphGraph.FEATURES.ignoresSuppliedIds;
//        FEATURES.supportsTransactions = EmbergraphGraph.FEATURES.supportsTransactions;
        FEATURES.supportsIndices = EmbergraphGraph.FEATURES.supportsIndices;
        FEATURES.supportsKeyIndices = EmbergraphGraph.FEATURES.supportsKeyIndices;
        FEATURES.supportsVertexKeyIndex = EmbergraphGraph.FEATURES.supportsVertexKeyIndex;
        FEATURES.supportsEdgeKeyIndex = EmbergraphGraph.FEATURES.supportsEdgeKeyIndex;
        FEATURES.supportsEdgeRetrieval = EmbergraphGraph.FEATURES.supportsEdgeRetrieval;
        FEATURES.supportsVertexProperties = EmbergraphGraph.FEATURES.supportsVertexProperties;
        FEATURES.supportsEdgeProperties = EmbergraphGraph.FEATURES.supportsEdgeProperties;
        FEATURES.supportsThreadedTransactions = EmbergraphGraph.FEATURES.supportsThreadedTransactions;
        
        // override
        FEATURES.supportsTransactions = false; //EmbergraphGraph.FEATURES.supportsTransactions;
        
    }

	@Override
	protected UUID setupQuery(EmbergraphSailRepositoryConnection cxn,
			ASTContainer astContainer, QueryType queryType, String extQueryId) {
		//This is a NOOP when using the REST client as the query management is implemented
		//in the rest client.
		return null;
	}

	@Override
	protected void tearDownQuery(UUID queryId) {
		//This is a NOOP when using the REST client as the query management is implemented
		//in the rest client.
		
	}

	@Override
	public Collection<RunningQuery> getRunningQueries() {
		try {
			return this.repo.showQueries();
		} catch (Exception e) {
			if(log.isDebugEnabled()){
				log.debug(e);
			}
		}
		
		throw new RuntimeException("Error while showing queries.");
	}

	@Override
	public void cancel(UUID queryId) {

		assert(queryId != null);

		try {
			this.repo.cancel(queryId);
		} catch (Exception e) {
			if(log.isDebugEnabled()) {
				log.debug(e);
			}
		}
	}

	@Override
	public void cancel(String queryId) {
		assert(queryId != null);
		cancel(UUID.fromString(queryId));
	}

	@Override
	public void cancel(RunningQuery r) {
		assert(r != null);
		cancel(r.getQueryUuid());
	}

	@Override
	public RunningQuery getQueryById(UUID queryId2) {
		//TODO:  Implement for REST API
		return null;
	}

	@Override
	public RunningQuery getQueryByExternalId(String extQueryId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean isQueryCancelled(UUID queryId) {
		// TODO Auto-generated method stub
		return false;
	}
	
   @Override
    public boolean isReadOnly() {
        return false;
    }


}
