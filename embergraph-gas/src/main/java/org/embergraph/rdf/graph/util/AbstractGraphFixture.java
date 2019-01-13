/*
Copyright (C) SYSTAP, LLC 2006-2012.  All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.embergraph.rdf.graph.util;

import org.openrdf.sail.SailConnection;

public abstract class AbstractGraphFixture implements IGraphFixture {

  @Override
  public void loadGraph(final String... resources) throws Exception {

    boolean ok = false;
    SailConnection cxn = null;
    try {
      cxn = getSail().getConnection();
      cxn.begin();
      newSailGraphLoader(cxn).loadGraph(null /* fallback */, resources);
      cxn.commit();
      ok = true;
    } finally {
      if (cxn != null) {
        if (!ok) cxn.rollback();
        cxn.close();
      }
    }
  }

  protected SailGraphLoader newSailGraphLoader(SailConnection cxn) {

    return new SailGraphLoader(cxn);
  }
}