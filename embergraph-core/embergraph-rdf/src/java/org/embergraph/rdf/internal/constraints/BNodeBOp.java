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
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;

/*
 * The BNODE()/BNODE(Literal) function as defined in <a
 * href="http://www.w3.org/TR/sparql11-query/#SparqlOps">SPARQL 1.1 Query Language for RDF</a>.
 *
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class BNodeBOp extends IVValueExpression<IV> implements INeedsMaterialization {

  private static final long serialVersionUID = -8448763718374010166L;

  public BNodeBOp(final GlobalAnnotations globals) {
    super(globals);
  }

  public BNodeBOp(final IValueExpression<? extends IV> x, final GlobalAnnotations globals) {
    this(new BOp[] {x}, anns(globals));
  }

  public BNodeBOp(BOp[] args, Map<String, Object> anns) {
    super(args, anns);
    if (args.length != 1 || args[0] == null) throw new IllegalArgumentException();
  }

  public BNodeBOp(BNodeBOp op) {
    super(op);
  }

  public Requirement getRequirement() {
    return Requirement.SOMETIMES;
  }

  public IV get(final IBindingSet bs) throws SparqlTypeErrorException {

    if (arity() == 0) {

      return super.asIV(getValueFactory().createBNode(), bs);
    }

    final Literal lit = getAndCheckLiteralValue(0, bs);

    final URI dt = lit.getDatatype();

    if (dt != null && !dt.stringValue().equals(XSD.STRING.stringValue()))
      throw new SparqlTypeErrorException();

    final EmbergraphBNode bnode = getValueFactory().createBNode("-bnode-func-" + lit.getLabel());

    return super.asIV(bnode, bs);
  }
}
