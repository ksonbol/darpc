/*
 * DaRPC: Data Center Remote Procedure Call
 *
 * Author: Patrick Stuedi <stu@zurich.ibm.com>
 *
 * Copyright (C) 2016, IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.ibm.darpc.examples.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.ibm.darpc.RpcServerEndpoint;
import com.ibm.darpc.RpcServerGroup;
import com.ibm.darpc.examples.protocol.RdmaRpcRequest;
import com.ibm.darpc.examples.protocol.RdmaRpcResponse;
import com.ibm.disni.rdma.*;
import com.ibm.disni.util.*;

public class DaRPCServer {
	private String ipAddress; 
	private int poolsize = 3;
	private int queueSize = 16;
	private int wqSize = queueSize;
	private int servicetimeout = 0;
	private boolean polling = false;
	private int maxinline = 0;
	private int connections = 16;
	
	public void run() throws Exception{
		InetAddress localHost = InetAddress.getByName(ipAddress);
		InetSocketAddress addr = new InetSocketAddress(localHost, 1919);	
		
		long[] clusterAffinities = new long[poolsize];
		for (int i = 0; i < poolsize; i++){
			long cpu = 1L << i;
			clusterAffinities[i] = cpu;
		}
		System.out.println("running...server " + ipAddress + ", poolsize " + poolsize + ", maxinline " + maxinline + ", polling " + polling + ", queueSize " + queueSize + ", cqSize " + queueSize*connections*2 + ", wqSize " + wqSize + ", rpcservice-timeout " + servicetimeout);
		RdmaRpcService rpcService = new RdmaRpcService(servicetimeout);
		RpcServerGroup<RdmaRpcRequest, RdmaRpcResponse> group = RpcServerGroup.createServerGroup(rpcService, clusterAffinities, -1, maxinline, polling, queueSize, queueSize*connections*2, wqSize); 
		RdmaServerEndpoint<RpcServerEndpoint<RdmaRpcRequest, RdmaRpcResponse>> serverEp = group.createServerEndpoint();
		serverEp.bind(addr, 1000);
		while(true){
			serverEp.accept();
		}		
	}
	
	public void launch(String[] args) throws Exception {
		String[] _args = args;
		if (args.length < 1) {
			System.exit(0);
		} else if (args[0].equals(DaRPCServer.class.getCanonicalName())) {
			_args = new String[args.length - 1];
			for (int i = 0; i < _args.length; i++) {
				_args[i] = args[i + 1];
			}
		}

		GetOpt go = new GetOpt(_args, "a:s:p:di:c:w:q:");
		go.optErr = true;
		int ch = -1;

		while ((ch = go.getopt()) != GetOpt.optEOF) {
			if ((char) ch == 'a') {
				ipAddress = go.optArgGet();
			} else if ((char) ch == 's') {
				int serialized_size = Integer.parseInt(go.optArgGet());
				RdmaRpcRequest.SERIALIZED_SIZE = serialized_size;
				RdmaRpcResponse.SERIALIZED_SIZE = serialized_size;
			} else if ((char) ch == 'p') {
				poolsize = Integer.parseInt(go.optArgGet());
			} else if ((char) ch == 't') {
				servicetimeout = Integer.parseInt(go.optArgGet());
			} else if ((char) ch == 'd') {
				polling = true;
			} else if ((char) ch == 'i') {
				maxinline = Integer.parseInt(go.optArgGet());
			} else if ((char) ch == 'c') {
				connections = Integer.parseInt(go.optArgGet());
			} else if ((char) ch == 'w') {
				wqSize = Integer.parseInt(go.optArgGet());
			} else if ((char) ch == 'q') {
				queueSize = Integer.parseInt(go.optArgGet());
			} else {
				System.exit(1); // undefined option
			}
		}	
		
		this.run();
	}
	
	public static void main(String[] args) throws Exception { 
		DaRPCServer rpcServer = new DaRPCServer();
		rpcServer.launch(args);		
	}	
}
