package com.anypresence.wsclient.requesthandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;

import com.anypresence.wsclient.OperationRequest;
import com.anypresence.wsclient.SoapClientException;
import com.google.gson.Gson;

class BareRequestWorker implements RequestWorker {

	private ClassLoader classLoader;
	private Gson gson;
	private Method operationMethod;
	private Object endpoint;

	BareRequestWorker(ClassLoader classLoader, Gson gson, Method operationMethod, Object endpoint) {
		this.classLoader = classLoader;
		this.gson = gson;
		this.operationMethod = operationMethod;
		this.endpoint = endpoint;
	}

	public void handle(OperationRequest req, Context context) throws SoapClientException {
		unmarshalRequest(req, context);
		instantiateResponseObject(req, context);
		gatherRequestParameters(req, context);
		buildParameterArray(context);
	}
	
	protected void unmarshalRequest(OperationRequest req, Context context) throws SoapClientException {
		if (operationMethod.getParameters().length != 1) {
			throw new SoapClientException("Expected 1 and only 1 parameter for method " + operationMethod.getName() + " on class " + operationMethod.getDeclaringClass());
		}
		
		Parameter param = operationMethod.getParameters()[0];
		
		Object requestInstance = null;
		try {
			requestInstance = param.getType().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new SoapClientException("Unable to instantiate request type " + param.getType());
		}
		
		Object requestWrapper = gson.fromJson(req.getParams(), requestInstance.getClass());
		
		context.setRequestWrapper(requestWrapper);
	}
	
	protected void instantiateResponseObject(OperationRequest req, Context context) throws SoapClientException {
		Object responseInstance = null;
		try {
			responseInstance = operationMethod.getReturnType().newInstance();
			context.setResponseInstance(responseInstance);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new SoapClientException("Unable to instantiate return type " + operationMethod.getReturnType());
		}
		context.setResponseInstance(responseInstance);
	}

	protected void gatherRequestParameters(OperationRequest req, Context context) throws SoapClientException {
		Parameter param = operationMethod.getParameters()[0];
		
		List<String> params = new ArrayList<String>();
		Class<?> type = param.getType();
		Field[] fields = type.getDeclaredFields();
		for (Field field : fields) {
			XmlElement elt = field.getDeclaredAnnotation(XmlElement.class);
			if (elt != null) {
				params.add(elt.name());
			}
			XmlElementRef eltRef = field.getDeclaredAnnotation(XmlElementRef.class);
			if (eltRef != null) {
				params.add(eltRef.name());
			}
		}
		context.setParameterNames(params.toArray(new String[params.size()]));
	}

	protected void buildParameterArray(Context context) throws SoapClientException {
		context.setOperationMethodParameters(new Object[] { context.getRequestWrapper() });
	}


}
