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
/*
 * Pulled in to extend TestCase.
 */
package org.openrdf.query.parser.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.SESAME;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.QueryResults;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerBase;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * A set of compliance tests on SPARQL query functionality which can not be easily executed using
 * the {@link SPARQL11ManifestTest} format. This includes tests on queries with non-deterministic
 * output (e.g. GROUP_CONCAT).
 *
 * @author Jeen Broekstra
 */
public abstract class ComplexSPARQLQueryTest extends TestCase {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Repository rep;

  protected RepositoryConnection conn;

  protected ValueFactory f;

  protected static final String EX_NS = "http://example.org/";

  private URI bob;

  private URI alice;

  private URI mary;

  /** @throws java.lang.Exception */
  @Before
  public void setUp() throws Exception {
    logger.debug("setting up test");
    this.rep = newRepository();
    rep.initialize();

    f = rep.getValueFactory();
    conn = rep.getConnection();

    conn.clear(); // clear existing data from repo

    bob = f.createURI(EX_NS, "bob");
    alice = f.createURI(EX_NS, "alice");
    mary = f.createURI(EX_NS, "mary");

    logger.debug("test setup complete.");
  }

  /** @throws java.lang.Exception */
  @After
  public void tearDown() throws Exception {
    logger.debug("tearing down...");
    conn.close();
    conn = null;

    rep.shutDown();
    rep = null;

    logger.debug("tearDown complete.");
  }

  @Test
  public void testNullContext1() throws Exception {
    loadTestData("/testdata-query/dataset-query.trig");

    String query = " SELECT * "
        + " FROM DEFAULT "
        + " WHERE { ?s ?p ?o } ";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      while (result.hasNext()) {
        BindingSet bs = result.next();
        assertNotNull(bs);

        Resource s = (Resource) bs.getValue("s");

        assertNotNull(s);
        assertFalse(bob.equals(s)); // should not be present in default
        // graph
        assertFalse(alice.equals(s)); // should not be present in default
        // graph
      }
      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testNullContext2() throws Exception {
    loadTestData("/testdata-query/dataset-query.trig");

    String query = getNamespaceDeclarations()
        + " SELECT * "
        + " FROM sesame:nil "
        + " WHERE { ?s ?p ?o } ";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      while (result.hasNext()) {
        BindingSet bs = result.next();
        assertNotNull(bs);

        Resource s = (Resource) bs.getValue("s");

        assertNotNull(s);
        assertFalse(bob.equals(s)); // should not be present in default
        // graph
        assertFalse(alice.equals(s)); // should not be present in default
        // graph
      }
      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testDescribeA() throws Exception {
    loadTestData("/testdata-query/dataset-describe.trig");

    String query = getNamespaceDeclarations()
        + "DESCRIBE ex:a";
    GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

    ValueFactory f = conn.getValueFactory();
    URI a = f.createURI("http://example.org/a");
    URI p = f.createURI("http://example.org/p");
    Model result = QueryResults.asModel(gq.evaluate());
    Set<Value> objects = result.filter(a, p, null).objects();
    assertNotNull(objects);
    for (Value object : objects) {
      if (object instanceof BNode) {
        assertTrue(result.contains((Resource) object, null, null));
        assertEquals(2, result.filter((Resource) object, null, null).size());
      }
    }
  }

  @Test
  public void testDescribeAWhere() throws Exception {
    loadTestData("/testdata-query/dataset-describe.trig");

    String query = getNamespaceDeclarations()
        + "DESCRIBE ?x WHERE {?x rdfs:label \"a\". } ";
    GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL,
        query);

    ValueFactory f = conn.getValueFactory();
    URI a = f.createURI("http://example.org/a");
    URI p = f.createURI("http://example.org/p");
    Model result = QueryResults.asModel(gq.evaluate());
    Set<Value> objects = result.filter(a, p, null).objects();
    assertNotNull(objects);
    for (Value object : objects) {
      if (object instanceof BNode) {
        assertTrue(result.contains((Resource) object, null, null));
        assertEquals(2, result.filter((Resource) object, null, null).size());
      }
    }
  }

  @Test
  public void testDescribeWhere() throws Exception {
    loadTestData("/testdata-query/dataset-describe.trig");

    String query = getNamespaceDeclarations()
        + "DESCRIBE ?x WHERE {?x rdfs:label ?y . } ";
    GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL,
        query);

    ValueFactory vf = conn.getValueFactory();
    URI a = vf.createURI("http://example.org/a");
    URI b = vf.createURI("http://example.org/b");
    URI c = vf.createURI("http://example.org/c");
    URI e = vf.createURI("http://example.org/e");
    URI f = vf.createURI("http://example.org/f");
    URI p = vf.createURI("http://example.org/p");

    Model result = QueryResults.asModel(gq.evaluate());
    assertTrue(result.contains(a, p, null));
    assertTrue(result.contains(b, RDFS.LABEL, null));
    assertTrue(result.contains(c, RDFS.LABEL, null));
    assertTrue(result.contains(null, p, b));
    assertTrue(result.contains(e, RDFS.LABEL, null));
    assertTrue(result.contains(null, p, e));
    assertFalse(result.contains(f, null, null));
    Set<Value> objects = result.filter(a, p, null).objects();
    assertNotNull(objects);
    for (Value object : objects) {
      if (object instanceof BNode) {
        assertTrue(result.contains((Resource) object, null, null));
        assertEquals(2, result.filter((Resource) object, null, null).size());
      }
    }
  }

  @Test
  public void testDescribeB() throws Exception {
    loadTestData("/testdata-query/dataset-describe.trig");

    String query = getNamespaceDeclarations()
        + "DESCRIBE ex:b";
    GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

    ValueFactory f = conn.getValueFactory();
    URI b = f.createURI("http://example.org/b");
    URI p = f.createURI("http://example.org/p");
    Model result = QueryResults.asModel(gq.evaluate());
    Set<Resource> subjects = result.filter(null, p, b).subjects();
    assertNotNull(subjects);
    for (Value subject : subjects) {
      if (subject instanceof BNode) {
        assertTrue(result.contains(null, null, subject));
      }
    }
  }

  @Test
  public void testDescribeD() throws Exception {
    loadTestData("/testdata-query/dataset-describe.trig");

    String query = getNamespaceDeclarations()
        + "DESCRIBE ex:d";
    GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

    ValueFactory f = conn.getValueFactory();
    URI d = f.createURI("http://example.org/d");
    URI p = f.createURI("http://example.org/p");
    URI e = f.createURI("http://example.org/e");
    Model result = QueryResults.asModel(gq.evaluate());

    assertNotNull(result);
    assertTrue(result.contains(null, p, e));
    assertFalse(result.contains(e, null, null));
    Set<Value> objects = result.filter(d, p, null).objects();
    assertNotNull(objects);
    for (Value object : objects) {
      if (object instanceof BNode) {
        Set<Value> childObjects = result.filter((BNode) object, null, null).objects();
        assertNotNull(childObjects);
        for (Value childObject : childObjects) {
          if (childObject instanceof BNode) {
            assertTrue(result.contains((BNode) childObject, null, null));
          }
        }
      }
    }
  }

  @Test
  public void testDescribeF() throws Exception {
    loadTestData("/testdata-query/dataset-describe.trig");

    String query = getNamespaceDeclarations()
        + "DESCRIBE ex:f";
    GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

    ValueFactory vf = conn.getValueFactory();
    URI f = vf.createURI("http://example.org/f");
    URI p = vf.createURI("http://example.org/p");
    Model result = QueryResults.asModel(gq.evaluate());

    assertNotNull(result);
    assertEquals(4, result.size());
    Set<Value> objects = result.filter(f, p, null).objects();
    assertNotNull(objects);
    for (Value object : objects) {
      if (object instanceof BNode) {
        Set<Value> childObjects = result.filter((BNode) object, null, null).objects();
        assertNotNull(childObjects);
        for (Value childObject : childObjects) {
          if (childObject instanceof BNode) {
            assertTrue(result.contains((BNode) childObject, null, null));
          }
        }
      }
    }
  }

  @Test
  public void testGroupConcatDistinct() throws Exception {
    loadTestData("/testdata-query/dataset-query.trig");

    String query = getNamespaceDeclarations()
        + "SELECT (GROUP_CONCAT(DISTINCT ?l) AS ?concat)"
        + "WHERE { ex:groupconcat-test ?p ?l . }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      while (result.hasNext()) {
        BindingSet bs = result.next();
        assertNotNull(bs);

        Value concat = bs.getValue("concat");

        assertTrue(concat instanceof Literal);

        String lexValue = ((Literal) concat).getLabel();

        int occ = countCharOccurrences(lexValue, 'a');
        assertEquals(1, occ);
        occ = countCharOccurrences(lexValue, 'b');
        assertEquals(1, occ);
        occ = countCharOccurrences(lexValue, 'c');
        assertEquals(1, occ);
        occ = countCharOccurrences(lexValue, 'd');
        assertEquals(1, occ);
      }
      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSameTermRepeatInOptional() throws Exception {
    loadTestData("/testdata-query/dataset-query.trig");

    String query = getNamespaceDeclarations()
        + " SELECT ?l ?opt1 ?opt2 "
        + " FROM ex:optional-sameterm-graph "
        + " WHERE { "
        + "          ?s ex:p ex:A ; "
        + "          { "
        + "              { "
        + "                 ?s ?p ?l ."
        + "                 FILTER(?p = rdfs:label) "
        + "              } "
        + "              OPTIONAL { "
        + "                 ?s ?p ?opt1 . "
        + "                 FILTER (?p = ex:prop1) "
        + "              } "
        + "              OPTIONAL { "
        + "                 ?s ?p ?opt2 . "
        + "                 FILTER (?p = ex:prop2) "
        + "              } "
        + "          }"
        + " } ";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        BindingSet bs = result.next();
        count++;
        assertNotNull(bs);

        System.out.println(bs);

        Value l = bs.getValue("l");
        assertTrue(l instanceof Literal);
        assertEquals("label", ((Literal) l).getLabel());

        Value opt1 = bs.getValue("opt1");
        assertNull(opt1);

        Value opt2 = bs.getValue("opt2");
        assertNull(opt2);
      }
      result.close();

      assertEquals(1, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSES1121VarNamesInOptionals() throws Exception {
    // Verifying that variable names have no influence on order of optionals
    // in query. See SES-1121.

    loadTestData("/testdata-query/dataset-ses1121.trig");

    String query1 = getNamespaceDeclarations()
        + " SELECT DISTINCT *\n"
        + " WHERE { GRAPH ?g { \n"
        + "          OPTIONAL { ?var35 ex:p ?b . } \n "
        + "          OPTIONAL { ?b ex:q ?c . } \n "
        + "       } \n"
        + " } \n";
    TupleQuery tq1 = conn.prepareTupleQuery(QueryLanguage.SPARQL, query1);
    String query2 = getNamespaceDeclarations()
        + " SELECT DISTINCT *\n"
        + " WHERE { GRAPH ?g { \n"
        + "          OPTIONAL { ?var35 ex:p ?b . } \n "
        + "          OPTIONAL { ?b ex:q ?var2 . } \n "
        + "       } \n"
        + " } \n";
    TupleQuery tq2 = conn.prepareTupleQuery(QueryLanguage.SPARQL, query2);

    try {
      TupleQueryResult result1 = tq1.evaluate();
      assertNotNull(result1);

      TupleQueryResult result2 = tq2.evaluate();
      assertNotNull(result2);

      List<BindingSet> qr1 = QueryResults.asList(result1);
      List<BindingSet> qr2 = QueryResults.asList(result2);

      // System.out.println(qr1);
      // System.out.println(qr2);

      // if optionals are not kept in same order, query results will be
      // different size.
      assertEquals(qr1.size(), qr2.size());

    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSES1081SameTermWithValues() throws Exception {
    loadTestData("/testdata-query/dataset-ses1081.trig");

    String query = "PREFIX ex: <http://example.org/>\n"
        + " SELECT * \n"
        + " WHERE { \n "
        + "          ?s ex:p ?a . \n"
        + "          FILTER sameTerm(?a, ?e) \n "
        + "          VALUES ?e { ex:b } \n "
        + " } ";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        BindingSet bs = result.next();
        count++;
        assertNotNull(bs);

        Value s = bs.getValue("s");
        Value a = bs.getValue("a");

        assertNotNull(s);
        assertNotNull(a);
        assertEquals(f.createURI("http://example.org/a"), s);
        assertEquals(f.createURI("http://example.org/b"), a);
      }
      result.close();

      assertEquals(1, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSES1898LeftJoinSemantics1() throws Exception {
    loadTestData("/testdata-query/dataset-ses1898.trig");

    String query = "  PREFIX : <http://example.org/> "
        + "  SELECT * WHERE { "
        + "    ?s :p1 ?v1 . "
        + "    OPTIONAL {?s :p2 ?v2 } ."
        + "     ?s :p3 ?v2 . "
        + "  } ";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        result.next();
        count++;
      }
      assertEquals(0, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSES1073InverseSymmetricPattern() throws Exception {
    URI a = f.createURI("http://example.org/a");
    URI b1 = f.createURI("http://example.org/b1");
    URI b2 = f.createURI("http://example.org/b2");
    URI c1 = f.createURI("http://example.org/c1");
    URI c2 = f.createURI("http://example.org/c2");
    URI a2b = f.createURI("http://example.org/a2b");
    URI b2c = f.createURI("http://example.org/b2c");
    conn.add(a, a2b, b1);
    conn.add(a, a2b, b2);
    conn.add(b1, b2c, c1);
    conn.add(b2, b2c, c2);
    String query = "select * ";
    query += "where{ ";
    query +=
        "?c1 ^<http://example.org/b2c>/^<http://example.org/a2b>/<http://example.org/a2b>/<http://example.org/b2c> ?c2 . ";
    query += " } ";
    TupleQueryResult qRes = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();
    try {
      assertTrue(qRes.hasNext());
      int count = 0;
      while (qRes.hasNext()) {
        BindingSet r = qRes.next();
        System.out.println(r);
        count++;
      }
      assertEquals(4, count);
    } finally {
      qRes.close();
    }
  }

  @Test
  public void testSES1970CountDistinctWildcard() throws Exception {
    loadTestData("/testdata-query/dataset-ses1970.trig");

    String query = "SELECT (COUNT(DISTINCT *) AS ?c) {?s ?p ?o }";

    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      assertTrue(result.hasNext());
      BindingSet s = result.next();
      Literal count = (Literal) s.getValue("c");
      assertNotNull(count);

      assertEquals(3, count.intValue());

      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSES1898LeftJoinSemantics2() throws Exception {
    loadTestData("/testdata-query/dataset-ses1898.trig");

    String query = "  PREFIX : <http://example.org/> "
        + "  SELECT * WHERE { "
        + "    ?s :p1 ?v1 . "
        + "    ?s :p3 ?v2 . "
        + "    OPTIONAL {?s :p2 ?v2 } ."
        + "  } ";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        result.next();
        count++;
      }
      assertEquals(1, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testIdenticalVariablesInStatementPattern() throws Exception {
    conn.add(alice, f.createURI("http://purl.org/dc/elements/1.1/publisher"), bob);

    String queryBuilder = "SELECT ?publisher "
        + "{ ?publisher <http://purl.org/dc/elements/1.1/publisher> ?publisher }";
    conn.prepareTupleQuery(QueryLanguage.SPARQL,
        queryBuilder)
        .evaluate(
            new TupleQueryResultHandlerBase() {

              public void handleSolution(BindingSet bindingSet) {
                fail("nobody is self published");
              }
            });
  }

  @Test
  public void testInComparison1() throws Exception {
    loadTestData("/testdata-query/dataset-ses1913.trig");

    String query = " PREFIX : <http://example.org/>\n"
        + " SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1/0 , 1)) } ";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    TupleQueryResult result = tq.evaluate();
    assertNotNull(result);
    assertTrue(result.hasNext());

    BindingSet bs = result.next();
    Value y = bs.getValue("y");
    assertNotNull(y);
    assertTrue(y instanceof Literal);
    assertEquals(f.createLiteral("1", XMLSchema.INTEGER), y);
  }

  @Test
  public void testInComparison2() throws Exception {
    loadTestData("/testdata-query/dataset-ses1913.trig");

    String query = " PREFIX : <http://example.org/>\n"
        + " SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1/0)) } ";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    TupleQueryResult result = tq.evaluate();
    assertNotNull(result);
    assertFalse(result.hasNext());
  }

  @Test
  public void testInComparison3() throws Exception {
    loadTestData("/testdata-query/dataset-ses1913.trig");

    String query = " PREFIX : <http://example.org/>\n"
        + " SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1, 1/0)) } ";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    TupleQueryResult result = tq.evaluate();
    assertNotNull(result);
    assertTrue(result.hasNext());

    BindingSet bs = result.next();
    Value y = bs.getValue("y");
    assertNotNull(y);
    assertTrue(y instanceof Literal);
    assertEquals(f.createLiteral("1", XMLSchema.INTEGER), y);
  }

  @Test
  public void testValuesInOptional() throws Exception {
    loadTestData("/testdata-query/dataset-ses1692.trig");

    String query = " PREFIX : <http://example.org/>\n"
        + " SELECT DISTINCT ?a ?name ?isX WHERE { ?b :p1 ?a . ?a :name ?name. OPTIONAL { ?a a :X . VALUES(?isX) { (:X) } } } ";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    TupleQueryResult result = tq.evaluate();
    assertNotNull(result);
    assertTrue(result.hasNext());

    int count = 0;
    while (result.hasNext()) {
      count++;
      BindingSet bs = result.next();
      System.out.println(bs);
      URI a = (URI) bs.getValue("a");
      assertNotNull(a);
      Value isX = bs.getValue("isX");
      Literal name = (Literal) bs.getValue("name");
      assertNotNull(name);
      if (a.stringValue().endsWith("a1")) {
        assertNotNull(isX);
      } else if (a.stringValue().endsWith(("a2"))) {
        assertNull(isX);
      }
    }
    assertEquals(2, count);
  }

  @Test
  public void testSameTermRepeatInUnion() throws Exception {
    loadTestData("/testdata-query/dataset-query.trig");

    String query = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
        + "SELECT * {\n"
        + "    {\n"
        + "        ?sameTerm foaf:mbox ?mbox\n"
        + "        FILTER sameTerm(?sameTerm,$william)\n"
        + "    } UNION {\n"
        + "        ?x foaf:knows ?sameTerm\n"
        + "        FILTER sameTerm(?sameTerm,$william)\n"
        + "    }\n"
        + "}";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
    tq.setBinding("william", conn.getValueFactory().createURI("http://example.org/william"));

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        BindingSet bs = result.next();
        count++;
        assertNotNull(bs);

        System.out.println(bs);

        Value mbox = bs.getValue("mbox");
        Value x = bs.getValue("x");

        assertTrue(mbox instanceof Literal || x instanceof URI);
      }
      result.close();

      assertEquals(3, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSameTermRepeatInUnionAndOptional() throws Exception {
    loadTestData("/testdata-query/dataset-query.trig");

    String query = getNamespaceDeclarations()
        + "SELECT * {\n"
        + "    {\n"
        + "        ex:a ?p ?prop1\n"
        + "        FILTER (?p = ex:prop1)\n"
        + "    } UNION {\n"
        + "          ?s ex:p ex:A ; "
        + "          { "
        + "              { "
        + "                 ?s ?p ?l ."
        + "                 FILTER(?p = rdfs:label) "
        + "              } "
        + "              OPTIONAL { "
        + "                 ?s ?p ?opt1 . "
        + "                 FILTER (?p = ex:prop1) "
        + "              } "
        + "              OPTIONAL { "
        + "                 ?s ?p ?opt2 . "
        + "                 FILTER (?p = ex:prop2) "
        + "              } "
        + "          }"
        + "    }\n"
        + "}";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        BindingSet bs = result.next();
        count++;
        assertNotNull(bs);

        System.out.println(bs);

        Value prop1 = bs.getValue("prop1");
        Value l = bs.getValue("l");

        assertTrue(prop1 instanceof Literal || l instanceof Literal);
        if (l instanceof Literal) {
          Value opt1 = bs.getValue("opt1");
          assertNull(opt1);

          Value opt2 = bs.getValue("opt2");
          assertNull(opt2);
        }
      }
      result.close();

      assertEquals(2, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testPropertyPathInTree() throws Exception {
    loadTestData("/testdata-query/dataset-query.trig");

    String query = getNamespaceDeclarations()
        + " SELECT ?node ?name "
        + " FROM ex:tree-graph "
        + " WHERE { ?node ex:hasParent+ ex:b . ?node ex:name ?name . }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      while (result.hasNext()) {
        BindingSet bs = result.next();
        assertNotNull(bs);

        System.out.println(bs);
      }
      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testFilterRegexBoolean() throws Exception {
    loadTestData("/testdata-query/dataset-query.trig");

    // test case for issue SES-1050
    // query.append("            FILTER(REGEX(?mbox, \"bob\")) ");

    String query = getNamespaceDeclarations()
        + " SELECT *"
        + " WHERE { "
        + "       ?x foaf:name ?name ; "
        + "          foaf:mbox ?mbox . "
        + "       FILTER(EXISTS { "
        + "            FILTER(REGEX(?name, \"Bo\") && REGEX(?mbox, \"bob\")) "
        + "            } )"
        + " } ";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      assertTrue(result.hasNext());
      int count = 0;
      while (result.hasNext()) {
        BindingSet bs = result.next();
        count++;
        System.out.println(bs);
      }
      assertEquals(1, count);

      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testGroupConcatNonDistinct() throws Exception {
    loadTestData("/testdata-query/dataset-query.trig");

    String query = getNamespaceDeclarations()
        + "SELECT (GROUP_CONCAT(?l) AS ?concat)"
        + "WHERE { ex:groupconcat-test ?p ?l . }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      while (result.hasNext()) {
        BindingSet bs = result.next();
        assertNotNull(bs);

        Value concat = bs.getValue("concat");

        assertTrue(concat instanceof Literal);

        String lexValue = ((Literal) concat).getLabel();

        int occ = countCharOccurrences(lexValue, 'a');
        assertEquals(1, occ);
        occ = countCharOccurrences(lexValue, 'b');
        assertEquals(2, occ);
        occ = countCharOccurrences(lexValue, 'c');
        assertEquals(2, occ);
        occ = countCharOccurrences(lexValue, 'd');
        assertEquals(1, occ);
      }
      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  /*
   * @see http://www.openrdf.org/issues/browse/SES-1091
   * @throws Exception
   */
  public void testArbitraryLengthPathWithBinding1() throws Exception {
    loadTestData("/testdata-query/alp-testdata.ttl");

    String query = getNamespaceDeclarations()
        + "SELECT ?parent ?child "
        + "WHERE { ?child a owl:Class . ?child rdfs:subClassOf+ ?parent . }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      // first execute without binding
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(7, count);

      // execute again, but this time setting a binding
      tq.setBinding("parent", OWL.THING);

      result = tq.evaluate();
      assertNotNull(result);

      count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(4, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  /*
   * @see http://www.openrdf.org/issues/browse/SES-1091
   * @throws Exception
   */
  public void testArbitraryLengthPathWithBinding2() throws Exception {
    loadTestData("/testdata-query/alp-testdata.ttl");

    // query without initializing ?child first.

    String query = getNamespaceDeclarations()
        + "SELECT ?parent ?child "
        + "WHERE { ?child rdfs:subClassOf+ ?parent . }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      // first execute without binding
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(7, count);

      // execute again, but this time setting a binding
      tq.setBinding("parent", OWL.THING);

      result = tq.evaluate();
      assertNotNull(result);

      count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(4, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  /*
   * @see http://www.openrdf.org/issues/browse/SES-1091
   * @throws Exception
   */
  public void testArbitraryLengthPathWithBinding3() throws Exception {
    loadTestData("/testdata-query/alp-testdata.ttl");

    // binding on child instead of parent.

    String query = getNamespaceDeclarations()
        + "SELECT ?parent ?child "
        + "WHERE { ?child rdfs:subClassOf+ ?parent . }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      // first execute without binding
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(7, count);

      // execute again, but this time setting a binding
      tq.setBinding("child", f.createURI(EX_NS, "C"));

      result = tq.evaluate();
      assertNotNull(result);

      count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(2, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  /*
   * @see http://www.openrdf.org/issues/browse/SES-1091
   * @throws Exception
   */
  public void testArbitraryLengthPathWithBinding4() throws Exception {
    loadTestData("/testdata-query/alp-testdata.ttl", this.alice);

    // binding on child instead of parent.

    String query = getNamespaceDeclarations()
        + "SELECT ?parent ?child "
        + "WHERE { ?child rdfs:subClassOf+ ?parent . }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      // first execute without binding
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(7, count);

      // execute again, but this time setting a binding
      tq.setBinding("child", f.createURI(EX_NS, "C"));

      result = tq.evaluate();
      assertNotNull(result);

      count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(2, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  /*
   * @see http://www.openrdf.org/issues/browse/SES-1091
   * @throws Exception
   */
  public void testArbitraryLengthPathWithBinding5() throws Exception {
    loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob);

    // binding on child instead of parent.

    String query = getNamespaceDeclarations()
        + "SELECT ?parent ?child "
        + "WHERE { ?child rdfs:subClassOf+ ?parent . }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      // first execute without binding
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      System.out.println("--- testArbitraryLengthPathWithBinding5 ---");

      int count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();

        System.out.println(bs);

        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(7, count);

      // execute again, but this time setting a binding
      tq.setBinding("child", f.createURI(EX_NS, "C"));

      result = tq.evaluate();
      assertNotNull(result);

      count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(2, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  /*
   * @see http://www.openrdf.org/issues/browse/SES-1091
   * @throws Exception
   */
  public void testArbitraryLengthPathWithBinding6() throws Exception {
    loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob, this.mary);

    // binding on child instead of parent.

    String query = getNamespaceDeclarations()
        + "SELECT ?parent ?child "
        + "WHERE { ?child rdfs:subClassOf+ ?parent . }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      // first execute without binding
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      System.out.println("--- testArbitraryLengthPathWithBinding6 ---");

      int count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();

        System.out.println(bs);

        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(7, count);

      // execute again, but this time setting a binding
      tq.setBinding("child", f.createURI(EX_NS, "C"));

      result = tq.evaluate();
      assertNotNull(result);

      count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(2, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  /*
   * @see http://www.openrdf.org/issues/browse/SES-1091
   * @throws Exception
   */
  public void testArbitraryLengthPathWithBinding7() throws Exception {
    loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob, this.mary);

    // binding on child instead of parent.

    String query = getNamespaceDeclarations()
        + "SELECT ?parent ?child "
        + "WHERE { ?child rdfs:subClassOf+ ?parent . }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);
    DatasetImpl dt = new DatasetImpl();
    dt.addDefaultGraph(this.alice);
    tq.setDataset(dt);

    try {
      // first execute without binding
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      System.out.println("--- testArbitraryLengthPathWithBinding7 ---");

      int count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();

        System.out.println(bs);

        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(7, count);

      // execute again, but this time setting a binding
      tq.setBinding("child", f.createURI(EX_NS, "C"));

      result = tq.evaluate();
      assertNotNull(result);

      count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(2, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  /*
   * @see http://www.openrdf.org/issues/browse/SES-1091
   * @throws Exception
   */
  public void testArbitraryLengthPathWithBinding8() throws Exception {
    loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob, this.mary);

    // binding on child instead of parent.

    String query = getNamespaceDeclarations()
        + "SELECT ?parent ?child "
        + "WHERE { ?child rdfs:subClassOf+ ?parent . }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);
    DatasetImpl dt = new DatasetImpl();
    dt.addDefaultGraph(this.alice);
    dt.addDefaultGraph(this.bob);
    tq.setDataset(dt);

    try {
      // first execute without binding
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);
      System.out.println("--- testArbitraryLengthPathWithBinding8 ---");
      int count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();

        System.out.println(bs);

        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(7, count);

      // execute again, but this time setting a binding
      tq.setBinding("child", f.createURI(EX_NS, "C"));

      result = tq.evaluate();
      assertNotNull(result);

      count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(2, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  /*
   * @see http://www.openrdf.org/issues/browse/SES-1091
   * @throws Exception
   */
  public void testArbitraryLengthPathWithFilter1() throws Exception {
    loadTestData("/testdata-query/alp-testdata.ttl");

    String query = getNamespaceDeclarations()
        + "SELECT ?parent ?child "
        + "WHERE { ?child a owl:Class . ?child rdfs:subClassOf+ ?parent . FILTER (?parent = owl:Thing) }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(4, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  /*
   * @see http://www.openrdf.org/issues/browse/SES-1091
   * @throws Exception
   */
  public void testArbitraryLengthPathWithFilter2() throws Exception {
    loadTestData("/testdata-query/alp-testdata.ttl");

    String query = getNamespaceDeclarations()
        + "SELECT ?parent ?child "
        + "WHERE { ?child rdfs:subClassOf+ ?parent . FILTER (?parent = owl:Thing) }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(4, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  /*
   * @see http://www.openrdf.org/issues/browse/SES-1091
   * @throws Exception
   */
  public void testArbitraryLengthPathWithFilter3() throws Exception {
    loadTestData("/testdata-query/alp-testdata.ttl");

    String query = getNamespaceDeclarations()
        + "SELECT ?parent ?child "
        + "WHERE { ?child rdfs:subClassOf+ ?parent . FILTER (?child = <http://example.org/C>) }";
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
        query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      int count = 0;
      while (result.hasNext()) {
        count++;
        BindingSet bs = result.next();
        assertTrue(bs.hasBinding("child"));
        assertTrue(bs.hasBinding("parent"));
      }
      assertEquals(2, count);
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSES1991UUIDEvaluation() throws Exception {
    loadTestData("/testdata-query/defaultgraph.ttl");
    String query = "SELECT ?uid WHERE {?s ?p ?o . BIND(UUID() as ?uid) } LIMIT 2";

    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      URI uuid1 = (URI) result.next().getValue("uid");
      URI uuid2 = (URI) result.next().getValue("uid");

      assertNotNull(uuid1);
      assertNotNull(uuid2);
      assertFalse(uuid1.equals(uuid2));

      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSES1991STRUUIDEvaluation() throws Exception {
    loadTestData("/testdata-query/defaultgraph.ttl");
    String query = "SELECT ?uid WHERE {?s ?p ?o . BIND(STRUUID() as ?uid) } LIMIT 2";

    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      Literal uid1 = (Literal) result.next().getValue("uid");
      Literal uid2 = (Literal) result.next().getValue("uid");

      assertNotNull(uid1);
      assertFalse(uid1.equals(uid2));

      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSES1991RANDEvaluation() throws Exception {
    loadTestData("/testdata-query/defaultgraph.ttl");
    String query = "SELECT ?r WHERE {?s ?p ?o . BIND(RAND() as ?r) } LIMIT 3";

    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      Literal r1 = (Literal) result.next().getValue("r");
      Literal r2 = (Literal) result.next().getValue("r");
      Literal r3 = (Literal) result.next().getValue("r");

      assertNotNull(r1);

      // there is a small chance that two successive calls to the random
      // number generator will generate the exact same value, so we check for
      // three successive calls (still theoretically possible to be
      // identical, but phenomenally unlikely).
      assertFalse(r1.equals(r2) && r1.equals(r3));

      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSES1991NOWEvaluation() throws Exception {
    loadTestData("/testdata-query/defaultgraph.ttl");
    String query = "SELECT ?d WHERE {?s ?p ?o . BIND(NOW() as ?d) } LIMIT 2";

    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      Literal d1 = (Literal) result.next().getValue("d");
      Literal d2 = (Literal) result.next().getValue("d");

      assertNotNull(d1);
      assertEquals(d1, d2);

      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSES2024PropertyPathAnonVarSharing() throws Exception {
    loadTestData("/testdata-query/dataset-ses2024.trig");
    String query =
        "PREFIX : <http://example.org/> SELECT * WHERE { ?x1 :p/:lit ?l1 . ?x1 :diff ?x2 . ?x2 :p/:lit ?l2 . }";

    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try {
      TupleQueryResult result = tq.evaluate();
      assertNotNull(result);

      BindingSet bs = result.next();
      Literal l1 = (Literal) bs.getValue("l1");
      Literal l2 = (Literal) bs.getValue("l2");

      assertNotNull(l1);
      assertFalse(l1.equals(l2));

      result.close();
    } catch (QueryEvaluationException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  /* private / protected methods */

  private int countCharOccurrences(String string, char ch) {
    int count = 0;
    for (int i = 0; i < string.length(); i++) {
      if (string.charAt(i) == ch) {
        count++;
      }
    }
    return count;
  }

  /*
   * Get a set of useful namespace prefix declarations.
   *
   * @return namespace prefix declarations for dc, foaf and ex.
   */
  protected String getNamespaceDeclarations() {

    String declarations = ("PREFIX dc: <" + DCTERMS.NAMESPACE + "> \n")
        + "PREFIX foaf: <" + FOAF.NAMESPACE + "> \n"
        + "PREFIX sesame: <" + SESAME.NAMESPACE + "> \n"
        + "PREFIX ex: <" + EX_NS + "> \n"
        + "\n";
    return declarations;
  }

  protected abstract Repository newRepository();

  protected void loadTestData(String dataFile, Resource... contexts)
      throws RDFParseException, RepositoryException, IOException {
    logger.debug("loading dataset {}", dataFile);
    InputStream dataset = ComplexSPARQLQueryTest.class.getResourceAsStream(dataFile);
    try {
      conn.add(dataset, "", RDFFormat.forFileName(dataFile), contexts);
    } finally {
      dataset.close();
    }
    logger.debug("dataset loaded.");
  }
}
