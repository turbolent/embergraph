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
 * Created on November 30, 2015
 */
package org.embergraph.rdf.internal.constraints;

import java.util.Map;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;

/*
 * Convert the {@link IV} to a <code>xsd:long</code>. Note that this is a non-standard extension.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 */
public class XsdLongBOp extends IVValueExpression<IV> implements INeedsMaterialization {

  private static final long serialVersionUID = -8564789336767221003L;

  private static final transient Logger log = Logger.getLogger(XsdLongBOp.class);

  public XsdLongBOp(final IValueExpression<? extends IV> x, final GlobalAnnotations globals) {

    this(new BOp[] {x}, anns(globals));
  }

  /** Required shallow copy constructor. */
  public XsdLongBOp(final BOp[] args, final Map<String, Object> anns) {

    super(args, anns);

    if (args.length != 1 || args[0] == null) throw new IllegalArgumentException();

    if (getProperty(Annotations.NAMESPACE) == null) throw new IllegalArgumentException();
  }

  /** Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}. */
  public XsdLongBOp(final XsdLongBOp op) {
    super(op);
  }

  public IV get(final IBindingSet bs) {

    final IV iv = getAndCheckBound(0, bs);

    if (log.isDebugEnabled()) {
      log.debug(iv);
    }

    final Value val = asValue(iv);

    if (log.isDebugEnabled()) {
      log.debug(val);
    }

    // use to create my simple literals
    final EmbergraphValueFactory vf = getValueFactory();

    try {
      if (val instanceof Literal) {
        final Literal lit = (Literal) val;
        if (lit.getDatatype() != null && lit.getDatatype().equals(XSD.LONG)) {
          // if xsd:unsignedLong literal return it
          return iv;
        } else {
          final EmbergraphLiteral str = vf.createLiteral(Long.valueOf(lit.getLabel()));
          return super.asIV(str, bs);
        }
      }
    } catch (Exception e) {
      // exception handling following
    }

    throw new SparqlTypeErrorException(); // fallback
  }

  /** This bop can only work with materialized terms. */
  public Requirement getRequirement() {

    return INeedsMaterialization.Requirement.SOMETIMES;
  }
}
