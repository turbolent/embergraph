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
 * Created on Sep 16, 2009
 */

package org.embergraph.rdf.sail;

import org.apache.log4j.Logger;

/**
 * Unit tests for named graphs. Specify <code>
 * -DtestClass=org.embergraph.rdf.sail.TestEmbergraphSailWithQuads</code> to run this test suite.
 *
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class QuadsTestCase extends ProxyEmbergraphSailTestCase {

  protected static final Logger log = Logger.getLogger(QuadsTestCase.class);

  /** */
  public QuadsTestCase() {}

  /** @param arg0 */
  public QuadsTestCase(String arg0) {
    super(arg0);
  }
}
