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
package org.embergraph.rdf.store;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/*
 * A simple class that represents a triple (or quad) pattern.
 *
 * @see <a href="http://trac.blazegraph.com/ticket/866" > Efficient batch remove of a collection of
 *     triple patterns </a>
 */
public class EmbergraphTriplePattern {

  //    private static final long serialVersionUID = 1L;

  private final Resource s;
  private final URI p;
  private final Value o;
  private final Resource c;

  public EmbergraphTriplePattern(final Resource subject, final URI predicate, final Value object) {

    this(subject, predicate, object, null);
  }

  public EmbergraphTriplePattern(
      final Resource subject, final URI predicate, final Value object, final Resource context) {

    this.s = subject;

    this.p = predicate;

    this.o = object;

    this.c = context;
  }

  public final Resource getSubject() {

    return s;
  }

  public final URI getPredicate() {

    return p;
  }

  public final Value getObject() {

    return o;
  }

  public final Resource getContext() {

    return c;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((c == null) ? 0 : c.hashCode());
    result = prime * result + ((o == null) ? 0 : o.hashCode());
    result = prime * result + ((p == null) ? 0 : p.hashCode());
    result = prime * result + ((s == null) ? 0 : s.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EmbergraphTriplePattern other = (EmbergraphTriplePattern) obj;
    if (c == null) {
      if (other.c != null) return false;
    } else if (!c.equals(other.c)) return false;
    if (o == null) {
      if (other.o != null) return false;
    } else if (!o.equals(other.o)) return false;
    if (p == null) {
      if (other.p != null) return false;
    } else if (!p.equals(other.p)) return false;
    if (s == null) {
      return other.s == null;
    } else return s.equals(other.s);
  }
}
