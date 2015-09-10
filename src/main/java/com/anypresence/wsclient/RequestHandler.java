package com.anypresence.wsclient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import com.google.gson.Gson;

public abstract class RequestHandler {
	
	protected ClassLoader classLoader;
	protected Gson gson;
	protected Method operationMethod;
	protected Object endpoint;
	
	public RequestHandler(ClassLoader classLoader, Gson gson, Method operationMethod, Object endpoint) {
		this.classLoader = classLoader;
		this.gson = gson;
		this.operationMethod = operationMethod;
		this.endpoint = endpoint;
	}
	
	public String handle(OperationRequest req) throws SoapClientException {
		System.out.println("operationMethod: " + operationMethod);
		Annotation[] requestWrappers = operationMethod.getAnnotationsByType(RequestWrapper.class);
		Annotation[] responseWrappers = operationMethod.getAnnotationsByType(ResponseWrapper.class);
		
		if (requestWrappers.length != 1) {
			throw new RuntimeException("Expected to find one and only one @RequestWrapper annotation on operation method, but instead found " + requestWrappers.length);
		}
		
		if (responseWrappers.length != 1) {
			throw new RuntimeException("Expected to find one and only one @ResponseWrapper annotation on operation method, but instead found " + responseWrappers.length);
		}
			
		RequestWrapper requestWrapperAnnotation = (RequestWrapper)requestWrappers[0];
		String requestWrapperClassName = requestWrapperAnnotation.className();
		Class<?> requestWrapperClass;
		try {
			requestWrapperClass = classLoader.loadClass(requestWrapperClassName);
		} catch (ClassNotFoundException e) {
			throw new SoapClientException("Unable to find request wrapper class " + requestWrapperClassName, e);
		}
		Object requestWrapper = gson.fromJson(req.getParams(), requestWrapperClass);		
		
		ResponseWrapper responseWrapperAnnotation = (ResponseWrapper)responseWrappers[0];
		String responseClassName = responseWrapperAnnotation.className();
		Class<?> responseClass;
		try {
			responseClass = classLoader.loadClass(responseClassName);
		} catch (ClassNotFoundException e) {
			throw new SoapClientException("Unable to find response class " + responseClassName, e);
		}
		Object responseInstance;
		try {
			responseInstance = responseClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new SoapClientException("Unable to instantiate class " + responseClassName + " due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
		
		handleImpl(req, requestWrapper, responseInstance);
		
		return gson.toJson(responseInstance);
	}

	protected abstract void handleImpl(OperationRequest req, Object requestInstance, Object responseInstance) throws SoapClientException;
}
