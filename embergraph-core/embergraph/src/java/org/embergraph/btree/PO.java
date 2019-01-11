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
 * Created on Nov 17, 2006
 */
package org.embergraph.btree;

import org.embergraph.btree.data.IAbstractNodeData;

/**
 * A persistent object.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class PO implements IIdentityAccess, IDirty {

    /**
     * The persistent identity (defined when the object is actually
     * persisted).
     */
    transient protected long identity = NULL;

    /**
     * True iff the object is deleted.
     */
    transient protected boolean deleted = false;

    /**
     * @todo Historically, a mutable node was always non-persistent. With the
     *       introduction of coding of nodes and leaves for transient B+Trees on
     *       eviction from the write retention queue, those nodes and leaves
     *       become read-only when they are coded without becoming persistent.
     *       While {@link IAbstractNodeData#isCoded()} currently reflects the
     *       same distinction as {@link IAbstractNodeData#isReadOnly()}, that
     *       might not always be the case. For example, it is possible to have
     *       mutable coded node/leaf impls.
     *       <p>
     *       There are a number of asserts for this which are violated by the
     *       change to support coding of nodes and leaves for the transient
     *       B+Tree. Often, those asserts need to test
     *       {@link IAbstractNodeData#isReadOnly()} instead, since that reflects
     *       the concern that the node/leaf is not mutable.
     */
    final public boolean isPersistent() {

        return identity != NULL;

    }

    final public boolean isDeleted() {
        
        return deleted;
        
    }
    
    final public long getIdentity() throws IllegalStateException {

        if (identity == NULL) {
            
            throw new IllegalStateException();

        }

        return identity;

    }

    /**
     * Used by the store to set the persistent identity.
     * 
     * Note: This method should not be public.
     * 
     * @param identity
     *            The identity.
     * 
     * @throws IllegalStateException
     *             If the identity is already defined.
     */
    // Note: public since also used by the htree package
    public void setIdentity(final long key) throws IllegalStateException {

        if (key == NULL) {

            throw new IllegalArgumentException();
            
        }

        if (this.identity != NULL) {

            throw new IllegalStateException("Object already persistent");
            
        }
        
        if( this.deleted) {
            
            throw new IllegalStateException("Object is deleted");
            
        }

        this.identity = key;

    }

    /**
     * New objects are considered to be dirty. When an object is
     * deserialized from the store the dirty flag MUST be explicitly
     * cleared.
     */
    transient protected boolean dirty = true;

    final public boolean isDirty() {

        return dirty;

    }

    final public void setDirty(final boolean dirty) {

        this.dirty = dirty;

    }

    /**
     * Extends the basic behavior to display the persistent identity of the
     * object iff the object is persistent and to mark objects that have been
     * deleted.
     */
    public String toString() {

        return toShortString();
        
    }

    /**
     * Returns a short representation of the class, identity (if assigned), the
     * object instance, and whether or not the {@link PO} is deleted.
     */
    public String toShortString() {

        final String s;
        
        if (identity != NULL) {

            s = super.toString() + "#" + identity;

        } else {

            s = super.toString();

        }

        if (deleted) {

            return s + "(deleted)";

        } else {

            return s;

        }

    }

    /**
     * Returns a string that may be used to indent a dump of the nodes in
     * the tree.
     * 
     * @param height
     *            The height.
     *            
     * @return A string suitable for indent at that height.
     */
    protected static String indent(final int height) {

        if( height == -1 ) {
        
            // The height is not defined.
            
            return "";
            
        }
        
        return ws.substring(0, Math.min(ws.length(), height * 4));

    }

    private static final transient String ws = "                                                                                                                                                                                                                                                                                                                                                                                                                                    ";

}
