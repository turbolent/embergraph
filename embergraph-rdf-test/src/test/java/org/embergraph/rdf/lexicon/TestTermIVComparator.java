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
 * Created on Jan 29, 2007
 */

package org.embergraph.rdf.lexicon;

import java.util.Arrays;
import java.util.Comparator;

import junit.framework.TestCase2;

import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.rdf.internal.TermIVComparator;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.util.BytesUtil;
import org.embergraph.util.BytesUtil.UnsignedByteArrayComparator;

/**
 * Test suite for {@link TermIVComparator}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestTermIVComparator extends TestCase2 {

    /**
     * 
     */
    public TestTermIVComparator() {
    }

    /**
     * @param name
     */
    public TestTermIVComparator(String name) {
        super(name);
    }

    public void test_termIdComparator() {

        final TermId<?> lmin = new TermId<EmbergraphURI>(VTE.URI, Long.MIN_VALUE);
        final TermId<?> lm1 = new TermId<EmbergraphURI>(VTE.URI, -1L);
        final TermId<?> l0 = new TermId<EmbergraphURI>(VTE.URI, 0L);
        final TermId<?> lp1 = new TermId<EmbergraphURI>(VTE.URI, 1L);
        final TermId<?> lmax = new TermId<EmbergraphURI>(VTE.URI, Long.MAX_VALUE);

        final EmbergraphValueFactory f = EmbergraphValueFactoryImpl
                .getInstance(getName()/*namespace*/);

        final EmbergraphValue vmin = f.createLiteral("a"); vmin.setIV( lmin);
        final EmbergraphValue vm1  = f.createLiteral("b"); vm1 .setIV( lm1 );
        final EmbergraphValue v0   = f.createLiteral("c"); v0  .setIV( l0 );
        final EmbergraphValue vp1  = f.createLiteral("d"); vp1 .setIV( lp1 );
        final EmbergraphValue vmax = f.createLiteral("e"); vmax.setIV( lmax);

        // IVs out of order.
        final TermId<?>[] actualIds = new TermId[] { lm1, lmax, l0, lp1, lmin };
        // IVs in order.
        final TermId<?>[] expectedIds = new TermId[] { lmin, lm1, l0, lp1, lmax };
        
        // values out of order.
        final EmbergraphValue[] terms = new EmbergraphValue[] { vmax, vm1, vmin, v0, vp1 };

        /*
         * Test conversion of longs to unsigned byte[]s using the KeyBuilder and
         * verify the order on those unsigned byte[]s.
         */
        {
            final byte[][] keys = new byte[actualIds.length][];
            final KeyBuilder keyBuilder = new KeyBuilder(8);
            for (int i = 0; i < actualIds.length; i++) {
                keys[i] = keyBuilder.reset().append(actualIds[i].getTermId())
                        .getKey();
            }
            Arrays.sort(keys, UnsignedByteArrayComparator.INSTANCE);
            for (int i = 0; i < actualIds.length; i++) {
                if (log.isInfoEnabled())
                    log.info(BytesUtil.toString(keys[i]));
                /*
                 * Decode and verify sorted into the expected order.
                 */
                assertEquals(expectedIds[i].getTermId(), KeyBuilder.decodeLong(
                        keys[i], 0));
            }
        }

        /*
         * Test unsigned long integer comparator.
         */

        if (log.isInfoEnabled())
            log.info("unsorted ids  : " + Arrays.toString(actualIds));
        Arrays.sort(actualIds);
        if (log.isInfoEnabled()) {
            log.info("sorted ids    : " + Arrays.toString(actualIds));
            log.info("expected ids  : " + Arrays.toString(expectedIds));
        }
        assertEquals("ids order", expectedIds, actualIds);

        /*
         * Test the term identifier comparator.
         */

        final Comparator<EmbergraphValue> c = TermIVComparator.INSTANCE;

        if (log.isInfoEnabled())
            log.info("unsorted terms: " + Arrays.toString(terms));
        Arrays.sort(terms, c);
        if (log.isInfoEnabled())
            log.info("sorted terms  : " + Arrays.toString(terms));

        assertTrue("kmin<km1", c.compare(vmin, vm1) < 0);
        assertTrue("km1<k0", c.compare(vm1, v0) < 0);
        assertTrue("k0<kp1", c.compare(v0, vp1) < 0);
        assertTrue("kp1<kmax", c.compare(vp1, vmax) < 0);
        assertTrue("kmin<kmax", c.compare(vmin, vmax) < 0);

    }

}
