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
 * Created on Jun 13, 2010
 */

package org.embergraph.ha;

import java.io.IOException;
import java.rmi.Remote;
import java.util.UUID;

/*
* A {@link Remote} interface for methods supporting high availability. This interface hierarchy
 * mirrors the {@link QuorumService} hierarchy and is broken down by the various facets of the high
 * availability functionality such as the write pipeline, quorum reads, the quorum commit protocol,
 * etc.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface HAGlueBase extends Remote {

  /*
   * The {@link UUID} of this service.
   *
   * @todo This should be handled as a smart proxy so this method does not actually perform RMI.
   */
  UUID getServiceId() throws IOException;
}
