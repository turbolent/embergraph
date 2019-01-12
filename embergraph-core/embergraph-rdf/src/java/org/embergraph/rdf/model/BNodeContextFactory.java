package org.embergraph.rdf.model;

import java.util.Date;
import java.util.UUID;
import javax.xml.datatype.XMLGregorianCalendar;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/*
* Class provides a document-scoped context for generating blank node identifiers (IDs). This class
 * is intended for use to generate blank node identifiers (IDs) for a source document within a
 * shared prefix. When that prefix is mapped onto the term2id index, all blank nodes for the same
 * source document will tend to be mapped onto the same index partition (this is more efficient
 * since the ordered writes are more local). The prefix itself is a {@link UUID}, which is how we
 * ensure that blank nodes generated for different sources are understood as distinct blank nodes by
 * the database (this is required).
 *
 * <p>Note: All {@link EmbergraphValue} instances are in fact created by the delegate instances
 * created by this class and by the delegate will appear to have been created by the same factory.
 * This is intentional - it makes the instances reusable by the base factory.
 *
 * <p>Note: {@link #nextID()} is NOT thread-safe, but different documents that are being passed
 * concurrently will have a distinct instance of this factory and processing a single document is
 * generally single-threaded.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: BNodeContextFactory.java 2547 2010-03-24 20:44:07Z thompsonbry $
 */
public class BNodeContextFactory implements EmbergraphValueFactory {

  public String getNamespace() {
    return valueFactory.getNamespace();
  }

  public void remove(/*String namespace*/ ) {
    // NOP
  }

  /** Provides the globally unique context for the generated blank node identifiers. */
  private final UUID contextUuid = UUID.randomUUID();

  /** The delegate factory. */
  private final EmbergraphValueFactory valueFactory;

  public BNodeContextFactory(final EmbergraphValueFactory valueFactory) {

    this.valueFactory = valueFactory;
  }

  /*
   * A globally unique blank node identifier (ID) created with prefix shared by all blank nodes
   * whose identifers are generated by this class.
   */
  protected String nextID() {

    return "_" + contextUuid + nextID++;
  }

  private int nextID;

  public EmbergraphBNode createBNode() {
    return valueFactory.createBNode(nextID());
  }

  /*
   * Delegate pattern.
   */

  @Override
  public EmbergraphValue asValue(Value v) {
    return valueFactory.asValue(v);
  }

  @Override
  public EmbergraphBNode createBNode(String id) {
    return valueFactory.createBNode(id);
  }

  @Override
  public EmbergraphBNode createBNode(EmbergraphStatement stmt) {
    return valueFactory.createBNode(stmt);
  }

  @Override
  public EmbergraphLiteral createLiteral(boolean arg0) {
    return valueFactory.createLiteral(arg0);
  }

  @Override
  public EmbergraphLiteral createLiteral(byte arg0) {
    return valueFactory.createLiteral(arg0);
  }

  @Override
  public EmbergraphLiteral createLiteral(double arg0) {
    return valueFactory.createLiteral(arg0);
  }

  @Override
  public EmbergraphLiteral createLiteral(float arg0) {
    return valueFactory.createLiteral(arg0);
  }

  @Override
  public EmbergraphLiteral createLiteral(int arg0, boolean unsigned) {
    return valueFactory.createLiteral(arg0, unsigned);
  }

  @Override
  public EmbergraphLiteral createLiteral(long arg0, boolean unsigned) {
    return valueFactory.createLiteral(arg0, unsigned);
  }

  @Override
  public EmbergraphLiteral createLiteral(short arg0, boolean unsigned) {
    return valueFactory.createLiteral(arg0, unsigned);
  }

  @Override
  public EmbergraphLiteral createLiteral(byte arg0, boolean unsigned) {
    return valueFactory.createLiteral(arg0, unsigned);
  }

  @Override
  public EmbergraphLiteral createLiteral(int arg0) {
    return valueFactory.createLiteral(arg0);
  }

  @Override
  public EmbergraphLiteral createLiteral(long arg0) {
    return valueFactory.createLiteral(arg0);
  }

  @Override
  public EmbergraphLiteral createLiteral(short arg0) {
    return valueFactory.createLiteral(arg0);
  }

  @Override
  public EmbergraphLiteral createLiteral(String label, String language) {
    return valueFactory.createLiteral(label, language);
  }

  @Override
  public EmbergraphLiteral createLiteral(String label, URI datatype) {
    return valueFactory.createLiteral(label, datatype);
  }

  @Override
  public EmbergraphLiteral createLiteral(String label, URI datatype, String language) {
    return valueFactory.createLiteral(label, datatype, language);
  }

  @Override
  public EmbergraphLiteral createLiteral(String label) {
    return valueFactory.createLiteral(label);
  }

  @Override
  public EmbergraphLiteral createLiteral(XMLGregorianCalendar arg0) {
    return valueFactory.createLiteral(arg0);
  }

  @Override
  public EmbergraphLiteral createLiteral(Date arg0) {
    return valueFactory.createLiteral(arg0);
  }

  @Override
  public EmbergraphStatement createStatement(Resource s, URI p, Value o) {
    return valueFactory.createStatement(s, p, o);
  }

  @Override
  public EmbergraphStatement createStatement(Resource s, URI p, Value o, Resource c) {
    return valueFactory.createStatement(s, p, o, c);
  }

  @Override
  public EmbergraphStatement createStatement(
      Resource s, URI p, Value o, Resource c, StatementEnum type) {
    return valueFactory.createStatement(s, p, o, c, type, false);
  }

  @Override
  public EmbergraphStatement createStatement(
      Resource s, URI p, Value o, Resource c, StatementEnum type, boolean userFlag) {
    return valueFactory.createStatement(s, p, o, c, type, userFlag);
  }

  @Override
  public EmbergraphURI createURI(String namespace, String localName) {
    return valueFactory.createURI(namespace, localName);
  }

  @Override
  public EmbergraphURI createURI(String uriString) {
    return valueFactory.createURI(uriString);
  }

  @Override
  public EmbergraphValueSerializer<EmbergraphValue> getValueSerializer() {
    return valueFactory.getValueSerializer();
  }

  /** Recursive contexts are not available (should not be necessary, right?) */
  @Override
  public EmbergraphValueFactory newBNodeContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public EmbergraphResource asValue(Resource v) {
    return valueFactory.asValue(v);
  }

  @Override
  public EmbergraphURI asValue(URI v) {
    return valueFactory.asValue(v);
  }

  @Override
  public EmbergraphLiteral asValue(Literal v) {
    return valueFactory.asValue(v);
  }

  @Override
  public EmbergraphBNode asValue(BNode v) {
    return valueFactory.asValue(v);
  }

  @Override
  public EmbergraphLiteral createXSDDateTime(long timestamp) {
    return valueFactory.createXSDDateTime(timestamp);
  }
}
