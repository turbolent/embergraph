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
package org.embergraph.bop.rdf.aggregate;

import java.util.Map;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.NV;
import org.embergraph.bop.aggregate.AggregateBase;
import org.embergraph.bop.solutions.PipelinedAggregationOp;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.constraints.INeedsMaterialization;
import org.embergraph.rdf.internal.constraints.IVValueExpression;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.sparql.ast.DummyConstantNode;

/**
 * Operator combines the string values over the presented binding sets for the given variable.
 * Missing values are ignored. The initial value is an empty plain literal.
 *
 * @author thompsonbry
 */
public class GROUP_CONCAT extends AggregateBase<IV> implements INeedsMaterialization {

  /** */
  private static final long serialVersionUID = 1L;

  public interface Annotations extends AggregateBase.Annotations {

    /** The namespace of the lexicon relation. */
    String NAMESPACE = GROUP_CONCAT.class.getName() + ".namespace";

    /**
     * Required string property provides the separator used when combining the {@link
     * IValueExpression} computed for each solution within the group.
     *
     * <p>Use basic string to match sparql scalarValues param
     */
    String SEPARATOR = "separator";

    /**
     * The maximum #of values to concatenate (positive integer and <code>-1</code> to indicate no
     * bound) (default {@value #DEFAULT_VALUE_LIMIT})
     */
    String VALUE_LIMIT = GROUP_CONCAT.class.getName() + ".valueLimit";

    /** The default indicates no limit. */
    int DEFAULT_VALUE_LIMIT = -1;

    /**
     * The maximum #of characters permitted in the generated value (positive integer and <code>-1
     * </code> to indicate no bound) (default {@value #DEFAULT_CHARACTER_LIMIT}).
     */
    String CHARACTER_LIMIT = GROUP_CONCAT.class.getName() + ".characterLimit";

    /** The default indicates no limit. */
    int DEFAULT_CHARACTER_LIMIT = -1;
  }

  public GROUP_CONCAT(GROUP_CONCAT op) {
    super(op);
  }

  public GROUP_CONCAT(BOp[] args, Map<String, Object> annotations) {

    super(args, annotations);

    if (!(getRequiredProperty(Annotations.NAMESPACE) instanceof String))
      throw new IllegalArgumentException();
    //        if (getProperty(Annotations.NAMESPACE) == null)
    //            throw new IllegalArgumentException();

  }

  /**
   * @param var The variable whose values will be combined.
   * @param sep The separator string (note that a space (0x20) is the default in the SPARQL
   *     recommendation).
   */
  public GROUP_CONCAT(
      final boolean distinct,
      final IValueExpression<IV> expr,
      final String namespace,
      final String sep) {

    this(
        new BOp[] {expr},
        NV.asMap(
            //                new NV(Annotations.FUNCTION_CODE, FunctionCode.GROUP_CONCAT),
            new NV(Annotations.DISTINCT, distinct),
            new NV(Annotations.NAMESPACE, namespace),
            new NV(Annotations.SEPARATOR, sep)));
  }

  private String sep() {
    if (sep == null) {
      sep = (String) getRequiredProperty(Annotations.SEPARATOR);
    }
    return sep;
  }

  private transient String sep;

  private int valueLimit() {
    if (valueLimit == 0) {
      valueLimit = getProperty(Annotations.VALUE_LIMIT, Annotations.DEFAULT_VALUE_LIMIT);
    }
    return valueLimit;
  }

  private transient int valueLimit;

  private int characterLimit() {
    if (characterLimit == 0) {
      characterLimit =
          getProperty(Annotations.CHARACTER_LIMIT, Annotations.DEFAULT_CHARACTER_LIMIT);
    }
    return characterLimit;
  }

  private transient int characterLimit;

  private EmbergraphValueFactory getValueFactory() {
    if (vf == null) {
      final String namespace = (String) getRequiredProperty(Annotations.NAMESPACE);
      vf = EmbergraphValueFactoryImpl.getInstance(namespace);
    }
    return vf;
  }

  protected transient EmbergraphValueFactory vf;

  /**
   * The running concatenation of observed bound values.
   *
   * <p>Note: This field is guarded by the monitor on the {@link GROUP_CONCAT} instance.
   */
  private transient StringBuilder aggregated = null;

  /**
   * The #of values in {@link #aggregated}.
   *
   * <p>Note: This field is guarded by the monitor on the {@link GROUP_CONCAT} instance.
   */
  private transient long nvalues = 0;

  /**
   * <code>false</code> unless either the value limit and/or the character length limit has been
   * exceeded.
   */
  private transient boolean done = false;

  private Throwable firstCause = null;

  public synchronized void reset() {

    aggregated = null;

    nvalues = 0;

    done = false;

    firstCause = null;

    cache();
  }

  /**
   * Cache stuff.
   *
   * <p>Note: The {@link PipelinedAggregationOp} does NOT invoke {@link #reset()} so we have to
   * conditionally cache stuff from {@link #get(IBindingSet)} and {@link #done()} as well.
   */
  private void cache() {

    sep();
    valueLimit();
    characterLimit();
    getValueFactory();
  }

  public synchronized IV done() {

    if (sep == null) cache();

    if (firstCause != null) {

      throw new RuntimeException(firstCause);
    }

    final EmbergraphValueFactory vf = getValueFactory();

    IV ret;
    if (aggregated == null) {
      ret = DummyConstantNode.toDummyIV(vf.createLiteral(""));
    } else {
      ret = DummyConstantNode.toDummyIV(vf.createLiteral(aggregated.toString()));
    }
    //        System.err.println("aggregated:=" + aggregated+" : "+ret);
    return ret;
  }

  public synchronized IV get(final IBindingSet bindingSet) {

    try {

      return doGet(bindingSet);

    } catch (Throwable t) {

      if (firstCause == null) {

        firstCause = t;
      }

      throw new RuntimeException(t);
    }
  }

  private IV doGet(final IBindingSet bindingSet) {

    if (sep == null) cache();

    final IValueExpression<IV<?, ?>> expr = (IValueExpression<IV<?, ?>>) get(0);

    final IV<?, ?> iv = expr.get(bindingSet);

    if (iv != null && !done) {

      final String str = IVValueExpression.asValue(iv).stringValue();
      /*
       * This kind of logic needs to be hidden.  It causes too many
       * problems.  Right now IVValueExpression.asValue(IV) knows how
       * to handle this correctly.
       */
      //            if (iv.isInline() && !iv.isExtension()) {
      //                str = iv.getInlineValue().toString();
      //            } else {
      //                str = iv.getValue().stringValue();
      //            }

      if (aggregated == null) aggregated = new StringBuilder(str);
      else {
        aggregated.append(sep());
        aggregated.append(str);
        //                System.err.println("aggregated:=" + aggregated);

      }

      nvalues++;

      if (characterLimit() != -1 && aggregated.length() >= characterLimit) {
        // Exceeded the character length limit.
        aggregated.setLength(characterLimit); // truncate.
        done = true;
      } else if (valueLimit() != -1 && nvalues >= valueLimit) {
        // Exceeded the value limit.
        done = true;
      }
    }

    // Note: Nothing returned until done().
    return null;
  }

  /** We always need to have the materialized values. */
  public Requirement getRequirement() {

    return INeedsMaterialization.Requirement.ALWAYS;
  }
}
