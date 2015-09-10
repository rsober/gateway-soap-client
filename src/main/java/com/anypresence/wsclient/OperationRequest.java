package com.anypresence.wsclient;


import com.google.gson.JsonElement;

public class OperationRequest {

	private String jarUrl;
	private String serviceName;
	private String endpointName;
	private String operationName;
	private String actionName;
	private JsonElement params;
	private WssePasswordSecurityCredentials wssePasswordCredentials;
	
	public OperationRequest() {
		// Empty constructor
	}

	public String getJarUrl() {
		return jarUrl;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
	public String getEndpointName() {
		return endpointName;
	}

	public String getOperationName() {
		return operationName;
	}
	
	public String getActionName() {
		return actionName;
	}

	public JsonElement getParams() {
		return params;
	}
	
	public WssePasswordSecurityCredentials getWssePasswordCredentials() {
		return wssePasswordCredentials;
	}

	@Override
	public String toString() {
		return "OperationRequest [jarUrl=" + jarUrl + ", serviceName=" + serviceName + ", endpointName=" + endpointName
				+ ", operationName=" + operationName + ", actionName=" + actionName + ", params=" + params
				+ ", wssePasswordCredentials=" + wssePasswordCredentials + "]";
	}
	
}
