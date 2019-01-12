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
 * Created on Jun 20, 2011
 */

package org.embergraph.rdf.sail;

import java.util.Map;

import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.parser.ParsedGraphQuery;

/**
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class EmbergraphParsedGraphQuery extends ParsedGraphQuery {

//    private final QueryType queryType;
//    private final Properties queryHints;
    
    /**
     * @param tupleExpr
     *        A tuple expression representing the query, formulated in Sail Query
     *        Model objects.
     * @param namespaces
     *        A mapping of namespace prefixes to namespace names representing the
     *        namespaces that are used in the query.
     */
    public EmbergraphParsedGraphQuery(final TupleExpr tupleExpr,
            final Map<String, String> namespaces//, final QueryType queryType
//            final Properties queryHints
            ) {

        super(tupleExpr, namespaces);
        
//        this.queryType = queryType;
        
//        this.queryHints = queryHints;
        
    }

//    public QueryType getQueryType() {
//        
//        return queryType;
//        
//    }

//    public Properties getQueryHints() {
//
//        return queryHints;
//        
//    }

}
