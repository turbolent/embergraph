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
 * Created on Sep 8, 2011
 */

package org.embergraph.rdf.sparql.ast.service;

import java.util.Set;
import org.embergraph.bop.IVariable;
import org.embergraph.rdf.sparql.ast.IVariableBindingRequirements;
import org.embergraph.rdf.sparql.ast.StaticAnalysis;

/** Factory for creating objects which can talk to SPARQL service end points. */
public interface ServiceFactory {

  /**
   * Return options for the service end point. Depending on the implementation, these options MAY be
   * configurable.
   */
  IServiceOptions getServiceOptions();

  /**
   * Create a service invocation object.
   *
   * @param params The pararameters, which are encapsulated by this interface.
   * @return The object which can be used to evaluate the SERVICE clause.
   */
  ServiceCall<?> create(ServiceCallCreateParams params);

  /**
   * Returns, for the given service node, the variables that must be bound prior to start executing
   * the service. This information is important to reason about the position where to place the
   * service within the execution proccess.
   *
   * <p>See {@link IVariableBindingRequirements#getRequiredBound(StaticAnalysis)}.
   */
  Set<IVariable<?>> getRequiredBound(final ServiceNode serviceNode);

  /**
   * Returns, for the given service node, the variables that are desired to be bound prior to start
   * executing the service. This information is important to reason about the position where to
   * place the service within the execution proccess.
   *
   * <p>See {@link IVariableBindingRequirements#getDesiredBound(StaticAnalysis)}.
   */
  Set<IVariable<?>> getDesiredBound(final ServiceNode serviceNode);
}
