/*
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

import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.impl.bnode.SidIV;
import org.embergraph.rdf.rio.UnificationException;
import org.embergraph.rdf.spo.SPO;
import org.openrdf.model.BNode;

/*
 * A blank node. Use {@link EmbergraphValueFactory} to create instances of this class.
 *
 * <p>Note: When {@link AbstractTripleStore.Options#STATEMENT_IDENTIFIERS} is enabled blank nodes in
 * the context position of a statement are recognized as statement identifiers by {@link
 * StatementBuffer}. It coordinates with this class in order to detect when a blank node is a
 * statement identifier and to defer the assertion of statements made using a statement identifier
 * until that statement identifier becomes defined by being paired with a statement.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class EmbergraphBNodeImpl extends EmbergraphResourceImpl implements EmbergraphBNode {

  /** */
  private static final long serialVersionUID = 2675602437833048872L;

  private final String id;

  /*
   * Boolean flag is set during conversion from an RDF interchange syntax into the internal {@link
   * SPO} model if the blank node is a statement identifier.
   */
  private boolean statementIdentifier;

  //    public EmbergraphBNodeImpl(String id) {
  //
  //        this(null, id);
  //
  //    }

  /** Used by {@link EmbergraphValueFactoryImpl}. */
  EmbergraphBNodeImpl(final EmbergraphValueFactory valueFactory, final String id) {

    this(valueFactory, id, null);
  }

  EmbergraphBNodeImpl(
      final EmbergraphValueFactory valueFactory, final String id, final EmbergraphStatement stmt) {

    super(valueFactory, null);

    if (id == null) throw new IllegalArgumentException();

    this.id = id;

    this.sid = stmt;
    if (stmt != null) {
      this.statementIdentifier = true;
    }
  }

  /** Used to detect ungrounded sids (self-referential). */
  private transient boolean selfRef = false;

  @Override
  public IV getIV() {

    if (super.iv == null && sid != null) {

      //    		if (sid.getSubject() == this || sid.getObject() == this)
      //				throw new UnificationException("illegal self-referential sid");

      if (selfRef) {
        throw new UnificationException("illegal self-referential sid");
      }

      // temporarily set it to true while we get the IVs on the sid
      selfRef = true;

      final IV s = sid.s();
      final IV p = sid.p();
      final IV o = sid.o();

      // if we make it to here then we have a fully grounded sid
      selfRef = false;

      if (s != null && p != null && o != null) {
        setIV(new SidIV(new SPO(s, p, o)));
      }
    }

    return super.iv;
  }

  @Override
  public String toString() {

    if (sid != null) {
      return "<" + sid.toString() + ">";
    }
    return "_:" + id;
  }

  @Override
  public String stringValue() {

    return id;
  }

  @Override
  public final boolean equals(final Object o) {

    if (!(o instanceof BNode)) return false;

    return equals((BNode) o);
  }

  public final boolean equals(final BNode o) {

    if (this == o) return true;

    if (o == null) return false;

    if ((o instanceof EmbergraphValue)
        && isRealIV()
        && ((EmbergraphValue) o).isRealIV()
        && ((EmbergraphValue) o).getValueFactory() == getValueFactory()) {

      return getIV().equals(((EmbergraphValue) o).getIV());

    } else if ((o instanceof EmbergraphBNode)
        && isStatementIdentifier()
        && ((EmbergraphBNode) o).isStatementIdentifier()) {

      return getStatement().equals(((EmbergraphBNode) o).getStatement());
    }

    return id.equals(o.getID());
  }

  @Override
  public final int hashCode() {

    return id.hashCode();
  }

  @Override
  public final String getID() {

    return id;
  }

  @Override
  public final void setStatementIdentifier(final boolean isStmtIdentifier) {

    this.statementIdentifier = isStmtIdentifier;
  }

  @Override
  public final boolean isStatementIdentifier() {

    return this.statementIdentifier;
  }

  /*
   * Mechanism permitting the attachment of a statement to a blank node when
   * we know the correlation between the blank node and the statement.
   * 6/1/2012.
   */

  /*
   * Marks this as a blank node which models the specified statement.
   *
   * @param sid The statement.
   */
  @Override
  public final void setStatement(final EmbergraphStatement sid) {
    this.statementIdentifier = true;
    this.sid = sid;
  }

  /** Return the statement modeled by this blank node. */
  @Override
  public final EmbergraphStatement getStatement() {
    return sid;
  }

  private transient EmbergraphStatement sid;
}
