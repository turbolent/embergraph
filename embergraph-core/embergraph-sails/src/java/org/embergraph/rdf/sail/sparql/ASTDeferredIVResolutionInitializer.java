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
 * Created on Aug 24, 2011
 */

package org.embergraph.rdf.sail.sparql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.embergraph.rdf.internal.DTE;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.internal.impl.literal.FullyInlineTypedLiteralIV;
import org.embergraph.rdf.internal.impl.literal.UUIDLiteralIV;
import org.embergraph.rdf.internal.impl.literal.XSDBooleanIV;
import org.embergraph.rdf.internal.impl.literal.XSDDecimalIV;
import org.embergraph.rdf.internal.impl.literal.XSDIntegerIV;
import org.embergraph.rdf.internal.impl.literal.XSDNumericIV;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedByteIV;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedIntIV;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedLongIV;
import org.embergraph.rdf.internal.impl.literal.XSDUnsignedShortIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.sail.sparql.ast.ASTBlankNode;
import org.embergraph.rdf.sail.sparql.ast.ASTFalse;
import org.embergraph.rdf.sail.sparql.ast.ASTIRI;
import org.embergraph.rdf.sail.sparql.ast.ASTNumericLiteral;
import org.embergraph.rdf.sail.sparql.ast.ASTOperationContainer;
import org.embergraph.rdf.sail.sparql.ast.ASTQName;
import org.embergraph.rdf.sail.sparql.ast.ASTRDFLiteral;
import org.embergraph.rdf.sail.sparql.ast.ASTRDFValue;
import org.embergraph.rdf.sail.sparql.ast.ASTString;
import org.embergraph.rdf.sail.sparql.ast.ASTTrue;
import org.embergraph.rdf.sail.sparql.ast.VisitorException;
import org.embergraph.rdf.store.BD;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.MalformedQueryException;

/*
 * Visits the AST model and builds a map from each RDF {@link Value} to {@link EmbergraphValue}
 * objects that have mock IVs assigned to them.
 *
 * <p>Note: The {@link PrefixDeclProcessor} will rewrite {@link ASTQName} nodes as {@link ASTIRI}
 * nodes. It MUST run before this processor.
 *
 * <p>Note: Any {@link ASTRDFLiteral} or {@link ASTIRI} nodes are annotated by this processor using
 * {@link ASTRDFValue#setRDFValue(Value)}. This includes IRIrefs in the {@link ASTDatasetClause},
 * which are matched as either {@link ASTIRI} or {@link ASTQName}.
 *
 * <p>Note: This is a part of deferred IV batch resolution, which is intended to replace the
 * functionality of the {@link EmbergraphValueReplacer}.
 *
 * <p>Note: {@link IValueExpression} nodes used in {@link SPARQLConstraint}s are allowed to use
 * values not actually in the database. MP
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @openrdf
 * @see https://jira.blazegraph.com/browse/BLZG-1176 (decouple SPARQL parser from DB)
 * @see https://jira.blazegraph.com/browse/BLZG-1519 (Refactor test suite to remove tight coupling
 *     with IVs while checking up parsed queries)
 */
public class ASTDeferredIVResolutionInitializer extends ASTVisitorBase {

  private static final Logger log = Logger.getLogger(ASTDeferredIVResolutionInitializer.class);

  private static final boolean INFO = log.isInfoEnabled();

  private static final List<URI> RDF_VOCAB =
      Arrays.asList(RDF.FIRST, RDF.REST, RDF.NIL, BD.VIRTUAL_GRAPH);

  private final Map<Value, EmbergraphValue> vocab;

  private final EmbergraphValueFactory valueFactory;

  private final LinkedHashMap<ASTRDFValue, EmbergraphValue> nodes;

  /*
   * Return a map from openrdf {@link Value} objects to the corresponding {@link EmbergraphValue}
   * objects for all {@link Value}s that appear in the parse tree.
   */
  public Map<Value, EmbergraphValue> getValues() {

    return vocab;
  }

  public ASTDeferredIVResolutionInitializer() {

    // Unnamed EmbergraphValueFactory is used to provide instances
    // of EmbergraphValue, which are required by existing test suite.
    // See also task https://jira.blazegraph.com/browse/BLZG-1519
    //        this.valueFactory =
    // EmbergraphValueFactoryImpl.getInstance("parser"+UUID.randomUUID().toString().replaceAll("-",
    // ""));
    this.valueFactory = new EmbergraphValueFactoryImpl();

    this.nodes = new LinkedHashMap<>();

    this.vocab = new LinkedHashMap<>();
  }

  /*
   * Visit the parse tree, locating and collecting references to all {@link ASTRDFValue} nodes
   * (including blank nodes iff we are in a told bnodes mode). The {@link ASTRDFValue}s are
   * collected in a {@link Map} which associates each one with a {@link EmbergraphValue} object
   * which is set using {@link ASTRDFValue#setRDFValue(org.openrdf.model.Value)}. The {@link
   * EmbergraphValue}s will be resolved later (in ASTDeferredIVResolution) in a batch against the
   * database, obtaining their {@link IVs}. Until then {@link EmbergraphValue}s in the parse tree
   * have unresolved {@link IV}s (TermID(0)).
   *
   * @param qc
   * @throws MalformedQueryException
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void process(final ASTOperationContainer qc) throws MalformedQueryException {

    try {

      /*
       * Collect all ASTRDFValue nodes into a map, paired with
       * EmbergraphValue objects.
       */
      qc.jjtAccept(new RDFValueResolver(), null);

    } catch (final VisitorException e) {

      // Turn the exception into a Query exception.
      throw new MalformedQueryException(e);
    }

    {

      /*
       * RDF Values actually appearing in the parse tree.
       */
      final Iterator<Entry<ASTRDFValue, EmbergraphValue>> itr = nodes.entrySet().iterator();

      while (itr.hasNext()) {

        final Entry<ASTRDFValue, EmbergraphValue> entry = itr.next();

        final ASTRDFValue value = entry.getKey();

        IV iv = null;
        EmbergraphValue embergraphValue = null;
        if (value.getRDFValue() != null
            && ((EmbergraphValue) value.getRDFValue()).getIV() != null) {
          embergraphValue = (EmbergraphValue) value.getRDFValue();
          iv = embergraphValue.getIV();
        } else if (value instanceof ASTIRI) {
          iv = new TermId<>(VTE.URI, 0);
          embergraphValue = valueFactory.createURI(((ASTIRI) value).getValue());
          if (!embergraphValue.isRealIV()) {
            embergraphValue.clearInternalValue();
            embergraphValue.setIV(iv);
          }
          iv.setValue(embergraphValue);
        } else if (value instanceof ASTRDFLiteral) {
          final ASTRDFLiteral rdfNode = (ASTRDFLiteral) value;
          final String lang = rdfNode.getLang();
          final ASTIRI dataTypeIri = rdfNode.getDatatype();
          URIImpl dataTypeUri = null;
          DTE dte = null;
          if (dataTypeIri != null && dataTypeIri.getValue() != null) {
            dataTypeUri = new URIImpl(dataTypeIri.getValue());
            dte = DTE.valueOf(dataTypeUri);
          }
          if (dte != null) {
            embergraphValue = getEmbergraphValue(rdfNode.getLabel().getValue(), dte);
            if (!embergraphValue.stringValue().equals(rdfNode.getLabel().getValue())) {
              // Data loss could occur if inline IV will be used, as string representation of
              // original value differ from decoded value
              embergraphValue =
                  valueFactory.createLiteral(rdfNode.getLabel().getValue(), dataTypeUri);
              iv = TermId.mockIV(VTE.valueOf(embergraphValue));
              embergraphValue.setIV(iv);
              iv.setValue(embergraphValue);
            }
          } else {
            iv = new TermId<>(VTE.LITERAL, 0);
            if (lang != null) {
              embergraphValue = valueFactory.createLiteral(rdfNode.getLabel().getValue(), lang);
            } else {
              embergraphValue =
                  valueFactory.createLiteral(rdfNode.getLabel().getValue(), dataTypeUri);
            }
            iv.setValue(embergraphValue);
            embergraphValue.setIV(iv);
          }
        } else if (value instanceof ASTNumericLiteral) {
          final ASTNumericLiteral rdfNode = (ASTNumericLiteral) value;
          final URI dataTypeUri = rdfNode.getDatatype();
          final DTE dte = DTE.valueOf(dataTypeUri);
          embergraphValue = getEmbergraphValue(rdfNode.getValue(), dte);
          if (!embergraphValue.stringValue().equals(rdfNode.getValue())) {
            // Data loss could occur if inline IV will be used, as string representation of original
            // value differ from decoded value
            //                        iv = embergraphValue.getIV();
            embergraphValue = valueFactory.createLiteral(rdfNode.getValue(), dataTypeUri);
            //                        embergraphValue.setIV(iv);
          }
        } else if (value instanceof ASTTrue) {
          embergraphValue = valueFactory.createLiteral(true);
          if (embergraphValue.isRealIV()) {
            iv = embergraphValue.getIV();
          } else {
            iv = TermId.mockIV(VTE.valueOf(embergraphValue));
            iv.setValue(embergraphValue);
            embergraphValue.setIV(iv);
          }
        } else if (value instanceof ASTFalse) {
          embergraphValue = valueFactory.createLiteral(false);
          if (embergraphValue.isRealIV()) {
            iv = embergraphValue.getIV();
          } else {
            iv = TermId.mockIV(VTE.valueOf(embergraphValue));
            iv.setValue(embergraphValue);
            embergraphValue.setIV(iv);
          }
        } else {
          iv = new FullyInlineTypedLiteralIV<>(value.toString(), true);
          embergraphValue = iv.getValue();
        }

        if (embergraphValue != null) {
          value.setRDFValue(embergraphValue);
          // filling in a dummy IV for EmbergraphExprBuilder
          // @see https://jira.blazegraph.com/browse/BLZG-1717 (IV not resolved)
          fillInDummyIV(embergraphValue);
          vocab.put(embergraphValue, embergraphValue);
        }
      }
    }

    /*
     * FIXME Why is this [vocab] still here? And why the IV assignment logic
     * if we are not doing any batch resolution?
     */

    // RDF Collection syntactic sugar vocabulary items.
    for (Value value : RDF_VOCAB) {
      EmbergraphValue embergraphValue = valueFactory.asValue(value);
      fillInDummyIV(embergraphValue);
      vocab.put(value, embergraphValue);
    }
  }

  /*
   * Note: Batch resolution the EmbergraphValue objects against the database
   * DOES NOT happen here. It will be done in ASTDeferredIVResolution.
   * Mock IVs used until then.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private void fillInDummyIV(EmbergraphValue value) {
    final IV iv = value.getIV();

    if (iv == null) {

      /*
       * Since the term identifier is NULL this value is not known
       * to the kb.
       */

      if (INFO) log.info("Not in knowledge base: " + value);

      /*
       * Create a dummy iv and cache the unknown value on it so
       * that it can be used during query evaluation.
       */
      final IV dummyIV = TermId.mockIV(VTE.valueOf(value));

      value.setIV(dummyIV);

      dummyIV.setValue(value);

    } else {

      iv.setValue(value);
    }
  }

  /*
   * Reconstructs EmbergraphValue out of IV, creating literals if needed
   *
   * <p>{@link IVUtility#decode(String, String)} is used by {@link
   * ASTDeferredIVResolutionInitializer} to convert parsed AST objects (ASTRDFLiteral and
   * ASTNumericalLiteral) to IVs wrapped up as EmbergraphValues, which are required on later stages
   * of processing.
   *
   * <p>There's no LexiconRelation available at this point, so all values converted in inlined mode.
   * {@link ASTDeferredIVResolution} converts these inlined IVs to term IV by
   * getLexiconRelation().addTerms in case if triple store configured to not use inlined values.
   *
   * @param iv the IV
   * @param dte data type of IV
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private EmbergraphValue getEmbergraphValue(final String value, final DTE dte) {
    // Check if lexical form is empty, and provide embergraph value
    // with FullyInlineTypedLiteralIV holding corresponding data type
    // @see https://jira.blazegraph.com/browse/BLZG-1716 (SPARQL Update parser fails on invalid
    // numeric literals)
    if (value.isEmpty()) {
      EmbergraphLiteral embergraphValue = valueFactory.createLiteral(value, dte.getDatatypeURI());
      IV iv =
          new FullyInlineTypedLiteralIV<>("", null, dte.getDatatypeURI(), true);
      embergraphValue.setIV(iv);
      iv.setValue(embergraphValue);
      return embergraphValue;
    }
    final IV iv = decode(value, dte.name());
    EmbergraphValue embergraphValue;
    if (!iv.hasValue() && iv instanceof AbstractLiteralIV) {
      switch (dte) {
        case XSDByte:
          embergraphValue = valueFactory.createLiteral(((AbstractLiteralIV) iv).byteValue());
          break;
        case XSDShort:
          embergraphValue = valueFactory.createLiteral(((AbstractLiteralIV) iv).shortValue());
          break;
        case XSDInt:
          embergraphValue = valueFactory.createLiteral(((AbstractLiteralIV) iv).intValue());
          break;
        case XSDLong:
          embergraphValue = valueFactory.createLiteral(((AbstractLiteralIV) iv).longValue());
          break;
        case XSDFloat:
          embergraphValue = valueFactory.createLiteral(((AbstractLiteralIV) iv).floatValue());
          break;
        case XSDDouble:
          embergraphValue = valueFactory.createLiteral(((AbstractLiteralIV) iv).doubleValue());
          break;
        case XSDBoolean:
          embergraphValue = valueFactory.createLiteral(((AbstractLiteralIV) iv).booleanValue());
          break;
        case XSDString:
          embergraphValue = valueFactory.createLiteral(iv.stringValue(), dte.getDatatypeURI());
          break;
        case XSDInteger:
          embergraphValue = valueFactory.createLiteral(iv.stringValue(), XMLSchema.INTEGER);
          break;
        case XSDDecimal:
          embergraphValue =
              valueFactory.createLiteral(iv.stringValue(), DTE.XSDDecimal.getDatatypeURI());
          break;
        case XSDUnsignedShort:
          embergraphValue =
              valueFactory.createLiteral(iv.stringValue(), DTE.XSDUnsignedShort.getDatatypeURI());
          break;
        case XSDUnsignedInt:
          embergraphValue =
              valueFactory.createLiteral(iv.stringValue(), DTE.XSDUnsignedInt.getDatatypeURI());
          break;
        case XSDUnsignedByte:
          embergraphValue =
              valueFactory.createLiteral(iv.stringValue(), DTE.XSDUnsignedByte.getDatatypeURI());
          break;
        case XSDUnsignedLong:
          embergraphValue =
              valueFactory.createLiteral(iv.stringValue(), DTE.XSDUnsignedLong.getDatatypeURI());
          break;
        default:
          throw new RuntimeException("unknown DTE " + dte);
      }
      embergraphValue.setIV(iv);
      iv.setValue(embergraphValue);
    } else {
      embergraphValue = iv.getValue();
    }
    return embergraphValue;
  }

  /*
   * FIXME Should this be using the {@link LexiconConfiguration} to create appropriate inline {@link
   * IV}s when and where appropriate?
   */
  private class RDFValueResolver extends ASTVisitorBase {

    @Override
    public Object visit(final ASTQName node, final Object data) throws VisitorException {

      throw new VisitorException("QNames must be resolved before resolving RDF Values");
    }

    /*
     * Note: Blank nodes within a QUERY are treated as anonymous variables, even when we are in a
     * told bnodes mode.
     */
    @Override
    public Object visit(final ASTBlankNode node, final Object data) throws VisitorException {

      throw new VisitorException(
          "Blank nodes must be replaced with variables before resolving RDF Values");
    }

    @Override
    public Void visit(final ASTIRI node, final Object data) throws VisitorException {

      try {

        nodes.put(node, valueFactory.createURI(node.getValue()));

        return null;

      } catch (final IllegalArgumentException e) {

        // invalid URI
        throw new VisitorException(e.getMessage());
      }
    }

    @Override
    public Void visit(final ASTRDFLiteral node, final Object data) throws VisitorException {

      // Note: This is handled by this ASTVisitor (see below in this
      // class).
      final String label = (String) node.getLabel().jjtAccept(this, null);

      final String lang = node.getLang();

      final ASTIRI datatypeNode = node.getDatatype();

      final EmbergraphLiteral literal;

      if (datatypeNode != null) {

        final URI datatype;

        try {

          datatype = valueFactory.createURI(datatypeNode.getValue());

        } catch (final IllegalArgumentException e) {

          // invalid URI
          throw new VisitorException(e);
        }

        literal = valueFactory.createLiteral(label, datatype);

      } else if (lang != null) {

        literal = valueFactory.createLiteral(label, lang);

      } else {

        literal = valueFactory.createLiteral(label);
      }

      nodes.put(node, literal);

      return null;
    }

    @Override
    public Void visit(final ASTNumericLiteral node, final Object data) {

      nodes.put(node, valueFactory.createLiteral(node.getValue(), node.getDatatype()));

      return null;
    }

    @Override
    public Void visit(final ASTTrue node, final Object data) {

      nodes.put(node, valueFactory.createLiteral(true));

      return null;
    }

    @Override
    public Void visit(final ASTFalse node, final Object data) {

      nodes.put(node, valueFactory.createLiteral(false));

      return null;
    }

    /** Note: This supports the visitor method for a Literal. */
    @Override
    public String visit(final ASTString node, final Object data) {

      return node.getValue();
    }
  }

  /*
   * Decode an IV from its string representation and type, provided in as ASTRDFLiteral node in AST
   * model.
   *
   * <p>Note: This is a very special case method. Normally logic should go through the
   * ILexiconRelation to resolve inline IVs. This always uses inline IVs, and thus defeats the
   * ILexiconConfiguration for the namespace.
   *
   * @param val the string representation
   * @param type value type
   * @return the IV
   * @see https://jira.blazegraph.com/browse/BLZG-1176 (SPARQL QUERY/UPDATE should not use db
   *     connection)
   *     <p>This method was moved from IVUtility class, as it is not used anywhere except AST
   *     Deferred resolution
   */
  @SuppressWarnings("rawtypes")
  public static IV decode(final String val, final String type) {
    final DTE dte = Enum.valueOf(DTE.class, type);
    switch (dte) {
      case XSDBoolean:
        {
          return XSDBooleanIV.valueOf((Boolean.valueOf(val)));
        }
      case XSDByte:
        {
          final byte x = Byte.valueOf(val);
          return new XSDNumericIV<>(x);
        }
      case XSDShort:
        {
          final short x = Short.valueOf(val);
          return new XSDNumericIV<>(x);
        }
      case XSDInt:
        {
          final int x = Integer.valueOf(val);
          return new XSDNumericIV<>(x);
        }
      case XSDLong:
        {
          final long x = Long.valueOf(val);
          return new XSDNumericIV<>(x);
        }
      case XSDFloat:
        {
          final float x = Float.valueOf(val);
          return new XSDNumericIV<>(x);
        }
      case XSDDouble:
        {
          final double x = Double.valueOf(val);
          return new XSDNumericIV<>(x);
        }
      case UUID:
        {
          final UUID x = UUID.fromString(val);
          return new UUIDLiteralIV<>(x);
        }
      case XSDInteger:
        {
          final BigInteger x = new BigInteger(val);
          return new XSDIntegerIV<>(x);
        }
      case XSDDecimal:
        {
          final BigDecimal x = new BigDecimal(val);
          return new XSDDecimalIV<>(x);
        }
      case XSDString:
        {
          return new FullyInlineTypedLiteralIV(val, null, XMLSchema.STRING, true);
        }
      case XSDUnsignedByte:
        {
          return new XSDUnsignedByteIV<>((byte) (Byte.valueOf(val) + Byte.MIN_VALUE));
        }
      case XSDUnsignedShort:
        {
          return new XSDUnsignedShortIV<>((short) (Short.valueOf(val) + Short.MIN_VALUE));
        }
      case XSDUnsignedInt:
        {
          return new XSDUnsignedIntIV((Integer.valueOf(val) + Integer.MIN_VALUE));
        }
      case XSDUnsignedLong:
        {
          return new XSDUnsignedLongIV<>(Long.valueOf(val) + Long.MIN_VALUE);
        }
      default:
        throw new UnsupportedOperationException("dte=" + dte);
    }
  }
}
