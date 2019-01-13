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
 * Created on Jan 27, 2009
 */

package org.embergraph.resources;

import java.util.Properties;
import org.embergraph.service.AbstractFederation;
import org.embergraph.service.AbstractTransactionService;

/*
 * Mock implementation supporting only those features required to bootstrap the resource manager
 * test suites.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
class MockTransactionService extends AbstractTransactionService {

  /** @param properties */
  public MockTransactionService(Properties properties) {

    super(properties);
  }

  @Override
  protected void abortImpl(TxState state) {
    // TODO Auto-generated method stub
  }

  @Override
  protected long commitImpl(TxState state) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  protected long findCommitTime(long timestamp) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  protected long findNextCommitTime(long commitTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long getLastCommitTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public AbstractFederation getFederation() {
    // TODO Auto-generated method stub
    return null;
  }

  //    public boolean committed(long tx, UUID dataService) throws IOException,
  //            InterruptedException, BrokenBarrierException {
  //        // TODO Auto-generated method stub
  //        return false;
  //    }
  //
  //    public long prepared(long tx, UUID dataService) throws IOException,
  //            InterruptedException, BrokenBarrierException {
  //        // TODO Auto-generated method stub
  //        return 0;
  //    }

}
