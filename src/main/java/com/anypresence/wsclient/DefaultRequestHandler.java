package com.anypresence.wsclient;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;

import com.google.gson.Gson;

import com.sun.xml.bind.api.impl.NameConverter;

public class DefaultRequestHandler extends RequestHandler {

	public DefaultRequestHandler(ClassLoader classLoader, Gson gson, Method operationMethod, Object endpoint) {
		super(classLoader, gson, operationMethod, endpoint);
	}
	
	@Override
	protected void handleImpl(OperationRequest req, Object requestInstance, Object responseInstance) throws SoapClientException {
		Parameter[] parameters = operationMethod.getParameters();
		Object[] parameterValues = new Object[parameters.length];
		int idx = 0;
		
		for (Parameter parameter: parameters) {
			Annotation[] webParams = parameter.getDeclaredAnnotationsByType(WebParam.class);
			for (Annotation annotation: webParams) {
				WebParam webParam = (WebParam)annotation;
				if (webParam.mode() != Mode.IN) {
					throw new SoapClientException("Expected only Mode.IN parameters");
				}
				
				String paramName = webParam.name();
				Field field = null;
				String xmlEltName = null;
				
				for (Field f:  requestInstance.getClass().getDeclaredFields()) {
					XmlElementRef xmlEltRef = f.getDeclaredAnnotation(XmlElementRef.class);
					XmlElement xmlElt = f.getDeclaredAnnotation(XmlElement.class);
					
					if (xmlEltRef != null) {
						if (xmlEltRef.name().equals(paramName)) {
							field = f;
							xmlEltName = xmlEltRef.name();
						}
					} else if (xmlElt != null) {
						if (xmlElt.name().equals(paramName)) {
							field = f;
							xmlEltName = xmlElt.name();
						}
					}
				}
				
				Method getter = null;
				
				try {
					BeanInfo info = Introspector.getBeanInfo(requestInstance.getClass());
					
					PropertyDescriptor[] pds = info.getPropertyDescriptors();
					for (PropertyDescriptor pd : pds) {
						if (NameConverter.standard.toVariableName(xmlEltName).equals(field.getName())) {
							getter = pd.getReadMethod();
						}
					}
				} catch(IntrospectionException e) {
					throw new SoapClientException("Unable to introspect on class " + requestInstance.getClass(), e);
				}
				
				
				try {
					parameterValues[idx++] = getter.invoke(requestInstance);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new SoapClientException("Unable to invoke getter method " + getter.getName() + " due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
				}
			}
		}
		
		Object response;
		try {
			response = operationMethod.invoke(endpoint, parameterValues);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new SoapClientException("Unable to invoke operation due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
		
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
			}
		}
		
		if (returnField == null) {
			throw new SoapClientException("Unable to find field of type " + operationMethod.getReturnType() + " on response object " + responseInstance);
		}
	}
	
}
