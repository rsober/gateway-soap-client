package com.anypresence.wsclient.dto;


import com.google.gson.JsonElement;

public class OperationRequest {

	private String jarUrl;
	private String wsdlUrl;
	private String serviceName;
	private String endpointName;
	private String operationName;
	private String actionName;
	private JsonElement params;
	private WssePasswordSecurityCredentials wssePasswordCredentials;
	private String url;
	private String key;
	private String keyAlias;
	
	public OperationRequest() {
		// Empty constructor
	}

	public String getJarUrl() {
		return jarUrl;
	}

	public String getWsdlUrl() {
		return wsdlUrl;
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

	public String getKey() { return key; }

	public String getKeyAlias() { return keyAlias; }
	
	public WssePasswordSecurityCredentials getWssePasswordCredentials() {
		return wssePasswordCredentials;
	}
	
	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return "OperationRequest [jarUrl=" + jarUrl + ", wsdlUrl= " + wsdlUrl + ", serviceName=" + serviceName + ", endpointName=" + endpointName
				+ ", operationName=" + operationName + ", actionName=" + actionName + ", params=" + params
				+ ", wssePasswordCredentials=" + wssePasswordCredentials + ", url=" + url + "]";
	}

	
	
}
