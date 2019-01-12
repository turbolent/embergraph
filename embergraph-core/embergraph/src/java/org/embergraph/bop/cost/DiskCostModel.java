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
 * Created on Sep 30, 2010
 */
package org.embergraph.bop.cost;

import java.io.Serializable;

/**
 * A cost model of the disk.
 *
 * @todo Develop disk models for SAS,SATA,SSD and various RAID configurations, including the #of
 *     spindles in the RAID array.
 * @todo Develop disk models for SAN, NAS, NFS, parallel file systems, etc.
 * @todo Conditionally copy the desired disk model parameters into the fields above to see the
 *     performance estimates for a given configuration.
 * @todo The scattered and sustained write rates can be estimated from the transfer rate. However,
 *     SCSI does much better than SATA when it can reorder the writes for improved locality.
 */
public class DiskCostModel implements Serializable {

  /**
   * @todo should be either Externalizable and explicitly managed versioning or Serializable with a
   *     public interface for versioning.
   */
  private static final long serialVersionUID = 1L;

  public static final DiskCostModel DEFAULT = new DiskCostModel(10d, 41943040);

  /** The average disk seek time (milliseconds). */
  public final double seekTime;

  /** The average disk transfer rate (megabytes per second). */
  public final double transferRate;

  /**
   * @param seekTime The average disk seek time (milliseconds).
   * @param transferRate The average disk transfer rate (megabytes per second).
   */
  public DiskCostModel(double seekTime, double transferRate) {

    this.seekTime = seekTime;

    this.transferRate = transferRate;
  }
}
