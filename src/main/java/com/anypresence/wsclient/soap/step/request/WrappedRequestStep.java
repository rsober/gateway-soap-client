package com.anypresence.wsclient.soap.step.request;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.ws.Holder;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import com.anypresence.wsclient.Log;
import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;
import com.anypresence.wsclient.soap.step.Context;
import com.anypresence.wsclient.soap.step.ProcessorStep;
import com.google.gson.Gson;

public class WrappedRequestStep implements ProcessorStep {

	private ClassLoader classLoader;
	private Gson gson;
	private Method operationMethod;

	public WrappedRequestStep(ClassLoader classLoader, Gson gson, Method operationMethod) {
		this.classLoader = classLoader;
		this.gson = gson;
		this.operationMethod = operationMethod;
	}
	
	@Override
	public void handle(OperationRequest req, Context context) throws SoapClientException {
		unmarshalRequest(req, context);
		instantiateResponseObject(req, context);
		gatherRequestParameters(req, context);
		buildParameterArray(context);
	}
	
	protected void unmarshalRequest(OperationRequest req, Context context) throws SoapClientException {
		Annotation[] requestWrappers = operationMethod.getAnnotationsByType(RequestWrapper.class);

		if (requestWrappers.length != 1) {
			throw new SoapClientException("Expected to find one and only one @RequestWrapper annotation on operation method, but instead found " + requestWrappers.length);
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
		
		context.setRequestWrapper(requestWrapper);
	}
	
	protected void instantiateResponseObject(OperationRequest req, Context context) throws SoapClientException {
		Log.debug("Attempting to invoke target method " + operationMethod.getName());

		Annotation[] responseWrappers = operationMethod.getAnnotationsByType(ResponseWrapper.class);
		
		if (responseWrappers.length != 1) {
			throw new SoapClientException("Expected to find one and only one @ResponseWrapper annotation on operation method, but instead found " + responseWrappers.length);
		}
		
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

		context.setResponseInstance(responseInstance);
	}
	
	protected void gatherRequestParameters(OperationRequest req, Context context) throws SoapClientException {
		List<String> inParams = new ArrayList<String>(operationMethod.getParameters().length);
		List<String> outParams = new ArrayList<String>(operationMethod.getParameters().length);
		
		for (Parameter parameter : operationMethod.getParameters()) {
			Annotation[] webParams = parameter.getDeclaredAnnotationsByType(WebParam.class);
			for (Annotation annotation: webParams) {
				WebParam webParam = (WebParam)annotation;
				String paramName = webParam.name();
				if (webParam.mode() == Mode.IN) {
					inParams.add(paramName);
				} else if (webParam.mode() == Mode.OUT) {
					if (parameter.getType() != Holder.class) {
						throw new SoapClientException("Found a WebParam with Mode.OUT that was not of type Holder");
					}
					outParams.add(webParam.name());
				}
			}
		}
		
		context.setParameterNames(inParams.toArray(new String[inParams.size()]));
		context.setOutputParameterNames(outParams.toArray(new String[outParams.size()]));
	}
	
	protected void buildParameterArray(Context context) throws SoapClientException {
		Object requestInstance = context.getRequestWrapper();
		String[] parameters = context.getParameterNames();
		Object[] parameterValues = new Object[parameters.length];
		
		context.setOperationMethodParameters(parameterValues);
		
		ProcessingUtils.populateParameterArray(parameters, requestInstance, parameterValues);
		
	}
	

}
