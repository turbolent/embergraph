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
 * Created on May 7, 2014
 */
package org.embergraph.search;

import junit.framework.TestCase2;

import org.embergraph.search.LanguageRange;

public class TestLanguageRange extends TestCase2 {

	public TestLanguageRange() {
	}
	

	public TestLanguageRange(String name) {
		super(name);
	}
	
	private void match(String range, String lang) {
		LanguageRange lr = new LanguageRange(range.toLowerCase());
		assertTrue(lr.extendedFilterMatch(lang));
	}

	private void nomatch(String range, String lang) {
		LanguageRange lr = new LanguageRange(range.toLowerCase());
		assertFalse(lr.extendedFilterMatch(lang));
	}
	

	public void testRFC4647() {
		for (String range: new String[]{"de-DE", "de-*-DE"}) {
			match(range, "de-DE");
			match(range, "de-Latn-DE");
			match(range, "de-Latf-DE");
			match(range, "de-DE-x-goethe");
			match(range, "de-Latn-DE-1996");
			match(range, "de-Deva-DE-1996");
			nomatch(range, "de");
			nomatch(range, "de-x-DE");
			nomatch(range, "de-Deva");
		}
		
	}
	

}
