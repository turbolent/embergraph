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
package org.embergraph.rdf.internal;

import java.util.UUID;

import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.internal.impl.literal.UUIDLiteralIV;

/**
 * Inline URI handler for UUIDs.
 */
public class InlineUUIDURIHandler extends InlineURIHandler {

    /**
     * Default URI namespace for inline UUIDs.
     */
    public static final String NAMESPACE = "urn:uuid:";

    public InlineUUIDURIHandler(final String namespace) {
        super(namespace);
    }
    
    @SuppressWarnings("rawtypes")
    protected AbstractLiteralIV createInlineIV(final String localName) {
        
        if (localName == null) {
            return null;
        }
        
        try {
            return new UUIDLiteralIV(UUID.fromString(localName));
        } catch (IllegalArgumentException ex) {
            /*
             * Could not parse localName into a UUID.  Fall through to TermIV.
             */
            return null;
        }
        
    }
    
}
