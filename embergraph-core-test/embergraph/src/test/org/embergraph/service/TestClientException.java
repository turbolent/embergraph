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
 * Created on May 14, 2008
 */

package org.embergraph.service;

import java.util.Arrays;
import junit.framework.TestCase;
import org.embergraph.service.ndx.ClientException;

/*
 * Class written to verify the stack trace printing behavior of {@link ClientException}.
 *
 * <p>Note: You have to inspect the test results by hand.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestClientException extends TestCase {

  /** */
  public TestClientException() {}

  /** @param arg0 */
  public TestClientException(String arg0) {
    super(arg0);
  }

  void ex1() {
    throw new RuntimeException("ex1");
  }

  void ex2() {
    throw new RuntimeException("ex2");
  }

  /*
   * This "test" throws an exception which should print out two "causes". Those causes are
   * exceptions thrown by {@link #ex1()} and {@link #ex2()}.
   */
  public void test01() {

    Throwable t1 = null;
    try {
      ex1();
    } catch (Throwable t) {
      t1 = t;
    }

    Throwable t2 = null;
    try {
      ex2();
    } catch (Throwable t) {
      t2 = t;
    }

    throw new ClientException("test", Arrays.asList(t1, t2));
  }
}
