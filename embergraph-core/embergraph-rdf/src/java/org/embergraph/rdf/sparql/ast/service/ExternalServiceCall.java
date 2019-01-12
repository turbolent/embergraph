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

import org.embergraph.rdf.internal.IV;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;

/**
 * Service invocation interface for an external (non-embergraph, but same JVM) service. Data
 * interchange with the external service uses the Sesame {@link BindingSet} and Sesame {@link Value}
 * objects. Embergraph {@link IV}s will be materialized as Sesame {@link Value}s for the
 * BindingsClause. Sesame {@link Value}s drained from the service will be resolved against the
 * database, turning them into {@link IV}s.
 *
 * @see ServiceRegistry
 * @see ServiceFactory
 */
public interface ExternalServiceCall extends ServiceCall<BindingSet> {}
