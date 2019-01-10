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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.bigdata.rawstore.IAllocationContext;
import com.bigdata.rawstore.IPSOutputStream;


/************************************************************************
 * PSOutputStream
 *
 * Provides stream interface direct to the low-level store.
 * <p>
 * Retrieved from an IObjectStore to enable output to the store.
 * <p>
 * The key idea here is that rather than a call like :
 * <pre>
 *	store.realloc(oldAddr, byteOutputStream)=> newAddress
 * 
 * instead :
 *	store.allocStream(oldAddr)=>PSOutputStream
 *
 * and then :
 *	stream.save()=> newAddress
 * </pre>
 * <p>
 * This will enable large data formats to be streamed to the data store,
 *	where the previous interface would have required that the entire
 *	resource was loaded into a memory structure first, before being
 *	written out in a single block to the store.
 * <p>
 * This new approach will also enable the removal of BLOB allocation
 *	strategy.  Instead, "BLOBS" will be served by linked fixed allocation
 *	blocks, flushed out by the stream.
 * <p>
 * A big advantage of this is that BLOB reallocation is now a lot simpler,
 *	since BLOB storage is simply a potentially large number of 8K blocks.
 * <p>
 * This also opens up the possibility of a Stream oriented data type, that
 *	could be used to serve up a variety of data streams.  By providing
 *	relevant interfaces with the client/server system, a server can then
 *	provide multiple streams to a high number of clients.
 * <p>
 * To this end, the output stream has a fixed buffer size, and they are recycled
 *	from a pool of output streams.
 * <p>
 * It is <em>important</em>  that output streams are bound to the IStore they
 *	are requested for.
 * <p>
 * See ticket #641 that discusses creating and using a pool for PSOutputStream
 * allocation.
 **/
public class PSOutputStream extends IPSOutputStream {
    
    private static final Logger log = Logger.getLogger(FixedAllocator.class);

	private static final transient String ERR_NO_STORE = "PSOutputStream with unitilialized store";

	private static final transient String ERR_ALREADY_SAVED = "Writing to saved PSOutputStream";

	/*
	 * PSOutputStream pooling.
	 * 
	 * @todo I would like to see this lifted into a class. The RWStore could
	 * then use an instance of that class to have a per-store pool of a given
	 * capacity. This should simplify this class (PSOutputStream), make the use
	 * of pooling optional, and allow greater concurrency if more than one
	 * RWStore is running since they will have distinct pools. (I also do not
	 * like the notion of JVM wide pools where they can be readily avoided).
	 */
	private static PSOutputStream m_poolHead = null;
	private static PSOutputStream m_poolTail = null;
	private static int m_streamCount = 0;
	
	public static synchronized PSOutputStream getNew(final IStore store, final int maxAlloc, final IAllocationContext context) {
		PSOutputStream ret = m_poolHead;
		if (ret != null) {
			m_streamCount--;
			
			m_poolHead = ret.next();
			if (m_poolHead == null) {
				m_poolTail = null;
			}
		} else {
			ret = new PSOutputStream();
		}
		
		ret.init(store, maxAlloc, context);
		
		return ret;
	}
	
	/*******************************************************************
	 * maintains pool of streams - in a normal situation there will only
	 *	me a single stream continually re-used, but with some patterns
	 *	there could be many streams.  For this reason it is worth checking
	 *	that the pool is not maintained at an unnecessarily large value, so
	 *	 maximum of 10 streams are maintained - adding up to 80K to the
	 *	 garbage collect copy.
	 **/
	static synchronized void returnStream(PSOutputStream stream) {
		if (m_streamCount > 10) {
			return;
		}
		
		stream.m_count = 0; // avoid overflow
		
		if (m_poolTail != null) {
			m_poolTail.setNext(stream);
		} else {
			m_poolHead = stream;
		}
		
		m_poolTail = stream;
		m_streamCount++;
	}

	/*
	 * PSOutputStream impl.
	 */
	
	private ArrayList<Integer> m_blobHeader = null;
	private byte[] m_buf = null;
	private boolean m_isSaved = false;
//	private long m_headAddr = 0;
//	private long m_prevAddr = 0;
	private int m_count = 0;
	private int m_bytesWritten = 0;
	private int m_blobThreshold = 0;
	private IStore m_store;
	
	private IAllocationContext m_context;
	
	private PSOutputStream m_next = null;

	private int m_blobHdrIdx;

	private boolean m_writingHdr = false;
	
	private PSOutputStream next() {
		return m_next;
	}
	
	private void setNext(PSOutputStream str) {
		m_next = str;
	}
	
	/****************************************************************
	 * resets private state variables for reuse of stream
	 **/
	void init(final IStore store, final int maxAlloc, final IAllocationContext context) {
		m_store = store;
		m_context = context;
		m_next = null;

		m_blobThreshold = maxAlloc-4; // allow for checksum
		
		if (m_buf == null || m_buf.length != m_blobThreshold)
			m_buf = new byte[m_blobThreshold];

		reset();
	}
	
	public void reset() {
		m_isSaved = false;
		
//		m_headAddr = 0;
//		m_prevAddr = 0;
		m_count = 0;
		m_bytesWritten = 0;
		m_isSaved = false;
		
		m_blobHeader = null;
		m_blobHdrIdx = 0;
	}

	/****************************************************************
	 * write a single byte
	 *
	 * this is the one place where the blob threshold is handled
	 *	and its done one byte at a time so should be easy enough,
	 *
	 * We no longer store continuation addresses, instead we allocate
	 * blob allocations via a blob header block.
	 **/
  public void write(final int b) throws IOException {
  	if (m_store == null) {
  		throw new IllegalStateException(ERR_NO_STORE);
  	}
  	
  	if (m_isSaved) {
  		throw new IllegalStateException(ERR_ALREADY_SAVED);
  	}
  	
  	if (m_count == m_blobThreshold && !m_writingHdr) {
  		if (m_blobHeader == null) {
  			m_blobHeader = new ArrayList<Integer>(); // only support header
  		}
  		
  		final int curAddr = (int) m_store.alloc(m_buf, m_count, m_context);
  		m_blobHeader.add(curAddr);
  		
  		m_count = 0;
  	}
  	
  	m_buf[m_count++] = (byte) b;

  	m_bytesWritten++;
  }
  
	/****************************************************************
	 * write a single 4 byte integer
	 **/
  public void writeInt(final int b) throws IOException {
  	write((b >>> 24) & 0xFF);
  	write((b >>> 16) & 0xFF);
  	write((b >>> 8) & 0xFF);
  	write(b & 0xFF);
  }
  
  public void writeLong(final long b) throws IOException {
  	final int hi = (int) (b >> 32);
  	final int lo = (int) (b & 0xFFFFFFFF);
   	writeInt(hi);
   	writeInt(lo);
  }
  
	/****************************************************************
	 * write byte array to the buffer
	 * 
	 * we need to be able to efficiently handle large arrays beyond size
	 * of the blobThreshold, so
	 **/
  public void write(final byte b[], final int off, final int len) throws IOException {
  	if (m_store == null) {
  		throw new IllegalStateException(ERR_NO_STORE);
  	}
  	
  	if (m_isSaved) {
  		throw new IllegalStateException(ERR_ALREADY_SAVED);
  	}
  	
  	if ((m_count + len) > m_blobThreshold) {
  		// not optimal, but this will include a disk write anyhow so who cares
  		for (int i = 0; i < len; i++) {
  			write(b[off+i]);
  		}
  	} else {
		System.arraycopy(b, off, m_buf, m_count, len);
		
		m_count += len;
		m_bytesWritten += len;
		
	}
  }
  
	/****************************************************************
	 * utility method that extracts data from the input stream
	 * @param instr and write to the store.
	 *
	 * This method can be used to stream external files into
	 *	the store.
	 **/
  public void write(final InputStream instr) throws IOException {
  	if (m_isSaved) {
  		throw new IllegalStateException(ERR_ALREADY_SAVED);
  	}
  	
  	final byte b[] = new byte[512];
  	
  	int r = instr.read(b);
  	while (r == 512) {
  		write(b, 0, r);
  		r = instr.read(b);
  	}
  	
  	if (r != -1) {
  		write(b, 0, r);
  	}
  }
  
  /****************************************************************
   * on save() the current buffer is allocated and written to the
   *	store, and the address of its location returned
   * If saving as Blob then addr must index to the BlobAllocator that then
   * points to the BlobHeader
   **/
  public long save() {
  	if (m_isSaved) {
  		throw new IllegalStateException(ERR_ALREADY_SAVED);
  	}
  	
  	if (m_store == null) {
  		return 0;
  	}
  	
  	if (m_count == 0) {
  		m_isSaved = true;
  		
  		return 0; // allow for empty stream
  	}
  	
  	int addr = (int) m_store.alloc(m_buf, m_count, m_context);
  	
  	if (m_blobHeader != null) {
  		try {
  			m_writingHdr  = true; // ensure that header CAN be a BLOB
	  		// m_blobHeader[m_blobHdrIdx++] = addr;
  			m_blobHeader.add(addr);
//	  		final int precount = m_count;
	  		m_count = 0;
			try {
//		  		writeInt(m_blobHdrIdx);
//		  		for (int i = 0; i < m_blobHdrIdx; i++) {
//		  			writeInt(m_blobHeader[i]);
//		 		}
				final int hdrBufSize = 4*(m_blobHeader.size() + 1);
				final ByteArrayOutputStream hdrbuf = new ByteArrayOutputStream(hdrBufSize);
				final DataOutputStream hdrout = new DataOutputStream(hdrbuf);
	  			hdrout.writeInt(m_blobHeader.size());
		  		for (int i = 0; i < m_blobHeader.size(); i++) {
		  			hdrout.writeInt(m_blobHeader.get(i));
		 		}
				hdrout.flush();
				
				final byte[] outbuf = hdrbuf.toByteArray();
		  		addr = (int) m_store.alloc(outbuf, hdrBufSize, m_context);
		  		
//				if (m_blobHdrIdx != ((m_blobThreshold - 1 + m_bytesWritten - m_count) / m_blobThreshold)) {
//					throw new IllegalStateException(
//							"PSOutputStream.save at : " + addr
//									+ ", bytes: " + m_bytesWritten
//									+ ", blocks: " + m_blobHdrIdx
//									+ ", last alloc: " + precount);
//				}
		  		
			} catch (IOException e) {
//				e.printStackTrace();
				throw new RuntimeException(e);
			}
  		} finally {
  			m_writingHdr = false;
  		}
   	}
  
  	m_isSaved = true;
  	
  	return addr;
  }
  
  public void close() throws IOException {
  	if (false && m_store != null) {
  		m_store = null;
  	
  		returnStream(this);
  	}
  }
  
  public int getBytesWritten() {
  	return m_bytesWritten;
  }
  
  public OutputStream getFilterWrapper(final boolean saveBeforeClose) {
  	
		return new FilterOutputStream(this) {
			public void close() throws IOException {
				if (saveBeforeClose) {
					flush();
					
					save();
					
					super.close();
				} else {
					super.close();
					
					save();
				}
			}
		};
	}

	public long getAddr() {
		
		long addr = save();
		addr <<= 32;
		addr += m_bytesWritten;
		
		return addr;
	}
			
			
}


/***********************************************
Jython test

from cutthecrap.gpo.client import OMClient;
from cutthecrap.gpo import *;
from java.io import *;

client = OMClient("", "D:/db/test.wo");

om = client.getObjectManager();

g = GPOMap(om);

os = om.createOutputStream();

ds = ObjectOutputStream(os);

ds.writeObject("Hi there World!");

ds.flush();
os.save();

g.set("stream", os);

ds.close();
os.close();

instr = g.get("stream");

ds = ObjectInputStream(instr);

print "read in", ds.readObject();



**/
