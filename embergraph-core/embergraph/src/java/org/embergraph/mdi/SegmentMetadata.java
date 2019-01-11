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
package org.embergraph.mdi;

import java.io.File;
import java.util.UUID;

import org.embergraph.btree.IndexSegment;

/**
 * Metadata for a single {@link IndexSegment}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class SegmentMetadata extends AbstractResourceMetadata {
    
    /**
     * 
     */
    private static final long serialVersionUID = -7296761796029541465L;

    public final boolean isIndexSegment() {
        
        return true;
        
    }
    
    public final boolean isJournal() {
        
        return false;
        
    }

    /**
     * De-serialization constructor.
     */
    public SegmentMetadata() {

    }

    public SegmentMetadata(final File file, /* long nbytes, */final UUID uuid,
            final long commitTime) {

        this(file.getName(),/* nbytes, */uuid, commitTime);

    }

    SegmentMetadata(final String filename, /* long nbytes, */final UUID uuid,
            final long commitTime) {

        super(filename, /* nbytes, */uuid, commitTime/* createTime */,
                commitTime/* commitTime */);

    }

}
