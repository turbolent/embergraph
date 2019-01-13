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

import java.io.OutputStream;

public class FixedOutputStream extends OutputStream {

  private final byte[] m_buf;
  private int m_count = 0;

  public FixedOutputStream(final byte[] buf) {
    m_buf = buf;
  }

  /*
   * ************************************************************** write a single 4 byte integer
   */
  public void writeInt(final int b) {
    m_buf[m_count++] = (byte) ((b >>> 24) & 0xFF);
    m_buf[m_count++] = (byte) ((b >>> 16) & 0xFF);
    m_buf[m_count++] = (byte) ((b >>> 8) & 0xFF);
    m_buf[m_count++] = (byte) ((b >>> 0) & 0xFF);
  }

  public void write(final int b) {
    m_buf[m_count++] = (byte) b;
  }

  public void write(final byte[] b, final int off, final int len) {
    System.arraycopy(b, off, m_buf, m_count, len);

    m_count += len;
  }

  public void writeLong(final long txReleaseTime) {
    writeInt((int) (txReleaseTime >> 32));
    writeInt((int) txReleaseTime & 0xFFFFFFFF);
  }
}
