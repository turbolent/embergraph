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
 * Created on Dec 21, 2010
 */

package org.embergraph.search;

import java.util.Locale;
import org.apache.lucene.analysis.Analyzer;

/**
 * Factory interface for obtaining an {@link Analyzer}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IAnalyzerFactory {

  /**
   * Return the token analyzer to be used for the given language code.
   *
   * @param languageCode The language code or <code>null</code> to use the default {@link Locale}.
   * @param filterStopwords if false, return an analyzer with no stopwords
   * @return The token analyzer best suited to the indicated language family.
   */
  Analyzer getAnalyzer(final String languageCode, final boolean filterStopwords);
}
