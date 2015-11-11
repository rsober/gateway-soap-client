package com.anypresence.wsclient.soap.step.request;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;

import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;
import com.anypresence.wsclient.soap.step.Context;
import com.anypresence.wsclient.soap.step.ProcessorStep;
import com.google.gson.Gson;

public class BareRequestStep implements ProcessorStep {

	private Gson gson;
	private Method operationMethod;

	public BareRequestStep(Gson gson, Method operationMethod) {
		this.gson = gson;
		this.operationMethod = operationMethod;
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
