package com.anypresence.wsinvoker;

import java.lang.reflect.Field;
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
	
	protected void handleImpl(OperationRequest req, Object requestInstance, Object responseInstance) throws Exception {
		Parameter[] parameters = operationMethod.getParameters();
		Object[] parameterValues = new Object[parameters.length];
		Map<String, Holder<?>> resultHolders = new HashMap<String, Holder<?>>();
		int idx = 0;
		for (Parameter parameter: parameters) {
			WebParam webParam = (WebParam)parameter.getAnnotation(WebParam.class);
			if (webParam != null) {
				if (webParam.mode() == Mode.IN) {
					String paramName = webParam.name();
					String methodName = "get" + Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1);
					Method getter = requestInstance.getClass().getMethod(methodName);
					parameterValues[idx++] = getter.invoke(requestInstance);
				} else if (webParam.mode() == Mode.OUT) {
					System.out.println("Found out param! " + webParam.name());
					if (parameter.getType() == Holder.class) {
						Holder<Object> holder = new Holder<Object>();
						parameterValues[idx++] = holder;
						resultHolders.put(webParam.name(), holder);
					} else {
						// TODO
					}
				} else {
					// TODO
				}
			} else {
				// TODO (huh?)
			}
		}
		System.out.println("Invoking");
		operationMethod.invoke(endpoint, parameterValues);
		System.out.println("Done invoking");
		
		for (Map.Entry<String, Holder<?>> resultHolder: resultHolders.entrySet()) {
			String setterName = "set" + Character.toUpperCase(resultHolder.getKey().charAt(0)) + resultHolder.getKey().substring(1);
			System.out.println("setterName: " + setterName);
			Method[] meths = responseInstance.getClass().getMethods();
			Method setter = null;
			for (Method meth: meths) {
				if (meth.getName().equals(setterName)) {
					setter = meth;
					break;
				}
			}
			if (setter == null) {
				Field field = responseInstance.getClass().getDeclaredField(resultHolder.getKey());
				if (field.getType() == List.class) {
					field.setAccessible(true);
					field.set(responseInstance, resultHolder.getValue().value);
				} else {
					// TODO ?
				}
			} else {
				System.out.println("Invoking setter");
				setter.invoke(responseInstance, resultHolder.getValue().value);
			}
		}
	}

}
