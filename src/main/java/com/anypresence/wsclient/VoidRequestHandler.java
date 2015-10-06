package com.anypresence.wsclient;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.ws.Holder;

import com.google.gson.Gson;

public class VoidRequestHandler extends RequestHandler {

	public VoidRequestHandler(ClassLoader classLoader, Gson gson, Method operationMethod, Object endpoint) {
		super(classLoader, gson, operationMethod, endpoint);
	}

	protected void handleImpl(OperationRequest req, Object requestInstance, Object responseInstance) throws SoapClientException {
		Parameter[] parameters = operationMethod.getParameters();
		Object[] parameterValues = new Object[parameters.length];
		Map<String, Holder<?>> resultHolders = new HashMap<String, Holder<?>>();
		int idx = 0;
		for (Parameter parameter: parameters) {
			WebParam webParam = (WebParam)parameter.getAnnotation(WebParam.class);
			if (webParam != null) {
				if (webParam.mode() == Mode.IN) {
					String paramName = webParam.name();
					
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
				} else if (webParam.mode() == Mode.OUT) {
					Log.debug("Found output holder paratmer " + webParam.name());
					if (parameter.getType() == Holder.class) {
						Holder<Object> holder = new Holder<Object>();
						parameterValues[idx++] = holder;
						resultHolders.put(webParam.name(), holder);
					} else {
						throw new SoapClientException("Found a WebParam with Mode.OUT that was not of type Holder");
					}
				} else {
					throw new SoapClientException("Found a WebParam that was not Mode.IN or Mode.OUT");
				}
			} else {
				throw new SoapClientException("Found a parameter that was not annotated by @WebParam");
			}
		}
		Log.debug("Invoking method " + operationMethod.getName());
		try {
			operationMethod.invoke(endpoint, parameterValues);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new SoapClientException("Unable to successfully invoke operation due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
		Log.debug("Done invoking method " + operationMethod.getName());

		for (Map.Entry<String, Holder<?>> resultHolder: resultHolders.entrySet()) {
			String setterName = "set" + Character.toUpperCase(resultHolder.getKey().charAt(0)) + resultHolder.getKey().substring(1);
			Method[] meths = responseInstance.getClass().getMethods();
			Method setter = null;
			for (Method meth: meths) {
				if (meth.getName().equals(setterName)) {
					setter = meth;
					break;
				}
			}
			if (setter == null) {
				Log.debug("Unable to find a setter method - falling back to field named " + resultHolder.getKey());
				Field field;
				try {
					field = responseInstance.getClass().getDeclaredField(resultHolder.getKey());
				} catch (NoSuchFieldException | SecurityException e) {
					throw new SoapClientException("Unable to successfully get field " + resultHolder.getKey() + " due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
				}
				if (field.getType() == List.class) {
					field.setAccessible(true);
					try {
						field.set(responseInstance, resultHolder.getValue().value);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new SoapClientException("Unable to inoke setter on result holder due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
					}
				} else {
					throw new SoapClientException("Field " + resultHolder.getKey() + " was not of type List");
				}
			} else {
				Log.debug("Invoking setter method " + setterName + " with value " + resultHolder.getValue().value);
				try {
					setter.invoke(responseInstance, resultHolder.getValue().value);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new SoapClientException("Unable to inoke setter on result holder due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
				}
			}
		}
	}

}
