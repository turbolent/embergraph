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
 * Created on Dec 11, 2006
 */

package org.embergraph.btree;

import org.apache.log4j.Level;

import org.embergraph.btree.keys.TestKeyBuilder;

/**
 * Test suite for iterators that visit only dirty nodes or leaves. This test
 * suite was factored apart from {@link TestIterators} since this suite relies
 * on (and to some extent validates) both node and leaf IO and copy-on-write
 * mechanisms.
 * 
 * @see Node#childIterator(boolean)
 * @see AbstractNode#postOrderNodeIterator(boolean, boolean)
 * 
 * @see TestIterators, which handles iterators that do not differentiate between
 *      clear and dirty nodes.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestDirtyIterators extends AbstractBTreeTestCase {

    /**
     * 
     */
    public TestDirtyIterators() {
    }

    /**
     * @param name
     */
    public TestDirtyIterators(String name) {
        super(name);
    }

    /**
     * Test ability to visit the direct dirty children of a node. For this test
     * we only verify that the dirty child iterator will visit the same children
     * as the normal child iterator. This is true since we never evict a node
     * onto the store during this test - see {@link #getBTree(int)}, which
     * throws an exception if the tree attempts a node eviction.
     */
    public void test_dirtyChildIterator01() {

        BTree btree = getBTree(3);

        final Leaf a = (Leaf) btree.root;
        
        SimpleEntry v1 = new SimpleEntry(1);
        SimpleEntry v2 = new SimpleEntry(2);
        SimpleEntry v3 = new SimpleEntry(3);
        SimpleEntry v5 = new SimpleEntry(5);
        SimpleEntry v7 = new SimpleEntry(7);
        SimpleEntry v9 = new SimpleEntry(9);

        // fill up the root leaf.
        btree.insert(TestKeyBuilder.asSortKey(3), v3);
        btree.insert(TestKeyBuilder.asSortKey(5), v5);
        btree.insert(TestKeyBuilder.asSortKey(7), v7);

        // split the root leaf.
        btree.insert(TestKeyBuilder.asSortKey(9), v9);
        final Node c = (Node) btree.root;
        assertKeys(new int[]{7},c);
        assertEquals(a,c.getChild(0));
        final Leaf b = (Leaf)c.getChild(1);
        assertKeys(new int[]{3,5},a);
        assertValues(new Object[]{v3,v5}, a);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        assertTrue(a.isDirty());
        assertTrue(b.isDirty());
        
        // verify visiting all children.
        assertSameIterator(new IAbstractNode[] { a, b }, ((Node) btree.root)
                .childIterator(false));
        assertSameIterator(new IAbstractNode[] { a, b }, ((Node) btree.root)
                .childIterator(true));

        /*
         * split another leaf so that there are now three children to visit. at
         * this point the root is full.
         */
        btree.insert(TestKeyBuilder.asSortKey(1), v1);
        btree.insert(TestKeyBuilder.asSortKey(2), v2);
        assertKeys(new int[]{3,7},c);
        assertEquals(a,c.getChild(0));
        Leaf d = (Leaf)c.getChild(1);
        assertEquals(b,c.getChild(2));
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2}, a);
        assertKeys(new int[]{3,5},d);
        assertValues(new Object[]{v3,v5}, d);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        assertTrue(a.isDirty());
        assertTrue(d.isDirty());
        assertTrue(b.isDirty());

        // verify visiting all children.
        assertSameIterator(new IAbstractNode[] { a, d, b }, ((Node) btree.root)
                .childIterator(false));
        assertSameIterator(new IAbstractNode[] { a, d, b }, ((Node) btree.root)
                .childIterator(true));

        /*
         * remove a key from a leaf forcing two leaves to join and verify the
         * visitation order.
         */
        assertEquals(v1,btree.remove(TestKeyBuilder.asSortKey(1)));
        assertKeys(new int[]{7},c);
        assertEquals(a,c.getChild(0));
        assertEquals(b,c.getChild(1));
        assertKeys(new int[]{2,3,5},a);
        assertValues(new Object[]{v2,v3,v5}, a);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        assertTrue(d.isDeleted());
        assertTrue(a.isDirty());
        assertTrue(b.isDirty());

        // verify visiting all children.
        assertSameIterator(new IAbstractNode[] { a, b }, ((Node) btree.root)
                .childIterator(false));
        assertSameIterator(new IAbstractNode[] { a, b }, ((Node) btree.root)
                .childIterator(true));

        /*
         * Note: the test ends here since there must be either 2 or 3 children
         * for the root node.  If we force the remaining leaves to join, then
         * the root node will be replaced by a root leaf.
         */

    }

    /**
     * Test ability to visit the direct dirty children of a node. This test
     * works by explicitly writing out either the root node or a leaf and
     * verifying that the dirty children iterator correctly visits only those
     * children that should be marked as dirty after the others have been
     * written onto the store. Note that this does not force the eviction of
     * nodes or leaves but rather requests that the are written out directly.
     * Whenever we make an immutable node or leaf mutable using copy-on-write,
     * we wind up with a new reference for that node or leaf and update the
     * variables in the test appropriately.
     */
    public void test_dirtyChildIterator02() {

        BTree btree = getBTree(3);

        final Leaf a = (Leaf) btree.root;
        
        SimpleEntry v1 = new SimpleEntry(1);
        SimpleEntry v2 = new SimpleEntry(2);
        SimpleEntry v3 = new SimpleEntry(3);
        SimpleEntry v5 = new SimpleEntry(5);
        SimpleEntry v7 = new SimpleEntry(7);
        SimpleEntry v8 = new SimpleEntry(8);
        SimpleEntry v9 = new SimpleEntry(9);

        // fill up the root leaf.
        btree.insert(TestKeyBuilder.asSortKey(3), v3);
        btree.insert(TestKeyBuilder.asSortKey(5), v5);
        btree.insert(TestKeyBuilder.asSortKey(7), v7);

        // split the root leaf.
        btree.insert(TestKeyBuilder.asSortKey(9), v9);
        final Node c = (Node) btree.root;
        assertKeys(new int[]{7},c);
        assertEquals(a,c.getChild(0));
        final Leaf b = (Leaf)c.getChild(1);
        assertKeys(new int[]{3,5},a);
        assertValues(new Object[]{v3,v5}, a);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        assertTrue(a.isDirty());
        assertTrue(b.isDirty());
        
        // verify visiting all children.
        assertSameIterator(new IAbstractNode[] { a, b }, ((Node) btree.root)
                .childIterator(true));

        /*
         * split another leaf so that there are now three children to visit. at
         * this point the root is full.
         */
        btree.insert(TestKeyBuilder.asSortKey(1), v1);
        btree.insert(TestKeyBuilder.asSortKey(2), v2);
        assertKeys(new int[]{3,7},c);
        assertEquals(a,c.getChild(0));
        Leaf d = (Leaf)c.getChild(1);
        assertEquals(b,c.getChild(2));
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2}, a);
        assertKeys(new int[]{3,5},d);
        assertValues(new Object[]{v3,v5}, d);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        assertTrue(a.isDirty());
        assertTrue(d.isDirty());
        assertTrue(b.isDirty());

        // verify visiting all children.
        assertSameIterator(new IAbstractNode[] { a, d, b }, ((Node) btree.root)
                .childIterator(true));

        // write (a) onto the store and verify that it is no longer visited.
        btree.writeNodeOrLeaf(a);
        assertFalse(a.isDirty());
        assertTrue(a.isPersistent());
        assertSameIterator(new IAbstractNode[] { d, b }, ((Node) btree.root)
                .childIterator(true));
        
        // write (b) onto the store and verify that it is no longer visited.
        btree.writeNodeOrLeaf(b);
        assertFalse(b.isDirty());
        assertTrue(b.isPersistent());
        assertSameIterator(new IAbstractNode[] { d }, ((Node) btree.root)
                .childIterator(true));
        
        // write (d) onto the store and verify that it is no longer visited.
        btree.writeNodeOrLeaf(d);
        assertFalse(d.isDirty());
        assertTrue(d.isPersistent());
        assertSameIterator(new IAbstractNode[] {}, ((Node) btree.root)
                .childIterator(true));
        
        /*
         * remove a key from a leaf forcing two leaves to join and verify the
         * visitation order.  this triggers copy-on-write for (a) and (a) is
         * dirty as a post-condition.
         */
        assertEquals(v1,btree.remove(TestKeyBuilder.asSortKey(1)));
        assertKeys(new int[]{7},c);
        assertNotSame(a,c.getChild(0));
        Leaf a1 = (Leaf)c.getChild(0);
        assertEquals(b,c.getChild(1));
        assertKeys(new int[]{2,3,5},a1);
        assertValues(new Object[]{v2,v3,v5}, a1);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        assertTrue(d.isDeleted());
        assertTrue(a1.isDirty());
        assertFalse(b.isDirty());

        // verify visiting dirty children.
        assertSameIterator(new IAbstractNode[] { a1 }, ((Node) btree.root)
                .childIterator(true));

        /*
         * insert a key that will go into (b).  since (b) is immutable this
         * triggers copy-on-write.
         */
        btree.insert(TestKeyBuilder.asSortKey(8),v8);
        assertKeys(new int[]{7},c);
        assertEquals(a1,c.getChild(0));
        assertNotSame(b,c.getChild(1));
        Leaf b1 = (Leaf)c.getChild(1);
        assertKeys(new int[]{2,3,5},a1);
        assertValues(new Object[]{v2,v3,v5}, a1);
        assertKeys(new int[]{7,8,9},b1);
        assertValues(new Object[]{v7,v8,v9}, b1);
        assertTrue(c.isDirty());
        assertTrue(d.isDeleted());
        assertTrue(a1.isDirty());
        assertTrue(b1.isDirty());

        // verify visiting dirty children.
        assertSameIterator(new IAbstractNode[] { a1, b1 }, ((Node) btree.root)
                .childIterator(true));

        /*
         * write the root node of the tree onto the store.
         */
        btree.writeNodeRecursive(c);
        assertFalse(c.isDirty());
        assertFalse(a1.isDirty());
        assertFalse(b1.isDirty());

        // verify visiting dirty children.
        assertSameIterator(new IAbstractNode[] {}, ((Node) btree.root)
                .childIterator(true));

        /*
         * remove a key from (a1). since (a1) is immutable this triggers
         * copy-on-write. since the root is immtuable, it is also copied.
         */
        assertEquals(v2,btree.remove(TestKeyBuilder.asSortKey(2)));
        assertNotSame(c,btree.root);
        Node c1 = (Node)btree.root;
        assertKeys(new int[]{7},c1);
        assertNotSame(a1,c1.getChild(0));
        Leaf a2 = (Leaf) c1.getChild(0);
        assertEquals( b1, c1.getChild(1));
        assertKeys(new int[]{3,5},a2);
        assertValues(new Object[]{v3,v5}, a2);
        assertKeys(new int[]{7,8,9},b1);
        assertValues(new Object[]{v7,v8,v9}, b1);
        assertTrue(c1.isDirty());
        assertTrue(a2.isDirty());
        assertFalse(b1.isDirty());
        
        // verify visiting dirty children.
        assertSameIterator(new IAbstractNode[] {a2}, ((Node) btree.root)
                .childIterator(true));

        /*
         * Note: the test ends here since there must be either 2 or 3 children
         * for the root node.  If we force the remaining leaves to join, then
         * the root node will be replaced by a root leaf.
         */

    }

    /**
     * Test ability to visit the dirty nodes of the tree in a post-order
     * traversal. This version of the test verifies that the dirty post-order
     * iterator will visit the same nodes as the normal post-order iterator
     * since all nodes are dirty.
     */
    public void test_dirtyPostOrderIterator01() {

        BTree btree = getBTree(3);

        final Leaf a = (Leaf) btree.root;
        
        SimpleEntry v1 = new SimpleEntry(1);
        SimpleEntry v2 = new SimpleEntry(2);
        SimpleEntry v3 = new SimpleEntry(3);
        SimpleEntry v4 = new SimpleEntry(4);
        SimpleEntry v6 = new SimpleEntry(6);
        SimpleEntry v5 = new SimpleEntry(5);
        SimpleEntry v7 = new SimpleEntry(7);
        SimpleEntry v9 = new SimpleEntry(9);

        // empty tree visits the root leaf.
        assertSameIterator(new IAbstractNode[] { btree.root }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { btree.root }, btree.root
                .postOrderNodeIterator(true));
        
        // fill up the root leaf.
        btree.insert(TestKeyBuilder.asSortKey(3), v3);
        btree.insert(TestKeyBuilder.asSortKey(5), v5);
        btree.insert(TestKeyBuilder.asSortKey(7), v7);

        // split the root leaf.
        btree.insert(TestKeyBuilder.asSortKey(9), v9);
        final Node c = (Node) btree.root;
        assertKeys(new int[]{7},c);
        assertEquals(a,c.getChild(0));
        final Leaf b = (Leaf)c.getChild(1);
        assertKeys(new int[]{3,5},a);
        assertValues(new Object[]{v3,v5}, a);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        
        // verify iterator.
        assertSameIterator(new IAbstractNode[] { a, b, c }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { a, b, c }, btree.root
                .postOrderNodeIterator(true));

        /*
         * split another leaf so that there are now three children to visit. at
         * this point the root is full.
         */
        btree.insert(TestKeyBuilder.asSortKey(1), v1);
        btree.insert(TestKeyBuilder.asSortKey(2), v2);
        assertKeys(new int[]{3,7},c);
        assertEquals(a,c.getChild(0));
        Leaf d = (Leaf)c.getChild(1);
        assertEquals(b,c.getChild(2));
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2}, a);
        assertKeys(new int[]{3,5},d);
        assertValues(new Object[]{v3,v5}, d);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);

        // verify iterator
        assertSameIterator(new IAbstractNode[] { a, d, b, c }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { a, d, b, c }, btree.root
                .postOrderNodeIterator(true));
        
        /*
         * cause another leaf (d) to split, forcing the split to propagate to and
         * split the root and the tree to increase in height.
         */
        btree.insert(TestKeyBuilder.asSortKey(4), v4);
        btree.insert(TestKeyBuilder.asSortKey(6), v6);
//        btree.dump(Level.DEBUG,System.err);
        assertNotSame(c,btree.root);
        final Node g = (Node)btree.root;
        assertKeys(new int[]{5},g);
        assertEquals(c,g.getChild(0));
        final Node f = (Node)g.getChild(1);
        assertKeys(new int[]{3},c);
        assertEquals(a,c.getChild(0));
        assertEquals(d,c.getChild(1));
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2}, a);
        assertKeys(new int[]{3,4},d);
        assertValues(new Object[]{v3,v4}, d);
        assertKeys(new int[]{7},f);
        Leaf e = (Leaf)f.getChild(0);
        assertEquals(b,f.getChild(1));
        assertKeys(new int[]{5,6},e);
        assertValues(new Object[]{v5,v6}, e);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);

        // verify iterator
        assertSameIterator(new IAbstractNode[] { a, d, c, e, b, f, g }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { a, d, c, e, b, f, g }, btree.root
                .postOrderNodeIterator(true));

        /*
         * remove a key (4) from (d) forcing (d,a) to merge into (d) and (a) to
         * be deleted. this causes (c,f) to merge as well, which in turn forces
         * the root to be replaced by (c).
         */
        assertEquals(v4,btree.remove(TestKeyBuilder.asSortKey(4)));
//        btree.dump(Level.DEBUG,System.err);
        assertKeys(new int[]{5,7},c);
        assertEquals(d,c.getChild(0));
        assertEquals(e,c.getChild(1));
        assertEquals(b,c.getChild(2));
        assertKeys(new int[]{1,2,3},d);
        assertValues(new Object[]{v1,v2,v3}, d);
        assertKeys(new int[]{5,6},e);
        assertValues(new Object[]{v5,v6}, e);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        assertTrue(a.isDeleted());

        // verify iterator
        assertSameIterator(new IAbstractNode[] { d, e, b, c }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { d, e, b, c }, btree.root
                .postOrderNodeIterator(true));

        /*
         * remove a key (7) from a leaf (b) forcing two leaves to join and
         * verify the visitation order.
         */
        assertEquals(v7,btree.remove(TestKeyBuilder.asSortKey(7)));
        btree.dump(Level.DEBUG,System.err);
        assertKeys(new int[]{5},c);
        assertEquals(d,c.getChild(0));
        assertEquals(b,c.getChild(1));
        assertKeys(new int[]{1,2,3},d);
        assertValues(new Object[]{v1,v2,v3}, d);
        assertKeys(new int[]{5,6,9},b);
        assertValues(new Object[]{v5,v6,v9}, b);
        assertTrue(e.isDeleted());

        // verify iterator
        assertSameIterator(new IAbstractNode[] { d, b, c }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { d, b, c }, btree.root
                .postOrderNodeIterator(true));

        /*
         * remove keys from a leaf forcing the remaining two leaves to join and
         * verify the visitation order.
         */
        assertEquals(v3,btree.remove(TestKeyBuilder.asSortKey(3)));
        assertEquals(v5,btree.remove(TestKeyBuilder.asSortKey(5)));
        assertEquals(v6,btree.remove(TestKeyBuilder.asSortKey(6)));
        assertKeys(new int[]{1,2,9},b);
        assertValues(new Object[]{v1,v2,v9}, b);
        assertTrue(d.isDeleted());
        assertTrue(c.isDeleted());

        // verify iterator
        assertSameIterator(new IAbstractNode[] { b }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { b }, btree.root
                .postOrderNodeIterator(true));

    }

    /**
     * Test ability to visit the dirty nodes of the tree in a post-order
     * traversal. This version of the test writes out some nodes and/or leaves
     * in order to verify that the post-order iterator will visit only those
     * nodes and leaves that are currently dirty. Note that writing out a node
     * or leaf makes it immutable. In order to make the node or leaf dirty again
     * we have to modify it, which triggers copy-on-write. Copy on write
     * propagates up from the leaf where we make the mutation and causes any
     * immutable parents to be cloned as well. Nodes and leaves that have been
     * cloned by copy-on-write are distinct objects from their immutable
     * predecessors.
     */
    public void test_dirtyPostOrderIterator02() {

        BTree btree = getBTree(3);

        Leaf a = (Leaf) btree.root;
        
        SimpleEntry v1 = new SimpleEntry(1);
        SimpleEntry v2 = new SimpleEntry(2);
        SimpleEntry v3 = new SimpleEntry(3);
        SimpleEntry v4 = new SimpleEntry(4);
        SimpleEntry v6 = new SimpleEntry(6);
        SimpleEntry v5 = new SimpleEntry(5);
        SimpleEntry v7 = new SimpleEntry(7);
        SimpleEntry v9 = new SimpleEntry(9);

        // empty tree visits the root leaf.
        assertSameIterator(new IAbstractNode[] { btree.root }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { btree.root }, btree.root
                .postOrderNodeIterator(true));
        /*
         * write out the root leaf on the store and verify that the dirty
         * iterator does not visit anything while the normal iterator visits the
         * root.
         */
        btree.writeNodeRecursive(btree.root);
        assertSameIterator(new IAbstractNode[] { btree.root }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] {}, btree.root
                .postOrderNodeIterator(true));
        
        /*
         * Fill up the root leaf. Since it was immutable, this will trigger
         * copy-on-write.  We verify that the root leaf reference is changed
         * and verify that both iterators now visit the root.
         */
        assertEquals(a,btree.root);
        btree.insert(TestKeyBuilder.asSortKey(3), v3);
        assertNotSame(a,btree.root);
        a = (Leaf)btree.root; // new reference for the root leaf.
        assertSameIterator(new IAbstractNode[] { btree.root }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { btree.root }, btree.root
                .postOrderNodeIterator(true));
        btree.insert(TestKeyBuilder.asSortKey(5), v5);
        btree.insert(TestKeyBuilder.asSortKey(7), v7);

        // split the root leaf.
        btree.insert(TestKeyBuilder.asSortKey(9), v9);
        Node c = (Node) btree.root;
        assertKeys(new int[]{7},c);
        assertEquals(a,c.getChild(0));
        Leaf b = (Leaf)c.getChild(1);
        assertKeys(new int[]{3,5},a);
        assertValues(new Object[]{v3,v5}, a);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        
        // verify iterator.
        assertSameIterator(new IAbstractNode[] { a, b, c }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { a, b, c }, btree.root
                .postOrderNodeIterator(true));
        
        /*
         * write out (a) and verify the iterator behaviors.
         */
        btree.writeNodeOrLeaf(a);
        assertTrue(a.isPersistent());
        assertFalse(b.isPersistent());
        assertFalse(c.isPersistent());
        assertSameIterator(new IAbstractNode[] { a, b, c }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { b, c }, btree.root
                .postOrderNodeIterator(true));

        /*
         * write out (c) and verify the iterator behaviors.
         */
        btree.writeNodeRecursive(c);
        assertTrue(a.isPersistent());
        assertTrue(b.isPersistent());
        assertTrue(c.isPersistent());
        assertSameIterator(new IAbstractNode[] { a, b, c }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { }, btree.root
                .postOrderNodeIterator(true));

        /*
         * split another leaf (a) so that there are now three children to visit.
         * at this point the root is full.
         */
        assertTrue(a.isPersistent());
        assertTrue(b.isPersistent());
        assertTrue(c.isPersistent());
        btree.insert(TestKeyBuilder.asSortKey(1), v1); // triggers copy on write for (a) and (c).
        assertNotSame(c,btree.root);
        c = (Node)btree.root;
        assertNotSame(a,c.getChild(0));
        a = (Leaf)c.getChild(0);
        assertEquals(b,c.getChild(1)); // b was not copied.
        assertFalse(a.isPersistent());
        assertTrue(b.isPersistent());
        assertFalse(c.isPersistent());
        btree.insert(TestKeyBuilder.asSortKey(2), v2);
        assertKeys(new int[]{3,7},c);
        assertEquals(a,c.getChild(0));
        Leaf d = (Leaf)c.getChild(1);
        assertEquals(b,c.getChild(2));
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2}, a);
        assertKeys(new int[]{3,5},d);
        assertValues(new Object[]{v3,v5}, d);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);

        // verify iterator
        assertFalse(a.isPersistent());
        assertTrue(b.isPersistent());
        assertFalse(c.isPersistent());
        assertSameIterator(new IAbstractNode[] { a, d, b, c }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { a, d, c }, btree.root
                .postOrderNodeIterator(true));
        
        /*
         * cause another leaf (d) to split, forcing the split to propagate to and
         * split the root and the tree to increase in height.
         */
        btree.insert(TestKeyBuilder.asSortKey(4), v4);
        btree.insert(TestKeyBuilder.asSortKey(6), v6);
//        btree.dump(Level.DEBUG,System.err);
        assertNotSame(c,btree.root);
        final Node g = (Node)btree.root;
        assertKeys(new int[]{5},g);
        assertEquals(c,g.getChild(0));
        final Node f = (Node)g.getChild(1);
        assertKeys(new int[]{3},c);
        assertEquals(a,c.getChild(0));
        assertEquals(d,c.getChild(1));
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2}, a);
        assertKeys(new int[]{3,4},d);
        assertValues(new Object[]{v3,v4}, d);
        assertKeys(new int[]{7},f);
        Leaf e = (Leaf)f.getChild(0);
        assertEquals(b,f.getChild(1));
        assertKeys(new int[]{5,6},e);
        assertValues(new Object[]{v5,v6}, e);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);

        // verify iterator
        assertSameIterator(new IAbstractNode[] { a, d, c, e, b, f, g }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { a, d, c, e, f, g }, btree.root
                .postOrderNodeIterator(true));
        
        /*
         * write out a subtree and revalidate the iterators.
         */
        btree.writeNodeRecursive(c);
        assertSameIterator(new IAbstractNode[] { a, d, c, e, b, f, g }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { e, f, g }, btree.root
                .postOrderNodeIterator(true));
        
        /*
         * write out a leaf and revalidate the iterators.
         */
        btree.writeNodeRecursive(e);
        assertSameIterator(new IAbstractNode[] { a, d, c, e, b, f, g }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { f, g }, btree.root
                .postOrderNodeIterator(true));

        /*
         * write out the entire tree and revalidate the iterators.
         */
        btree.writeNodeRecursive(g);
        assertSameIterator(new IAbstractNode[] { a, d, c, e, b, f, g }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] {}, btree.root
                .postOrderNodeIterator(true));

        /*
         * remove a key (4) from (d) forcing (d,a) to merge into (d) and (a) to
         * be deleted. this causes (c,f) to merge as well, which in turn forces
         * the root to be replaced by (c).
         * 
         * the following are cloned: d, c, g.
         */
        assertEquals(v4,btree.remove(TestKeyBuilder.asSortKey(4)));
        assertNotSame(g,btree.root);
        assertNotSame(c,btree.root);
        c = (Node) btree.root;
        assertNotSame(d,c.getChild(0));
        d = (Leaf) c.getChild(0);
//        btree.dump(Level.DEBUG,System.err);
        assertKeys(new int[]{5,7},c);
        assertEquals(d,c.getChild(0));
        assertEquals(e,c.getChild(1));
        assertEquals(b,c.getChild(2));
        assertKeys(new int[]{1,2,3},d);
        assertValues(new Object[]{v1,v2,v3}, d);
        assertKeys(new int[]{5,6},e);
        assertValues(new Object[]{v5,v6}, e);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);
        assertTrue(a.isDeleted());
        assertTrue(f.isDeleted());

        // verify iterator
        assertSameIterator(new IAbstractNode[] { d, e, b, c }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { d, c }, btree.root
                .postOrderNodeIterator(true));

        /*
         * remove a key (7) from a leaf (b) forcing two leaves (b,e) into (b) to
         * join and verify the visitation order.
         */
        assertEquals(v7,btree.remove(TestKeyBuilder.asSortKey(7)));
        btree.dump(Level.DEBUG,System.err);
        assertKeys(new int[]{5},c);
        assertEquals(d,c.getChild(0));
        assertNotSame(b,c.getChild(1));
        b = (Leaf) c.getChild(1);
        assertKeys(new int[]{1,2,3},d);
        assertValues(new Object[]{v1,v2,v3}, d);
        assertKeys(new int[]{5,6,9},b);
        assertValues(new Object[]{v5,v6,v9}, b);
        assertTrue(e.isDeleted());

        // verify iterator
        assertSameIterator(new IAbstractNode[] { d, b, c }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { d, b, c }, btree.root
                .postOrderNodeIterator(true));
        /*
         * write out the root and verify the visitation orders.
         */
        btree.writeNodeRecursive(c);
        assertSameIterator(new IAbstractNode[] { d, b, c }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] {}, btree.root
                .postOrderNodeIterator(true));

        /*
         * remove keys from a leaf (b) forcing the remaining two leaves (b,d) to
         * join into (b). Since there is only one leaf, that leaf now becomes
         * the new root leaf of the tree.
         */
        assertEquals(c,btree.root);
        assertEquals(d,c.getChild(0));
        assertEquals(b,c.getChild(1));
        assertEquals(v3, btree.remove(TestKeyBuilder.asSortKey(3))); // remove from (d)
        assertNotSame(c,btree.root); // c was cloned.
        c = (Node) btree.root;
        assertNotSame(d,c.getChild(0));
        d = (Leaf)c.getChild(0); // d was cloned.
        assertEquals(b,c.getChild(1));
        assertEquals(v5,btree.remove(TestKeyBuilder.asSortKey(5))); // remove from (b)
        assertNotSame(b,c.getChild(1));
        b = (Leaf)c.getChild(1); // b was cloned.
        assertEquals(v6,btree.remove(TestKeyBuilder.asSortKey(6))); // remove from (b)
        assertKeys(new int[]{1,2,9},b);
        assertValues(new Object[]{v1,v2,v9}, b);
        assertTrue(d.isDeleted());
        assertTrue(c.isDeleted());

        // verify iterator
        assertSameIterator(new IAbstractNode[] { b }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] { b }, btree.root
                .postOrderNodeIterator(true));

        /*
         * write out the root and reverify the iterators.
         */
        btree.writeNodeRecursive(b);
        assertSameIterator(new IAbstractNode[] { b }, btree.root
                .postOrderNodeIterator(false));
        assertSameIterator(new IAbstractNode[] {}, btree.root
                .postOrderNodeIterator(true));
        
    }

}
