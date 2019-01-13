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
package org.embergraph.rdf.internal.constraints;

import java.util.Map;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpBase;
import org.embergraph.bop.BOpContextBase;
import org.embergraph.bop.ContextBindingSet;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.NV;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.ILexiconConfiguration;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.NotMaterializedException;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.sparql.ast.DummyConstantNode;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;

/*
 * A specialized IValueExpression that evaluates to an IV. The inputs are usually, but not strictly
 * limited to, IVs as well. This class also contains many useful helper methods for evaluation,
 * including providing access to the EmbergraphValueFactory and LexiconConfiguration.
 */
public abstract class IVValueExpression<T extends IV> extends BOpBase
    implements IValueExpression<T> {

  /** */
  private static final long serialVersionUID = -7068219781217676085L;

  public interface Annotations extends BOpBase.Annotations {

    /** The namespace of the lexicon. */
    String NAMESPACE = IVValueExpression.class.getName() + ".namespace";

    /** The timestamp of the query. */
    String TIMESTAMP = IVValueExpression.class.getName() + ".timestamp";
  }

  /*
   * Note: The double-checked locking pattern <em>requires</em> the keyword <code>volatile</code>.
   */
  private transient volatile EmbergraphValueFactory vf;

  /*
   * Note: The double-checked locking pattern <em>requires</em> the keyword <code>volatile</code>.
   */
  private transient volatile ILexiconConfiguration<EmbergraphValue> lc;

  /*
   * Used by subclasses to create the annotations object from the global annotations and the custom
   * annotations for the particular VE.
   *
   * @param globals The global annotations, including the lexicon namespace.
   * @param anns Any additional custom annotations.
   */
  protected static Map<String, Object> anns(final GlobalAnnotations globals, final NV... anns) {

    final int size = 2 + (anns != null ? anns.length : 0);

    final NV[] nv = new NV[size];
    nv[0] = new NV(Annotations.NAMESPACE, globals.lex);
    nv[1] = new NV(Annotations.TIMESTAMP, globals.timestamp);

    if (anns != null) {
      System.arraycopy(anns, 0, nv, 2, anns.length);
    }

    return NV.asMap(nv);
  }

  /*
   * Zero arg convenience constructor.
   *
   * @param globals The global annotations, including the lexicon namespace.
   * @param anns Any additional custom annotations.
   */
  public IVValueExpression(final GlobalAnnotations globals, final NV... anns) {

    this(BOpBase.NOARGS, anns(globals, anns));
  }

  /*
   * One arg convenience constructor.
   *
   * @param globals The global annotations, including the lexicon namespace.
   * @param anns Any additional custom annotations.
   */
  public IVValueExpression(
      final IValueExpression<? extends IV> x, final GlobalAnnotations globals, final NV... anns) {

    this(new BOp[] {x}, anns(globals, anns));
  }

  /** Required shallow copy constructor. */
  public IVValueExpression(final BOp[] args, final Map<String, Object> anns) {
    super(args, anns);

    if (areGlobalsRequired()) {

      if (getProperty(Annotations.NAMESPACE) == null) {
        throw new IllegalArgumentException("must set the lexicon namespace");
      }

      if (getProperty(Annotations.TIMESTAMP) == null) {
        throw new IllegalArgumentException("must set the query timestamp");
      }
    }
  }

  /** Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}. */
  public IVValueExpression(final IVValueExpression<T> op) {
    super(op);

    if (areGlobalsRequired()) {

      if (getProperty(Annotations.NAMESPACE) == null) {
        throw new IllegalArgumentException("must set the lexicon namespace");
      }

      if (getProperty(Annotations.TIMESTAMP) == null) {
        throw new IllegalArgumentException("must set the query timestamp");
      }
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public IValueExpression<? extends IV> get(final int i) {

    try {

      return (IValueExpression<? extends IV>) super.get(i);

    } catch (ClassCastException ex) {

      throw new SparqlTypeErrorException();
    }
  }

  /*
   * Returns <code>true</code> unless overridden, meaning the {@link GlobalAnnotations} are required
   * for this value expression (certain boolean value expressions do not require them). Global
   * annotations allow the method getValueFactory and getLexiconConfiguration to work.
   */
  protected boolean areGlobalsRequired() {

    return true;
  }

  /*
   * Return the {@link EmbergraphValueFactory} for the {@link LexiconRelation}.
   *
   * <p>Note: This is lazily resolved and then cached.
   */
  protected EmbergraphValueFactory getValueFactory() {

    if (vf == null) {

      synchronized (this) {
        if (vf == null) {

          final String namespace = getNamespace();

          vf = EmbergraphValueFactoryImpl.getInstance(namespace);
        }
      }
    }

    return vf;
  }

  /** Return the namespace of the {@link LexiconRelation}. */
  protected String getNamespace() {

    return (String) getRequiredProperty(Annotations.NAMESPACE);
  }

  /** Return the timestamp for the query. */
  protected long getTimestamp() {

    return (Long) getRequiredProperty(Annotations.TIMESTAMP);
  }

  /*
   * Return the {@link ILexiconConfiguration}. The result is cached. The cache it will not be
   * serialized when crossing a node boundary.
   *
   * <p>Note: It is more expensive to obtain the {@link ILexiconConfiguration} than the {@link
   * EmbergraphValueFactory} because we have to resolve the {@link LexiconRelation} view. However,
   * this happens once per function bop in a query per node, so the cost is amortized.
   *
   * @param bset A binding set flowing through this operator.
   * @throws ContextNotAvailableException if the context was not accessible on the solution.
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/513">Expose the
   *     LexiconConfiguration to function BOPs </a>
   *     <p>TODO This locates the last committed view of the {@link LexiconRelation}. Unlike {@link
   *     AbstractAccessPathOp}, the {@link LiteralBooleanBOp} does not declares the TIMESTAMP of the
   *     view. We really need that annotation to recover the right view of the {@link
   *     LexiconRelation}. However, the {@link ILexiconConfiguration} metadata is immutable so it is
   *     Ok to use the last committed time for that view. This is NOT true of if we were going to
   *     read data from the {@link LexiconRelation}.
   */
  protected ILexiconConfiguration<EmbergraphValue> getLexiconConfiguration(final IBindingSet bset) {

    if (lc == null) {

      synchronized (this) {
        if (lc == null) {

          if (!(bset instanceof ContextBindingSet)) {

            /*
             * This generally indicates a failure to propagate the
             * context wrapper for the binding set to a new binding
             * set during a copy (projection), bind (join), etc. It
             * could also indicate a failure to wrap binding sets
             * when they are vectored into an operator after being
             * received at a node on a cluster.
             */

            throw new ContextNotAvailableException(this.toString());
          }

          final BOpContextBase context = ((ContextBindingSet) bset).getBOpContext();

          final String namespace = getNamespace();

          //                    final long timestamp = ITx.READ_COMMITTED;
          final long timestamp = getTimestamp();

          final LexiconRelation lex = (LexiconRelation) context.getResource(namespace, timestamp);

          lc = lex.getLexiconConfiguration();

          if (vf != null) {

            // Available as an attribute here.
            vf = lc.getValueFactory();
          }
        }
      }
    }

    return lc;
  }

  /*
   * Return the {@link Literal} for the {@link IV}.
   *
   * @param iv The {@link IV}.
   * @return The {@link Literal}.
   * @throws SparqlTypeErrorException if the argument is <code>null</code>.
   * @throws SparqlTypeErrorException if the argument does not represent a {@link Literal}.
   * @throws NotMaterializedException if the {@link IVCache} is not set and the {@link IV} can not
   *     be turned into a {@link Literal} without an index read.
   */
  @SuppressWarnings("rawtypes")
  public static Literal asLiteral(final IV iv) {

    if (iv == null) throw new SparqlTypeErrorException();

    if (!iv.isLiteral()) throw new SparqlTypeErrorException();

    if (iv.isInline() && !iv.needsMaterialization()) {

      return (Literal) iv;

    } else if (iv.hasValue()) {

      return (EmbergraphLiteral) iv.getValue();

    } else {

      throw new NotMaterializedException();
    }
  }

  /*
   * Return the {@link Value} for the {@link IV}.
   *
   * @param iv The {@link IV}.
   * @return The {@link Value}.
   * @throws SparqlTypeErrorException if the argument is <code>null</code>.
   * @throws NotMaterializedException if the {@link IVCache} is not set and the {@link IV} can not
   *     be turned into a {@link Literal} without an index read.
   */
  @SuppressWarnings("rawtypes")
  public static Value asValue(final IV iv) {

    if (iv == null) throw new SparqlTypeErrorException();

    if (iv.isInline() && !iv.needsMaterialization()) {

      return iv;

    } else if (iv.hasValue()) {

      return iv.getValue();

    } else {

      throw new NotMaterializedException();
    }
  }

  /*
   * Return the {@link String} label for the {@link IV}.
   *
   * @param iv The {@link IV}.
   * @return {@link Literal#getLabel()} for that {@link IV}.
   * @throws NullPointerException if the argument is <code>null</code>.
   * @throws NotMaterializedException if the {@link IVCache} is not set and the {@link IV} must be
   *     materialized before it can be converted into an RDF {@link Value}.
   */
  @SuppressWarnings("rawtypes")
  protected final String literalLabel(final IV iv) throws NotMaterializedException {

    return asLiteral(iv).getLabel();
  }

  /*
   * Get the function argument (a value expression) and evaluate it against the source solution. The
   * evaluation of value expressions is recursive.
   *
   * @param i The index of the function argument ([0...n-1]).
   * @param bs The source solution.
   * @return The result of evaluating that argument of this function.
   * @throws IndexOutOfBoundsException if the index is not the index of an operator for this
   *     operator.
   * @throws SparqlTypeErrorException if the value expression at that index can not be evaluated.
   * @throws NotMaterializedException if evaluation encountered an {@link IV} whose {@link IVCache}
   *     was not set when the value expression required a materialized RDF {@link Value}.
   */
  protected IV getAndCheckLiteral(final int i, final IBindingSet bs)
      throws SparqlTypeErrorException, NotMaterializedException {

    final IV<?, ?> iv = getAndCheckBound(i, bs);

    if (!iv.isLiteral()) throw new SparqlTypeErrorException();

    if (iv.needsMaterialization() && !iv.hasValue()) throw new NotMaterializedException();

    return iv;
  }

  /*
   * Get the function argument (a value expression) and evaluate it against the source solution. The
   * evaluation of value expressions is recursive.
   *
   * @param i The index of the function argument ([0...n-1]).
   * @param bs The source solution.
   * @return The result of evaluating that argument of this function.
   * @throws IndexOutOfBoundsException if the index is not the index of an operator for this
   *     operator.
   * @throws SparqlTypeErrorException if the value expression at that index can not be evaluated.
   */
  protected IV getAndCheckBound(final int i, final IBindingSet bs)
      throws SparqlTypeErrorException, NotMaterializedException {

    final IV<?, ?> iv = get(i).get(bs);

    if (iv == null) throw new SparqlTypeErrorException.UnboundVarException();

    return iv;
  }

  /** Combination of {@link #getAndCheckLiteral(int, IBindingSet)} and {@link #asLiteral(IV)}. */
  protected Literal getAndCheckLiteralValue(final int i, final IBindingSet bs) {

    return asLiteral(getAndCheckLiteral(i, bs));
  }

  /*
   * Return an {@link IV} for the {@link Value}.
   *
   * @param value The {@link Value}.
   * @param bs The bindings on the solution are ignored, but the reference is used to obtain the
   *     {@link ILexiconConfiguration}.
   * @return An {@link IV} for that {@link Value}.
   */
  protected final IV asIV(final Value value, final IBindingSet bs) {

    /*
     * Convert to a EmbergraphValue if not already one.
     *
     * If it is a EmbergraphValue, then make sure that it is associated with
     * the namespace for the lexicon relation.
     */

    if (value instanceof IV) {

      return (IV) value;
    }

    final EmbergraphValue v = getValueFactory().asValue(value);

    return asIV(v, bs);
  }

  /*
   * Return an {@link IV} for the {@link Value}.
   *
   * <p>If the supplied EmbergraphValue has an IV, cache the EmbergraphValue on the IV and return
   * it. If there is no IV, first check the LexiconConfiguration to see if an inline IV can be
   * created. As a last resort, create a "dummy IV" (a TermIV with a "0" reference) for the value.
   *
   * @param value The {@link EmbergraphValue}
   * @param bs The bindings on the solution are ignored, but the reference is used to obtain the
   *     {@link ILexiconConfiguration}.
   * @return An {@link IV} for that {@link EmbergraphValue}.
   */
  protected IV asIV(final EmbergraphValue value, final IBindingSet bs) {

    // first check to see if there is already an IV
    IV iv = value.getIV();
    if (iv != null) {
      // cache the value
      iv.setValue(value);
      return iv;
    }

    @SuppressWarnings("rawtypes")
    ILexiconConfiguration lc = null;
    try {
      lc = getLexiconConfiguration(bs);
    } catch (ContextNotAvailableException ex) {
      // can't access the LC (e.g. some test cases)
      // log.warn?
    }

    // see if we happen to have the value in the vocab or can otherwise
    // create an inline IV for it
    if (lc != null) {
      iv = lc.createInlineIV(value);
      if (iv != null) {
        // cache the value only if it's something that would require materialization
        if (iv.needsMaterialization()) {
          iv.setValue(value);
        }
        return iv;
      }
    }

    // toDummyIV will also necessarily cache the value since the IV
    // doesn't mean anything
    return DummyConstantNode.toDummyIV(value);
  }
}
