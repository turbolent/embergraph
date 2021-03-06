package org.embergraph.rdf.sparql.ast.service;

import org.embergraph.bop.IBindingSet;

/*
 * Service invocation interface for an in-process service which knows how to process {@link IV}s.
 *
 * @see ServiceRegistry
 * @see ServiceFactory
 */
public interface EmbergraphServiceCall extends ServiceCall<IBindingSet> {}
