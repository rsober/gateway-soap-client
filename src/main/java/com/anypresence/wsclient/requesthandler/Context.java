package com.anypresence.wsclient.requesthandler;

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Holder;

class Context 
{
	
	static final String REQUEST_WRAPPER = "requestWrapper";
	static final String RESPONSE_INSTANCE = "responseInstance";
	static final String PARAMETER_NAMES = "parameterNames";
	static final String OUTPUT_PARAMETER_NAMES = "outputParameterNames";
	static final String OPERATION_METHOD_PARAMETERS = "operationMethodParameters";
	static final String RESULT_HOLDERS = "resultHolders";
	static final String OPERATION_RESPONSE = "operationResponse";
	
	private Map<String, Object> ctxMap;
	
	Context() {
		ctxMap = new HashMap<String, Object>();
	}

	void set(String prop, Object obj) {
		ctxMap.put(prop, obj);
	}
	
	Object get(String prop) {
		return ctxMap.get(prop);
	}
	
	Object getRequestWrapper() {
		return get(REQUEST_WRAPPER);
	}
	
	void setRequestWrapper(Object requestWrapper) {
		set(REQUEST_WRAPPER, requestWrapper);
	}
	
	Object getResponseInstance() {
		return get(RESPONSE_INSTANCE);
	}
	
	void setResponseInstance(Object responseInstance) {
		set(RESPONSE_INSTANCE, responseInstance);
	}
	
	String[] getParameterNames() {
		return (String[])get(PARAMETER_NAMES);
	}
	
	void setParameterNames(String[] paramNames) {
		set(PARAMETER_NAMES, paramNames);
	}
	
	String[] getOutputParameterNames() {
		return (String[])get(OUTPUT_PARAMETER_NAMES);
	}
	
	void setOutputParameterNames(String[] paramNames) {
		set(OUTPUT_PARAMETER_NAMES, paramNames);
	}
	
	Object[] getOperationMethodParameters() {
		return (Object[])get(OPERATION_METHOD_PARAMETERS);
	}
	
	void setOperationMethodParameters(Object[] operationMethodParameters) {
		set(OPERATION_METHOD_PARAMETERS, operationMethodParameters);
	}
	
	Map<String, Holder<?>> getResultHolders() {
		return (Map<String, Holder<?>>)get(RESULT_HOLDERS);
	}
	
	void setResultHolders(Map<String, Holder<?>> resultHolders) {
		set(RESULT_HOLDERS, resultHolders);
	}
	
	Object getOperationResponse() {
		return get(OPERATION_RESPONSE);
	}
	
	void setOperationResponse(Object operationResponse) {
		set(OPERATION_RESPONSE, operationResponse);
	}
	
}
