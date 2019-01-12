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
package org.embergraph.btree.raba.codec;

import org.embergraph.btree.raba.IRaba;
import org.embergraph.io.AbstractFixedByteArrayBuffer;

/*
 * Interface for an coded logical byte[][]. Implementations of this interface MUST be thread-safe
 * since the B+Tree guarantees the ability to perform concurrent read operations and instances of
 * this interface are used to code the keys and values of B+Tree nodes and leaves.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @see IRabaCoder
 */
public interface ICodedRaba extends IRaba {

  /** The coded (aka compressed) data. */
  AbstractFixedByteArrayBuffer data();
}
