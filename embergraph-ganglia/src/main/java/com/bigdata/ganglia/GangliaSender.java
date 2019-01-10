/*
   Copyright (C) SYSTAP, LLC 2006-2012.  All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.bigdata.ganglia;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import com.bigdata.ganglia.xdr.XDROutputBuffer;

/**
 * Class for sending metrics to Ganglia.
 */
public class GangliaSender  {

	private final InetSocketAddress[] metricsServers;

	protected final XDROutputBuffer xdr;

	private final DatagramSocket datagramSocket;
	
	public GangliaSender(final InetSocketAddress[] metricServers,
			final int bufferSize) throws SocketException {

		if (metricServers == null)
			throw new IllegalArgumentException();
		
		this.metricsServers = metricServers;
		
		this.xdr = new XDROutputBuffer(bufferSize);
		
		this.datagramSocket = new DatagramSocket();
		
	}

	/**
	 * Overridden to close the datagram socket.
	 */
	@Override
	protected void finalize() throws Throwable {

		close();
		
		super.finalize();
		
	}
	
	/**
	 * Method to close the datagram socket
	 */
	public void close() {

		if (datagramSocket != null) {
		
			datagramSocket.close();
			
		}
		
	}

	/**
	 * Send an XDR formatted message to the metric server(s).
	 * 
	 * @param xdr
	 *            The XDR formatted message.
	 *            
	 * @throws IOException
	 */
	public void sendMessage(final XDROutputBuffer xdr) throws IOException {

		for (SocketAddress socketAddress : metricsServers) {

			final DatagramPacket packet = new DatagramPacket(xdr.getBuffer(),
					xdr.getLength(), socketAddress);

			datagramSocket.send(packet);

		}

	}

}
