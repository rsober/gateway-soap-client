package com.anypresence.wsclient;

public class OperationResponse {

	private boolean success;
	private Object response;
	private Object error;
	
	public OperationResponse() {
		
	}
	
	private OperationResponse(boolean success, Object response, Object error) {
		this.success = success;
		this.response = response;
		this.error = error;
	}
	
	public static OperationResponse newSuccessfulOperationResponse(Object response) {
		return new OperationResponse(true, response, null);
	}
	
	public static OperationResponse newFailedOperationResponse(Object error) {
		return new OperationResponse(false, null, error);
	}

	public boolean isSuccess() {
		return success;
	}

	public Object getResponse() {
		return response;
	}

	public Object getError() {
		return error;
	}

	@Override
	public String toString() {
		return "OperationResponse [success=" + success + ", response=" + response + ", error=" + error + "]";
	}
	
}
