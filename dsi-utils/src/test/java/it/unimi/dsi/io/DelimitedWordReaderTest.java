package it.unimi.dsi.io;

import junit.framework.TestCase;

public class DelimitedWordReaderTest extends TestCase {

  public void testToSpec() {
    String className = DelimitedWordReader.class.getName();
    assertEquals(className + "(\"_\")", new DelimitedWordReader("_").toSpec());
    assertEquals(className + "(100,\"_\")", new DelimitedWordReader("100", "_").toSpec());
  }
}
