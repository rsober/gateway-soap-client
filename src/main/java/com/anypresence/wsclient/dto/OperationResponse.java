package com.anypresence.wsclient.dto;

import javax.xml.soap.SOAPFault;

public class OperationResponse {

	private boolean success;
	private Object response;
	private Object error;
	private SOAPFault fault;
	
	public OperationResponse() {
		
	}
	
	private OperationResponse(boolean success, Object response, Object error, SOAPFault fault) {
		this.success = success;
		this.response = response;
		this.error = error;
		this.fault = fault;
	}
	
	public static OperationResponse newSuccessfulOperationResponse(Object response) {
		return new OperationResponse(true, response, null, null);
	}
	
	public static OperationResponse newFailedOperationResponse(Object error) {
		return new OperationResponse(false, null, error, null);
	}
	
	public static OperationResponse newFaultOperationResponse(SOAPFault fault) {
		return new OperationResponse(false, null, null, fault);
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
	
	public SOAPFault getFault() {
		return fault;
	}

	@Override
	public String toString() {
		return "OperationResponse [success=" + success + ", response=" + response + ", error=" + error + ", fault="
				+ fault + "]";
	}
	
}
