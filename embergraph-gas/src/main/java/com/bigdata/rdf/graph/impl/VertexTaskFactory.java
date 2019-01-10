/**
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
package com.bigdata.rdf.graph.impl;

import java.util.concurrent.Callable;

import org.openrdf.model.Value;

/**
 * A factory for tasks that are applied to each vertex in the frontier.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan
 *         Thompson</a>
 */
public interface VertexTaskFactory<T> {

    /**
     * Return a new task that will evaluate the vertex.
     * 
     * @param u
     *            The vertex to be evaluated.
     * 
     * @return The task.
     */
    Callable<T> newVertexTask(Value u);

}
