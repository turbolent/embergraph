/*

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
/*
 * Created on Aug 18, 2008
 */

package com.bigdata.bop.joinGraph;

import java.io.Serializable;

import com.bigdata.relation.rule.IRule;
import com.bigdata.relation.rule.eval.IJoinNexus;

/**
 * A factory for evaluation plans.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IEvaluationPlanFactory extends Serializable {

    /**
     * Return a plan for the rule.
     * 
     * @param joinNexus
     *            The join nexus.
     * @param rule
     *            The rule.
     *            
     * @return The evaluation plan.
     */
    public IEvaluationPlan newPlan(IJoinNexus joinNexus, IRule rule);
    
}
