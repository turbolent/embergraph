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
 * Created on Aug 7, 2008
 */

package org.embergraph.striterator;

import java.util.Arrays;

import org.embergraph.striterator.ChunkedStriterator;
import org.embergraph.striterator.ChunkedWrappedIterator;
import org.embergraph.striterator.IChunkedIterator;
import org.embergraph.striterator.MergeFilter;

import junit.framework.TestCase2;

/**
 * Unit tests for {@link MergeFilter}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestMergeFilter extends TestCase2 {

    /**
     * 
     */
    public TestMergeFilter() {
     
    }

    /**
     * @param arg0
     */
    public TestMergeFilter(String arg0) {
        super(arg0);
     
    }

    public void test_filter() {

        final IChunkedIterator<Long> actual = (IChunkedIterator<Long>) new ChunkedStriterator<IChunkedIterator<Long>, Long>(
                Arrays.asList(new Long[] { 1L, 3L, 5L }).iterator())
                .addFilter(new MergeFilter<IChunkedIterator<Long>, Long>(
                        new ChunkedWrappedIterator<Long>(Arrays.asList(
                                new Long[] { 2L, 3L, 4L }).iterator())));

        assertEquals(new Long[] { 1L, 2L, 3L, 4L, 5L }, actual.nextChunk());

    }

}
