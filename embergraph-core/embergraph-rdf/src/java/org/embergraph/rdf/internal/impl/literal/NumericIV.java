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

package org.embergraph.rdf.internal.impl.literal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.embergraph.rdf.internal.DTE;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;

/*
* Superclass for the inline numerics - {@link XSDNumericIV}, {@link XSDIntegerIV}, and {@link
 * XSDDecimalIV}.
 */
public abstract class NumericIV<V extends EmbergraphLiteral, T> extends AbstractLiteralIV<V, T>
    implements Literal {

  /** */
  private static final long serialVersionUID = -2878889877313783890L;

  /*
   * Definition of numeric datatypes according to
   * http://www.w3.org/TR/sparql11-query/#operandDataTypes: "numeric denotes typed literals with
   * datatypes xsd:integer, xsd:decimal, xsd:float, and xsd:double."
   *
   * <p>See https://github.com/SYSTAP/bigdata-gpu/issues/257.
   */
  public static final Set<URI> numericalDatatypes =
      Collections.unmodifiableSet(
          new HashSet<URI>(
              Arrays.asList(
                  // basic numeric data types
                  XSD.INTEGER,
                  XSD.DECIMAL,
                  XSD.FLOAT,
                  XSD.DOUBLE,
                  // derived numeric data types
                  XSD.NON_POSITIVE_INTEGER,
                  XSD.NEGATIVE_INTEGER,
                  XSD.LONG,
                  XSD.INT,
                  XSD.SHORT,
                  XSD.BYTE,
                  XSD.NON_NEGATIVE_INTEGER,
                  XSD.UNSIGNED_LONG,
                  XSD.UNSIGNED_INT,
                  XSD.UNSIGNED_SHORT,
                  XSD.UNSIGNED_INT,
                  XSD.POSITIVE_INTEGER)));

  public NumericIV(final DTE dte) {

    super(dte);
  }
}
