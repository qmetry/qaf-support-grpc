/*******************************************************************************
 * Copyright (c) 2019 Infostretch Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.qmetry.qaf.automation.step;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;
import static com.qmetry.qaf.automation.grpc.GrpcClientUtil.call;
import static com.qmetry.qaf.automation.grpc.GrpcClientUtil.getGrpcResponse;
import static com.qmetry.qaf.automation.grpc.GrpcClientUtil.listMethods;
import static com.qmetry.qaf.automation.grpc.GrpcClientUtil.renderDescriptor;
import static com.qmetry.qaf.automation.util.Validator.assertThat;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.JsonPath;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.grpc.GrpcClientUtil;
import com.qmetry.qaf.automation.grpc.GrpcRequestBean;
import com.qmetry.qaf.automation.grpc.GrpcResponse;
import com.qmetry.qaf.automation.util.StringUtil;
import com.qmetry.qaf.automation.util.Validator;

import me.dinowernli.grpc.polyglot.protobuf.ProtocInvoker.ProtocInvocationException;
/**
 * 
 * @author chirag.jayswal
 *
 */
public class GrpcSteps {

	@QAFTestStep(description = "client sends a {grpc-endpoint} request to the server")
	public static GrpcResponse callGrpcMethod(String endpoint) {
		return callGrpcMethodUsingData(endpoint, "{}", ImmutableMap.of());
	}

	@QAFTestStep(description = "client sends a {grpc-endpoint} request  with {payload} to the server")
	public static GrpcResponse callGrpcMethodWithPayload(String endpoint, String payload) {
		return callGrpcMethodUsingData(endpoint, payload, ImmutableMap.of());
	}

    @QAFTestStep(description = "client sends a {grpc-endpoint} request  with {payload} to the server using {data}")
	public static GrpcResponse callGrpcMethodUsingData(String endpoint, String payload, Map<String, Object> data){
    	String server = getBundle().getString("grpc.baseurl",getBundle().getString("grpc.server"));
    	if(StringUtil.countMatches(endpoint, "/")>1) {
    		String[] parts = endpoint.split("/",2);
    		if(StringUtil.isNotBlank(parts[0])) {
    			server=parts[0];
    		}
    		endpoint=parts[1];
    	}
		GrpcRequestBean bean = GrpcRequestBean.from(String.format("{'endpoint':'%s'}", endpoint)).with(data);
		bean.setBaseUrl(server);
		if(StringUtil.isNotBlank(payload)) {
			bean.setBody(payload);
		}
		return GrpcClientUtil.call(bean);
	}

	@QAFTestStep(description = "get grpc method names from {proto}")
	public static List<String> getGRPCMethodNames(String protofile) {
		try {
			ConfigurationManager.getBundle().setProperty("grpc.proto_discovery_root", "resources/proto");

			return listMethods(protofile).stream().map(m -> m.getName()).collect(Collectors.toList());
		} catch (ProtocInvocationException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	@QAFTestStep(description = "get grpc method {method} details from {proto}")
	public static Map<String, String> getGRPCMethodDetails(String protofile, String method) {
		try {
			return listMethods(protofile).stream().filter(m -> m.getName().equalsIgnoreCase(method)).map(m -> {
				Map<String, String> details = new HashMap<>();
				details.put("baseurl", ConfigurationManager.getBundle().getString("grpc.server", "grpc.server"));
				details.put("endpoint", m.getFullName().split("." + method)[0] + "/" + method);
				details.put("input", renderDescriptor(m.getInputType(), ""));
				return details;
			}).findFirst().get();

		} catch (ProtocInvocationException e) {
			e.printStackTrace();
			return Collections.emptyMap();
		}
	}

	@QAFTestStep(description = "client does {reqcall} grpc call")
	public static void doGrpcClientCall(Object request) {
		doGrpcClientCall(request, ImmutableMap.of());
	}

	@QAFTestStep(description = "client does {reqcall} grpc call with {data}", stepName = "doGrpcClientCallWithData")
	public static GrpcResponse doGrpcClientCall(Object request, Map<String, Object> data) {
		GrpcRequestBean bean = GrpcRequestBean.from(request).with(data);
		return call(bean);
	}

	/**
	 * This method check for the response status of GRPC call
	 * <p>
	 * Example:
	 * <p>
	 * BDD
	 * </p>
	 * <code>
	 * response has status 'OK'<br/>
	 * </code>
	 * <p>
	 * KWD
	 * </p>
	 * 
	 * @param status : {0} : Status string for Example: OK, UNAUTHENTICATED
	 * @see {@link Code StatusCodes}
	 */
	@QAFTestStep(description = "grpc response status is {status}")
	public static void grpcResponseStatusIs(String status) {
		assertThat("Response Status", getGrpcResponse().getStatus().getCode().name(), Matchers.equalToIgnoringCase(status));
	}

	/**
	 * This method check for the response description of GRPC call
	 * <p>
	 * Example:
	 * <p>
	 * BDD
	 * </p>
	 * <code>
	 * grpc response description is 'service description'<br/>
	 * </code>
	 * <p>
	 * KWD
	 * </p>
	 * 
	 * @param description : {0} : description string
	 */
	@QAFTestStep(description = "grpc response description is {description}")
	public static void grpcResponseDescriptionIs(String description) {
		assertThat("Response Status", getGrpcResponse().getStatus().getDescription(),
				Matchers.equalToIgnoringCase(description));
	}

	/**
	 * This method check given jsonpath is there in grpc response of web service
	 * <p>
	 * Example:
	 * </p>
	 * <code>
	 * grpc response has 'user.username'
	 * </code>
	 * <p />
	 * 
	 * @param path : jsonpath
	 */
	@QAFTestStep(description = "grpc response has jsonpath {jsonpath}")
	public static void grpcResponseHasJsonPath(String path) {
		if (!path.startsWith("$"))
			path = "$." + path;
		assertThat("Response Body has " + path, hasJsonPath(getGrpcResponse().getMessageBody(), path),
				Matchers.equalTo(true));
	}

	/**
	 * This method check given jsonpath is not in grpc response of web service
	 * <p>
	 * Example:
	 * </p>
	 * <code>
	 * grpc response not have 'user.username'
	 * </code>
	 * <p />
	 * 
	 * @param path : jsonpath
	 */
	@QAFTestStep(description = "grpc response not have jsonpath {jsonpath}")
	public static void grpcResponseNotHaveJsonPath(String path) {
		if (!path.startsWith("$"))
			path = "$." + path;
		assertThat("Response Body has not " + path, hasJsonPath(getGrpcResponse().getMessageBody(), path),
				Matchers.equalTo(false));
	}

	/**
	 * This method validates value for given jsonpath
	 * <p>
	 * Example:
	 * </p>
	 * <code>
	 * grpc response has 'admin' at 'user.username'
	 * </code>
	 * <p />
	 * 
	 * @param expectedValue : expected value
	 * @param path          : jsonpath
	 */
	@QAFTestStep(description = "grpc response has {expectedvalue} at {jsonpath}")
	public static void grpcResponseHasKeyWithValue(Object expectedValue, String path) {
		if (!path.startsWith("$"))
			path = "$." + path;
		Object actual = JsonPath.read(getGrpcResponse().getMessageBody(), path);
		if (null != actual && Number.class.isAssignableFrom(actual.getClass())) {
			assertThat(new BigDecimal(String.valueOf(actual)),
					Matchers.equalTo(new BigDecimal(String.valueOf(expectedValue))));
		} else {
			assertThat(actual, Matchers.equalTo((Object) expectedValue));
		}
	}

	@QAFTestStep(description = "grpc response contains json {expectedJsonStr}")
	public static void assertGrpcResponseContains(String expectedJsonStr) {
		Validator.assertJsonContains(getGrpcResponse().getMessageBody(), expectedJsonStr);
	}

	@QAFTestStep(description = "grpc response should contain json {expectedJsonStr}")
	public static void verifyGrpcResponseContains(String expectedJsonStr) {
		Validator.verifyJsonContains(getGrpcResponse().getMessageBody(), expectedJsonStr);
	}

	@QAFTestStep(description = "grpc response matches json {expectedJsonStr}")
	public static void assertGrpcResponseMatches(String expectedJsonStr) {
		Validator.assertJsonMatches(getGrpcResponse().getMessageBody(), expectedJsonStr);
	}

	@QAFTestStep(description = "grpc response should match json {expectedJsonStr}")
	public static void verifyGrpcResponseMatches(String expectedJsonStr) {
		Validator.verifyJsonMatches(getGrpcResponse().getMessageBody(), expectedJsonStr);
	}

    @QAFTestStep(description = "resolve grpc request call {0} with {data}")
    public GrpcRequestBean resolveGrpcCallwithData(Object request, Map<String, Object> data) {
    	GrpcRequestBean bean = GrpcRequestBean.from(request);
		bean.fillData(request);
		bean.resolveParameters(data);
		return bean;
    }
	/**
	 * @param json
	 * @param path
	 * @return
	 */
	private static boolean hasJsonPath(String json, String path) {
		try {
			Object res = JsonPath.read(json, path);
			JSONObject resObject = new JSONObject(res);
			return !resObject.optBoolean("empty");
		} catch (Exception exception) {
			return false;
		}
	}
}
