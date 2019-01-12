/*
 * Licensed to Aduna under one or more contributor license agreements.
 * See the NOTICE.txt file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD
 * License (the "License"); you may not use this file except in compliance
 * with the License. See the LICENSE.txt file distributed with this work
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.embergraph.rdf.rio.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.openrdf.model.Value;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;

/*
* A TupleQueryResultWriter that writes query results in the <a
 * href="http://www.w3.org/TR/rdf-sparql-json-res/">SPARQL Query Results JSON Format</a>.
 */
public class EmbergraphSPARQLResultsJSONWriter extends SPARQLJSONWriterBase
    implements TupleQueryResultWriter {

  /*--------------*
   * Constructors *
   *--------------*/

  public EmbergraphSPARQLResultsJSONWriter(Writer writer) {
    super(writer);
  }

  public EmbergraphSPARQLResultsJSONWriter(OutputStream out) {
    super(out);
  }

  /*---------*
   * Methods *
   *---------*/

  @Override
  public final TupleQueryResultFormat getTupleQueryResultFormat() {
    return TupleQueryResultFormat.JSON;
  }

  @Override
  public TupleQueryResultFormat getQueryResultFormat() {
    return getTupleQueryResultFormat();
  }

  @Override
  protected void writeValue(final Value value) throws IOException, QueryResultHandlerException {

    if (value instanceof EmbergraphBNode && ((EmbergraphBNode) value).isStatementIdentifier()) {

      writeSid((EmbergraphBNode) value);

    } else {

      super.writeValue(value);
    }
  }

  protected void writeSid(final EmbergraphBNode sid)
      throws IOException, QueryResultHandlerException {

    jg.writeStartObject();

    jg.writeStringField("type", EmbergraphSPARQLResultsJSONParser.SID);

    final EmbergraphStatement stmt = sid.getStatement();

    jg.writeFieldName(EmbergraphSPARQLResultsJSONParser.SUBJECT);
    writeValue(stmt.getSubject());

    jg.writeFieldName(EmbergraphSPARQLResultsJSONParser.PREDICATE);
    writeValue(stmt.getPredicate());

    jg.writeFieldName(EmbergraphSPARQLResultsJSONParser.OBJECT);
    writeValue(stmt.getObject());

    if (stmt.getContext() != null) {
      jg.writeFieldName(EmbergraphSPARQLResultsJSONParser.CONTEXT);
      writeValue(stmt.getContext());
    }

    jg.writeEndObject();
  }
}
