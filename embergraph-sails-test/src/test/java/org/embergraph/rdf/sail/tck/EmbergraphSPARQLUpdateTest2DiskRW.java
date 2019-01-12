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

import java.io.File;
import java.util.Properties;
import org.embergraph.journal.BufferMode;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSail.Options;

/*
 * A variant of the test suite using {@link BufferMode#DiskRW}.
 *
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/531">SPARQL UPDATE Extensions
 *     (Trac) </a>
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/555" > Support
 *     PSOutputStream/InputStream at IRawStore </a>
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: EmbergraphSPARQLUpdateTxTest2.java 7168 2013-05-28 21:30:38Z thompsonbry $
 */
public class EmbergraphSPARQLUpdateTest2DiskRW extends EmbergraphSPARQLUpdateTest2 {

  /** */
  public EmbergraphSPARQLUpdateTest2DiskRW() {}

  @Override
  public Properties getProperties() {

    final Properties props = new Properties(super.getProperties());

    final File journal = EmbergraphStoreTest.createTempFile();

    props.setProperty(EmbergraphSail.Options.FILE, journal.getAbsolutePath());

    props.setProperty(Options.BUFFER_MODE, BufferMode.DiskRW.toString());
    //        props.setProperty(Options.BUFFER_MODE, BufferMode.DiskWORM.toString());

    return props;
  }
}
