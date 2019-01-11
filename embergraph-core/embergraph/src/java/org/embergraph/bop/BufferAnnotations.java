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
 * Created on Sep 24, 2010
 */

package org.embergraph.bop;

import java.util.concurrent.TimeUnit;

import org.embergraph.relation.accesspath.BlockingBuffer;
import org.embergraph.relation.accesspath.IBuffer;

/**
 * Annotations for {@link BlockingBuffer} as used by various kinds of operators.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface BufferAnnotations {

    /**
     * The maximum #of chunks that can be buffered before an the producer would
     * block (default {@value #DEFAULT_CHUNK_OF_CHUNKS_CAPACITY}). Note that
     * partial chunks may be combined into full chunks whose nominal capacity is
     * specified by {@link #CHUNK_CAPACITY}.
     */
    String CHUNK_OF_CHUNKS_CAPACITY = BlockingBuffer.class.getName()
            + ".chunkOfChunksCapacity";

    /**
     * Default for {@link #CHUNK_OF_CHUNKS_CAPACITY}
     * 
     * @todo Try smaller capacities in benchmarks
     */
    int DEFAULT_CHUNK_OF_CHUNKS_CAPACITY = 5;//5;//trunk=1000

    /**
     * Sets the capacity of the {@link IBindingSet}[]s used to accumulate a
     * chunk of {@link IBindingSet}s (default {@value #DEFAULT_CHUNK_CAPACITY}).
     * Partial chunks may be automatically combined into full chunks.
     * 
     * @see #CHUNK_OF_CHUNKS_CAPACITY
     */
    String CHUNK_CAPACITY = IBuffer.class.getName() + ".chunkCapacity";

    /**
     * Default for {@link #CHUNK_CAPACITY}
     */
    int DEFAULT_CHUNK_CAPACITY = 100;//trunk=100

    /**
     * The timeout in milliseconds that the {@link BlockingBuffer} will wait for
     * another chunk to combine with the current chunk before returning the
     * current chunk (default {@value #DEFAULT_CHUNK_TIMEOUT}). This may be ZERO
     * (0) to disable the chunk combiner.
     */
    String CHUNK_TIMEOUT = BlockingBuffer.class.getName() + ".chunkTimeout";

    /**
     * The default for {@link #CHUNK_TIMEOUT}.
     * 
     * @todo this is probably much larger than we want. Try 10ms.
     */
    int DEFAULT_CHUNK_TIMEOUT = 10;//trunk=1000

    /**
     * The {@link TimeUnit}s in which the {@link #CHUNK_TIMEOUT} is measured.
     */
    TimeUnit chunkTimeoutUnit = TimeUnit.MILLISECONDS;

}
