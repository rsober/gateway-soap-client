package com.anypresence.wsclient.soap.step;

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Holder;

public class Context 
{
	
	static final String REQUEST_WRAPPER = "requestWrapper";
	static final String RESPONSE_INSTANCE = "responseInstance";
	static final String PARAMETER_NAMES = "parameterNames";
	static final String OUTPUT_PARAMETER_NAMES = "outputParameterNames";
	static final String OPERATION_METHOD_PARAMETERS = "operationMethodParameters";
	static final String RESULT_HOLDERS = "resultHolders";
	static final String OPERATION_RESPONSE = "operationResponse";
	
	private Map<String, Object> ctxMap;
	
	public Context() {
		ctxMap = new HashMap<String, Object>();
	}

	public void set(String prop, Object obj) {
		ctxMap.put(prop, obj);
	}
	
	public Object get(String prop) {
		return ctxMap.get(prop);
	}
	
	public Object getRequestWrapper() {
		return get(REQUEST_WRAPPER);
	}
	
	public void setRequestWrapper(Object requestWrapper) {
		set(REQUEST_WRAPPER, requestWrapper);
	}
	
	public Object getResponseInstance() {
		return get(RESPONSE_INSTANCE);
	}
	
	public void setResponseInstance(Object responseInstance) {
		set(RESPONSE_INSTANCE, responseInstance);
	}
	
	public String[] getParameterNames() {
		return (String[])get(PARAMETER_NAMES);
	}
	
	public void setParameterNames(String[] paramNames) {
		set(PARAMETER_NAMES, paramNames);
	}
	
	public String[] getOutputParameterNames() {
		return (String[])get(OUTPUT_PARAMETER_NAMES);
	}
	
	public void setOutputParameterNames(String[] paramNames) {
		set(OUTPUT_PARAMETER_NAMES, paramNames);
	}
	
	public Object[] getOperationMethodParameters() {
		return (Object[])get(OPERATION_METHOD_PARAMETERS);
	}
	
	public void setOperationMethodParameters(Object[] operationMethodParameters) {
		set(OPERATION_METHOD_PARAMETERS, operationMethodParameters);
	}
	
	public Map<String, Holder<?>> getResultHolders() {
		return (Map<String, Holder<?>>)get(RESULT_HOLDERS);
	}
	
	public void setResultHolders(Map<String, Holder<?>> resultHolders) {
		set(RESULT_HOLDERS, resultHolders);
	}
	
	public Object getOperationResponse() {
		return get(OPERATION_RESPONSE);
	}
	
	public void setOperationResponse(Object operationResponse) {
		set(OPERATION_RESPONSE, operationResponse);
	}
	
}
