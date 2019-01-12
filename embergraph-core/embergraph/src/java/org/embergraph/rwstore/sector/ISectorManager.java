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

package org.embergraph.rwstore.sector;

/**
 * The {@link ISectorManager} defines the contract required to manage a set of {@link
 * SectorAllocator}s.
 *
 * <p>The {@link ISectorManager} is passed to the {@link SectorAllocator} constructors and they will
 * callback to manage their free list availability, and to trim the allocated storage if required.
 *
 * @author Martyn Cutcher
 */
public interface ISectorManager {

  /**
   * This request is made when the sectorAllocator no longer has a full set of block allocations
   * available.
   *
   * <p>The allocator will issue this callback to help the SectorManager manage an effective
   * freelist of available allocators.
   *
   * @param sectorAllocator to be removed
   */
  void removeFromFreeList(SectorAllocator sectorAllocator);

  /**
   * When sufficient allocations have been freed for recycling that a threshold of availability of
   * reached for all block sizes, then the allocator calls back to the SectorManager to signal it is
   * available to be returned to the free list.
   *
   * @param sectorAllocator to be added
   */
  void addToFreeList(SectorAllocator sectorAllocator);

  /**
   * When a sector is first created, it will remain at the head of the free list until one of two
   * conditions has been reached:
   *
   * <ol>
   *   <li>The allocation has been saturated.
   *   <li>The bit space has been filled.
   *   <li>
   * </ol>
   *
   * In the case of (2), then it is possible that significant allocation space cannot be utilized -
   * which will happen if the average allocation is less than 1K. In this situation, the sector can
   * be trimmed and the space made available to the next sector.
   *
   * <p>trimSector will only be called in this condition - on the first occasion that the allocator
   * is removed from the freeList.
   *
   * @param trim - the amount by which the sector allocation can be reduced
   */
  void trimSector(long trim, SectorAllocator sector);
}
