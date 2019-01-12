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
/* Portions of this code are:
 *
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
/*
 * Created on Aug 21, 2011
 */

package org.embergraph.rdf.sail.sparql;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.embergraph.bop.aggregate.AggregateBase;
import org.embergraph.bop.rdf.aggregate.GROUP_CONCAT;
import org.embergraph.rdf.internal.constraints.IriBOp;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.sail.sparql.ast.ASTAbs;
import org.embergraph.rdf.sail.sparql.ast.ASTAggregate;
import org.embergraph.rdf.sail.sparql.ast.ASTAnd;
import org.embergraph.rdf.sail.sparql.ast.ASTAvg;
import org.embergraph.rdf.sail.sparql.ast.ASTBNodeFunc;
import org.embergraph.rdf.sail.sparql.ast.ASTBound;
import org.embergraph.rdf.sail.sparql.ast.ASTCeil;
import org.embergraph.rdf.sail.sparql.ast.ASTCoalesce;
import org.embergraph.rdf.sail.sparql.ast.ASTCompare;
import org.embergraph.rdf.sail.sparql.ast.ASTConcat;
import org.embergraph.rdf.sail.sparql.ast.ASTContains;
import org.embergraph.rdf.sail.sparql.ast.ASTCount;
import org.embergraph.rdf.sail.sparql.ast.ASTDatatype;
import org.embergraph.rdf.sail.sparql.ast.ASTDay;
import org.embergraph.rdf.sail.sparql.ast.ASTEncodeForURI;
import org.embergraph.rdf.sail.sparql.ast.ASTExistsFunc;
import org.embergraph.rdf.sail.sparql.ast.ASTFloor;
import org.embergraph.rdf.sail.sparql.ast.ASTFunctionCall;
import org.embergraph.rdf.sail.sparql.ast.ASTGroupConcat;
import org.embergraph.rdf.sail.sparql.ast.ASTGroupCondition;
import org.embergraph.rdf.sail.sparql.ast.ASTHours;
import org.embergraph.rdf.sail.sparql.ast.ASTIRIFunc;
import org.embergraph.rdf.sail.sparql.ast.ASTIf;
import org.embergraph.rdf.sail.sparql.ast.ASTIn;
import org.embergraph.rdf.sail.sparql.ast.ASTInfix;
import org.embergraph.rdf.sail.sparql.ast.ASTIsBlank;
import org.embergraph.rdf.sail.sparql.ast.ASTIsIRI;
import org.embergraph.rdf.sail.sparql.ast.ASTIsLiteral;
import org.embergraph.rdf.sail.sparql.ast.ASTIsNumeric;
import org.embergraph.rdf.sail.sparql.ast.ASTLang;
import org.embergraph.rdf.sail.sparql.ast.ASTLangMatches;
import org.embergraph.rdf.sail.sparql.ast.ASTLowerCase;
import org.embergraph.rdf.sail.sparql.ast.ASTMD5;
import org.embergraph.rdf.sail.sparql.ast.ASTMath;
import org.embergraph.rdf.sail.sparql.ast.ASTMax;
import org.embergraph.rdf.sail.sparql.ast.ASTMin;
import org.embergraph.rdf.sail.sparql.ast.ASTMinutes;
import org.embergraph.rdf.sail.sparql.ast.ASTMonth;
import org.embergraph.rdf.sail.sparql.ast.ASTNot;
import org.embergraph.rdf.sail.sparql.ast.ASTNotExistsFunc;
import org.embergraph.rdf.sail.sparql.ast.ASTNotIn;
import org.embergraph.rdf.sail.sparql.ast.ASTNow;
import org.embergraph.rdf.sail.sparql.ast.ASTOr;
import org.embergraph.rdf.sail.sparql.ast.ASTRand;
import org.embergraph.rdf.sail.sparql.ast.ASTRegexExpression;
import org.embergraph.rdf.sail.sparql.ast.ASTReplace;
import org.embergraph.rdf.sail.sparql.ast.ASTRound;
import org.embergraph.rdf.sail.sparql.ast.ASTSHA1;
import org.embergraph.rdf.sail.sparql.ast.ASTSHA224;
import org.embergraph.rdf.sail.sparql.ast.ASTSHA256;
import org.embergraph.rdf.sail.sparql.ast.ASTSHA384;
import org.embergraph.rdf.sail.sparql.ast.ASTSHA512;
import org.embergraph.rdf.sail.sparql.ast.ASTSTRUUID;
import org.embergraph.rdf.sail.sparql.ast.ASTSameTerm;
import org.embergraph.rdf.sail.sparql.ast.ASTSample;
import org.embergraph.rdf.sail.sparql.ast.ASTSeconds;
import org.embergraph.rdf.sail.sparql.ast.ASTStr;
import org.embergraph.rdf.sail.sparql.ast.ASTStrAfter;
import org.embergraph.rdf.sail.sparql.ast.ASTStrBefore;
import org.embergraph.rdf.sail.sparql.ast.ASTStrDt;
import org.embergraph.rdf.sail.sparql.ast.ASTStrEnds;
import org.embergraph.rdf.sail.sparql.ast.ASTStrLang;
import org.embergraph.rdf.sail.sparql.ast.ASTStrLen;
import org.embergraph.rdf.sail.sparql.ast.ASTStrStarts;
import org.embergraph.rdf.sail.sparql.ast.ASTSubstr;
import org.embergraph.rdf.sail.sparql.ast.ASTSum;
import org.embergraph.rdf.sail.sparql.ast.ASTTimezone;
import org.embergraph.rdf.sail.sparql.ast.ASTTz;
import org.embergraph.rdf.sail.sparql.ast.ASTUUID;
import org.embergraph.rdf.sail.sparql.ast.ASTUpperCase;
import org.embergraph.rdf.sail.sparql.ast.ASTYear;
import org.embergraph.rdf.sail.sparql.ast.Node;
import org.embergraph.rdf.sail.sparql.ast.SimpleNode;
import org.embergraph.rdf.sail.sparql.ast.VisitorException;
import org.embergraph.rdf.sparql.ast.AssignmentNode;
import org.embergraph.rdf.sparql.ast.ConstantNode;
import org.embergraph.rdf.sparql.ast.ExistsNode;
import org.embergraph.rdf.sparql.ast.FunctionNode;
import org.embergraph.rdf.sparql.ast.FunctionRegistry;
import org.embergraph.rdf.sparql.ast.GraphPatternGroup;
import org.embergraph.rdf.sparql.ast.IGroupMemberNode;
import org.embergraph.rdf.sparql.ast.IValueExpressionNode;
import org.embergraph.rdf.sparql.ast.NotExistsNode;
import org.embergraph.rdf.sparql.ast.ValueExpressionNode;
import org.embergraph.rdf.sparql.ast.VarNode;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.FN;

/**
 * Visitor pattern builds {@link IValueExpressionNode}s.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @see FunctionRegistry
 * @openrdf
 */
public class ValueExprBuilder extends EmbergraphASTVisitorBase {

  //    private static final Logger log = Logger.getLogger(ValueExprBuilder.class);

  /**
   * Used to manage collection and nesting of graph patterns. This is mostly handled by {@link
   * GroupGraphPatternBuilder}.
   *
   * <p>Note: Both {@link ASTExistsFunc} and {@link ASTNotExistsFunc} have an inner graph pattern by
   * they appear within value expressions.
   */
  protected GroupGraphPattern graphPattern;

  public ValueExprBuilder(final EmbergraphASTContext context) {

    super(context);
  }

  protected ValueExpressionNode left(final SimpleNode node) throws VisitorException {

    return (ValueExpressionNode) node.jjtGetChild(0).jjtAccept(this, null);
  }

  protected ValueExpressionNode right(final SimpleNode node) throws VisitorException {

    return (ValueExpressionNode) node.jjtGetChild(1).jjtAccept(this, null);
  }

  /** Handle a simple function without any arguments. */
  protected FunctionNode noneary(final SimpleNode node, final URI functionURI)
      throws VisitorException {

    return new FunctionNode(functionURI, null /* scalarValues */, new ValueExpressionNode[] {});
  }

  /** Handle a simple unary function (the child of the node is the argument to the function). */
  protected FunctionNode unary(final SimpleNode node, final URI functionURI)
      throws VisitorException {

    return new FunctionNode(
        functionURI, null /* scalarValues */, new ValueExpressionNode[] {left(node)});
  }

  /** Handle a simple binary function (both children of the node are arguments to the function). */
  protected FunctionNode binary(final SimpleNode node, final URI functionURI)
      throws VisitorException {

    return new FunctionNode(
        functionURI, null /* scalarValues */, new ValueExpressionNode[] {left(node), right(node)});
  }

  /**
   * Handle a simple ternary function (there are three children of the node which are the arguments
   * to the function).
   */
  protected FunctionNode ternary(final SimpleNode node, final URI functionURI)
      throws VisitorException {

    return new FunctionNode(
        functionURI,
        null /* scalarValues */,
        new ValueExpressionNode[] {
          left(node), right(node), (ValueExpressionNode) node.jjtGetChild(2).jjtAccept(this, null)
        });
  }

  /**
   * Handle a function with four arguments (there are four children of the node which are the
   * arguments to the function).
   */
  protected FunctionNode quadary(final SimpleNode node, final URI functionURI)
      throws VisitorException {

    return new FunctionNode(
        functionURI,
        null /* scalarValues */,
        new ValueExpressionNode[] {
          left(node),
          right(node),
          (ValueExpressionNode) node.jjtGetChild(2).jjtAccept(this, null),
          (ValueExpressionNode) node.jjtGetChild(3).jjtAccept(this, null)
        });
  }

  /** Handle a simple nary function (all children of the node are arguments to the function). */
  protected FunctionNode nary(final SimpleNode node, final URI functionURI)
      throws VisitorException {

    final int nargs = node.jjtGetNumChildren();

    final ValueExpressionNode[] args = new ValueExpressionNode[nargs];

    for (int i = 0; i < nargs; i++) {

      final Node argNode = node.jjtGetChild(i);

      args[i] = (ValueExpressionNode) argNode.jjtAccept(this, null);
    }

    if (functionURI.equals(FunctionRegistry.COALESCE)) {

      return new FunctionNode(FunctionRegistry.COALESCE, null /* scalarValues */, args);

    } else if (functionURI.equals(FN.SUBSTRING)) {

      return new FunctionNode(FunctionRegistry.SUBSTR, null /* scalarValues */, args);

    } else if (functionURI.equals(FN.CONCAT)) {

      return new FunctionNode(FunctionRegistry.CONCAT, null /* scalarValues */, args);

    } else {

      throw new IllegalArgumentException();
    }
  }

  protected FunctionNode aggregate(final ASTAggregate node, final URI functionURI)
      throws VisitorException {

    Map<String, Object> scalarValues = null;

    if (node.isDistinct()) {

      scalarValues =
          Collections.singletonMap(AggregateBase.Annotations.DISTINCT, (Object) Boolean.TRUE);
    }

    if (node instanceof ASTCount && ((ASTCount) node).isWildcard()) {

      /*
       * Note: The wildcard is dropped by openrdf.
       */

      return new FunctionNode(
          functionURI, scalarValues, new ValueExpressionNode[] {new VarNode("*")});
    }

    return new FunctionNode(functionURI, scalarValues, new ValueExpressionNode[] {left(node)});
  }

  //    @Override
  //    final public FunctionNode visit(ASTBind node, Object data) throws VisitorException {
  //        return new AssignmentNode((VarNode) right(node), left(node));
  //    }

  @Override
  public final FunctionNode visit(ASTOr node, Object data) throws VisitorException {
    return binary(node, FunctionRegistry.OR);
  }

  @Override
  public final FunctionNode visit(ASTAnd node, Object data) throws VisitorException {
    return binary(node, FunctionRegistry.AND);
  }

  @Override
  public final FunctionNode visit(ASTNot node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.NOT);
  }

  @Override
  public final FunctionNode visit(ASTCoalesce node, Object data) throws VisitorException {
    return nary(node, FunctionRegistry.COALESCE);
  }

  @Override
  public final FunctionNode visit(ASTCompare node, Object data) throws VisitorException {

    final URI functionURI;
    switch (node.getOperator()) {
      case LT:
        functionURI = FunctionRegistry.LT;
        break;
      case GT:
        functionURI = FunctionRegistry.GT;
        break;
      case EQ:
        functionURI = FunctionRegistry.EQ;
        break;
      case LE:
        functionURI = FunctionRegistry.LE;
        break;
      case GE:
        functionURI = FunctionRegistry.GE;
        break;
      case NE:
        functionURI = FunctionRegistry.NE;
        break;
      default:
        throw new UnsupportedOperationException(node.getOperator().getSymbol());
    }

    return binary(node, functionURI);
  }

  @Override
  public final FunctionNode visit(ASTSubstr node, Object data) throws VisitorException {
    return nary(node, FN.SUBSTRING);
  }

  @Override
  public final FunctionNode visit(ASTConcat node, Object data) throws VisitorException {
    return nary(node, FN.CONCAT);
  }

  @Override
  public final FunctionNode visit(ASTAbs node, Object data) throws VisitorException {
    return unary(node, FN.NUMERIC_ABS);
  }

  @Override
  public final FunctionNode visit(ASTCeil node, Object data) throws VisitorException {
    return unary(node, FN.NUMERIC_CEIL);
  }

  @Override
  public final FunctionNode visit(ASTContains node, Object data) throws VisitorException {
    return binary(node, FN.CONTAINS);
  }

  @Override
  public final FunctionNode visit(ASTFloor node, Object data) throws VisitorException {
    return unary(node, FN.NUMERIC_FLOOR);
  }

  @Override
  public final FunctionNode visit(ASTRound node, Object data) throws VisitorException {
    return unary(node, FN.NUMERIC_ROUND);
  }

  @Override
  public final FunctionNode visit(ASTRand node, Object data) throws VisitorException {
    return noneary(node, FunctionRegistry.RAND);
  }

  @Override
  public final FunctionNode visit(ASTSameTerm node, Object data) throws VisitorException {
    return binary(node, FunctionRegistry.SAME_TERM);
  }

  @Override
  public final FunctionNode visit(ASTMath node, Object data) throws VisitorException {

    final URI functionURI;
    switch (node.getOperator()) {
      case PLUS:
        functionURI = FunctionRegistry.ADD;
        break;
      case MINUS:
        functionURI = FunctionRegistry.SUBTRACT;
        break;
      case MULTIPLY:
        functionURI = FunctionRegistry.MULTIPLY;
        break;
      case DIVIDE:
        functionURI = FunctionRegistry.DIVIDE;
        break;
      default:
        throw new UnsupportedOperationException(node.getOperator().getSymbol());
    }

    return binary(node, functionURI);
  }

  /** ASTFunctionCall (IRIRef, ArgList). */
  @Override
  public final FunctionNode visit(ASTFunctionCall node, Object data) throws VisitorException {

    final ConstantNode uriNode = (ConstantNode) node.jjtGetChild(0).jjtAccept(this, null);

    final EmbergraphURI functionURI = (EmbergraphURI) uriNode.getValue();

    final int nargs = node.jjtGetNumChildren() - 1;

    final ValueExpressionNode[] args = new ValueExpressionNode[nargs];

    for (int i = 0; i < nargs; i++) {

      final Node argNode = node.jjtGetChild(i + 1);

      args[i] = (ValueExpressionNode) argNode.jjtAccept(this, null);
    }

    return new FunctionNode(functionURI, null /* scalarValues */, args);
  }

  @Override
  public final FunctionNode visit(ASTEncodeForURI node, Object data) throws VisitorException {
    return unary(node, FN.ENCODE_FOR_URI);
  }

  @Override
  public final FunctionNode visit(ASTStr node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.STR);
  }

  @Override
  public final FunctionNode visit(ASTStrDt node, Object data) throws VisitorException {
    return binary(node, FunctionRegistry.STR_DT);
  }

  @Override
  public final FunctionNode visit(ASTStrStarts node, Object data) throws VisitorException {
    return binary(node, FN.STARTS_WITH);
  }

  @Override
  public final FunctionNode visit(ASTStrEnds node, Object data) throws VisitorException {
    return binary(node, FN.ENDS_WITH);
  }

  @Override
  public final FunctionNode visit(ASTStrLen node, Object data) throws VisitorException {
    return unary(node, FN.STRING_LENGTH);
  }

  @Override
  public final FunctionNode visit(ASTStrAfter node, Object data) throws VisitorException {
    return binary(node, FunctionRegistry.STR_AFTER);
    //        return createFunctionCall(FN.SUBSTRING_AFTER.toString(), node, 2, 2);
  }

  @Override
  public final FunctionNode visit(ASTStrBefore node, Object data) throws VisitorException {
    return binary(node, FunctionRegistry.STR_BEFORE);
    //        return createFunctionCall(FN.SUBSTRING_BEFORE.toString(), node, 2, 2);
  }

  @Override
  public final FunctionNode visit(ASTUpperCase node, Object data) throws VisitorException {
    return unary(node, FN.UPPER_CASE);
  }

  @Override
  public final FunctionNode visit(ASTLowerCase node, Object data) throws VisitorException {
    return unary(node, FN.LOWER_CASE);
  }

  @Override
  public final FunctionNode visit(ASTStrLang node, Object data) throws VisitorException {
    return binary(node, FunctionRegistry.STR_LANG);
  }

  @Override
  public final FunctionNode visit(ASTNow node, Object data) throws VisitorException {
    return noneary(node, FunctionRegistry.NOW);
  }

  @Override
  public final FunctionNode visit(ASTYear node, Object data) throws VisitorException {
    return unary(node, FN.YEAR_FROM_DATETIME);
  }

  @Override
  public final FunctionNode visit(ASTMonth node, Object data) throws VisitorException {
    return unary(node, FN.MONTH_FROM_DATETIME);
  }

  @Override
  public final FunctionNode visit(ASTDay node, Object data) throws VisitorException {
    return unary(node, FN.DAY_FROM_DATETIME);
  }

  @Override
  public final FunctionNode visit(ASTHours node, Object data) throws VisitorException {
    return unary(node, FN.HOURS_FROM_DATETIME);
  }

  @Override
  public final FunctionNode visit(ASTMinutes node, Object data) throws VisitorException {
    return unary(node, FN.MINUTES_FROM_DATETIME);
  }

  @Override
  public final FunctionNode visit(ASTSeconds node, Object data) throws VisitorException {
    return unary(node, FN.SECONDS_FROM_DATETIME);
  }

  @Override
  public final FunctionNode visit(ASTTimezone node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.TIMEZONE);
  }

  @Override
  public final FunctionNode visit(ASTTz node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.TZ);
  }

  @Override
  public final FunctionNode visit(ASTMD5 node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.MD5);
  }

  @Override
  public final FunctionNode visit(ASTSHA1 node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.SHA1);
  }

  @Override
  public final FunctionNode visit(ASTSHA224 node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.SHA224);
  }

  @Override
  public final FunctionNode visit(ASTSHA256 node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.SHA256);
  }

  @Override
  public final FunctionNode visit(ASTSHA384 node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.SHA384);
  }

  @Override
  public final FunctionNode visit(ASTSHA512 node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.SHA512);
  }

  @Override
  public FunctionNode visit(ASTUUID node, Object data) throws VisitorException {
    return noneary(node, FunctionRegistry.UUID);
  }

  @Override
  public FunctionNode visit(ASTSTRUUID node, Object data) throws VisitorException {
    return noneary(node, FunctionRegistry.STRUUID);
  }

  @Override
  public final FunctionNode visit(ASTIRIFunc node, Object data) throws VisitorException {

    return new FunctionNode(
        FunctionRegistry.IRI,
        //                null/* scalarValues */,
        Collections.singletonMap(IriBOp.Annotations.BASE_URI, (Object) node.getBaseURI()),
        new ValueExpressionNode[] {left(node)});

    //        return unary(node, FunctionRegistry.IRI);
    /*
     * FIXME baseURI resolution needs to be handled for IRI functions, most
     * likely in unary(), binary(), nary(), and aggregate().  Write AST
     * layer unit tests for this.
     */
    //        ValueExpr expr = (ValueExpr)node.jjtGetChild(0).jjtAccept(this, null);
    //        IRIFunction fn = new IRIFunction(expr);
    //        fn.setBaseURI(node.getBaseURI());
    //        return fn;
  }

  @Override
  public final FunctionNode visit(ASTLang node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.LANG);
  }

  @Override
  public final FunctionNode visit(ASTDatatype node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.DATATYPE);
  }

  @Override
  public final FunctionNode visit(ASTLangMatches node, Object data) throws VisitorException {
    return binary(node, FunctionRegistry.LANG_MATCHES);
  }

  @Override
  public final FunctionNode visit(ASTBound node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.BOUND);
  }

  @Override
  public final FunctionNode visit(ASTIsIRI node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.IS_IRI);
  }

  @Override
  public final FunctionNode visit(ASTIsBlank node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.IS_BLANK);
  }

  @Override
  public final FunctionNode visit(ASTIsLiteral node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.IS_LITERAL);
  }

  @Override
  public final FunctionNode visit(ASTIsNumeric node, Object data) throws VisitorException {
    return unary(node, FunctionRegistry.IS_NUMERIC);
  }

  /** TODO Same functionURI for BNode() and BNode(Literal)? */
  @Override
  public final FunctionNode visit(ASTBNodeFunc node, Object data) throws VisitorException {
    if (node.jjtGetNumChildren() == 0) {
      return noneary(node, FunctionRegistry.BNODE);
    }
    return unary(node, FunctionRegistry.BNODE);
  }

  @Override
  public final FunctionNode visit(ASTRegexExpression node, Object data) throws VisitorException {
    if (node.jjtGetNumChildren() == 2) {
      return binary(node, FunctionRegistry.REGEX);
    }
    return ternary(node, FunctionRegistry.REGEX);
  }

  @Override
  public FunctionNode visit(ASTReplace node, Object data) throws VisitorException {
    if (node.jjtGetNumChildren() == 3) {
      return ternary(node, FunctionRegistry.REPLACE);
    }
    return quadary(node, FunctionRegistry.REPLACE);
    //        return createFunctionCall(FN.REPLACE.toString(), node, 3, 4);
  }

  /**
   * Note: EXISTS is basically an ASK subquery.
   *
   * @see ExistsNode
   */
  @Override
  public final ExistsNode visit(final ASTExistsFunc node, Object data) throws VisitorException {

    final VarNode anonvar = context.createAnonVar("-exists-");

    /*
     * Use a new (empty) graph pattern to prevent the accept of the child
     * from being attached into the parent's graph pattern context.
     */

    final GroupGraphPattern parentGP = graphPattern;

    graphPattern = scopedGroupGraphPattern(node);

    @SuppressWarnings("unchecked")
    final GraphPatternGroup<IGroupMemberNode> innerGraphPattern =
        (GraphPatternGroup<IGroupMemberNode>)
            node.jjtGetChild(0).jjtAccept(this /* visitor */, null);

    final ExistsNode fn = new ExistsNode(anonvar, innerGraphPattern);

    // Restore the parent's context.
    graphPattern = parentGP;

    return fn;
  }

  /**
   * See EXISTS above.
   *
   * @see NotExistsNode
   */
  @Override
  public final NotExistsNode visit(final ASTNotExistsFunc node, Object data)
      throws VisitorException {

    final VarNode anonvar = context.createAnonVar("-exists-");

    /*
     * Use a new (empty) graph pattern to prevent the accept of the child
     * from being attached into the parent's graph pattern context.
     */

    final GroupGraphPattern parentGP = graphPattern;

    graphPattern = scopedGroupGraphPattern(node);

    @SuppressWarnings("unchecked")
    final GraphPatternGroup<IGroupMemberNode> innerGraphPattern =
        (GraphPatternGroup<IGroupMemberNode>)
            node.jjtGetChild(0).jjtAccept(this /* visitor */, null);

    final NotExistsNode fn = new NotExistsNode(anonvar, innerGraphPattern);

    // Restore the parent's context.
    graphPattern = parentGP;

    return fn;
  }

  @Override
  public final FunctionNode visit(ASTIf node, Object data) throws VisitorException {
    return ternary(node, FunctionRegistry.IF);
  }

  /**
   * Unwrap an {@link ASTInfix} node, returning the inner {@link FunctionNode} constructed for it.
   *
   * <p>Note: Sesame has picked up the notion of an ASTInfix node, but they are handling it slightly
   * differently.
   *
   * @see http://www.openrdf.org/issues/browse/SES-818
   */
  @Override
  public final FunctionNode visit(ASTInfix node, Object data) throws VisitorException {

    final Node left = node.jjtGetChild(0);

    final Node op = node.jjtGetChild(1);

    op.jjtInsertChild(left, 0);

    return (FunctionNode) op.jjtAccept(this, data);
  }

  /**
   * "IN" and "NOT IN" are infix notation operators. The syntax for "IN" is [NumericExpression IN
   * ArgList]. However, the IN operators declared by the {@link FunctionRegistry} require that the
   * outer NumericExpression is their first argument. The function registry optimizes several
   * different cases, including where the set is empty, where it has one member, and where the
   * members of the set are constants and the outer expression is a variable.
   *
   * @see FunctionRegistry#IN
   * @see FunctionRegistry#NOT_IN
   * @see http://www.openrdf.org/issues/browse/SES-818
   */
  @Override
  public final FunctionNode visit(ASTIn node, Object data) throws VisitorException {

    //        final int nargs = node.jjtGetNumChildren();
    //
    //        final ValueExpressionNode[] args = new ValueExpressionNode[nargs + 1];
    //
    //        /*
    //         * Reach up to the parent's 1st child for the left argument to the infix
    //         * IN operator.
    //         */
    //        final ValueExpressionNode leftArg = (ValueExpressionNode) node
    //                .jjtGetParent().jjtGetChild(0).jjtAccept(this, null);
    //
    //        args[0] = leftArg;
    //
    //        /*
    //         * Handle the ArgList for IN.
    //         */
    //
    //        for (int i = 0; i < nargs; i++) {
    //
    //            final Node argNode = node.jjtGetChild(i);
    //
    //            args[i + 1] = (ValueExpressionNode) argNode.jjtAccept(this, null);
    //
    //        }

    /*
     * Handle the ArgList for IN.
     *
     * Note: This assumes that the left argument of IN has been rotated into
     * place as its first child.
     *
     * @see InfixProcessor
     */

    final int nargs = node.jjtGetNumChildren();

    final ValueExpressionNode[] args = new ValueExpressionNode[nargs];

    for (int i = 0; i < nargs; i++) {

      final Node argNode = node.jjtGetChild(i);

      args[i] = (ValueExpressionNode) argNode.jjtAccept(this, null);
    }

    return new FunctionNode(FunctionRegistry.IN, null /* scalarValues */, args);
  }

  /** See IN above. */
  @Override
  public final FunctionNode visit(ASTNotIn node, Object data) throws VisitorException {
    //
    //        final int nargs = node.jjtGetNumChildren();
    //
    //        final ValueExpressionNode[] args = new ValueExpressionNode[nargs + 1];
    //
    //        /*
    //         * Reach up to the parent's 1st child for the left argument to the infix
    //         * NOT IN operator.
    //         */
    //        final ValueExpressionNode leftArg = (ValueExpressionNode) node
    //                .jjtGetParent().jjtGetChild(0).jjtAccept(this, null);
    //
    //        args[0] = leftArg;
    //
    //        /*
    //         * Handle the ArgList for NOT IN.
    //         */
    //
    //        for (int i = 0; i < nargs; i++) {
    //
    //            final Node argNode = node.jjtGetChild(i);
    //
    //            args[i + 1] = (ValueExpressionNode) argNode.jjtAccept(this, null);
    //
    //        }

    /*
     * Handle the ArgList for NOT_IN.
     *
     * Note: This assumes that the left argument of the NOT_IN has been
     * rotated into place as its first child.
     */

    final int nargs = node.jjtGetNumChildren();

    final ValueExpressionNode[] args = new ValueExpressionNode[nargs];

    for (int i = 0; i < nargs; i++) {

      final Node argNode = node.jjtGetChild(i);

      args[i] = (ValueExpressionNode) argNode.jjtAccept(this, null);
    }

    return new FunctionNode(FunctionRegistry.NOT_IN, null /* scalarValues */, args);
  }

  /**
   * Aggregate value expressions in GROUP BY clause. The content of this production can be a {@link
   * VarNode}, {@link AssignmentNode}, or {@link FunctionNode} (which handles both built-in
   * functions and extension functions). However, we always wrap it as an {@link AssignmentNode} and
   * return that. A {@link VarNode} will just bind itself. A bare {@link FunctionNode} will bind an
   * anonymous variable.
   */
  @Override
  public final AssignmentNode visit(final ASTGroupCondition node, Object data)
      throws VisitorException {

    // The first value-expression.
    final IValueExpressionNode ve =
        (IValueExpressionNode) node.jjtGetChild(0).jjtAccept(this, data);

    if (node.jjtGetNumChildren() == 2) {

      /*
       * If there are two value expressions, then BIND(ve,ve2).
       */
      final IValueExpressionNode asVar =
          (IValueExpressionNode) node.jjtGetChild(1).jjtAccept(this, data);

      return new AssignmentNode((VarNode) asVar, ve);
    }

    if (ve instanceof VarNode) {

      // Assign to self.
      return new AssignmentNode((VarNode) ve, (VarNode) ve);
    }

    // Wrap with assignment to an anonymous variable.
    return new AssignmentNode(context.createAnonVar("-groupBy-"), ve);
  }

  /*
   * Aggregate functions. Each accepts a scalar boolean "distinct" argument.
   * In addition, COUNT may be used with "*" as the inner expression.
   */

  @Override
  public final FunctionNode visit(ASTCount node, Object data) throws VisitorException {
    return aggregate(node, FunctionRegistry.COUNT);
  }

  @Override
  public final FunctionNode visit(ASTMax node, Object data) throws VisitorException {
    return aggregate(node, FunctionRegistry.MAX);
  }

  @Override
  public final FunctionNode visit(ASTMin node, Object data) throws VisitorException {
    return aggregate(node, FunctionRegistry.MIN);
  }

  @Override
  public final FunctionNode visit(ASTSum node, Object data) throws VisitorException {
    return aggregate(node, FunctionRegistry.SUM);
  }

  @Override
  public final FunctionNode visit(ASTAvg node, Object data) throws VisitorException {
    return aggregate(node, FunctionRegistry.AVERAGE);
  }

  @Override
  public final FunctionNode visit(ASTSample node, Object data) throws VisitorException {
    return aggregate(node, FunctionRegistry.SAMPLE);
  }

  /**
   * TODO additional scalar values (sparql.jjt specifies "separator EQ" as a constant in the
   * grammar, but we support additional scalar values for {@link GROUP_CONCAT}. Also, the grammar is
   * permissive and allows Expression for separator rather than a quoted string (per the W3C draft).
   */
  @Override
  public final FunctionNode visit(ASTGroupConcat node, Object data) throws VisitorException {

    Map<String, Object> scalarValues = new LinkedHashMap<String, Object>();

    if (node.isDistinct()) {

      scalarValues.put(AggregateBase.Annotations.DISTINCT, Boolean.TRUE);
    }

    if (node.jjtGetNumChildren() > 1) {

      final ConstantNode separator = (ConstantNode) node.jjtGetChild(1).jjtAccept(this, data);

      scalarValues.put(GROUP_CONCAT.Annotations.SEPARATOR, separator.getValue().stringValue());
    }

    return new FunctionNode(
        FunctionRegistry.GROUP_CONCAT, scalarValues, new ValueExpressionNode[] {left(node)});
  }
}
