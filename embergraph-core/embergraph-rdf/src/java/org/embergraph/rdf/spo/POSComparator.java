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
package org.embergraph.rdf.spo;

import java.util.Comparator;


/**
 * Imposes p:o:s ordering based on termIds only.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class POSComparator implements Comparator<ISPO> {

    public static final transient Comparator<ISPO> INSTANCE = new POSComparator();

    private POSComparator() {
        
    }

    public int compare(ISPO stmt1, ISPO stmt2) {

        if (stmt1 == stmt2)
            return 0;

        /*
         * Note: logic avoids possible overflow of [long] by not computing the
         * difference between two longs.
         */
        int ret;
        
        ret = stmt1.p().compareTo(stmt2.p());
        
        if( ret == 0 ) {
        
            ret = stmt1.o().compareTo(stmt2.o());
            
            if( ret == 0 ) {
                
                ret = stmt1.s().compareTo(stmt2.s());
                
            }
            
        }

        return ret;
        
    }
    
}
