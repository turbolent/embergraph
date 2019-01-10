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
/*
 * Created on Aug 19, 2010
 */

package org.embergraph.bop.ap;

import java.io.Serializable;

import org.embergraph.bop.IElement;

/**
 * An element for the test {@link R relation}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class E implements IElement, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    final String name;

    final String value;

    public E(final String name, final String value) {

        if (name == null)
            throw new IllegalArgumentException();

        if (value == null)
            throw new IllegalArgumentException();

        this.name = name;

        this.value = value;

    }

    public String toString() {
        
        return "E{name=" + name + ",value=" + value + "}";
        
    }
    
    public Object get(final int index) {
        switch (index) {
        case 0:
            return name;
        case 1:
            return value;
        }
        throw new IllegalArgumentException();
    }

    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof E))
            return false;
        if (!name.equals(((E) o).name))
            return false;
        if (!value.equals(((E) o).value))
            return false;
        return true;
    }
    
    public int hashCode() {
    	return name.hashCode() + value.hashCode();
    }
    
}
