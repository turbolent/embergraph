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

package org.embergraph.rwstore;

import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.embergraph.rawstore.IAllocationContext;
import org.embergraph.rwstore.RWStore.AllocationStats;

public interface Allocator extends Comparable {
  int getBlockSize();

  void setIndex(int index);

  boolean verify(int addr);

  long getStartAddr();

  boolean addressInRange(int addr);

  boolean free(int addr, int size);

  int alloc(RWStore store, int size, IAllocationContext context);

  int getDiskAddr();

  void setDiskAddr(int addr);

  long getPhysicalAddress(int offset);

  boolean isAllocated(int offset);

  int getPhysicalSize(int offset);

  byte[] write();

  void read(DataInputStream str);

  boolean hasFree();

  void setFreeList(ArrayList list);

  String getStats(AtomicLong counter);

  void addAddresses(ArrayList addrs);

  int getRawStartAddr();

  int getIndex();

  void appendShortStats(StringBuilder str, AllocationStats[] stats);

  boolean canImmediatelyFree(int addr, int size, IAllocationContext context);
}
