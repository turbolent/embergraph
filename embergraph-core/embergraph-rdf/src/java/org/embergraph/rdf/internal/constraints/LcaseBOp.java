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
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.openrdf.model.Literal;

/*
*
 *
 * <pre>http://www.w3.org/2005/xpath-functions#lower-case</pre>
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class LcaseBOp extends IVValueExpression<IV> implements INeedsMaterialization {

  private static final long serialVersionUID = -6847688419473046477L;

  public LcaseBOp(final IValueExpression<? extends IV> x, final GlobalAnnotations globals) {
    super(x, globals);
  }

  /*
   * Required shallow copy constructor.
   *
   * @param args The function arguments (value expressions).
   * @param anns The function annotations.
   */
  public LcaseBOp(final BOp[] args, final Map<String, Object> anns) {

    super(args, anns);

    if (args.length != 1 || args[0] == null) {

      /*
       * There must be exactly one argument for this function.
       */

      throw new IllegalArgumentException();
    }
  }

  /*
   * Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}.
   *
   * @param op
   */
  public LcaseBOp(final LcaseBOp op) {

    super(op);
  }

  /*
   * This is a {@link Requirement#SOMETIMES} because it can operate on inline {@link IV}s without
   * materialization but requires materialization of non-inline {@link IV}s.
   */
  @Override
  public Requirement getRequirement() {
    return Requirement.SOMETIMES;
  }

  @Override
  public IV get(final IBindingSet bs) {

    final Literal in = getAndCheckLiteralValue(0, bs);

    final EmbergraphValueFactory vf = getValueFactory();

    final String label = in.getLabel().toLowerCase();

    final EmbergraphLiteral out;

    if (in.getLanguage() != null) {

      out = vf.createLiteral(label, in.getLanguage());

    } else if (in.getDatatype() != null) {

      out = vf.createLiteral(label, in.getDatatype());

    } else {

      out = vf.createLiteral(label);
    }

    return super.asIV(out, bs);
  }
}
