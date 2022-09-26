/**
 * 
 */
package com.qmetry.qaf.automation.grpc;

import io.grpc.Status;

/**
 * @author chirag.jayswal
 *
 */
public class GrpcResponse {

	private String body;
	private Throwable error;
	private Status status;

	public GrpcResponse(String body) {
		this.body = body;
		status = Status.OK;
	}

	public GrpcResponse(String body, Throwable error) {
		this.error = error;
		this.body = body;
		status = Status.fromThrowable(error);
	}

	public String getMessageBody() {
		return body;
	}

	public Throwable getError() {
		return error;
	}

	public Status getStatus() {
		return status;
	}
}
