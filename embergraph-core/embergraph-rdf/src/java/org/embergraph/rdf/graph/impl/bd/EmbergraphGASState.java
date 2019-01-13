package org.embergraph.rdf.graph.impl.bd;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.embergraph.rdf.graph.IGASEngine;
import org.embergraph.rdf.graph.IGASProgram;
import org.embergraph.rdf.graph.IGASSchedulerImpl;
import org.embergraph.rdf.graph.IStaticFrontier;
import org.embergraph.rdf.graph.impl.GASState;
import org.embergraph.rdf.graph.impl.bd.EmbergraphGASEngine.EmbergraphGraphAccessor;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.IVUtility;
import org.embergraph.rdf.internal.impl.bnode.SidIV;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

public class EmbergraphGASState<VS, ES, ST> extends GASState<VS, ES, ST> {

  private static final Logger log = Logger.getLogger(EmbergraphGASState.class);

  @Override
  protected EmbergraphGraphAccessor getGraphAccessor() {

    return (EmbergraphGraphAccessor) super.getGraphAccessor();
  }

  public EmbergraphGASState(
      final IGASEngine gasEngine,
      final EmbergraphGraphAccessor graphAccessor,
      final IStaticFrontier frontier,
      final IGASSchedulerImpl gasScheduler,
      final IGASProgram<VS, ES, ST> gasProgram) {

    super(gasEngine, graphAccessor, frontier, gasScheduler, gasProgram);
  }

  /*
   * {@inheritDoc}
   *
   * <p>TODO EDGE STATE: edge state should be traced out also.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void traceState() {

    super.traceState();

    if (!log.isTraceEnabled()) return;

    final AbstractTripleStore kb = getGraphAccessor().getKB();

    // Get all terms in the frontier.
    final Set<IV<?, ?>> tmp = new HashSet<>();

    for (Value v : frontier()) {

      tmp.add((IV) v);
    }

    // Add all IVs for the vertexState.
    tmp.addAll((Collection) vertexState.keySet());

    // Batch resolve all IVs.
    final Map<IV<?, ?>, EmbergraphValue> m = kb.getLexiconRelation().getTerms(tmp);

    log.trace("frontier: size=" + frontier().size());

    for (Value v : frontier()) {

      log.trace("frontier: iv=" + v + " (" + m.get(v) + ")");
    }

    log.trace("vertexState: size=" + vertexState.size());

    for (Map.Entry<Value, VS> e : vertexState.entrySet()) {

      final Value v = e.getKey();

      final EmbergraphValue val = m.get(v);

      log.trace("vertexState: vertex=" + v + " (" + val + "), state=" + e.getValue());
    }
  }

  @Override
  public String toString(final Statement e) {

    return getGraphAccessor().getKB().toString((ISPO) e);
  }

  /*
   * {@inheritDoc}
   *
   * <p>Note: The {@link IV} classes sometimes implement more than one kind of {@link Value}. E.g.,
   * {@link TermId} can be a {@link BNode}, {@link URI}, or {@link Literal} and implements ALL of
   * those interfaces. So we have to make an {@link IV}-specific check here.
   *
   * <p>TODO This is visiting all edges, including link attributes (aka hyperedges or statements
   * about statements). Should we further restrict traversal to only simple edges (by checking that
   * the source and target vertices are not {@link SidIV}s).
   */
  @Override
  public boolean isEdge(final Statement e) {

    final ISPO spo = (ISPO) e;

    /*
     * For the early development of the GAS API, this test was written using o.isURI() rather than
     * o.isResource(). That caused edges that ended in a bnode to be ignored, which means that a lot
     * of the FOAF data set we were using was ignored. This was changed in r7365 to use
     * isResource(). That change invalidates the historical baseline for the BFS and SSSP
     * performance. This is also documented at the ticket below.
     *
     * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/629#comment:33">Graph Mining
     *     API </a>
     */
    return spo.o().isResource();
    //        return spo.o().isURI();

  }

  @Override
  public boolean isAttrib(final Statement e) {
    return !isEdge(e);
  }

  @Override
  public boolean isLinkAttrib(final Statement e, final URI linkAttribType) {
    final ISPO edge = (ISPO) e;
    if (!edge.p().equals(linkAttribType)) {
      // Edge does not use the specified link attribute type.
      return false;
    }
    // The subject of the edge is not a Statement.
    return edge.s() instanceof SidIV;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Statement decodeStatement(final Value v) {

    if (!(v instanceof IV)) return null;

    final IV tmp = (IV) v;

    if (!tmp.isStatement()) return null;

    final ISPO decodedEdge = (ISPO) tmp.getInlineValue();

    return decodedEdge;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public int compareTo(final Value u, final Value v) {

    return IVUtility.compare((IV) u, (IV) v);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Value getOtherVertex(final Value u, final Statement e) {

    if (e.getSubject() instanceof SidIV) {

      final ISPO spo = ((SidIV) e.getSubject()).getInlineValue();

      if (spo.s().equals(u)) return spo.o();

      return spo.s();

    } else {

      if (e.getSubject().equals(u)) return e.getObject();

      return e.getSubject();
    }
  }

  /** This will only work for the EmbergraphGASState. */
  @Override
  public Literal getLinkAttr(final Value u, final Statement e) {

    if (e.getObject() instanceof IV) {

      final IV iv = (IV) e.getObject();

      if (iv.isLiteral()) {

        if (iv.isInline()) {

          return (Literal) iv;

        } else {

          return (Literal) iv.getValue();
        }
      }

    } else if (e.getObject() instanceof Literal) {

      return (Literal) e.getObject();
    }

    return null;
  }
}
