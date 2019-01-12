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

public class TestConfigurableAsDefaultAnalyzerFactory extends AbstractDefaultAnalyzerFactoryTest {

  public TestConfigurableAsDefaultAnalyzerFactory() {}

  public TestConfigurableAsDefaultAnalyzerFactory(String arg0) {
    super(arg0);
  }

  @Override
  String[] getExtraProperties() {
    return new String[] {
      FullTextIndex.Options.ANALYZER_FACTORY_CLASS,
      ConfigurableAnalyzerFactory.class.getName(),
      ConfigurableAnalyzerFactory.Options.NATURAL_LANGUAGE_SUPPORT,
      "true"
    };
  }

  @Override
  boolean isBroken() {
    return false;
  }
}
