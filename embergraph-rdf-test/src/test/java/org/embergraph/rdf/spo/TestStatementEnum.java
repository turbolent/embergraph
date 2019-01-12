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
 * Created on Oct 22, 2007
 */

package org.embergraph.rdf.spo;

import junit.framework.TestCase;
import org.embergraph.rdf.model.StatementEnum;

/**
 * Test suite for {@link StatementEnum}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestStatementEnum extends TestCase {

  /** */
  public TestStatementEnum() {}

  /** @param arg0 */
  public TestStatementEnum(String arg0) {
    super(arg0);
  }

  public void test_max() {

    assertEquals(
        StatementEnum.Explicit, StatementEnum.max(StatementEnum.Axiom, StatementEnum.Explicit));

    assertEquals(
        StatementEnum.Explicit, StatementEnum.max(StatementEnum.Inferred, StatementEnum.Explicit));

    assertEquals(
        StatementEnum.Axiom, StatementEnum.max(StatementEnum.Inferred, StatementEnum.Axiom));

    assertEquals(
        StatementEnum.Inferred, StatementEnum.max(StatementEnum.Inferred, StatementEnum.Inferred));
  }
}
