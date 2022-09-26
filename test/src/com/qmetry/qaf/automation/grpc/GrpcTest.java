/**
 * 
 */
package com.qmetry.qaf.automation.grpc;

import static com.qmetry.qaf.automation.step.GrpcSteps.*;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.qmetry.qaf.automation.core.ConfigurationManager;
/**
 * @author chirag
 *
 */
public class GrpcTest {
	
	@Test
	public void testUnaryCall() {
		ConfigurationManager.getBundle().setProperty("grpc.proto_discovery_root", "proto");
		// --endpoint=grpcb.in:9000","--full_method=grpcbin.GRPCBin/Index
		doGrpcClientCall("{'baseurl':'grpcb.inn:9000','endpoint':'grpcbin.GRPCBin/Index'}", ImmutableMap.of());
		responseStatusIs("UNAVAILABLE");
		
		doGrpcClientCall("{'baseurl':'grpcb.in:9000','endpoint':'grpcbin.GRPCBin/Index'}", ImmutableMap.of());
		responseStatusIs("OK");
		
		System.out.println("Error:: " + doGrpcClientCall("{'baseurl':'grpcb.in:9000','endpoint':'grpcbin.GRPCBin/Index'}", ImmutableMap.of()).getError());
		System.out.println(doGrpcClientCall("{'baseurl':'grpcb.in:9000','endpoint':'grpcbin.GRPCBin/HeadersUnary'}", ImmutableMap.of()).getMessageBody());
		//System.out.println("Res:: " + doGrpcClientCall("{'baseurl':'grpcb.in:9000','endpoint':'grpcbin.GRPCBin/RandomError'}", ImmutableMap.of()).getError());

		doGrpcClientCall("{'baseurl':'grpcb.in:9000','endpoint':'grpcbin.GRPCBin/SpecificError','body':\"{'code':1,'reason':'CANCELLED'}\"}", ImmutableMap.of());

		responseStatusIs("CANCELLED");

	}

}
