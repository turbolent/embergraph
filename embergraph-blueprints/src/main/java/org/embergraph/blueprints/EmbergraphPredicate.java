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
package org.embergraph.blueprints;

import com.tinkerpop.blueprints.Compare;
import com.tinkerpop.blueprints.Contains;
import com.tinkerpop.blueprints.Predicate;

public enum EmbergraphPredicate implements Predicate {
  EQ,
  NE,
  GT,
  GTE,
  LT,
  LTE,
  IN,
  NIN;

  @Override
  public boolean evaluate(Object arg0, Object arg1) {
    throw new RuntimeException();
  }

  @SuppressWarnings("deprecation")
  public static EmbergraphPredicate toEmbergraphPredicate(final Predicate p) {

    if (p instanceof EmbergraphPredicate) {
      return (EmbergraphPredicate) p;
    }

    if (p instanceof Compare) {
      final Compare c = (Compare) p;
      switch (c) {
        case EQUAL:
          return EmbergraphPredicate.EQ;
        case NOT_EQUAL:
          return EmbergraphPredicate.NE;
        case GREATER_THAN:
          return EmbergraphPredicate.GT;
        case GREATER_THAN_EQUAL:
          return EmbergraphPredicate.GTE;
        case LESS_THAN:
          return EmbergraphPredicate.LT;
        case LESS_THAN_EQUAL:
          return EmbergraphPredicate.LTE;
      }
    } else if (p instanceof Contains) {
      final Contains c = (Contains) p;
      switch (c) {
        case IN:
          return EmbergraphPredicate.IN;
        case NOT_IN:
          return EmbergraphPredicate.NIN;
      }
    } else if (p instanceof com.tinkerpop.blueprints.Query.Compare) {
      final com.tinkerpop.blueprints.Query.Compare c = (com.tinkerpop.blueprints.Query.Compare) p;
      switch (c) {
        case EQUAL:
          return EmbergraphPredicate.EQ;
        case NOT_EQUAL:
          return EmbergraphPredicate.NE;
        case GREATER_THAN:
          return EmbergraphPredicate.GT;
        case GREATER_THAN_EQUAL:
          return EmbergraphPredicate.GTE;
        case LESS_THAN:
          return EmbergraphPredicate.LT;
        case LESS_THAN_EQUAL:
          return EmbergraphPredicate.LTE;
      }
    }

    throw new IllegalArgumentException();
  }
}
