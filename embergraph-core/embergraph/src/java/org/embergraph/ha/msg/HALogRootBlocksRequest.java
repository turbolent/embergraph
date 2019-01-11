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
package org.embergraph.ha.msg;

public class HALogRootBlocksRequest implements IHALogRootBlocksRequest {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private final long commitCounter;
    
    /**
     * 
     * @param commitCounter
     *            The commit counter used to identify the desired commit point
     *            (the commit counter of the closing root block).
     */
    public HALogRootBlocksRequest(final long commitCounter) {

        this.commitCounter = commitCounter;
        
    }
    
    @Override
    public long getCommitCounter() {

        return commitCounter;
        
    }

    public String toString() {

        return getClass() + "{commitCounter=" + getCommitCounter() + "}";
        
    }

}
