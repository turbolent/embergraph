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
package org.embergraph.gom.alchemy.owl;

import java.util.Iterator;
import org.embergraph.gom.gpo.BasicSkin;
import org.embergraph.gom.gpo.GPO;
import org.embergraph.gom.gpo.IGPO;
import org.embergraph.gom.gpo.IGenericSkin;
import org.embergraph.gom.om.IObjectManager;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;

public class OWLOntologySkin extends BasicSkin implements IGenericSkin {

  public OWLOntologySkin(IGPO gpo) {
    super(gpo);
  }

  public static OWLOntologySkin getOntology(IObjectManager om) {
    final IGPO owl = om.getGPO(OWL.ONTOLOGY);

    return (OWLOntologySkin) ((GPO) owl).getSkin(OWLOntologySkin.class);
  }

  /*
   * Returns a list of defined OWLClasses. The classes do not in fact have any reference to the
   * Ontology instance, but the skin supports the fiction.
   */
  public Iterator<OWLClassSkin> getClasses() {
    final IObjectManager om = m_gpo.getObjectManager();

    final IGPO classClass = om.getGPO(OWL.CLASS);

    final Iterator<IGPO> owlClasses = classClass.getLinksIn(RDF.TYPE).iterator();

    return new Iterator<OWLClassSkin>() {

      @Override
      public boolean hasNext() {
        return owlClasses.hasNext();
      }

      @Override
      public OWLClassSkin next() {
        IGPO nxt = owlClasses.next();
        return (OWLClassSkin) ((GPO) nxt).getSkin(OWLClassSkin.class);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}
