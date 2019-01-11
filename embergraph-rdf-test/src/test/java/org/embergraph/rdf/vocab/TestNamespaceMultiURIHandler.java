package org.embergraph.rdf.vocab;

import org.embergraph.rdf.internal.InlineHexUUIDURIHandler;
import org.embergraph.rdf.internal.InlineNamespaceMultiURIHandler;
import org.embergraph.rdf.internal.InlineSuffixedHexUUIDURIHandler;
import org.embergraph.rdf.internal.InlineURIFactory;

public class TestNamespaceMultiURIHandler extends InlineURIFactory {

	public TestNamespaceMultiURIHandler() {
		super();

		/*
		 * Examples of how to configure Hex-encoded UUID based URIs for
		 * inlining. You may also do this with integers with prefixes,
		 * suffixes, or a combination.
		 * 
		 * Each namespace inlined must have a corresponding vocabulary
		 * declaration.
		 */

		// http://blazegraph.com/Data#Position_010072F0000038090100000000D56C9E
		// http://blazegraph.com/Data#Position_010072F0000038090100000000D56C9E_TaxCost
		// http://blazegraph.com/Data#Position_010072F0000038090100000000D56C9E_UnrealizedGain
		// http://blazegraph.com/Data#Position_010072F0000038090100000000D56C9E_WashSale
		
		InlineNamespaceMultiURIHandler mHandler = new InlineNamespaceMultiURIHandler(
				"http://embergraph.org/Data#Position_");

		mHandler.addHandler(new InlineSuffixedHexUUIDURIHandler(
				"http://embergraph.org/Data#Position_", "_TaxCost"));

		mHandler.addHandler(new InlineSuffixedHexUUIDURIHandler(
				"http://embergraph.org/Data#Position_", "_UnrealizedGain"));

		mHandler.addHandler(new InlineSuffixedHexUUIDURIHandler(
				"http://embergraph.org/Data#Position_", "_WashSale"));

		mHandler.addHandler(new InlineHexUUIDURIHandler(
				"http://embergraph.org/Data#Position_"));


		this.addHandler(mHandler);
	}


}
