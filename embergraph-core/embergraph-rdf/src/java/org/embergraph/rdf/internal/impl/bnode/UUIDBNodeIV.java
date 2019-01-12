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
package org.embergraph.rdf.internal.impl.bnode;

import java.util.UUID;

import org.embergraph.rdf.model.EmbergraphBNode;
import org.openrdf.model.BNode;

import org.embergraph.rdf.internal.DTE;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.util.Bytes;

/**
 * Class for inline RDF blank nodes. Blank nodes MUST be based on UUIDs in
 * order to be lined.
 * <p>
 * {@inheritDoc}
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan
 *         Thompson</a>
 * @version $Id: TestEncodeDecodeKeys.java 2753 2010-05-01 16:36:59Z
 *          thompsonbry $
 * 
 * @see AbstractTripleStore.Options
 */
public class UUIDBNodeIV<V extends EmbergraphBNode> extends
        AbstractBNodeIV<V, UUID> {

    /**
     * 
     */
    private static final long serialVersionUID = -4560216387427028030L;
    
    private final UUID id;
    
    public IV<V, UUID> clone(final boolean clearCache) {

        final UUIDBNodeIV<V> tmp = new UUIDBNodeIV<V>(id);

        if (!clearCache) {

            tmp.setValue(getValueCache());
            
        }
        
        return tmp;

    }
    
    public UUIDBNodeIV(final UUID id) {

        super(DTE.UUID);

        if (id == null)
            throw new IllegalArgumentException();

        this.id = id;

    }

    final public UUID getInlineValue() {
     
        return id;
        
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof UUIDBNodeIV<?>) {
            return this.id.equals(((UUIDBNodeIV<?>) o).id);
        }
        return false;
    }

    @Override
    public int _compareTo(IV o) {
         
        return id.compareTo(((UUIDBNodeIV) o).id);
        
    }
    
    public int hashCode() {
        return id.hashCode();
    }
 
    public int byteLength() {
        return 1 + Bytes.SIZEOF_UUID;
    }
    
    /**
     * Implements {@link BNode#getID()}.
     */
    @Override
    public String getID() {
        
        return 'u' + String.valueOf(id);

    }

    /**
     * Does not need materialization to answer BNode interface methods.
     */
	@Override
	public boolean needsMaterialization() {
		
		return false;
		
	}

    
}
