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

import org.embergraph.journal.AbstractJournal;
import org.embergraph.journal.IJournal;
import org.embergraph.journal.Journal;

/**
 * Metadata required to locate a {@link Journal} resource.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class JournalMetadata extends AbstractResourceMetadata {

    /**
     * 
     */
    private static final long serialVersionUID = 3783897093328558238L;

    public final boolean isIndexSegment() {
        
        return false;
        
    }
    
    public final boolean isJournal() {
        
        return true;
        
    }

    /**
     * Return the file whose contents are the persistent state for the 
     * journal.
     * 
     * @param journal
     *            The journal.
     *
     * @return The file.
     */
    private static String getFileString(final IJournal journal) {
    
        final File file = journal.getFile();

        if (file == null)
            return "";
        
        return file.getName();//toString();
                
    }
    
    /**
     * De-serialization constructor.
     */
    public JournalMetadata() {
        
    }

    /**
     * The description of a journal. The {@link JournalMetadata} state will not
     * change as writes are made on the journal since it does not reflect
     * anything exception the {@link UUID}, the filename, and the create time.
     * 
     * @param journal
     *            The journal.
     */
    public JournalMetadata(final AbstractJournal journal) {

        this(getFileString(journal), //journal.getBufferStrategy().getExtent(),
                journal.getRootBlockView().getUUID(), //
                journal.getRootBlockView().getCreateTime(), // createTime.
                0L // commitTime
                );

    }

    /**
     * Constructor variant used to indicate a read from a specific commitTime on
     * a journal.
     * 
     * @param journal
     *            The journal.
     * @param commitTime
     *            The commitTime.
     */
    public JournalMetadata(final AbstractJournal journal, final long commitTime) {
     
        this(getFileString(journal), //journal.getBufferStrategy().getExtent(),
                journal.getRootBlockView().getUUID(),//
                journal.getRootBlockView().getCreateTime(),//
                commitTime
                );

    }

    /**
     * Data only constructor used by some unit tests.
     * 
     * @param file
     * @param uuid
     * @param createTime
     */
    public JournalMetadata(final File file, /* long nbytes, */final UUID uuid,
            final long createTime, final long commitTime) {

        this(file.getName()/* ,nbytes */, uuid, createTime, commitTime);

    }

    /**
     * Package private constructor used by the other constructors and when
     * deserializing records.
     * 
     * @param file
     * @param uuid
     * @param createTime
     */
    JournalMetadata(final String file, /* long nbytes, */final UUID uuid,
            final long createTime, final long commitTime) {

        super(file, /*nbytes, */ uuid, createTime, commitTime);

    }

}
