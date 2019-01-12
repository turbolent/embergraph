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
package org.embergraph.bop.solutions;

import java.util.concurrent.FutureTask;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;

public class MockQuery extends PipelineOp {

  private static final long serialVersionUID = 1L;

  public MockQuery() {
    super(new BOp[] {}, NV.asMap(new NV[] {}));
  }

  @Override
  public FutureTask<Void> eval(BOpContext<IBindingSet> context) {
    return null;
  }
}
