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
 * Created on Mar 17, 2012
 */

package org.embergraph.bop.rdf.update;

import org.embergraph.bop.engine.BOpStats;
import org.embergraph.counters.CAT;

/**
 * Extends {@link BOpStats} to report RDF parser specific statistics.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan
 *         Thompson</a>
 * @version $Id$
 */
public class ParserStats extends BOpStats {

    private static final long serialVersionUID = 1L;

    public final CAT toldTriples = new CAT();

    @Override
    public void add(final BOpStats o) {

        super.add(o);

        if (o instanceof ParserStats) {

            final ParserStats t = (ParserStats) o;

            toldTriples.add(t.toldTriples.get());

        }

    }

    @Override
    protected void toString(final StringBuilder sb) {
        sb.append(",toldTriples=" + toldTriples.get());
    }

}
