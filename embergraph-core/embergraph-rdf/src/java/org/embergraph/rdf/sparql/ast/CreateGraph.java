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
 * Created on Mar 10, 2012
 */

package org.embergraph.rdf.sparql.ast;

import java.util.Map;

import org.embergraph.bop.BOp;
import org.embergraph.rdf.model.EmbergraphStatement;

/**
 * This operation creates a graph in the Graph Store (this operation is a NOP
 * for embergraph).
 * 
 * <pre>
 * CREATE ( SILENT )? GRAPH IRIref
 * </pre>
 * 
 * @see http://www.w3.org/TR/sparql11-update/#create
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class CreateGraph extends AbstractOneGraphManagement {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public interface Annotations extends AbstractOneGraphManagement.Annotations {
        
        /**
         * The {@link EmbergraphStatement}[] which provides the provisioning
         * information for the named solution set (optional, even when creating
         * a named solution set).
         */
        String PARAMS = "params";
        
    }
    
    public CreateGraph() {

        super(UpdateType.Create);
        
    }

    /**
     * @param op
     */
    public CreateGraph(final CreateGraph op) {
        
        super(op);
        
    }

    /**
     * @param args
     * @param anns
     */
    public CreateGraph(final BOp[] args, final Map<String, Object> anns) {

        super(args, anns);
        
    }

    /**
     * Return the parameters used to provision a named solution set.
     */
    public EmbergraphStatement[] getParams() {

        return (EmbergraphStatement[]) getProperty(Annotations.PARAMS);

    }

    public void setParams(final EmbergraphStatement[] params) {

        setProperty(Annotations.PARAMS, params);

    }

}
