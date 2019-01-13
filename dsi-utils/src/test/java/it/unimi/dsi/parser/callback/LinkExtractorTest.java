package it.unimi.dsi.parser.callback;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import junit.framework.TestCase;

public class LinkExtractorTest extends TestCase {

  public void testExtractor() {
    //		char[] text = IOUtils.toCharArray( this.getClass().getResourceAsStream(
    // "LinkExtractorTest1.html" ), "UTF-8" );
    //
    //		BulletParser parser = new BulletParser();
    //		LinkExtractor linkExtractor = new LinkExtractor();
    //		parser.setCallback( linkExtractor );
    //		parser.parse( text );
    //
    //		testExtractorResults( linkExtractor );
    //		Test resource not included in 1.10.0 source distribution
    assertTrue(true);
  }

  private void testExtractorResults(final LinkExtractor linkExtractor) {
    assertEquals(
        new ObjectLinkedOpenHashSet<>(
            new String[]{
                "manual.css", "http://link.com/", "http://anchor.com/", "http://badanchor.com/"
            }),
        linkExtractor.urls);
    assertEquals("http://base.com/", linkExtractor.base());
    assertEquals("http://refresh.com/", linkExtractor.metaRefresh());
    assertEquals("http://location.com/", linkExtractor.metaLocation());
  }
}
