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

package org.embergraph.rdf.sparql.ast.eval.service;

import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.BaseVocabularyDecl;
import org.embergraph.rdf.vocab.core.EmbergraphCoreVocabulary_v20160317;
import org.openrdf.model.impl.URIImpl;

/** Test class for GeoSpatial data type extensions. */
public class GeoSpatialTestVocabulary extends EmbergraphCoreVocabulary_v20160317 {

  /** De-serialization ctor. */
  public GeoSpatialTestVocabulary() {

    super();
  }

  /*
   * Used by {@link AbstractTripleStore#create()}.
   *
   * @param namespace The namespace of the KB instance.
   */
  public GeoSpatialTestVocabulary(final String namespace) {

    super(namespace);
  }

  @Override
  protected void addValues() {

    super.addValues();
    addDecl(
        new BaseVocabularyDecl(
            new URIImpl("http://my.custom.datatype/lat-lon-time"),
            new URIImpl("http://my.custom.datatype/time-lat-lon"),
            new URIImpl("http://my.custom.datatype/lat-time-lon"),
            new URIImpl("http://my.custom.datatype/lat-lon"),
            new URIImpl("http://my.custom.datatype/lat-lon-coord"),
            new URIImpl("http://my.custom.datatype/time-coord"),
            new URIImpl("http://my.custom.datatype/lat-lon-time-coordsystem"),
            new URIImpl("http://my.custom.datatype/lat-lon-coordsystem"),
            new URIImpl("http://www.opengis.net/ont/geosparql#wktLiteral"),
            new URIImpl("http://my.custom.datatype/x-y-z"),
            new URIImpl("http://my.custom.datatype/x-y-z-lat-lon"),
            new URIImpl("http://my.custom.datatype/x-y-z-lat-lon-time"),
            new URIImpl("http://my.custom.datatype/time-x-y-z"),
            new URIImpl("http://my.custom.datatype/x-y-z-lat-lon-time-coord"),
            new URIImpl("http://my-lat-lon-starttime-endtime-dt"),
            new URIImpl("http://width-height-length-dt")));
  }
}
