package org.embergraph.rdf.internal.impl.literal;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.datatype.XMLGregorianCalendar;
import org.embergraph.rdf.internal.INonInlineExtensionCodes;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.AbstractNonInlineExtensionIVWithDelegateIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;

/**
 * A {@link Literal} modeled as a datatype {@link IV} plus an inline Unicode <code>label</code>.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <V>
 */
public class PartlyInlineTypedLiteralIV<V extends EmbergraphLiteral>
    extends AbstractNonInlineExtensionIVWithDelegateIV<V, Literal> implements Literal {

  /** */
  private static final long serialVersionUID = 1503294264280588030L;

  public IV<V, Literal> clone(final boolean clearCache) {

    final PartlyInlineTypedLiteralIV<V> tmp =
        new PartlyInlineTypedLiteralIV<V>(getDelegate(), getExtensionIV());

    if (!clearCache) {

      tmp.setValue(getValueCache());
    }

    return tmp;
  }

  public PartlyInlineTypedLiteralIV(
      final AbstractLiteralIV<EmbergraphLiteral, ?> delegate, final IV<?, ?> datatype) {

    super(VTE.LITERAL, delegate, datatype);
  }

  /** Human readable representation includes the datatype {@link IV} and the <code>label</code>. */
  public String toString() {

    return "Literal(datatypeIV="
        + getExtensionIV()
        + getVTE().getCharCode()
        + ", localName="
        + getDelegate()
        + ")";
  }

  @Override
  public final byte getExtensionByte() {

    return INonInlineExtensionCodes.LiteralDatatypeIV;
  }

  /** Implements {@link Value#stringValue()}. */
  @Override
  public String stringValue() {
    return getDelegate().stringValue();
  }

  /**
   * Implements {@link Literal#getLabel()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public String getLabel() {
    return getDelegate().getLabel();
  }

  /**
   * Implements {@link Literal#booleanValue()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public boolean booleanValue() {
    return getDelegate().booleanValue();
  }

  /**
   * Implements {@link Literal#byteValue()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public byte byteValue() {
    return getDelegate().byteValue();
  }

  /**
   * Implements {@link Literal#calendarValue()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public XMLGregorianCalendar calendarValue() {
    return getDelegate().calendarValue();
  }

  /**
   * Implements {@link Literal#decimalValue()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public BigDecimal decimalValue() {
    return getDelegate().decimalValue();
  }

  /**
   * Implements {@link Literal#doubleValue()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public double doubleValue() {
    return getDelegate().doubleValue();
  }

  /**
   * Implements {@link Literal#floatValue()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public float floatValue() {
    return getDelegate().floatValue();
  }

  /**
   * Implements {@link Literal#intValue()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public int intValue() {
    return getDelegate().intValue();
  }

  /**
   * Implements {@link Literal#integerValue()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public BigInteger integerValue() {
    return getDelegate().integerValue();
  }

  /**
   * Implements {@link Literal#longValue()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public long longValue() {
    return getDelegate().longValue();
  }

  /**
   * Implements {@link Literal#shortValue()}.
   *
   * <p>We can use the inline delegate for this.
   */
  @Override
  public short shortValue() {
    return getDelegate().shortValue();
  }
}
