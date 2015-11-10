package com.anypresence.wsclient.requesthandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.anypresence.wsclient.OperationRequest;
import com.anypresence.wsclient.SoapClientException;
import com.google.gson.Gson;

public class WrappedResponseWorker implements RequestWorker {

	private ClassLoader classLoader;
	private Gson gson;
	private Method operationMethod;
	private Object endpoint;

	WrappedResponseWorker(ClassLoader classLoader, Gson gson, Method operationMethod, Object endpoint) {
		this.classLoader = classLoader;
		this.gson = gson;
		this.operationMethod = operationMethod;
		this.endpoint = endpoint;
	}
	
	@Override
	public void handle(OperationRequest req, Context context) throws SoapClientException {
		Object responseInstance = context.getResponseInstance();
		Object response = context.getOperationResponse();
		
		Field[] fields = responseInstance.getClass().getDeclaredFields();
		Field returnField = null;
		for (Field field: fields) {
			if (field.getType() == operationMethod.getReturnType()) {
				field.setAccessible(true);
				try {
					field.set(responseInstance, response);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new SoapClientException("Unable to invoke setter to hold response due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
				}
				returnField = field;
				break;
			} else if (field.getGenericType() != null && field.getGenericType().toString().equals((JAXBElement.class.getName() + "<" + operationMethod.getReturnType().getName() + ">"))) {
				
				XmlType xmlType = operationMethod.getReturnType().getDeclaredAnnotation(XmlType.class);
				QName qn = new QName(xmlType.namespace(), xmlType.name());
				
				field.setAccessible(true);
				try {
					field.set(responseInstance, new JAXBElement(qn, operationMethod.getReturnType(), response));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new SoapClientException("Unable to invoke setter to hold response due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
				}
				returnField = field;
				break;
			}
		}
		
		if (returnField == null) {
			throw new SoapClientException("Unable to find field of type " + operationMethod.getReturnType() + " on response object " + responseInstance);
		}
	}

}
