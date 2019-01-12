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

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;

/**
 * Edge implementation that wraps an Edge statement and points to a {@link EmbergraphGraph}
 * instance.
 *
 * @author mikepersonick
 */
public class EmbergraphEdge extends EmbergraphElement implements Edge {

  private static final transient Logger log = Logger.getLogger(EmbergraphEdge.class);

  private static final List<String> blacklist = Arrays.asList("id", "", "label");

  protected final Statement stmt;

  public EmbergraphEdge(final Statement stmt, final EmbergraphGraph graph) {
    super(stmt.getPredicate(), graph);

    this.stmt = stmt;
  }

  @Override
  public Object getId() {

    if (log.isInfoEnabled()) log.info("");

    return graph.factory.fromURI(uri);
  }

  @Override
  public void remove() {

    if (log.isInfoEnabled()) log.info("");

    graph.removeEdge(this);
  }

  @Override
  public String getLabel() {

    if (log.isInfoEnabled()) log.info("");

    return (String) graph.getProperty(uri, graph.getValueFactory().getLabelURI());
  }

  @Override
  public Vertex getVertex(final Direction dir) throws IllegalArgumentException {

    if (log.isInfoEnabled()) log.info("(" + dir + ")");

    if (dir == Direction.BOTH) {
      throw new IllegalArgumentException();
    }

    final URI uri = (URI) (dir == Direction.OUT ? stmt.getSubject() : stmt.getObject());

    final String id = graph.factory.fromURI(uri);

    return graph.getVertex(id);
  }

  public EmbergraphVertex getFrom() {
    return new EmbergraphVertex((URI) stmt.getSubject(), graph);
  }

  public EmbergraphVertex getTo() {
    return new EmbergraphVertex((URI) stmt.getObject(), graph);
  }

  @Override
  public void setProperty(final String prop, final Object val) {

    if (log.isInfoEnabled()) log.info("(" + prop + ", " + val + ")");

    if (prop == null || blacklist.contains(prop)) {
      throw new IllegalArgumentException();
    }

    super.setProperty(prop, val);
  }

  @Override
  public String toString() {

    final URI s = (URI) stmt.getSubject();
    final URI p = stmt.getPredicate();
    final URI o = (URI) stmt.getObject();
    return "e[" + p.getLocalName() + "][" + s.getLocalName() + "->" + o.getLocalName() + "]";
  }

  @Override
  public <T> T getProperty(final String prop) {

    if (log.isInfoEnabled()) log.info("(" + prop + ")");

    return super.getProperty(prop);
  }

  @Override
  public Set<String> getPropertyKeys() {

    if (log.isInfoEnabled()) log.info("");

    return super.getPropertyKeys();
  }

  @Override
  public <T> T removeProperty(final String prop) {

    if (log.isInfoEnabled()) log.info("(" + prop + ")");

    return super.removeProperty(prop);
  }

  //    @Override
  //    public void addProperty(final String prop, final Object val) {
  //
  //        if (log.isInfoEnabled())
  //            log.info("("+prop+", "+val+")");
  //
  //        super.addProperty(prop, val);
  //
  //    }
  //
  //    @Override
  //    public <T> List<T> getProperties(final String prop) {
  //
  //        if (log.isInfoEnabled())
  //            log.info("("+prop+")");
  //
  //        return super.getProperties(prop);
  //
  //    }

}
