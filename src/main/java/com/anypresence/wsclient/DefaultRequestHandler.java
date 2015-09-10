package com.anypresence.wsclient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import javax.jws.WebParam;
import javax.jws.WebParam.Mode;

import com.google.gson.Gson;

public class DefaultRequestHandler extends RequestHandler {

	public DefaultRequestHandler(ClassLoader classLoader, Gson gson, Method operationMethod, Object endpoint) {
		super(classLoader, gson, operationMethod, endpoint);
	}
	
	@Override
	protected void handleImpl(OperationRequest req, Object requestInstance, Object responseInstance) throws Exception {
		Parameter[] parameters = operationMethod.getParameters();
		Object[] parameterValues = new Object[parameters.length];
		int idx = 0;
		
		for (Parameter parameter: parameters) {
			Annotation[] webParams = parameter.getDeclaredAnnotationsByType(WebParam.class);
			for (Annotation annotation: webParams) {
				WebParam webParam = (WebParam)annotation;
				if (webParam.mode() != Mode.IN) {
					throw new RuntimeException("Expected only Mode.IN parameters");
				}
				
				String paramName = webParam.name();
				String methodName = "get" + Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1);
				Method getter = requestInstance.getClass().getMethod(methodName);
				parameterValues[idx++] = getter.invoke(requestInstance);
			}
		}
		
		Object response = operationMethod.invoke(endpoint, parameterValues);
		
		Field[] fields = responseInstance.getClass().getDeclaredFields();
		Field returnField = null;
		for (Field field: fields) {
			if (field.getType() == operationMethod.getReturnType()) {
				field.setAccessible(true);
				field.set(responseInstance, response);
				returnField = field;
				break;
			}
		}
		
		if (returnField == null) {
			throw new RuntimeException("Unable to find field of type " + operationMethod.getReturnType() + " on response object " + responseInstance);
		}
	}

}
