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

import org.embergraph.gom.skin.GenericSkinRegistry;

/**
 * This is just a hook class to register the OWL GPO skins.
 *
 * @author Martyn Cutcher
 */
public class OWLSkins {

  public static void register() {
    GenericSkinRegistry.registerClass(OWLOntologySkin.class);
    GenericSkinRegistry.registerClass(OWLClassSkin.class);
    GenericSkinRegistry.registerClass(OWLPropertySkin.class);
  }
}
