/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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

package com.bigdata.rwstore;

import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import com.bigdata.rawstore.IAllocationContext;
import com.bigdata.rwstore.RWStore.AllocationStats;


public interface Allocator extends Comparable {
  public int getBlockSize();
  public void setIndex(int index);
  public boolean verify(int addr);
  public long getStartAddr();
  public boolean addressInRange(int addr);
  public boolean free(int addr, int size);
  public int alloc(RWStore store, int size, IAllocationContext context);
  public int getDiskAddr();
  public void setDiskAddr(int addr);
  public long getPhysicalAddress(int offset);
  public boolean isAllocated(int offset);
  public int getPhysicalSize(int offset);
  public byte[] write();
  public void read(DataInputStream str);
  public boolean hasFree();
  public void setFreeList(ArrayList list);
  public String getStats(AtomicLong counter);
  public void addAddresses(ArrayList addrs);
  public int getRawStartAddr();
  public int getIndex();
  public void appendShortStats(StringBuilder str, AllocationStats[] stats);
  public boolean canImmediatelyFree(int addr, int size, IAllocationContext context);
}
	
