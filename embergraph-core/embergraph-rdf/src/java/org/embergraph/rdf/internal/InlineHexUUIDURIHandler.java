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

import java.nio.ByteBuffer;
import java.util.UUID;
import javax.xml.bind.DatatypeConverter;
import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.internal.impl.literal.UUIDLiteralIV;
import org.embergraph.rdf.model.EmbergraphLiteral;

/*
 * Inline URI Handler to handle URI's in the form of a Hex UUID such as:
 *
 * <pre>
 *   http://blazegraph.com/element/010072F0000038090100000000D56C9E
 *  </pre>
 *
 * {@link https://jira.blazegraph.com/browse/BLZG-1936}
 *
 * @author beebs
 */
public class InlineHexUUIDURIHandler extends InlineURIHandler {

  /** Default URI namespace for inline UUIDs. */
  public static final String NAMESPACE = "urn:hex:uuid:";

  public InlineHexUUIDURIHandler(final String namespace) {
    super(namespace);
  }

  @SuppressWarnings("rawtypes")
  protected AbstractLiteralIV createInlineIV(final String localName) {

    if (localName == null) {
      return null;
    }

    try {
      final byte[] uuid = DatatypeConverter.parseHexBinary(localName);
      return new UUIDLiteralIV(asUuid(uuid));
    } catch (IllegalArgumentException ex) {
      /*
       * Could not parse localName into a UUID.  Fall through to TermIV.
       */
      return null;
    }
  }

  @SuppressWarnings("rawtypes")
  @Override
  public String getLocalNameFromDelegate(AbstractLiteralIV<EmbergraphLiteral, ?> delegate) {

    final UUID uuid = ((UUIDLiteralIV) delegate).getInlineValue();

    final String localName = DatatypeConverter.printHexBinary(asBytes(uuid));

    return localName;
  }

  public static UUID asUuid(byte[] bytes) {
    ByteBuffer bb = ByteBuffer.wrap(bytes);
    long firstLong = bb.getLong();
    long secondLong = bb.getLong();
    return new UUID(firstLong, secondLong);
  }

  public static byte[] asBytes(UUID uuid) {
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }
}
