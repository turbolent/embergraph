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
/* Portions of this code are:
 *
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
/*
 * Created on Aug 21, 2011
 */

package org.embergraph.rdf.sail.sparql;

import java.util.Map;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.sail.sparql.ast.VisitorException;
import org.embergraph.rdf.sparql.ast.ConstantNode;
import org.embergraph.rdf.sparql.ast.VarNode;
import org.openrdf.model.Value;

/*
 * Object provides context required in various stages of parsing queries or updates.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @openrdf
 */
public class EmbergraphASTContext {

  protected final EmbergraphValueFactory valueFactory;

  /*
   * A mapping of parsed RDF Values and well known vocabulary items used when generating the AST to
   * resolved {@link EmbergraphValue}s. This includes everything which was parsed plus certain
   * well-known items such as {@link RDF#FIRST}, {@link RDF#REST}, and {@link RDF#NIL} which are
   * only used when handling syntactic sugar constructions.
   */
  protected final Map<Value, EmbergraphValue> vocab;

  /** Counter used to generate unique (within query) variable names. */
  private int constantVarID = 1;

  /*
   * @param values A map from the distinct RDF {@link Value} objects in the QUERY or UPDATE request
   *     to the corresponding {@link EmbergraphValue} objects that will be used to process the
   *     request.
   */
  public EmbergraphASTContext(final Map<Value, EmbergraphValue> values) {

    this.valueFactory = EmbergraphValueFactoryImpl.getInstance("");

    this.vocab = values;
  }

  /*
   * Create an anonymous variable. The variable name will be unique (within the scope of the query
   * parser) and {@link VarNode#isAnonymous()} will return <code>true</code>.
   *
   * @param varName The prefix of the name of an anonymous variable. This should have the pattern
   *     <code>-foo-</code>. An unique (within query) variable identifier will be appended to the
   *     prefix.
   * @return The anonymous variable.
   */
  protected VarNode createAnonVar(final String varName) {

    final VarNode var = new VarNode(varName + constantVarID++);

    var.setAnonymous(true);

    return var;
  }

  /*
   * Return a constant for a pre-defined vocabulary item.
   *
   * @throws VisitorException
   */
  protected ConstantNode createConstVar(final Value value) throws VisitorException {

    final EmbergraphValue v = vocab.get(value);

    if (v == null) throw new VisitorException("Undefined vocabulary: " + value);

    return new ConstantNode(v.getIV());
  }

  /*
   * Return a new variable for a SID (Statement Identifier)
   *
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/526">Reification Done Right</a>
   */
  protected VarNode createSidVar() {

    final VarNode var = new VarNode("-sid-" + constantVarID++);

    return var;
  }
}
