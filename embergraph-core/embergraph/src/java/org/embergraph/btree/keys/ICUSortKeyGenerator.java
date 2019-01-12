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
 * Created on Nov 30, 2007
 */
package org.embergraph.btree.keys;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;
import java.util.Locale;
import org.apache.log4j.Logger;

/**
 * Implementation that uses the ICU library (which supports compressed sort keys).
 *
 * <p>This class uses <a href="http://icu.sourceforge.net">ICU4J</a>. There are several advantages
 * to the ICU libraries: (1) the collation keys are compressed; (2) the libraries are faster than
 * the jdk classes; (3) the libraries support Unicode 5; and (4) the libraries have the same
 * behavior under Java and C/C++ so you can have interoperable code. There is also JNI (Java Native
 * Interface) implementation for many platforms for even greater performance and compatibility.
 *
 * <p>Note: The sort keys generated by the ICU libraries critically depend on the {@link Locale} and
 * MAY be different depending on the software version of the ICU libraries. ICU makes available a
 * version number that is changed each time a software revision would result in a change in the
 * generated sort order. Further note that the sort keys generated by the ICU libraries are NOT
 * compatible with those generated by the JDK {@link java.text.Collator} classes.
 *
 * <p>Note: This is a top-level class in order to avoid runtime dependencies on the ICU libraries
 * when they are not required.
 *
 * @see http://site.icu-project.org/
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
class ICUSortKeyGenerator implements UnicodeSortKeyGenerator {

  private static final Logger log = Logger.getLogger(ICUSortKeyGenerator.class);

  /**
   * Used to encode unicode strings into compact byte[]s that have the same sort order (aka sort
   * keys).
   */
  private final RuleBasedCollator collator;

  /** The {@link Locale} used to configure this object. */
  private final Locale locale;

  /** The {@link Locale} used to configure this object. */
  public Locale getLocale() {

    return locale;
  }

  ICUSortKeyGenerator(final Locale locale, final Object strength, final DecompositionEnum mode) {

    if (locale == null) throw new IllegalArgumentException();

    this.locale = locale;

    if (log.isInfoEnabled()) log.info("locale=" + locale);

    this.collator = (RuleBasedCollator) Collator.getInstance(locale);

    if (strength != null) {

      if (strength instanceof Integer) {

        final int str = ((Integer) strength).intValue();

        if (log.isInfoEnabled()) log.info("strength=" + str);

        collator.setStrength(str);

      } else {

        final StrengthEnum str = (StrengthEnum) strength;

        if (log.isInfoEnabled()) log.info("strength=" + str);

        switch (str) {
          case Primary:
            collator.setStrength(Collator.PRIMARY);
            break;

          case Secondary:
            collator.setStrength(Collator.SECONDARY);
            break;

          case Tertiary:
            collator.setStrength(Collator.TERTIARY);
            break;

          case Quaternary:
            collator.setStrength(Collator.QUATERNARY);
            break;

          case Identical:
            collator.setStrength(Collator.IDENTICAL);
            break;

          default:
            throw new UnsupportedOperationException("strength=" + strength);
        }
      }
    }

    if (mode != null) {

      if (log.isInfoEnabled()) log.info("mode=" + mode);

      switch (mode) {
        case None:
          collator.setDecomposition(Collator.NO_DECOMPOSITION);
          break;

        case Full:
          collator.setDecomposition(Collator.FULL_DECOMPOSITION);
          break;

        case Canonical:
          collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
          break;

        default:
          throw new UnsupportedOperationException("mode=" + mode);
      }
    }
  }

  /** Buffer is reused for each {@link String} from which a sort key is derived. */
  private final RawCollationKey raw = new RawCollationKey(128);

  public void appendSortKey(final KeyBuilder keyBuilder, final String s) {

    //        RawCollationKey raw = collator.getRawCollationKey(s, null);

    collator.getRawCollationKey(s, raw);

    keyBuilder.append(raw.bytes, 0, raw.size - 1 /*
                                                     * do not include the nul
                                                     * byte
                                                     */);
  }
}
