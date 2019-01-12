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
package org.embergraph.rdf.model;

import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.impl.bnode.SidIV;
import org.embergraph.rdf.spo.ModifiedEnum;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.IRawTripleStore;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;

/**
 * Implementation reveals whether a statement is explicit, inferred, or an axiom and the internal
 * term identifiers for the subject, predicate, object, the context bound on that statement (when
 * present). When statement identifiers are enabled, the context position (if bound) will be a blank
 * node that represents the statement having that subject, predicate, and object and its term
 * identifier, when assigned, will report <code>true</code> for {@link
 * AbstractTripleStore#isStatement(IV)}. When used to model a quad, the 4th position will be a
 * {@link EmbergraphValue} but its term identifier will report <code>false</code> for {@link
 * AbstractTripleStore#isStatement(IV)}.
 *
 * <p>Note: The ctors are intentionally protected. Use the {@link EmbergraphValueFactory} to create
 * instances of this class - it will ensure that term identifiers are propagated iff the backing
 * lexicon is the same.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class EmbergraphStatementImpl implements EmbergraphStatement {

  /** */
  private static final long serialVersionUID = 6739949195958368365L;

  private final EmbergraphResource s;
  private final EmbergraphURI p;
  private final EmbergraphValue o;
  private final EmbergraphResource c;
  private IV sid = null;
  private StatementEnum type;
  private boolean userFlag;
  private transient boolean override = false;
  //    private transient boolean modified = false;
  private transient ModifiedEnum modified = ModifiedEnum.NONE;

  /** Used by {@link EmbergraphValueFactory} */
  public EmbergraphStatementImpl(
      final EmbergraphResource subject,
      final EmbergraphURI predicate,
      final EmbergraphValue object,
      final EmbergraphResource context,
      final StatementEnum type,
      final boolean userFlag) {

    if (subject == null) throw new IllegalArgumentException();

    if (predicate == null) throw new IllegalArgumentException();

    if (object == null) throw new IllegalArgumentException();

    // Note: context MAY be null

    // Note: type MAY be null.

    this.s = subject;

    this.p = predicate;

    this.o = object;

    this.c = context;

    this.type = type;

    this.userFlag = userFlag;
  }

  @Override
  public final EmbergraphResource getSubject() {

    return s;
  }

  @Override
  public final EmbergraphURI getPredicate() {

    return p;
  }

  @Override
  public final EmbergraphValue getObject() {

    return o;
  }

  @Override
  public final EmbergraphResource getContext() {

    return c;
  }

  @Override
  public final boolean hasStatementType() {

    return type != null;
  }

  @Override
  public final StatementEnum getStatementType() {

    return type;
  }

  @Override
  public final void setStatementType(final StatementEnum type) {

    if (type == null) {

      throw new IllegalArgumentException();
    }

    if (this.type != null && type != this.type) {

      throw new IllegalStateException();
    }

    this.type = type;
  }

  @Override
  public final void setUserFlag(boolean userFlag) {

    this.userFlag = userFlag;
  }

  @Override
  public final boolean isAxiom() {

    return StatementEnum.Axiom == type;
  }

  @Override
  public final boolean isInferred() {

    return StatementEnum.Inferred == type;
  }

  @Override
  public final boolean isExplicit() {

    return StatementEnum.Explicit == type;
  }

  @Override
  public final boolean getUserFlag() {

    return userFlag;
  }

  @Override
  public boolean equals(final Object o) {

    return equals((Statement) o);
  }

  /**
   * Note: implementation per {@link Statement} interface, which specifies that only the (s,p,o)
   * positions are to be considered.
   */
  public boolean equals(final Statement stmt) {

    return s.equals(stmt.getSubject())
        && p.equals(stmt.getPredicate())
        && o.equals(stmt.getObject());
  }

  /** Note: implementation per Statement interface, which does not consider the context position. */
  @Override
  public final int hashCode() {

    if (hash == 0) {

      hash = 961 * s.hashCode() + 31 * p.hashCode() + o.hashCode();
    }

    return hash;
  }

  private int hash = 0;

  @Override
  public String toString() {

    return "<"
        + s
        + ", "
        + p
        + ", "
        + o
        + (c == null ? "" : ", " + c)
        + ">"
        + (type == null ? "" : " : " + type)
        + (isModified() ? " : modified(" + modified + ")" : "");
  }

  @Override
  public final IV s() {

    return s.getIV();
  }

  @Override
  public final IV p() {

    return p.getIV();
  }

  @Override
  public final IV o() {

    return o.getIV();
  }

  @Override
  public final IV c() {

    if (c == null) return null;

    return c.getIV();
  }

  @Override
  public IV get(final int index) {

    switch (index) {
      case 0:
        return s.getIV();
      case 1:
        return p.getIV();
      case 2:
        return o.getIV();
      case 3: // 4th position MAY be unbound.
        return (c == null) ? null : c.getIV();
      default:
        throw new IllegalArgumentException();
    }
  }

  @Override
  public final boolean isFullyBound() {

    return s() != null && p() != null && o() != null;
  }

  //    public final void setStatementIdentifier(final boolean sidable) {
  //
  //        if (sidable && type != StatementEnum.Explicit) {
  //
  //            // Only allowed for explicit statements.
  //            throw new IllegalStateException();
  //
  //        }
  //
  ////        if (c == null) {
  ////
  ////        	// this SHOULD not ever happen
  ////        	throw new IllegalStateException();
  ////
  ////        }
  ////
  ////        c.setIV(new SidIV(this));
  //
  //        this.sid = new SidIV(this);
  //
  //    }

  @Override
  public final IV getStatementIdentifier() {

    //        if (!hasStatementIdentifier())
    //            throw new IllegalStateException("No statement identifier: "
    //                    + toString());
    //
    //        return c.getIV();

    if (sid == null && type == StatementEnum.Explicit) {

      sid = new SidIV(this);
    }

    return sid;
  }

  @Override
  public final boolean hasStatementIdentifier() {

    //        return c != null && c.getIV().isStatement();

    return type == StatementEnum.Explicit;
  }

  @Override
  public final boolean isOverride() {

    return override;
  }

  @Override
  public final void setOverride(final boolean override) {

    this.override = override;
  }

  /**
   * Note: this implementation is equivalent to {@link #toString()} since the {@link Value}s are
   * already resolved.
   */
  @Override
  public String toString(final IRawTripleStore storeIsIgnored) {

    return toString();
  }

  @Override
  public boolean isModified() {

    return modified != ModifiedEnum.NONE;
  }

  @Override
  public void setModified(final ModifiedEnum modified) {

    this.modified = modified;
  }

  @Override
  public ModifiedEnum getModified() {

    return modified;
  }
}
