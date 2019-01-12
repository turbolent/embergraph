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
package org.embergraph.bop.controller;

import org.embergraph.bop.join.SolutionSetHashJoinOp;

/**
 * Marker interface for named subquery evaluation. Solutions from the pipeline flow through this
 * operator without modification. The subquery is evaluated exactly once, the first time this
 * operator is invoked, and the solutions for the subquery are written onto a hash index. Those
 * solutions are then joined back within the query at latter points in the query plan using a
 * solution set hash join.
 *
 * @see SolutionSetHashJoinOp
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface INamedSubqueryOp {}
