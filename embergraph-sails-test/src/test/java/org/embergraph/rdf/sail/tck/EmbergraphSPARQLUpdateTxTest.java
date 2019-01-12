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
 * Created on Mar 18, 2012
 */

package org.embergraph.rdf.sail.tck;

import java.util.Properties;
import org.embergraph.rdf.sail.EmbergraphSail.Options;

/**
 * A variant of the test suite using full read/write transactions.
 *
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/531" > SPARQL UPDATE for NAMED
 *     SOLUTION SETS </a>
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class EmbergraphSPARQLUpdateTxTest extends EmbergraphSPARQLUpdateTest {

  /** */
  public EmbergraphSPARQLUpdateTxTest() {}

  @Override
  protected Properties getProperties() {

    final Properties props = super.getProperties();

    props.setProperty(Options.ISOLATABLE_INDICES, "true");

    return props;
  }
}
