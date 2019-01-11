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
 * Created on Jun 12, 2008
 */

package org.embergraph.btree.filter;

import java.util.UUID;

import org.embergraph.btree.AbstractBTree;
import org.embergraph.btree.AbstractTupleCursorTestCase;
import org.embergraph.btree.BTree;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleCursor2;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.TestTuple;
import org.embergraph.btree.Tuple;
import org.embergraph.btree.AbstractBTreeTupleCursor.MutableBTreeTupleCursor;
import org.embergraph.rawstore.SimpleMemoryRawStore;

/**
 * Test suite for the {@link Removerator}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestRemoverator extends AbstractTupleCursorTestCase {

    /**
     * 
     */
    public TestRemoverator() {
    }

    /**
     * @param name
     */
    public TestRemoverator(String name) {
        super(name);
    }

    /**
     * Test verifies that we can remove each tuple as we visit it and that the
     * tuple returned to the caller is not invalidated by that remove().
     */
    public void test() {
    
        final BTree btree = BTree.create(new SimpleMemoryRawStore(),
                new IndexMetadata(UUID.randomUUID()));

        btree.insert(10, "Bryan");
        btree.insert(20, "Mike");
        btree.insert(30, "James");

        final ITupleIterator<String> itr = new TupleRemover<String>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected boolean remove(ITuple<String> e) {
                // all visited tuples will be removed.
                return true;
            }
        }.filterOnce(newCursor(btree), null/* context */);
        
        assertTrue(itr.hasNext());

        assertTrue(btree.contains(10));

        assertEquals(new TestTuple<String>(10,"Bryan"),itr.next());
        
        assertFalse(btree.contains(10));
        
        assertTrue(itr.hasNext());

        assertTrue(btree.contains(20));

        assertEquals(new TestTuple<String>(20,"Mike"),itr.next());

        assertFalse(btree.contains(20));

        assertTrue(itr.hasNext());

        assertTrue(btree.contains(30));

        assertEquals(new TestTuple<String>(30,"James"),itr.next());

        assertFalse(itr.hasNext());

        assertFalse(btree.contains(30));

        assertEquals(0,btree.getEntryCount());
        
    }

    @Override
    protected ITupleCursor2<String> newCursor(AbstractBTree btree, int flags, byte[] fromKey, byte[] toKey) {
    
        return new MutableBTreeTupleCursor<String>((BTree) btree,
                new Tuple<String>(btree, flags), fromKey, toKey);

    }
    
}
