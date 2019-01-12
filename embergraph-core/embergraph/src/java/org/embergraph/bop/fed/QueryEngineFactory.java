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
 * Created on Sep 30, 2010
 */

package org.embergraph.bop.fed;

import org.apache.log4j.Logger;

import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.rdf.sparql.ast.eval.IExternalAST2BOp;
import org.embergraph.util.ClassPathUtil;

/**
 * Singleton factory for a query controller.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * 
 * @see BLZG-1471 (Convert QueryEngineFactory to use getInstance() and public
 *      interface for accessor methods.)
 */
public class QueryEngineFactory {

	private static final Logger log = Logger.getLogger(QueryEngineFactory.class);

	static final IQueryEngineFactory instance;
	
	static {

		instance = ClassPathUtil.classForName(
				"org.embergraph.rdf.gpu.bop.engine.GpuQueryEngineFactory", // preferredClassName,
				QueryEngineFactoryBase.class, // defaultClass,
				IQueryEngineFactory.class, // sharedInterface,
				QueryEngineFactory.class.getClassLoader() // classLoader
		);

//		instance = new QueryEngineFactoryBase();

		if (log.isInfoEnabled())
			log.info("Factory class is " + instance.getClass().getName());

	}

	public static IQueryEngineFactory getInstance() {

		return instance;

	}

}
