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
 * Created on Aug 14, 2008
 */

package org.embergraph.rdf.model;

import java.util.Date;
import javax.xml.datatype.XMLGregorianCalendar;
import org.embergraph.rdf.store.IRawTripleStore;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;

/**
 * Interface strengthens the return types and adds some custom extensions.
 *
 * @see EmbergraphValueFactoryImpl#getInstance(String)
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface EmbergraphValueFactory extends ValueFactory {

  /** The namespace of the KB instance associated with the value factory. */
  String getNamespace();

  /** Remove instance of valueFactory from static cache */
  void remove(/*final String namespace*/ );

  /**
   * Returns a factory that will assign its blank node IDs within a globally unique namespace. This
   * factory should be used when processing a document as the generated IDs are clustered and make
   * the ordered writes on the lexicon more efficient since all blank nodes for the same document
   * tend to be directed to the same index partition. All {@link EmbergraphValue}s are actually
   * created by <i>this</i> factory, it is only the semantics of blank node ID generation that are
   * overridden.
   *
   * @see BNodeContextFactory
   */
  EmbergraphValueFactory newBNodeContext();

  //    /**
  //     * Create a blank node and flag it as a statement identifier.
  //     */
  //    EmbergraphBNodeImpl createSID();
  //
  //    /**
  //     * Create a blank node with the specified ID and flag it as a statement
  //     * identifier.
  //     */
  //    EmbergraphBNodeImpl createSID(String id);

  EmbergraphBNode createBNode();

  EmbergraphBNode createBNode(String id);

  EmbergraphBNode createBNode(EmbergraphStatement stmt);

  EmbergraphLiteral createLiteral(String label);

  EmbergraphLiteral createLiteral(boolean arg0);

  EmbergraphLiteral createLiteral(byte arg0);

  EmbergraphLiteral createLiteral(short arg0);

  EmbergraphLiteral createLiteral(int arg0);

  EmbergraphLiteral createLiteral(long arg0);

  EmbergraphLiteral createLiteral(byte arg0, boolean unsigned);

  EmbergraphLiteral createLiteral(short arg0, boolean unsigned);

  EmbergraphLiteral createLiteral(int arg0, boolean unsigned);

  EmbergraphLiteral createLiteral(long arg0, boolean unsigned);

  EmbergraphLiteral createLiteral(float arg0);

  EmbergraphLiteral createLiteral(double arg0);

  EmbergraphLiteral createLiteral(XMLGregorianCalendar arg0);

  EmbergraphLiteral createLiteral(Date arg0);

  EmbergraphLiteral createXSDDateTime(long timestamp);

  EmbergraphLiteral createLiteral(String label, String language);

  EmbergraphLiteral createLiteral(String label, URI datatype);

  EmbergraphLiteral createLiteral(String label, URI datatype, String language);

  EmbergraphURI createURI(String uriString);

  EmbergraphURI createURI(String namespace, String localName);

  /** Create a statement whose {@link StatementEnum} is NOT specified. */
  EmbergraphStatement createStatement(Resource s, URI p, Value o);

  /** Create a statement whose {@link StatementEnum} is NOT specified. */
  EmbergraphStatement createStatement(Resource s, URI p, Value o, Resource c);

  /**
   * Create a statement (core impl). The s,p,o, and the optional c arguments will be normalized to
   * this {@link EmbergraphValueFactory} using {@link #asValue(Value)}.
   *
   * @param s The subject.
   * @param p The predicate.
   * @param o The object.
   * @param c The context (optional). Note: When non-<code>null</code> and statement identifiers are
   *     enabled, then this will be a blank node whose term identifier is the statement identifier.
   * @param type The statement type (optional).
   */
  EmbergraphStatement createStatement(Resource s, URI p, Value o, Resource c, StatementEnum type);

  /**
   * Create a statement (core impl). The s,p,o, and the optional c arguments will be normalized to
   * this {@link EmbergraphValueFactory} using {@link #asValue(Value)}.
   *
   * @param s The subject.
   * @param p The predicate.
   * @param o The object.
   * @param c The context (optional). Note: When non-<code>null</code> and statement identifiers are
   *     enabled, then this will be a blank node whose term identifier is the statement identifier.
   * @param type The statement type (optional).
   * @param userFlag The user flag
   */
  EmbergraphStatement createStatement(
      Resource s, URI p, Value o, Resource c, StatementEnum type, boolean userFlag);

  /**
   * Converts a {@link Value} into a {@link EmbergraphValue}. If the value is already a {@link
   * EmbergraphValue} and it was allocated by <i>this</i> {@link EmbergraphValueFactoryImpl} then it
   * is returned unchanged. Otherwise a new {@link EmbergraphValue} will be creating using the same
   * data as the given value and the term identifier on the new {@link EmbergraphValue} will be
   * initialized to {@link IRawTripleStore#NULL}.
   *
   * <p>All {@link EmbergraphValue}s created by a {@link EmbergraphValueFactoryImpl} internally
   * store a transient reference to the {@link EmbergraphValueFactoryImpl}. This reference is used
   * to decide if a {@link EmbergraphValue} MIGHT have been created by a different lexicon (term
   * identifiers generated by different lexicons CAN NOT be used interchangeably). This has the
   * effect of protecting against incorrect use of the term identifier with a database backed by a
   * different lexicon while allowing reuse of the {@link EmbergraphValue}s when possible.
   *
   * @param v The value.
   * @return A {@link EmbergraphValue} with the same data. If the value is <code>null</code> then
   *     <code>null</code> is returned.
   */
  EmbergraphValue asValue(Value v);

  /** Strongly typed for {@link Resource}s. */
  EmbergraphResource asValue(Resource v);

  /** Strongly typed for {@link URI}s. */
  EmbergraphURI asValue(URI v);

  /** Strongly typed for {@link Literal}s. */
  EmbergraphLiteral asValue(Literal v);

  /** Strongly typed for {@link BNode}s. */
  EmbergraphBNode asValue(BNode v);

  /**
   * An object that can efficiently (de-)serialize {@link Value}s using this {@link ValueFactory}.
   * When the values are de-serialized they will have a reference to this {@link
   * EmbergraphValueFactoryImpl}. That reference can be used to identify when two {@link
   * EmbergraphValue}s MIGHT be from different lexicons.
   *
   * @return
   */
  EmbergraphValueSerializer<EmbergraphValue> getValueSerializer();
}
