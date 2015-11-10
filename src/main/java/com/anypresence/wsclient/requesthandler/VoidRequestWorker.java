package com.anypresence.wsclient.requesthandler;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Holder;

import com.anypresence.wsclient.Log;
import com.anypresence.wsclient.OperationRequest;
import com.anypresence.wsclient.SoapClientException;

class VoidRequestWorker implements RequestWorker {

	private Method operationMethod;
	private Object endpoint;
	
	VoidRequestWorker(Method operationMethod, Object endpoint) {
		this.operationMethod = operationMethod;
		this.endpoint = endpoint;
	}

	@Override
	public void handle(OperationRequest req, Context context) throws SoapClientException {
		Object requestInstance = context.getRequestWrapper();
		Object responseInstance = context.getResponseInstance();
		String[] inputParamNames = context.getParameterNames();
		String[] outputParams = context.getOutputParameterNames();
		
		Parameter[] parameters = operationMethod.getParameters();
		Object[] parameterValues = new Object[parameters.length];
		Map<String, Holder<?>> resultHolders = new HashMap<String, Holder<?>>();
		int idx = 0;
		
		context.setResultHolders(resultHolders);
		
		for (String paramName : inputParamNames) {
			Method getter = null;
			String methodName = "get" + Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1);
			try {
				getter = requestInstance.getClass().getMethod(methodName);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new SoapClientException("Encountered exception attempting to retrieve Method object for method " + methodName + " due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
			}

			try {
				parameterValues[idx++] = getter.invoke(requestInstance);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SoapClientException("Unable to successfully invoke method " + getter.getName() + " on object due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
			}
		}
		
		for (String parameter: outputParams) {
			//Log.debug("Found output holder parameter " + webParam.name());
			Holder<Object> holder = new Holder<Object>();
			parameterValues[idx++] = holder;
			resultHolders.put(parameter, holder);
		}
		
		Log.debug("Invoking method " + operationMethod.getName());
		try {
			operationMethod.invoke(endpoint, parameterValues);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new SoapClientException("Unable to successfully invoke operation due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
		Log.debug("Done invoking method " + operationMethod.getName());

		
	}

}
