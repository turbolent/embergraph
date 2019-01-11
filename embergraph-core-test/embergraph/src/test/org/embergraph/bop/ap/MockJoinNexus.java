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

import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.Var;
import org.embergraph.btree.keys.ISortKeyBuilder;
import org.embergraph.journal.IIndexManager;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.eval.AbstractJoinNexus;
import org.embergraph.relation.rule.eval.IJoinNexus;
import org.embergraph.relation.rule.eval.IJoinNexusFactory;

/**
 * Mock object.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
class MockJoinNexus extends AbstractJoinNexus implements IJoinNexus {

    protected MockJoinNexus(final IJoinNexusFactory joinNexusFactory,
            final IIndexManager indexManager) {
     
        super(joinNexusFactory, indexManager);
        
    }

    public IConstant fakeBinding(IPredicate predicate, Var var) {
        // TODO Auto-generated method stub
        return null;
    }

    public ISortKeyBuilder<IBindingSet> newBindingSetSortKeyBuilder(IRule rule) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected ISortKeyBuilder<?> newSortKeyBuilder(IPredicate<?> head) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
