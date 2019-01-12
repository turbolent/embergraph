package org.embergraph.rdf.sail.model;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAll extends TestCase {

  public static TestSuite suite() {

    final TestSuite suite = new TestSuite("JsonSerialization");

    suite.addTest(new TestSuite(TestJsonModelSerialization.class));

    return suite;
  }
}
