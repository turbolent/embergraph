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
 * Created on Feb 20, 2008
 */
package org.embergraph.btree;

/**
 * An object that delegates the {@link IIndex} and {@link ILinearList}
 * interfaces.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class DelegateBTree extends DelegateIndex implements ILinearList {

    private final ILinearList delegate;

    public DelegateBTree(final ILinearList ndx) {

        super((IIndex) ndx);

        this.delegate = ndx;

    }

    public DelegateBTree(final BTree btree) {

        super(btree);

        this.delegate = btree;

    }

    @Override
    public long indexOf(final byte[] key) {

        return delegate.indexOf(key);
        
    }

    @Override
    public byte[] keyAt(final long index) {
        
        return delegate.keyAt(index);
        
    }

    @Override
    public byte[] valueAt(final long index) {

        return delegate.valueAt(index);
        
    }

}
