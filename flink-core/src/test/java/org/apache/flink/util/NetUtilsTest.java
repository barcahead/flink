/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.util;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.ServerSocket;
import java.net.BindException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

public class NetUtilsTest {

	@Test
	public void testIPv4toURL() {
		try {
			final String addressString = "192.168.0.1";

			InetAddress address = InetAddress.getByName(addressString);
			assertEquals(addressString, NetUtils.ipAddressToUrlString(address));
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testIPv6toURL() {
		try {
			final String addressString = "2001:01db8:00:0:00:ff00:42:8329";
			final String normalizedAddress = "[2001:1db8::ff00:42:8329]";

			InetAddress address = InetAddress.getByName(addressString);
			assertEquals(normalizedAddress, NetUtils.ipAddressToUrlString(address));
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testIPv4URLEncoding() {
		try {
			final String addressString = "10.244.243.12";
			final int port = 23453;
			
			InetAddress address = InetAddress.getByName(addressString);
			InetSocketAddress socketAddress = new InetSocketAddress(address, port);
			
			assertEquals(addressString, NetUtils.ipAddressToUrlString(address));
			assertEquals(addressString + ':' + port, NetUtils.ipAddressAndPortToUrlString(address, port));
			assertEquals(addressString + ':' + port, NetUtils.socketAddressToUrlString(socketAddress));
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testIPv6URLEncoding() {
		try {
			final String addressString = "2001:db8:10:11:12:ff00:42:8329";
			final String bracketedAddressString = '[' + addressString + ']';
			final int port = 23453;

			InetAddress address = InetAddress.getByName(addressString);
			InetSocketAddress socketAddress = new InetSocketAddress(address, port);

			assertEquals(bracketedAddressString, NetUtils.ipAddressToUrlString(address));
			assertEquals(bracketedAddressString + ':' + port, NetUtils.ipAddressAndPortToUrlString(address, port));
			assertEquals(bracketedAddressString + ':' + port, NetUtils.socketAddressToUrlString(socketAddress));
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}



	@Test
	public void testFreePortRangeUtility() {
		// inspired by Hadoop's example for "yarn.app.mapreduce.am.job.client.port-range"
		String rangeDefinition = "50000-50050, 50100-50200,51234 "; // this also contains some whitespaces
		Iterator<Integer> portsIter = NetUtils.getPortRangeFromString(rangeDefinition);
		Set<Integer> ports = new HashSet<>();
		while(portsIter.hasNext()) {
			Assert.assertTrue("Duplicate element", ports.add(portsIter.next()));
		}

		Assert.assertEquals(51+101+1, ports.size());
		// check first range
		Assert.assertThat(ports, hasItems(50000, 50001, 50002, 50050));
		// check second range and last point
		Assert.assertThat(ports, hasItems(50100, 50101, 50110, 50200, 51234));
		// check that only ranges are included
		Assert.assertThat(ports, not(hasItems(50051, 50052, 1337, 50201, 49999, 50099)));


		// test single port "range":
		portsIter = NetUtils.getPortRangeFromString(" 51234");
		Assert.assertTrue(portsIter.hasNext());
		Assert.assertEquals(51234, (int)portsIter.next());
		Assert.assertFalse(portsIter.hasNext());

		// test port list
		portsIter = NetUtils.getPortRangeFromString("5,1,2,3,4");
		Assert.assertTrue(portsIter.hasNext());
		Assert.assertEquals(5, (int)portsIter.next());
		Assert.assertEquals(1, (int)portsIter.next());
		Assert.assertEquals(2, (int)portsIter.next());
		Assert.assertEquals(3, (int)portsIter.next());
		Assert.assertEquals(4, (int)portsIter.next());
		Assert.assertFalse(portsIter.hasNext());


		Throwable error = null;

		// try some wrong values: String
		try { NetUtils.getPortRangeFromString("localhost"); } catch(Throwable t) { error = t; }
		Assert.assertTrue(error instanceof NumberFormatException);
		error = null;

		// incomplete range
		try { NetUtils.getPortRangeFromString("5-"); } catch(Throwable t) { error = t; }
		Assert.assertTrue(error instanceof NumberFormatException);
		error = null;

		// incomplete range
		try { NetUtils.getPortRangeFromString("-5"); } catch(Throwable t) { error = t; }
		Assert.assertTrue(error instanceof NumberFormatException);
		error = null;

		// empty range
		try { NetUtils.getPortRangeFromString(",5"); } catch(Throwable t) { error = t; }
		Assert.assertTrue(error instanceof NumberFormatException);
		error = null;

	}

	@Test
	public void testFormatAddress() throws UnknownHostException {
		{
			// IPv4
			String host = "1.2.3.4";
			int port = 42;
			Assert.assertEquals(host + ":" + port, NetUtils.unresolvedHostAndPortToNormalizedString(host, port));
		}
		{
			// IPv6
			String host = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
			int port = 42;
			Assert.assertEquals("[2001:db8:85a3::8a2e:370:7334]:" + port, NetUtils.unresolvedHostAndPortToNormalizedString(host, port));
		}
		{
			// Hostnames
			String host = "somerandomhostname";
			int port = 99;
			Assert.assertEquals(host + ":" + port, NetUtils.unresolvedHostAndPortToNormalizedString(host, port));
		}
		{
			// Whitespace
			String host = "  somerandomhostname  ";
			int port = 99;
			Assert.assertEquals(host.trim() + ":" + port, NetUtils.unresolvedHostAndPortToNormalizedString(host, port));
		}
		{
			// Illegal hostnames
			String host = "illegalhost.";
			int port = 42;
			try {
				NetUtils.unresolvedHostAndPortToNormalizedString(host, port);
				fail();
			} catch (Exception ignored) {}
			// Illegal hostnames
			host = "illegalhost:fasf";
			try {
				NetUtils.unresolvedHostAndPortToNormalizedString(host, port);
				fail();
			} catch (Exception ignored) {}
		}
		{
			// Illegal port ranges
			String host = "1.2.3.4";
			int port = -1;
			try {
				NetUtils.unresolvedHostAndPortToNormalizedString(host, port);
				fail();
			} catch (Exception ignored) {}
		}
		{
			// lower case conversion of hostnames
			String host = "CamelCaseHostName";
			int port = 99;
			Assert.assertEquals(host.toLowerCase() + ":" + port, NetUtils.unresolvedHostAndPortToNormalizedString(host, port));
		}
	}

	@Test
	public void testCreateServerFromPorts() {
		String rangeDefinition = "50000-50050";
		Iterator<Integer> portsIter = NetUtils.getPortRangeFromString(rangeDefinition);

		ServerSocket server = null;

		// normal case
		try {
			server = NetUtils.createServerFromPorts("localhost", portsIter, new NetUtils.ServerFactory<ServerSocket>() {
				@Override
				public ServerSocket create(String address, int port) throws Exception {
					return new ServerSocket(port);
				}
			});

			int port = server.getLocalPort();
			Assert.assertTrue("Server created in specified port range", port >= 50000 && port <= 50050);
		} catch(Exception e) {
			fail();
		} finally {
			if (server != null) {
				try {
					server.close();
				} catch (Exception ignored) {}
			}
		}

		// handle exception during creating server
		portsIter = NetUtils.getPortRangeFromString(rangeDefinition);
		server = null;
		try {
			server = NetUtils.createServerFromPorts("localhost", portsIter, new NetUtils.ServerFactory<ServerSocket>() {
				@Override
				public ServerSocket create(String address, int port) throws Exception {
					if (port < 50010) {
						throw new Exception();
					}
					else {
						return new ServerSocket(port);
					}
				}
			});

			int port = server.getLocalPort();
			Assert.assertTrue("Server created in specified port range", port >= 50010 && port <= 50050);
		} catch(Exception e) {
			fail();
		} finally {
			if (server != null) {
				try {
					server.close();
				} catch (Exception ignored) {}
			}
		}

		// exhaust all ports
		portsIter = NetUtils.getPortRangeFromString(rangeDefinition);
		try {
			NetUtils.createServerFromPorts("localhost", portsIter, new NetUtils.ServerFactory<ServerSocket>() {
				@Override
				public ServerSocket create(String address, int port) throws Exception {
					throw new Exception();
				}
			});
			fail();
		} catch(Exception e) {
			Assert.assertTrue(e instanceof BindException);
		}
	}
}
