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
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.openrdf.model.Literal;

/** Return the language tag of the literal argument. */
@SuppressWarnings("rawtypes")
public class LangBOp extends IVValueExpression<IV> implements INeedsMaterialization {

  /** */
  private static final long serialVersionUID = 7391999162162545704L;

  //	private static final transient Logger log = Logger.getLogger(LangBOp.class);

  public LangBOp(final IValueExpression<? extends IV> x, final GlobalAnnotations globals) {

    super(x, globals);
  }

  /** Required shallow copy constructor. */
  public LangBOp(final BOp[] args, final Map<String, Object> anns) {

    super(args, anns);

    if (args.length != 1 || args[0] == null) throw new IllegalArgumentException();

    if (getProperty(Annotations.NAMESPACE) == null) throw new IllegalArgumentException();
  }

  /** Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}. */
  public LangBOp(final LangBOp op) {

    super(op);
  }

  @Override
  public IV get(final IBindingSet bs) {

    final Literal literal = getAndCheckLiteralValue(0, bs);

    String langTag = literal.getLanguage();

    if (langTag == null) {

      langTag = "";
    }

    final EmbergraphValueFactory vf = getValueFactory();

    final EmbergraphValue lang = vf.createLiteral(langTag);

    return super.asIV(lang, bs);
  }

  @Override
  public Requirement getRequirement() {

    return INeedsMaterialization.Requirement.SOMETIMES;
  }
}
