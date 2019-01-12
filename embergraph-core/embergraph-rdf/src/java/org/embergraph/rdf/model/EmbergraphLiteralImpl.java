/**
 * The Notice below must appear in each file of the Source Code of any copy you distribute of the
 * Licensed Product. Contributors to any Modifications may add their own copyright notices to
 * identify their own contributions.
 *
 * <p>License:
 *
 * <p>The contents of this file are subject to the CognitiveWeb Open Source License Version 1.1 (the
 * License). You may not copy or use this file, in either source code or executable form, except in
 * compliance with the License. You may obtain a copy of the License from
 *
 * <p>http://www.CognitiveWeb.org/legal/license/
 *
 * <p>Software distributed under the License is distributed on an AS IS basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyrights:
 *
 * <p>Portions created by or assigned to CognitiveWeb are Copyright (c) 2003-2003 CognitiveWeb. All
 * Rights Reserved. Contact information for CognitiveWeb is available at
 *
 * <p>http://www.CognitiveWeb.org
 *
 * <p>Portions Copyright (c) 2002-2003 Bryan Thompson.
 *
 * <p>Acknowledgements:
 *
 * <p>Special thanks to the developers of the Jabber Open Source License 1.0 (JOSL), from which this
 * License was derived. This License contains terms that differ from JOSL.
 *
 * <p>Special thanks to the CognitiveWeb Open Source Contributors for their suggestions and support
 * of the Cognitive Web.
 *
 * <p>Modifications:
 */
/*
 * Created on Apr 16, 2008
 */

package org.embergraph.rdf.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.datatype.XMLGregorianCalendar;
import org.openrdf.model.Literal;
import org.openrdf.model.datatypes.XMLDatatypeUtil;

/**
 * A literal. Use {@link EmbergraphValueFactory} to create instances of this class.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class EmbergraphLiteralImpl extends EmbergraphValueImpl implements EmbergraphLiteral {

  /** */
  private static final long serialVersionUID = 2301819664179569810L;

  private final String label;
  private final String language;
  private final EmbergraphURI datatype;

  /** Used by {@link EmbergraphValueFactoryImpl}. */
  EmbergraphLiteralImpl(
      final EmbergraphValueFactory valueFactory,
      final String label,
      final String language,
      final EmbergraphURI datatype) {

    super(valueFactory, null);

    if (label == null) throw new IllegalArgumentException();

    if (language != null && datatype != null) throw new IllegalArgumentException();

    this.label = label;

    // force to lowercase (Sesame does this too).
    this.language = (language != null ? language.toLowerCase().intern() : null);
    //        this.language = language;

    this.datatype = datatype;
  }

  @Override
  public String toString() {

    final StringBuilder sb = new StringBuilder();

    sb.append('\"');

    sb.append(label);

    sb.append('\"');

    if (language != null) {

      sb.append('@');

      sb.append(language);

    } else if (datatype != null) {

      sb.append("^^<");

      sb.append(datatype);

      sb.append('>');
    }

    return sb.toString();
  }

  @Override
  public String stringValue() {

    return label;
  }

  @Override
  public final String getLabel() {

    return label;
  }

  @Override
  public final String getLanguage() {

    return language;
  }

  @Override
  public final EmbergraphURI getDatatype() {

    return datatype;
  }

  public final int hashCode() {

    return label.hashCode();
  }

  public final boolean equals(Object o) {

    if (!(o instanceof Literal)) return false;

    return equals((Literal) o);
  }

  public final boolean equals(final Literal o) {

    if (this == o) return true;

    if (o == null) return false;

    if ((o instanceof EmbergraphValue)
        && isRealIV()
        && ((EmbergraphValue) o).isRealIV()
        && ((EmbergraphValue) o).getValueFactory() == getValueFactory()) {

      return getIV().equals(((EmbergraphValue) o).getIV());
    }

    if (!label.equals(o.getLabel())) return false;

    if (language != null) {

      // the language code is case insensitive.
      return language.equalsIgnoreCase(o.getLanguage());

    } else if (o.getLanguage() != null) {

      return false;
    }

    if (datatype != null) {

      return datatype.equals(o.getDatatype());

    } else
      return o.getDatatype() == null;

  }

  /*
   * XSD stuff.
   */

  @Override
  public final boolean booleanValue() {

    return XMLDatatypeUtil.parseBoolean(label);
  }

  @Override
  public final byte byteValue() {

    return XMLDatatypeUtil.parseByte(label);
  }

  @Override
  public final short shortValue() {

    return XMLDatatypeUtil.parseShort(label);
  }

  @Override
  public final int intValue() {

    return XMLDatatypeUtil.parseInt(label);
  }

  @Override
  public final long longValue() {

    return XMLDatatypeUtil.parseLong(label);
  }

  @Override
  public final float floatValue() {

    return XMLDatatypeUtil.parseFloat(label);
  }

  @Override
  public final double doubleValue() {

    return XMLDatatypeUtil.parseDouble(label);
  }

  @Override
  public final BigInteger integerValue() {

    return XMLDatatypeUtil.parseInteger(label);
  }

  @Override
  public final BigDecimal decimalValue() {

    return XMLDatatypeUtil.parseDecimal(label);
  }

  @Override
  public final XMLGregorianCalendar calendarValue() {

    return XMLDatatypeUtil.parseCalendar(label);
  }
}
