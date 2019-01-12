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
 * Created on Feb 2, 2009
 */

package org.embergraph.resources;

import java.util.Comparator;
import org.embergraph.btree.BTreeCounters;

/**
 * Helper class assigns a raw and a normalized score to each index based on its per-index {@link
 * BTreeCounters} and on the global (non-restart safe) {@link BTreeCounters} for the data service
 * during the life cycle of the last journal.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
class Score implements Comparable<Score> {

  /** The name of the index partition. */
  public final String name;

  /** The counters collected for that index partition. */
  public final BTreeCounters bTreeCounters;

  /** The raw (write) score computed for that index partition. */
  public final double rawScore;

  /** The normalized score computed for that index partition. */
  public final double score;

  /** The rank in [0:#scored]. This is an index into the Scores[]. */
  public int rank = -1;

  /** The normalized double precision rank in [0.0:1.0]. */
  public double drank = -1d;

  public String toString() {

    return "Score{name="
        + name
        + ", rawScore="
        + rawScore
        + ", score="
        + score
        + ", rank="
        + rank
        + ", drank="
        + drank
        + "}";
  }

  public Score(final String name, final BTreeCounters bTreeCounters, final double totalRawScore) {

    assert name != null;

    assert bTreeCounters != null;

    this.name = name;

    this.bTreeCounters = bTreeCounters;

    rawScore = bTreeCounters.computeRawWriteScore();

    score = BTreeCounters.normalize(rawScore, totalRawScore);
  }

  /**
   * Places elements into order by ascending {@link #rawScore}. The {@link #name} is used to break
   * any ties.
   */
  public int compareTo(final Score arg0) {

    if (rawScore < arg0.rawScore) {

      return -1;

    } else if (rawScore > arg0.rawScore) {

      return 1;
    }

    return name.compareTo(arg0.name);
  }

  /**
   * Places {@link Score} into ascending order (lowest score to highest score). Ties are broken
   * based on an alpha sort of the index name.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  public static class ASC implements Comparator<Score> {

    public int compare(Score arg0, Score arg1) {

      if (arg0.rawScore < arg1.rawScore) {

        return -1;

      } else if (arg0.rawScore > arg1.rawScore) {

        return 1;
      }

      return arg0.name.compareTo(arg1.name);
    }
  }

  /**
   * Places {@link Score} into descending order (highest score to lowest score). Ties are broken
   * based on an alpha sort of the index name.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  public static class DESC implements Comparator<Score> {

    public int compare(Score arg0, Score arg1) {

      if (arg1.rawScore < arg0.rawScore) {

        return -1;

      } else if (arg1.rawScore > arg0.rawScore) {

        return 1;
      }

      return arg0.name.compareTo(arg1.name);
    }
  }
}
