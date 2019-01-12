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
 * Created on Sep 20, 2011
 */

package org.embergraph.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
* Class provides static information about project dependencies.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class Depends {

  public interface Dependency {

    /** The component name. */
    String getName();

    /** The project URL. */
    String projectURL();

    /** The license URL. */
    String licenseURL();
  }

  private static class OrderByName implements Comparator<Dependency> {

    @Override
    public int compare(final Dependency o1, final Dependency o2) {

      return o1.getName().compareTo(o2.getName());
    }
  }

  @SuppressWarnings("unused")
  private static class OrderByLicense implements Comparator<Dependency> {

    @Override
    public int compare(Dependency o1, Dependency o2) {
      return o1.licenseURL().compareTo(o2.licenseURL());
    }
  }

  /** Metadata for dependencies. */
  private static class Dep implements Dependency {

    private final String component;

    private final String projectURL;

    private final String licenseURL;

    public Dep(final String component, final String projectURL, final String licenseURL) {

      if (component == null || projectURL == null || licenseURL == null)
        throw new IllegalArgumentException();

      this.component = component;
      this.projectURL = projectURL;
      this.licenseURL = licenseURL;
    }

    @Override
    public final String getName() {
      return component;
    }

    @Override
    public final String projectURL() {
      return projectURL;
    }

    @Override
    public final String licenseURL() {
      return licenseURL;
    }

    @Override
    public String toString() {

      return "{name=" + component + ", project=" + projectURL + ", license=" + licenseURL + "}";
    }
  }

  /** An Apache project. */
  private static class ApacheDep extends Dep {

    public ApacheDep(String component, String projectURL) {
      super(component, projectURL, "http://www.apache.org/licenses/LICENSE-2.0.html");
    }
  }

  /** A project which we are redistributing under LGPL v2.1. */
  private static class LGPL21Dep extends Dep {

    public LGPL21Dep(String component, String projectURL) {
      super(component, projectURL, "http://www.gnu.org/licenses/lgpl-2.1.html");
    }
  }

  private static final Dep jini = new ApacheDep("river", "http://river.apache.org/");

  private static final Dep zookeeper =
      new ApacheDep("zookeeper", "http://hadoop.apache.org/zookeeper/");

  private static final Dep log4j = new ApacheDep("log4j", "http://logging.apache.org/log4j/1.2/");

  private static final Dep lucene =
      new ApacheDep("lucene", "http://lucene.apache.org/core/index.html");

  /*
   * http client and its dependencies are used for remote SPARQL requests and
   * the NanoSparqlServer's client-side API.
   */

  private static final Dep apacheCommonsCodec =
      new ApacheDep("commons-codec", "http://commons.apache.org/codec/");

  private static final Dep apacheCommonsFileUpload =
      new ApacheDep("commons-fileupload", "http://commons.apache.org/fileupload/");

  private static final Dep apacheCommonsIO =
      new ApacheDep("commons-io", "http://commons.apache.org/io/");

  private static final Dep apacheCommonsLogging =
      new ApacheDep("commons-logging", "http://commons.apache.org/logging/");

  private static final Dep apacheHttpClient = new ApacheDep("httpclient", "http://hc.apache.org/");

  private static final Dep apacheHttpClientCache =
      new ApacheDep("httpclient-cache", "http://hc.apache.org/");

  private static final Dep apacheHttpCore = new ApacheDep("httpcore", "http://hc.apache.org/");

  private static final Dep apacheHttpMime = new ApacheDep("httpmime", "http://hc.apache.org/");

  /*
   * Note: This embergraph module is under a different license (Apache 2.0) than the other
   * embergraph modules.
   */
  private static final Dep embergraphGanglia =
      new ApacheDep("embergraph-ganglia", "https://sourceforge.net/projects/embergraph/");

  private static final Dep colt =
      new Dep(
          "colt",
          "http://acs.lbl.gov/software/colt/",
          "http://acs.lbl.gov/software/colt/license.html");

  private static final Dep dsiutils = new LGPL21Dep("dsiutils", "http://dsiutils.dsi.unimi.it/");

  private static final Dep fastutil =
      new Dep(
          "fastutil",
          "http://fastutil.dsi.unimi.it/",
          "http://www.apache.org/licenses/LICENSE-2.0.html");

  //    private final static Dep iris = new LGPL21Dep("iris",
  //            "http://www.iris-reasoner.org");
  //
  //    private final static Dep jgrapht = new LGPL21Dep("jgrapht",
  //            "http://www.jgrapht.org/");

  //    private final static Dep tuprolog = new LGPL21Dep("tuprolog",
  //            "http://www.alice.unibo.it/xwiki/bin/view/Tuprolog/");

  private static final Dep highScaleLib =
      new Dep(
          "high-scale-lib",
          "https://sourceforge.net/projects/high-scale-lib/",
          "http://creativecommons.org/licenses/publicdomain");

  //    private final static Dep cweb = new Dep(
  //            "cweb",
  //            "http://www.cognitiveweb.org/",
  //            "http://www.cognitiveweb.org/legal/license/CognitiveWebOpenSourceLicense-1.1.html");

  private static final Dep flot =
      new Dep(
          "flot",
          "http://code.google.com/p/flot/",
          "http://www.opensource.org/licenses/mit-license.php");

  /*
   * Dual licensed under the MIT (MIT-LICENSE.txt) and GPL (GPL-LICENSE.txt) licenses. (We use the
   * MIT license).
   */
  private static final Dep jquery =
      new Dep(
          "jquery",
          "http://jquery.com/",
          "https://github.com/jquery/jquery/blob/master/MIT-LICENSE.txt");

  private static final Dep slf4j =
      new Dep("slf4j", "http://www.slf4j.org/", "http://www.slf4j.org/license.html");

  private static final Dep sesame =
      new Dep("sesame", "http://www.openrdf.org/", "http://www.openrdf.org/download.jsp");

  // Used for RDFa support.  Apache2 License
  private static final Dep semargl =
      new Dep(
          "semargl",
          "http://semarglproject.org",
          "https://github.com/levkhomich/semargl/blob/master/LICENSE");

  private static final Dep icu =
      new Dep(
          "ICU",
          "http://site.icu-project.org/",
          "http://source.icu-project.org/repos/icu/icu/trunk/license.html");

  //    private final static Dep nxparser = new Dep("nxparser",
  //            "http://sw.deri.org/2006/08/nxparser/",
  //            "http://sw.deri.org/2006/08/nxparser/license.txt");

  private static final Dep nanohttp =
      new Dep(
          "nanohttp",
          "http://elonen.iki.fi/code/nanohttpd/",
          "http://elonen.iki.fi/code/nanohttpd/#license");

  /*
   * Dual licensed under apache 2.0 and Eclipse Public License 1.0. We use the Apache 2.0 license.
   *
   * @see http://www.eclipse.org/jetty/licenses.php
   */
  private static final Dep jetty =
      new Dep(
          "jetty",
          "http://www.eclipse.org/jetty/",
          "http://www.apache.org/licenses/LICENSE-2.0.html");

  private static final Dep jsonld =
      new Dep(
          "jsonld",
          "https://github.com/jsonld-java/jsonld-java/",
          "https://raw.githubusercontent.com/jsonld-java/jsonld-java/master/LICENCE");

  private static final Dep servletApi = new ApacheDep("servlet-api", "http://tomcat.apache.org");

  /** Dual licensed under apache 2.0 and LGPL 2.1. We use the apache 2.0 license. */
  private static final Dep jacksonCore =
      new Dep(
          "jackson-core",
          "http://wiki.fasterxml.com/JacksonHome",
          "http://www.apache.org/licenses/LICENSE-2.0.html");

  private static final Dep blueprintsCore =
      new Dep(
          "blueprints-core",
          "https://github.com/tinkerpop/blueprints",
          "https://github.com/tinkerpop/blueprints/blob/master/LICENSE.txt");

  private static final Dep rexsterCore =
      new Dep(
          "rexster-core",
          "https://github.com/tinkerpop/rexster",
          "https://github.com/tinkerpop/rexster/blob/master/LICENSE.txt");

  // Note: This is a test-only dependency at this time.
  @SuppressWarnings("unused")
  private static final Dep hamcrestCore =
      new Dep(
          "hamcrest-core",
          "https://code.google.com/p/hamcrest/",
          "http://opensource.org/licenses/BSD-3-Clause");

  private static final Dep[] depends;

  static {
    depends =
        new Dep[] {
          // standalone
          log4j,
          lucene,
          colt,
          dsiutils,
          fastutil,
          highScaleLib,
          apacheCommonsCodec,
          apacheCommonsFileUpload,
          apacheCommonsIO,
          apacheCommonsLogging,
          apacheHttpClient,
          apacheHttpClientCache,
          apacheHttpCore,
          apacheHttpMime,
          // webapp
          //            cweb,
          slf4j,
          sesame,
          semargl,
          icu,
          //            nxparser,
          nanohttp,
          jetty,
          jsonld,
          servletApi,
          jacksonCore,
          blueprintsCore,
          rexsterCore,
          embergraphGanglia,
          // scale-out
          jini,
          zookeeper,
          // javascript
          flot,
          jquery,
          // linked, but not used.
          //            iris,
          //            jgrapht,
          //            tuprolog,
        };
    Arrays.sort(depends, new OrderByName());
  }

  /** Return an unmodifiable list of the dependencies. */
  public static final List<Dep> depends() {

    return Collections.unmodifiableList(Arrays.asList(depends));
  }
}
