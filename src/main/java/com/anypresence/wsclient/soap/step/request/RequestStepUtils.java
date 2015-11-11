package com.anypresence.wsclient.soap.step.request;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;

import com.anypresence.wsclient.SoapClientException;
import com.sun.xml.bind.api.impl.NameConverter;

class RequestStepUtils {

	static void populateParameterArray(String[] inputParamNames, Object requestInstance, Object[] parameterValues) throws SoapClientException {
		int idx = 0;
		
		for (String paramName : inputParamNames) {
			
			Field field = null;
			
			for (Field f:  requestInstance.getClass().getDeclaredFields()) {
				XmlElementRef xmlEltRef = f.getDeclaredAnnotation(XmlElementRef.class);
				XmlElement xmlElt = f.getDeclaredAnnotation(XmlElement.class);
				
				if (xmlEltRef != null) {
					if (xmlEltRef.name().equals(paramName)) {
						field = f;
					}
				} else if (xmlElt != null) {
					if (xmlElt.name().equals(paramName)) {
						field = f;
					}
				}
			}
			
			if (field == null) {
				try {
					field = requestInstance.getClass().getDeclaredField(paramName);
				} catch (NoSuchFieldException | SecurityException e) {
					throw new SoapClientException("Unable to find field named " + paramName + " on requestInstance " + requestInstance);
				}
			}
			
			Method getter = null;
			
			try {
				BeanInfo info = Introspector.getBeanInfo(requestInstance.getClass());
				
				PropertyDescriptor[] pds = info.getPropertyDescriptors();
				for (PropertyDescriptor pd : pds) {
					if (NameConverter.standard.toVariableName(pd.getName()).equals(field.getName())) {
						getter = pd.getReadMethod();
					}
				}
			} catch(IntrospectionException e) {
				throw new SoapClientException("Unable to introspect on class " + requestInstance.getClass(), e);
			}
			
			
			try {
				if (getter.getReturnType() == JAXBElement.class) {
					JAXBElement<?> jaxbElt = (JAXBElement<?>)getter.invoke(requestInstance);
					parameterValues[idx++] = jaxbElt == null ? null : jaxbElt.getValue();
				} else {
					parameterValues[idx++] = getter.invoke(requestInstance);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SoapClientException("Unable to invoke getter method " + getter.getName() + " due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
			}
			
		}
	}
	
}
