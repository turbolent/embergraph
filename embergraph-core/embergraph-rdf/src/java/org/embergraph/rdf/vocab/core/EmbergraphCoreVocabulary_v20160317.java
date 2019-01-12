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

package org.embergraph.rdf.vocab.core;

import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.BaseVocabularyDecl;
import org.embergraph.service.geospatial.GeoSpatial;

/**
 * Core Embergraph vocabulary.
 *
 * <p>Note: Do not modify this class. Create an entirely new vocabulary that extends this one and
 * edit {@link AbstractTripleStore.Options#DEFAULT_VOCABULARY_CLASS}.
 */
public class EmbergraphCoreVocabulary_v20160317 extends EmbergraphCoreVocabulary_v20151210 {

  /** De-serialization ctor. */
  public EmbergraphCoreVocabulary_v20160317() {

    super();
  }

  /**
   * Used by {@link AbstractTripleStore#create()}.
   *
   * @param namespace The namespace of the KB instance.
   */
  public EmbergraphCoreVocabulary_v20160317(final String namespace) {

    super(namespace);
  }

  @Override
  protected void addValues() {

    super.addValues();

    /*
     * Some new URIs for graph and RDR management.
     */
    addDecl(
        new BaseVocabularyDecl(
            GeoSpatial.DEFAULT_DATATYPE_LAT_LON, GeoSpatial.DEFAULT_DATATYPE_LAT_LON_TIME));
  }
}
