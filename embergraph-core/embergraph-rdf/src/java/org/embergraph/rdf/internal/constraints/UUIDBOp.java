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
import java.util.UUID;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.ImmutableBOp;
import org.embergraph.bop.NV;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sparql.ast.DummyConstantNode;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;

/** Implements the now() operator. */
public class UUIDBOp extends IVValueExpression<IV> implements INeedsMaterialization {

  /** */
  private static final long serialVersionUID = 9136864442064392445L;

  public interface Annotations extends ImmutableBOp.Annotations {

    String STR = UUIDBOp.class.getName() + ".str";
  }

  public UUIDBOp(final GlobalAnnotations globals, final boolean str) {

    this(BOp.NOARGS, anns(globals, new NV(Annotations.STR, str)));
  }

  /*
   * Required shallow copy constructor.
   *
   * @param args The operands.
   * @param op The operation.
   */
  public UUIDBOp(final BOp[] args, Map<String, Object> anns) {

    super(args, anns);
  }

  /*
   * Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}.
   *
   * @param op
   */
  public UUIDBOp(final UUIDBOp op) {

    super(op);
  }

  public final IV get(final IBindingSet bs) {

    final EmbergraphValueFactory vf = super.getValueFactory();

    final UUID uuid = UUID.randomUUID();

    if (str()) {

      final EmbergraphLiteral l = vf.createLiteral(uuid.toString());

      return DummyConstantNode.toDummyIV(l);

    } else {

      final EmbergraphURI uri = vf.createURI("urn:uuid:" + uuid.toString());

      return DummyConstantNode.toDummyIV(uri);
    }
  }

  public boolean str() {

    return (boolean) getProperty(Annotations.STR);
  }

  public String toString() {

    return str() ? "struuid()" : "uuid()";
  }

  public Requirement getRequirement() {
    return Requirement.NEVER;
  }
}
