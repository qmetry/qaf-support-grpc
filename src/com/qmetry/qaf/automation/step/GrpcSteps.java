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

import static com.qmetry.qaf.automation.grpc.GrpcClientUtil.call;
import static com.qmetry.qaf.automation.grpc.GrpcClientUtil.getResponse;
import static com.qmetry.qaf.automation.util.Validator.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.hamcrest.Matchers;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.JsonPath;
import com.qmetry.qaf.automation.grpc.GrpcRequestBean;
import com.qmetry.qaf.automation.grpc.GrpcResponse;
import com.qmetry.qaf.automation.util.Validator;


/**
 * 
 * @author chirag.jayswal
 *
 */
public class GrpcSteps {

	@QAFTestStep(description = "client does {reqcall} grpc call")
	public static void doGrpcClientCall(Object request) {
		doGrpcClientCall(request, ImmutableMap.of());

	}

	@QAFTestStep(description = "client does {reqcall} grpc call with {data}", stepName = "doGrpcClientCallWithData")
	public static GrpcResponse doGrpcClientCall(Object request, Map<String, Object> data) {
		GrpcRequestBean bean = GrpcRequestBean.from(request).with(data);
		return call(bean);
	}

	public static void listMethods(String protoFile) {
		
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
	public static void responseStatusIs(String status) {
		assertThat("Response Status", getResponse().getStatus().getCode().name(), Matchers.equalToIgnoringCase(status));
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
	public static void responseDescriptionIs(String description) {
		assertThat("Response Status", getResponse().getStatus().getDescription(),
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
		assertThat("Response Body has " + path, hasJsonPath(getResponse().getMessageBody(), path),
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
		assertThat("Response Body has not " + path, hasJsonPath(getResponse().getMessageBody(), path),
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
		Object actual = JsonPath.read(getResponse().getMessageBody(), path);
		if (null != actual && Number.class.isAssignableFrom(actual.getClass())) {
			assertThat(new BigDecimal(String.valueOf(actual)),
					Matchers.equalTo(new BigDecimal(String.valueOf(expectedValue))));
		} else {
			assertThat(actual, Matchers.equalTo((Object) expectedValue));
		}
	}

	@QAFTestStep(description = "grpc response contains json {expectedJsonStr}")
	public static void assertResponseContains(String expectedJsonStr) {
		Validator.assertJsonContains(getResponse().getMessageBody(), expectedJsonStr);
	}

	@QAFTestStep(description = "grpc response should contain json {expectedJsonStr}")
	public static void verifyResponseContains(String expectedJsonStr) {
		Validator.verifyJsonContains(getResponse().getMessageBody(), expectedJsonStr);
	}

	@QAFTestStep(description = "grpc response matches json {expectedJsonStr}")
	public static void assertResponseMatches(String expectedJsonStr) {
		Validator.assertJsonMatches(getResponse().getMessageBody(), expectedJsonStr);
	}

	@QAFTestStep(description = "grpc response should match json {expectedJsonStr}")
	public static void verifyResponseMatches(String expectedJsonStr) {
		Validator.verifyJsonMatches(getResponse().getMessageBody(), expectedJsonStr);
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
