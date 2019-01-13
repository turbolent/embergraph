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
package org.embergraph.rdf.graph.impl.ram;

import org.embergraph.rdf.graph.IGASEngine;
import org.embergraph.rdf.graph.IGraphAccessor;
import org.embergraph.rdf.graph.impl.ram.RAMGASEngine.RAMGraph;
import org.embergraph.rdf.graph.util.AbstractGraphFixture;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;

public class RAMGraphFixture extends AbstractGraphFixture {

  private RAMGraph g;

  public RAMGraphFixture() {
    g = new RAMGraph();
  }

  public Sail getSail() {
    throw new UnsupportedOperationException();
  }

  /** Return the {@link RAMGraphFixture}. */
  public RAMGraph getGraph() {
    return g;
  }

  @Override
  public void destroy() {
    g = null;
  }

  @Override
  public IGASEngine newGASEngine(int nthreads) {

    return new RAMGASEngine(nthreads);
  }

  @Override
  public IGraphAccessor newGraphAccessor(SailConnection cxnIsIgnored) {

    //        return new RAMGraphAccessor(cxnIsIgnored);
    throw new UnsupportedOperationException();
  }
}
