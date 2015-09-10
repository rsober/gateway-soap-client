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
	
	public String handle(OperationRequest req) throws Exception {
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
		Class<?> requestWrapperClass = classLoader.loadClass(requestWrapperClassName);
		Object requestWrapper = gson.fromJson(req.getParams(), requestWrapperClass);		
		
		ResponseWrapper responseWrapperAnnotation = (ResponseWrapper)responseWrappers[0];
		String responseClassName = responseWrapperAnnotation.className();
		Class<?> responseClass = classLoader.loadClass(responseClassName);
		Object responseInstance = responseClass.newInstance();
		
		handleImpl(req, requestWrapper, responseInstance);
		
		return gson.toJson(responseInstance);
	}

	protected abstract void handleImpl(OperationRequest req, Object requestInstance, Object responseInstance) throws Exception;
}
