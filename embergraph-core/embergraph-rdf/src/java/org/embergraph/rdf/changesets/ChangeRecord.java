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
package org.embergraph.rdf.changesets;

import java.util.Comparator;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPOComparator;

public class ChangeRecord implements IChangeRecord {

  private final ISPO stmt;

  private final ChangeAction action;

  public ChangeRecord(final ISPO stmt, final ChangeAction action) {

    this.stmt = stmt;
    this.action = action;
  }

  public ChangeAction getChangeAction() {

    return action;
  }

  public ISPO getStatement() {

    return stmt;
  }

  @Override
  public int hashCode() {

    return stmt.hashCode();
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) return true;

    if (o instanceof IChangeRecord == false) return false;

    final IChangeRecord rec = (IChangeRecord) o;

    final ISPO stmt2 = rec.getStatement();

    // statements are equal
    if (stmt == stmt2 || (stmt != null && stmt.equals(stmt2))) {

      // actions are equal
      return action == rec.getChangeAction();
    }

    return false;
  }

  public String toString() {

    return action + ": " + stmt;
  }

  /** Comparator imposes an {@link ISPO} order. */
  public static final Comparator<IChangeRecord> COMPARATOR =
      (r1, r2) -> {

        final ISPO spo1 = r1.getStatement();
        final ISPO spo2 = r2.getStatement();

        return SPOComparator.INSTANCE.compare(spo1, spo2);
      };
}
