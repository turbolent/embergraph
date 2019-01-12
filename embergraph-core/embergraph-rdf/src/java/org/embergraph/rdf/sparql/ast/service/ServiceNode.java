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
 * Created on Aug 18, 2011
 */

package org.embergraph.rdf.sparql.ast.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.sparql.ast.GraphPatternGroup;
import org.embergraph.rdf.sparql.ast.GroupMemberNodeBase;
import org.embergraph.rdf.sparql.ast.IGraphPatternContainer;
import org.embergraph.rdf.sparql.ast.IGroupMemberNode;
import org.embergraph.rdf.sparql.ast.IJoinNode;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.QueryBase;
import org.embergraph.rdf.sparql.ast.StaticAnalysis;
import org.embergraph.rdf.sparql.ast.TermNode;
import org.embergraph.rdf.sparql.ast.UnionNode;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.model.URI;

/**
 * An extension point for external service calls which produce solution multisets (a SPARQL <code>
 * SERVICE</code>).
 *
 * <p>TODO It would make the internal APIs significantly easier if we modeled this as a type of
 * {@link GraphPatternGroup}, similar to {@link JoinGroupNode} and {@link UnionNode}.
 */
public class ServiceNode extends GroupMemberNodeBase<IGroupMemberNode>
    implements IJoinNode, IGraphPatternContainer {

  private static final long serialVersionUID = 1L;

  interface Annotations
      extends GroupMemberNodeBase.Annotations,
          IJoinNode.Annotations,
          IGraphPatternContainer.Annotations {

    /** The {@link TermNode} for the SERVICE URI (either a simple variable or a simple constant). */
    String SERVICE_REF = "serviceRef";

    /**
     * The namespace of the {@link AbstractTripleStore} instance (not the namespace of the lexicon
     * relation). This resource will be located and made available to the {@link ServiceCall}.
     */
    String NAMESPACE = "namespace";

    /**
     * The "SELECT" option.
     *
     * <p>TODO Lift out. This is used for many things in SPARQL UPDATE, not just for SPARQL
     * Federation.
     */
    String SILENT = "silent";

    boolean DEFAULT_SILENT = false;

    /**
     * The timeout in milliseconds before a SERVICE request is failed.
     *
     * @see #DEFAULT_TIMEOUT
     */
    String TIMEOUT = "timeout";

    final long DEFAULT_TIMEOUT = Long.MAX_VALUE;

    /**
     * The text "image" of the original SPARQL SERVICE clause. The "image" of the original graph
     * pattern is what gets sent to a remote SPARQL end point when we evaluate the SERVICE node.
     * Because the original "image" of the graph pattern is being used, we also need to have the
     * prefix declarations so we can generate a valid SPARQL request.
     */
    String EXPR_IMAGE = "exprImage";

    /**
     * The prefix declarations for the SPARQL query from which the {@link #EXPR_IMAGE} was taken.
     * This is needed in order to generate a valid SPARQL query for a remote SPARQL end point when
     * we evaluate the SERVICE request.
     */
    String PREFIX_DECLS = "prefixDecls";

    /**
     * The set of variables which can flow in/out of the SERVICE.
     *
     * <p>TODO Use the {@link QueryBase.Annotations#PROJECTION} for this?
     */
    String PROJECTED_VARS = "projectedVars";
  }

  /** Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}. */
  public ServiceNode(final ServiceNode op) {

    super(op);
  }

  /** Required shallow copy constructor. */
  public ServiceNode(BOp[] args, Map<String, Object> anns) {

    super(args, anns);
  }

  /**
   * Construct a function node in the AST.
   *
   * @param serviceRef The value expression for the SERVICE URI.
   * @param groupNode The graph pattern used to invoke the service.
   * @see ServiceRegistry
   */
  public ServiceNode(
      final TermNode serviceRef, final GraphPatternGroup<IGroupMemberNode> groupNode) {

    super(new BOp[] {}, null /* anns */);

    setServiceRef(serviceRef);

    setGraphPattern(groupNode);
  }

  /** The service reference. */
  public TermNode getServiceRef() {

    return (TermNode) getRequiredProperty(Annotations.SERVICE_REF);
  }

  /** Set the service reference. */
  public void setServiceRef(final TermNode serviceRef) {

    if (serviceRef == null) throw new IllegalArgumentException();

    setProperty(Annotations.SERVICE_REF, serviceRef);
  }

  @SuppressWarnings("unchecked")
  public GraphPatternGroup<IGroupMemberNode> getGraphPattern() {

    return (GraphPatternGroup<IGroupMemberNode>) getRequiredProperty(Annotations.GRAPH_PATTERN);
  }

  @Override
  public void setGraphPattern(final GraphPatternGroup<IGroupMemberNode> graphPattern) {

    if (graphPattern == null) throw new IllegalArgumentException();

    /*
     * Clear the parent reference on the new where clause.
     *
     * Note: This handles cases where a join group is lifted into a named
     * subquery. If we do not clear the parent reference on the lifted join
     * group it will still point back to its parent in the original join
     * group.
     */
    graphPattern.setParent(null);

    super.setProperty(Annotations.GRAPH_PATTERN, graphPattern);
  }

  /** Returns <code>false</code>. */
  public final boolean isOptional() {

    return false;
  }

  /** Returns <code>false</code>. */
  public final boolean isMinus() {

    return false;
  }

  public final boolean isSilent() {

    return getProperty(Annotations.SILENT, Annotations.DEFAULT_SILENT);
  }

  public final void setSilent(final boolean silent) {

    setProperty(Annotations.SILENT, silent);
  }

  public String getExprImage() {

    return (String) getProperty(Annotations.EXPR_IMAGE);
  }

  /**
   * Set the text "image" of the SPARQL SERVICE clause. This will be used IFF we generate a SPARQL
   * query for a remote SPARQL end point. You must also specify the prefix declarations for that
   * text "image".
   */
  public void setExprImage(final String serviceExpressionString) {

    setProperty(Annotations.EXPR_IMAGE, serviceExpressionString);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Map<String, String> getPrefixDecls() {

    return (Map) getProperty(Annotations.PREFIX_DECLS);
  }

  /**
   * Set the prefix declarations for the group graph pattern. This will be used IFF we generate a
   * SPARQL query for a remote SPARQL end point. You must also specify the text "image".
   */
  public void setPrefixDecls(final Map<String, String> prefixDecls) {

    setProperty(Annotations.PREFIX_DECLS, prefixDecls);
  }

  /**
   * @param projectedVars
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/510">Blank nodes in SERVICE
   *     graph patterns </a>
   */
  public void setProjectedVars(final Set<IVariable<?>> projectedVars) {

    setProperty(Annotations.PROJECTED_VARS, projectedVars);
  }

  /** @see Annotations#PROJECTED_VARS */
  @SuppressWarnings("unchecked")
  public Set<IVariable<?>> getProjectedVars() {

    return (Set<IVariable<?>>) getProperty(Annotations.PROJECTED_VARS);
  }

  public void setTimeout(final Long timeout) {

    setProperty(Annotations.TIMEOUT, timeout);
  }

  /**
   * Return the timeout for evaluation of this SERVICE request.
   *
   * @return The timeout -or- {@link Annotations#DEFAULT_TIMEOUT} if the timeout was not explicitly
   *     configured.
   * @see Annotations#TIMEOUT
   */
  public long getTimeout() {

    return getProperty(Annotations.TIMEOUT, Annotations.DEFAULT_TIMEOUT);
  }

  public final List<FilterNode> getAttachedJoinFilters() {

    @SuppressWarnings("unchecked")
    final List<FilterNode> filters = (List<FilterNode>) getProperty(Annotations.FILTERS);

    if (filters == null) {

      return Collections.emptyList();
    }

    return Collections.unmodifiableList(filters);
  }

  public final void setAttachedJoinFilters(final List<FilterNode> filters) {

    setProperty(Annotations.FILTERS, filters);
  }

  // TODO toString() for all attributes.
  @Override
  public String toString(int indent) {

    final StringBuilder sb = new StringBuilder();

    final TermNode serviceRef = getServiceRef();

    final long timeout = getTimeout();

    sb.append("\n");
    sb.append(indent(indent));
    sb.append("SERVICE");
    if (isSilent()) {
      sb.append(" SILENT");
    }
    if (serviceRef.isConstant()) {
      sb.append(" <");
      sb.append(serviceRef);
      sb.append(">");
    } else {
      sb.append(" ?");
      sb.append(serviceRef);
    }

    if (timeout != Long.MAX_VALUE) {
      sb.append(" [timeout=" + timeout + "ms]");
    }

    if (getGraphPattern() != null) {

      sb.append(" {");

      sb.append(getGraphPattern().toString(indent + 1));

      sb.append("\n").append(indent(indent)).append("}");
    }

    final List<FilterNode> filters = getAttachedJoinFilters();
    if (!filters.isEmpty()) {
      for (FilterNode filter : filters) {
        sb.append(filter.toString(indent + 1));
      }
    }

    if (getQueryHints() != null && !getQueryHints().isEmpty()) {
      sb.append("\n");
      sb.append(indent(indent + 1));
      sb.append(Annotations.QUERY_HINTS);
      sb.append("=");
      sb.append(getQueryHints().toString());
    }

    return sb.toString();
  }

  @Override
  public Set<IVariable<?>> getRequiredBound(StaticAnalysis sa) {
    return getResponsibleServiceFactory().getRequiredBound(this);
  }

  @Override
  public Set<IVariable<?>> getDesiredBound(StaticAnalysis sa) {
    return getResponsibleServiceFactory().getDesiredBound(this);
  }

  /**
   * Returns the service factory that is responsible for handling this service node.
   *
   * @return the associated {@link ServiceFactory}
   */
  public ServiceFactory getResponsibleServiceFactory() {

    final ServiceRegistry serviceRegistry = ServiceRegistry.getInstance();

    final IVariableOrConstant<?> serviceRef = getServiceRef().getValueExpression();

    URI serviceUri = null; // will be set if there is a URI
    if (serviceRef != null && serviceRef instanceof IConstant) {
      final IConstant<?> serviceRefConst = (IConstant<?>) serviceRef;
      final Object val = serviceRefConst.get();

      if (val instanceof TermId<?>) {
        final TermId<?> valTerm = (TermId<?>) val;
        final EmbergraphValue bdVal = valTerm.getValue();
        if (bdVal != null && bdVal instanceof URI) {
          serviceUri = (URI) bdVal;
        }
      }
    }

    return serviceRegistry.getServiceFactoryByServiceURI(serviceUri);
  }
}
