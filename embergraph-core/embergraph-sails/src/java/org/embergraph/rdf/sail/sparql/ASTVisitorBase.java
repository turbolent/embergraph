/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.embergraph.rdf.sail.sparql;

import org.embergraph.rdf.sail.sparql.ast.ASTAbs;
import org.embergraph.rdf.sail.sparql.ast.ASTAdd;
import org.embergraph.rdf.sail.sparql.ast.ASTAnd;
import org.embergraph.rdf.sail.sparql.ast.ASTAskQuery;
import org.embergraph.rdf.sail.sparql.ast.ASTAvg;
import org.embergraph.rdf.sail.sparql.ast.ASTBNodeFunc;
import org.embergraph.rdf.sail.sparql.ast.ASTBaseDecl;
import org.embergraph.rdf.sail.sparql.ast.ASTBasicGraphPattern;
import org.embergraph.rdf.sail.sparql.ast.ASTBind;
import org.embergraph.rdf.sail.sparql.ast.ASTBindingSet;
import org.embergraph.rdf.sail.sparql.ast.ASTBindingValue;
import org.embergraph.rdf.sail.sparql.ast.ASTBindingsClause;
import org.embergraph.rdf.sail.sparql.ast.ASTBlankNode;
import org.embergraph.rdf.sail.sparql.ast.ASTBlankNodePropertyList;
import org.embergraph.rdf.sail.sparql.ast.ASTBound;
import org.embergraph.rdf.sail.sparql.ast.ASTCeil;
import org.embergraph.rdf.sail.sparql.ast.ASTClear;
import org.embergraph.rdf.sail.sparql.ast.ASTCoalesce;
import org.embergraph.rdf.sail.sparql.ast.ASTCollection;
import org.embergraph.rdf.sail.sparql.ast.ASTCompare;
import org.embergraph.rdf.sail.sparql.ast.ASTConcat;
import org.embergraph.rdf.sail.sparql.ast.ASTConstraint;
import org.embergraph.rdf.sail.sparql.ast.ASTConstruct;
import org.embergraph.rdf.sail.sparql.ast.ASTConstructQuery;
import org.embergraph.rdf.sail.sparql.ast.ASTContains;
import org.embergraph.rdf.sail.sparql.ast.ASTCopy;
import org.embergraph.rdf.sail.sparql.ast.ASTCount;
import org.embergraph.rdf.sail.sparql.ast.ASTCreate;
import org.embergraph.rdf.sail.sparql.ast.ASTCreateEntailments;
import org.embergraph.rdf.sail.sparql.ast.ASTDatasetClause;
import org.embergraph.rdf.sail.sparql.ast.ASTDatatype;
import org.embergraph.rdf.sail.sparql.ast.ASTDay;
import org.embergraph.rdf.sail.sparql.ast.ASTDeleteClause;
import org.embergraph.rdf.sail.sparql.ast.ASTDeleteData;
import org.embergraph.rdf.sail.sparql.ast.ASTDeleteWhere;
import org.embergraph.rdf.sail.sparql.ast.ASTDescribe;
import org.embergraph.rdf.sail.sparql.ast.ASTDescribeQuery;
import org.embergraph.rdf.sail.sparql.ast.ASTDisableEntailments;
import org.embergraph.rdf.sail.sparql.ast.ASTDrop;
import org.embergraph.rdf.sail.sparql.ast.ASTDropEntailments;
import org.embergraph.rdf.sail.sparql.ast.ASTEnableEntailments;
import org.embergraph.rdf.sail.sparql.ast.ASTEncodeForURI;
import org.embergraph.rdf.sail.sparql.ast.ASTExistsFunc;
import org.embergraph.rdf.sail.sparql.ast.ASTFalse;
import org.embergraph.rdf.sail.sparql.ast.ASTFloor;
import org.embergraph.rdf.sail.sparql.ast.ASTFunctionCall;
import org.embergraph.rdf.sail.sparql.ast.ASTGraphGraphPattern;
import org.embergraph.rdf.sail.sparql.ast.ASTGraphOrDefault;
import org.embergraph.rdf.sail.sparql.ast.ASTGraphPatternGroup;
import org.embergraph.rdf.sail.sparql.ast.ASTGraphRefAll;
import org.embergraph.rdf.sail.sparql.ast.ASTGroupClause;
import org.embergraph.rdf.sail.sparql.ast.ASTGroupConcat;
import org.embergraph.rdf.sail.sparql.ast.ASTGroupCondition;
import org.embergraph.rdf.sail.sparql.ast.ASTHavingClause;
import org.embergraph.rdf.sail.sparql.ast.ASTHours;
import org.embergraph.rdf.sail.sparql.ast.ASTIRI;
import org.embergraph.rdf.sail.sparql.ast.ASTIRIFunc;
import org.embergraph.rdf.sail.sparql.ast.ASTIf;
import org.embergraph.rdf.sail.sparql.ast.ASTIn;
import org.embergraph.rdf.sail.sparql.ast.ASTInfix;
import org.embergraph.rdf.sail.sparql.ast.ASTInlineData;
import org.embergraph.rdf.sail.sparql.ast.ASTInsertClause;
import org.embergraph.rdf.sail.sparql.ast.ASTInsertData;
import org.embergraph.rdf.sail.sparql.ast.ASTIsBlank;
import org.embergraph.rdf.sail.sparql.ast.ASTIsIRI;
import org.embergraph.rdf.sail.sparql.ast.ASTIsLiteral;
import org.embergraph.rdf.sail.sparql.ast.ASTIsNumeric;
import org.embergraph.rdf.sail.sparql.ast.ASTLang;
import org.embergraph.rdf.sail.sparql.ast.ASTLangMatches;
import org.embergraph.rdf.sail.sparql.ast.ASTLet;
import org.embergraph.rdf.sail.sparql.ast.ASTLimit;
import org.embergraph.rdf.sail.sparql.ast.ASTLoad;
import org.embergraph.rdf.sail.sparql.ast.ASTLowerCase;
import org.embergraph.rdf.sail.sparql.ast.ASTMD5;
import org.embergraph.rdf.sail.sparql.ast.ASTMath;
import org.embergraph.rdf.sail.sparql.ast.ASTMax;
import org.embergraph.rdf.sail.sparql.ast.ASTMin;
import org.embergraph.rdf.sail.sparql.ast.ASTMinusGraphPattern;
import org.embergraph.rdf.sail.sparql.ast.ASTMinutes;
import org.embergraph.rdf.sail.sparql.ast.ASTModify;
import org.embergraph.rdf.sail.sparql.ast.ASTMonth;
import org.embergraph.rdf.sail.sparql.ast.ASTMove;
import org.embergraph.rdf.sail.sparql.ast.ASTNamedSubquery;
import org.embergraph.rdf.sail.sparql.ast.ASTNamedSubqueryInclude;
import org.embergraph.rdf.sail.sparql.ast.ASTNot;
import org.embergraph.rdf.sail.sparql.ast.ASTNotExistsFunc;
import org.embergraph.rdf.sail.sparql.ast.ASTNotIn;
import org.embergraph.rdf.sail.sparql.ast.ASTNow;
import org.embergraph.rdf.sail.sparql.ast.ASTNumericLiteral;
import org.embergraph.rdf.sail.sparql.ast.ASTObjectList;
import org.embergraph.rdf.sail.sparql.ast.ASTOffset;
import org.embergraph.rdf.sail.sparql.ast.ASTOptionalGraphPattern;
import org.embergraph.rdf.sail.sparql.ast.ASTOr;
import org.embergraph.rdf.sail.sparql.ast.ASTOrderClause;
import org.embergraph.rdf.sail.sparql.ast.ASTOrderCondition;
import org.embergraph.rdf.sail.sparql.ast.ASTPathAlternative;
import org.embergraph.rdf.sail.sparql.ast.ASTPathElt;
import org.embergraph.rdf.sail.sparql.ast.ASTPathMod;
import org.embergraph.rdf.sail.sparql.ast.ASTPathOneInPropertySet;
import org.embergraph.rdf.sail.sparql.ast.ASTPathSequence;
import org.embergraph.rdf.sail.sparql.ast.ASTPrefixDecl;
import org.embergraph.rdf.sail.sparql.ast.ASTProjectionElem;
import org.embergraph.rdf.sail.sparql.ast.ASTPropertyList;
import org.embergraph.rdf.sail.sparql.ast.ASTPropertyListPath;
import org.embergraph.rdf.sail.sparql.ast.ASTQName;
import org.embergraph.rdf.sail.sparql.ast.ASTQuadsNotTriples;
import org.embergraph.rdf.sail.sparql.ast.ASTQueryContainer;
import org.embergraph.rdf.sail.sparql.ast.ASTRDFLiteral;
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
import org.embergraph.rdf.sail.sparql.ast.ASTSelect;
import org.embergraph.rdf.sail.sparql.ast.ASTSelectQuery;
import org.embergraph.rdf.sail.sparql.ast.ASTServiceGraphPattern;
import org.embergraph.rdf.sail.sparql.ast.ASTSolutionsRef;
import org.embergraph.rdf.sail.sparql.ast.ASTStr;
import org.embergraph.rdf.sail.sparql.ast.ASTStrAfter;
import org.embergraph.rdf.sail.sparql.ast.ASTStrBefore;
import org.embergraph.rdf.sail.sparql.ast.ASTStrDt;
import org.embergraph.rdf.sail.sparql.ast.ASTStrEnds;
import org.embergraph.rdf.sail.sparql.ast.ASTStrLang;
import org.embergraph.rdf.sail.sparql.ast.ASTStrLen;
import org.embergraph.rdf.sail.sparql.ast.ASTStrStarts;
import org.embergraph.rdf.sail.sparql.ast.ASTString;
import org.embergraph.rdf.sail.sparql.ast.ASTSubstr;
import org.embergraph.rdf.sail.sparql.ast.ASTSum;
import org.embergraph.rdf.sail.sparql.ast.ASTTRefPattern;
import org.embergraph.rdf.sail.sparql.ast.ASTTimezone;
import org.embergraph.rdf.sail.sparql.ast.ASTTriplesSameSubject;
import org.embergraph.rdf.sail.sparql.ast.ASTTriplesSameSubjectPath;
import org.embergraph.rdf.sail.sparql.ast.ASTTrue;
import org.embergraph.rdf.sail.sparql.ast.ASTTz;
import org.embergraph.rdf.sail.sparql.ast.ASTUUID;
import org.embergraph.rdf.sail.sparql.ast.ASTUnionGraphPattern;
import org.embergraph.rdf.sail.sparql.ast.ASTUnparsedQuadDataBlock;
import org.embergraph.rdf.sail.sparql.ast.ASTUpdate;
import org.embergraph.rdf.sail.sparql.ast.ASTUpdateContainer;
import org.embergraph.rdf.sail.sparql.ast.ASTUpdateSequence;
import org.embergraph.rdf.sail.sparql.ast.ASTUpperCase;
import org.embergraph.rdf.sail.sparql.ast.ASTVar;
import org.embergraph.rdf.sail.sparql.ast.ASTWhereClause;
import org.embergraph.rdf.sail.sparql.ast.ASTYear;
import org.embergraph.rdf.sail.sparql.ast.SimpleNode;
import org.embergraph.rdf.sail.sparql.ast.SyntaxTreeBuilderVisitor;
import org.embergraph.rdf.sail.sparql.ast.VisitorException;

/*
* Base class for visitors of the SPARQL AST.
 *
 * @author arjohn
 * @openrdf
 */
public abstract class ASTVisitorBase implements SyntaxTreeBuilderVisitor {

  public Object visit(ASTAbs node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTUpdateSequence node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTBindingValue node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTInlineData node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTUnparsedQuadDataBlock node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTUpdateContainer node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTAdd node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTBindingSet node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTClear node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTCopy node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTCreate node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTDeleteClause node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTDeleteData node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTDeleteWhere node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTDrop node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTGraphOrDefault node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTGraphRefAll node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTInfix node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTInsertClause node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTInsertData node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTLoad node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTModify node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTMove node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTNow node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTYear node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTMonth node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTDay node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTHours node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTTz node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTMinutes node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSeconds node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTTimezone node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTAnd node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTAskQuery node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTAvg node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTMD5 node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSHA1 node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSHA224 node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSHA256 node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSHA384 node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSHA512 node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTBaseDecl node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTBasicGraphPattern node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTBind node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTLet node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTBindingsClause node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTBlankNode node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTBlankNodePropertyList node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTBNodeFunc node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTBound node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTCeil node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTCoalesce node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTConcat node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTContains node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTCollection node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTCompare node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTConstraint node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTConstruct node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTConstructQuery node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTCount node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTDatasetClause node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTDatatype node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTDescribe node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTDescribeQuery node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTExistsFunc node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTEncodeForURI node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTFalse node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTFloor node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTFunctionCall node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTGraphGraphPattern node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTGraphPatternGroup node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTGroupClause node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTGroupConcat node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTGroupCondition node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTHavingClause node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTIf node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTIn node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTIRI node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTIRIFunc node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTIsBlank node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTIsIRI node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTIsLiteral node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTIsNumeric node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTLang node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTLangMatches node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTLimit node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTLowerCase node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTMath node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTMax node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTMin node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTMinusGraphPattern node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTNot node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTNotExistsFunc node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTNotIn node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTNumericLiteral node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTObjectList node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTOffset node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTOptionalGraphPattern node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTOr node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTOrderClause node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTOrderCondition node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTPathAlternative node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTPathElt node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTPathMod node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTPathOneInPropertySet node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTPathSequence node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTPrefixDecl node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTProjectionElem node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTPropertyList node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTPropertyListPath node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTQName node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTQueryContainer node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTRand node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTRDFLiteral node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTRegexExpression node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTReplace node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTRound node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSameTerm node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSample node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSelect node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSelectQuery node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTServiceGraphPattern node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTStr node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTStrAfter node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTStrBefore node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTStrDt node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTStrEnds node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTString node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTUUID node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSTRUUID node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTStrLang node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTStrLen node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTStrStarts node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSubstr node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSum node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTTriplesSameSubject node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTTriplesSameSubjectPath node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTQuadsNotTriples node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTTrue node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTUnionGraphPattern node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTUpdate node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTUpperCase node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTVar node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTWhereClause node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(SimpleNode node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTNamedSubquery node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTNamedSubqueryInclude node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTSolutionsRef node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTTRefPattern node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTCreateEntailments node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTDisableEntailments node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTDropEntailments node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }

  public Object visit(ASTEnableEntailments node, Object data) throws VisitorException {
    return node.childrenAccept(this, data);
  }
}
