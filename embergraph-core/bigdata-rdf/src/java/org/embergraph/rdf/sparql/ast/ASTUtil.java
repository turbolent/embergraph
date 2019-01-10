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
 * Created on Aug 31, 2011
 */

package org.embergraph.rdf.sparql.ast;

import com.bigdata.bop.IVariable;

/**
 * Some utility methods for AST/IV conversions.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class ASTUtil {

    /**
     * Convert an {@link IVariable}[] into a {@link VarNode}[].
     */
    static public VarNode[] convert(final IVariable[] a) {

        if (a == null)
            return null;

        final VarNode[] b = new VarNode[a.length];

        for (int i = 0; i < a.length; i++) {

            b[i] = new VarNode(a[i].getName());

        }

        return b;

    }

    /**
     * Convert an {@link VarNode}[] into an {@link IVariable}[].
     */
    static public IVariable[] convert(final VarNode[] a) {

        if (a == null)
            return null;

        final IVariable[] b = new IVariable[a.length];

        for (int i = 0; i < a.length; i++) {

            b[i] = a[i].getValueExpression();

        }

        return b;

    }

}
