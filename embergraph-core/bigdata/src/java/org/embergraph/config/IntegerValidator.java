/*

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
/*
 * Created on Nov 23, 2008
 */

package org.embergraph.config;

/**
 * Base impl for {@link Integer}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class IntegerValidator implements IValidator<Integer> {

    /**
     * Allows all values.
     */
    public static final transient IValidator<Integer> DEFAULT = new IntegerValidator();
    
    /**
     * Only allows non-negative values (GTE ZERO).
     */
    public static final transient IValidator<Integer> GTE_ZERO = new IntegerValidator() {
        
        public void accept(String key, String val, Integer arg) {
            if (arg < 0)
                throw new ConfigurationException(key, val,
                        "Must be non-negative");
        }
        
    };

    /**
     * Only allows positive values (GT ZERO).
     */
    public static final transient IValidator<Integer> GT_ZERO = new IntegerValidator() {
        
        public void accept(String key, String val, Integer arg) {
            if (arg <= 0)
                throw new ConfigurationException(key, val,
                        "Must be positive");
        }
        
    };
    
    public Integer parse(String key, String val) {
        
        return Integer.parseInt(val);
        
    }
    
    /**
     * Accepts all values by default.
     */
    public void accept(String key, String val, Integer arg)
            throws ConfigurationException {
        
    }

}
