/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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
package com.bigdata.rdf.sparql.ast.eval;

/**
 * Annotations for communication with the GPU acceleration module. This should only
 * include those annotations that need to be declared in the embergraph core in order
 * to support the GPU acceleration module.
 * 
 * @author bryan
 */
public interface GpuAnnotations {
	
	/**
	 * When true, attempt to evaluate on the mapgraph runtime.
	 * 
	 * @see #DEFAULT_EVALUATE_ON_GPU
	 */
	String EVALUATE_ON_GPU = "evaluateOnGPU";

	/**
	 * Default is false.
	 * 
	 * @see #EVALUATE_ON_GPU
	 */
	boolean DEFAULT_EVALUATE_ON_GPU = false;
}
