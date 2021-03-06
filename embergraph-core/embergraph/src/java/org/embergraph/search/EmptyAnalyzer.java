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
 * Created on May 6, 2014 by Jeremy J. Carroll, Syapse Inc.
 */
package org.embergraph.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;

/*
 * An analyzer that always returns an {@link EmptyTokenStream}, this can be used with {@link
 * ConfigurableAnalyzerFactory} to switch off indexing and searching for specific language tags.
 *
 * @author jeremycarroll
 */
public class EmptyAnalyzer extends Analyzer {

  @Override
  protected TokenStreamComponents createComponents(String input) {
    Tokenizer source =
        new Tokenizer() {
          @Override
          public boolean incrementToken() {
            return false;
          }
        };
    return new TokenStreamComponents(source);
  }
}
