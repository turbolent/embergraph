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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KeyBuilder;

/*
 * This is the default implementation but should be regarded as legacy since it fails to use the
 * correct {@link Analyzer} for almost all languages (other than English). It uses the correct
 * natural language analyzer only for literals tagged with certain three letter ISO 639 codes:
 * "por", "deu", "ger", "zho", "chi", "jpn", "kor", "ces", "cze", "dut", "nld", "gre", "ell", "fra",
 * "fre", "rus" and "tha". All other tags are treated as English. These codes do not work if they
 * are used with subtagse.g. "ger-AT" is treated as English. No two letter code, other than "en"
 * works correctly: note that the W3C and IETF recommend the use of the two letter forms instead of
 * the three letter forms.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @deprecated Using {@link ConfigurableAnalyzerFactory} with the {@link
 *     ConfigurableAnalyzerFactory.Options#NATURAL_LANGUAGE_SUPPORT} uses the appropriate natural
 *     language analyzers for the two letter codes and for tags which include sub-tags.
 * @version $Id$
 */
public class DefaultAnalyzerFactory implements IAnalyzerFactory {

  private final FullTextIndex fullTextIndex;

  public DefaultAnalyzerFactory(final FullTextIndex fullTextIndex) {

    if (fullTextIndex == null) throw new IllegalArgumentException();

    this.fullTextIndex = fullTextIndex;
  }

  public Analyzer getAnalyzer(final String languageCode, final boolean filterStopwords) {

    final IKeyBuilder keyBuilder = fullTextIndex.getKeyBuilder();

    Map<String, AnalyzerConstructor> map = getAnalyzers();

    AnalyzerConstructor ctor = null;

    if (languageCode == null) {

      if (keyBuilder.isUnicodeSupported()) {

        // The configured local for the database.
        final Locale locale = ((KeyBuilder) keyBuilder).getSortKeyGenerator().getLocale();

        // The analyzer for that locale.
        Analyzer a = getAnalyzer(locale.getLanguage(), filterStopwords);

        if (a != null) return a;
      }

      // fall through

    } else {

      /*
       * Check the declared analyzers. We first check the three letter
       * language code. If we do not have a match there then we check the
       * 2 letter language code.
       */

      String code = languageCode;

      if (code.length() > 3) {

        code = code.substring(0, 2);

        ctor = map.get(languageCode);
      }

      if (ctor == null && code.length() > 2) {

        code = code.substring(0, 1);

        ctor = map.get(languageCode);
      }
    }

    if (ctor == null) {

      // request the default analyzer.

      ctor = map.get("");

      if (ctor == null) {

        throw new IllegalStateException("No entry for empty string?");
      }
    }

    Analyzer a = ctor.newInstance(filterStopwords);

    return a;
  }

  private abstract static class AnalyzerConstructor {

    public abstract Analyzer newInstance(final boolean filterStopwords);
  }

  /*
   * A map containing instances of the various kinds of analyzers that we know about.
   *
   * <p>Note: There MUST be an entry under the empty string (""). This entry will be requested when
   * there is no entry for the specified language code.
   */
  private Map<String, AnalyzerConstructor> analyzers;

  /*
   * Initializes the various kinds of analyzers that we know about.
   *
   * <p>Note: Each {@link Analyzer} is registered under both the 3 letter and the 2 letter language
   * codes. See <a href="http://www.loc.gov/standards/iso639-2/php/code_list.php">ISO 639-2</a>.
   *
   * @todo get some informed advice on which {@link Analyzer}s map onto which language codes.
   * @todo thread safety? Analyzers produce token processors so maybe there is no problem here once
   *     things are initialized. If so, maybe this could be static.
   * @todo configuration. Could be configured by a file containing a class name and a list of codes
   *     that are handled by that class.
   * @todo strip language code down to 2/3 characters during lookup.
   * @todo There are a lot of pidgins based on frenchenglish, and other languages that are not being
   *     assigned here.
   */
  private synchronized Map<String, AnalyzerConstructor> getAnalyzers() {

    if (analyzers != null) {

      return analyzers;
    }

    analyzers = new HashMap<String, AnalyzerConstructor>();

    final CharArraySet emptyStopwords = CharArraySet.EMPTY_SET;

    {
      AnalyzerConstructor a =
          new AnalyzerConstructor() {
            public Analyzer newInstance(final boolean filterStopwords) {
              return filterStopwords
                  ? new BrazilianAnalyzer()
                  : new BrazilianAnalyzer(emptyStopwords);
            }
          };
      analyzers.put("por", a);
      analyzers.put("pt", a);
    }

    /*
     * Claims to handle Chinese. Does single character extraction. Claims to
     * produce smaller indices as a result.
     *
     * Note: you can not tokenize with the Chinese analyzer and the do
     * search using the CJK analyzer and visa versa.
     *
     * Note: I have no idea whether this would work for Japanese and Korean
     * as well. I expect so, but no real clue.
     */
    {
      AnalyzerConstructor a =
          new AnalyzerConstructor() {
            public Analyzer newInstance(final boolean filterStopwords) {
              return filterStopwords
                  ? new SmartChineseAnalyzer()
                  : new SmartChineseAnalyzer(emptyStopwords);
            }
          };
      analyzers.put("zho", a);
      analyzers.put("chi", a);
      analyzers.put("zh", a);
    }

    /*
     * Claims to handle Chinese, Japanese, Korean. Does double character
     * extraction with overlap.
     */
    {
      AnalyzerConstructor a =
          new AnalyzerConstructor() {
            public Analyzer newInstance(final boolean filterStopwords) {
              return filterStopwords ? new CJKAnalyzer() : new CJKAnalyzer(emptyStopwords);
            }
          };
      //            analyzers.put("zho", a);
      //            analyzers.put("chi", a);
      //            analyzers.put("zh", a);
      analyzers.put("jpn", a);
      analyzers.put("ja", a);
      analyzers.put("jpn", a);
      analyzers.put("kor", a);
      analyzers.put("ko", a);
    }

    {
      AnalyzerConstructor a =
          new AnalyzerConstructor() {
            public Analyzer newInstance(final boolean filterStopwords) {
              return filterStopwords ? new CzechAnalyzer() : new CzechAnalyzer(emptyStopwords);
            }
          };
      analyzers.put("ces", a);
      analyzers.put("cze", a);
      analyzers.put("cs", a);
    }

    {
      AnalyzerConstructor a =
          new AnalyzerConstructor() {
            public Analyzer newInstance(final boolean filterStopwords) {
              return filterStopwords ? new DutchAnalyzer() : new DutchAnalyzer(emptyStopwords);
            }
          };
      analyzers.put("dut", a);
      analyzers.put("nld", a);
      analyzers.put("nl", a);
    }

    {
      AnalyzerConstructor a =
          new AnalyzerConstructor() {
            public Analyzer newInstance(final boolean filterStopwords) {
              return filterStopwords ? new FrenchAnalyzer() : new FrenchAnalyzer(emptyStopwords);
            }
          };
      analyzers.put("fra", a);
      analyzers.put("fre", a);
      analyzers.put("fr", a);
    }

    /*
     * Note: There are a lot of language codes for German variants that
     * might be useful here.
     */
    {
      AnalyzerConstructor a =
          new AnalyzerConstructor() {
            public Analyzer newInstance(final boolean filterStopwords) {
              return filterStopwords ? new GermanAnalyzer() : new GermanAnalyzer(emptyStopwords);
            }
          };
      analyzers.put("deu", a);
      analyzers.put("ger", a);
      analyzers.put("de", a);
    }

    // Note: ancient greek has a different code (grc).
    {
      AnalyzerConstructor a =
          new AnalyzerConstructor() {
            public Analyzer newInstance(final boolean filterStopwords) {
              return filterStopwords ? new GreekAnalyzer() : new GreekAnalyzer(emptyStopwords);
            }
          };
      analyzers.put("gre", a);
      analyzers.put("ell", a);
      analyzers.put("el", a);
    }

    // @todo what about other Cyrillic scripts?
    {
      AnalyzerConstructor a =
          new AnalyzerConstructor() {
            public Analyzer newInstance(final boolean filterStopwords) {
              return filterStopwords ? new RussianAnalyzer() : new RussianAnalyzer(emptyStopwords);
            }
          };
      analyzers.put("rus", a);
      analyzers.put("ru", a);
    }

    {
      AnalyzerConstructor a =
          new AnalyzerConstructor() {
            public Analyzer newInstance(final boolean filterStopwords) {
              return new ThaiAnalyzer();
            }
          };
      analyzers.put("tha", a);
      analyzers.put("th", a);
    }

    // English
    {
      AnalyzerConstructor a =
          new AnalyzerConstructor() {
            public Analyzer newInstance(final boolean filterStopwords) {
              return filterStopwords
                  ? new StandardAnalyzer()
                  : new StandardAnalyzer(emptyStopwords);
            }
          };
      analyzers.put("eng", a);
      analyzers.put("en", a);
      /*
       * Note: There MUST be an entry under the empty string (""). This
       * entry will be requested when there is no entry for the specified
       * language code.
       */
      analyzers.put("", a);
    }

    return analyzers;
  }
}
