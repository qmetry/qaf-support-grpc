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
package com.qmetry.qaf.automation.grpc;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.Serializable;
import java.util.Map;

import com.qmetry.qaf.automation.ws.WsRequestBean;

/**
 * @author chirag.jayswal
 *
 */
public class GrpcRequestBean extends WsRequestBean implements Serializable {

	private static final long serialVersionUID = 6795612295416203373L;

	private String proto_discovery_root;

	private String config_set_path;

	private GrpcRequestBean() {
		//proto_discovery_root = getBundle().getString("grpc.proto_discovery_root");
		config_set_path = getBundle().getString("grpc.config_set_path");
		setBody("{}");
		setMethod("GRPC");
		setBaseUrl(getBundle().getString("grpc.server"));
	}

	public static GrpcRequestBean from(Object o) {
		GrpcRequestBean bean = new GrpcRequestBean();
		if (null != o) {
			bean.fillData(o);
		}
		return bean;
	}
	
	/*public String getQualifiedMethod() {
		StringBuilder qm = new StringBuilder(getEndPoint());
		if(StringUtil.isNotBlank(getMethod())) {
			qm.append("/");
			qm.append(getMethod());
		}
		if(qm.charAt(0)=='/'){
			qm.deleteCharAt(0);
		}	
		return qm.toString().replaceAll("(/)\\1+","/");
	}*/

	public GrpcRequestBean with(Map<String, Object> data) {
		resolveParameters(data);
		return this;
	}

	public String getProto_discovery_root() {
		return proto_discovery_root;
	}

	public void setProto_discovery_root(String proto_discovery_root) {
		this.proto_discovery_root = proto_discovery_root;
	}

	public String getConfig_set_path() {
		return config_set_path;
	}

	public void setConfig_set_path(String config_set_path) {
		this.config_set_path = config_set_path;
	}
	/*
	public static void main(String[] args) {
		System.out.println(GrpcRequestBean.from("{'endpoint':'search.indexer.indexer.Indexer','method':'LogFailedEntities'}").getQualifiedMethod());
		System.out.println(GrpcRequestBean.from("{'endpoint':'/search.indexer.indexer.Indexer','method':'LogFailedEntities'}").getQualifiedMethod());
		System.out.println(GrpcRequestBean.from("{'endpoint':'search.indexer.indexer.Indexer/','method':'LogFailedEntities'}").getQualifiedMethod());
		System.out.println(GrpcRequestBean.from("{'endpoint':'search.indexer.indexer.Indexer/','method':'/LogFailedEntities'}").getQualifiedMethod());
		System.out.println(GrpcRequestBean.from("{'endpoint':'search.indexer.indexer.Indexer/LogFailedEntities'}").getQualifiedMethod());
		System.out.println(GrpcRequestBean.from("{'method':'search.indexer.indexer.Indexer/LogFailedEntities'}").getQualifiedMethod());
	}
	*/
}
