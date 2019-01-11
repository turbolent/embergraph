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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IVariable;

/**
 * Recursive container for ground {@link StatementPatternNode}s. This is used
 * for {@link InsertData} and {@link DeleteData}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class AbstractStatementContainer<E extends IStatementContainer> extends
        GroupNodeBase<E> {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AbstractStatementContainer() {
        
    }
    
    /**
     * @param op
     */
    public AbstractStatementContainer(final AbstractStatementContainer<E> op) {
    
        super(op);

    }

    /**
     * @param args
     * @param anns
     */
    public AbstractStatementContainer(final BOp[] args,
            final Map<String, Object> anns) {

        super(args, anns);

    }

    @Override
    public Set<IVariable<?>> getRequiredBound(StaticAnalysis sa) {
        return new HashSet<IVariable<?>>();
    }

    @Override
    public Set<IVariable<?>> getDesiredBound(StaticAnalysis sa) {
       return new HashSet<IVariable<?>>();
    }

}
