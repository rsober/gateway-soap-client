package com.anypresence.wsclient.soap.step.response;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Holder;

import com.anypresence.wsclient.Log;
import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;
import com.anypresence.wsclient.soap.step.Context;
import com.anypresence.wsclient.soap.step.ProcessorStep;

public class VoidResponseStep implements ProcessorStep {
	
	@Override
	public void handle(OperationRequest req, Context context) throws SoapClientException {
		// TODO Auto-generated method stub

		Object responseInstance = context.getResponseInstance();
		Map<String, Holder<?>> resultHolders = context.getResultHolders();
		
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
