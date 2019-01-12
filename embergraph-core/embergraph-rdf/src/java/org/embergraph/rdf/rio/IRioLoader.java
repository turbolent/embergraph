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
 * Created on Jan 27, 2007
 */

package org.embergraph.rdf.rio;

import java.io.InputStream;
import java.io.Reader;
import org.openrdf.rio.RDFFormat;

/*
 * Interface for parsing RDF data using the Sesame RIO parser.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IRioLoader {

  long getStatementsAdded();

  long getInsertTime();

  long getInsertRate();

  /*
   * Register a listener.
   *
   * @param l The listener.
   */
  void addRioLoaderListener(RioLoaderListener l);

  /*
   * Remove a listener.
   *
   * @param l The listener.
   */
  void removeRioLoaderListener(RioLoaderListener l);

  /*
   * Parse RDF data.
   *
   * @param reader The source from which the data will be read.
   * @param baseURL The base URL for those data.
   * @param rdfFormat The interchange format.
   * @param defaultGraph The default graph.
   * @param options Options to be applied to the {@link RDFParser}.
   * @throws Exception
   */
  void loadRdf(
      Reader reader,
      String baseURL,
      RDFFormat rdfFormat,
      String defaultGraph,
      RDFParserOptions options)
      throws Exception;

  /*
   * Parse RDF data.
   *
   * @param is The source from which the data will be read.
   * @param baseURL The base URL for those data.
   * @param rdfFormat The interchange format.
   * @param defaultGraph The default graph.
   * @param options Options to be applied to the {@link RDFParser}.
   * @throws Exception
   */
  void loadRdf(
      InputStream is,
      String baseURI,
      RDFFormat rdfFormat,
      String defaultGraph,
      RDFParserOptions options)
      throws Exception;
}
