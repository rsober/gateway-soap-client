package com.anypresence.wsclient.soap.step.execute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;
import com.anypresence.wsclient.soap.step.Context;
import com.anypresence.wsclient.soap.step.ProcessorStep;

public class DefaultExecuteStep implements ProcessorStep {

	private Method operationMethod;
	private Object endpoint;
	
	public DefaultExecuteStep(Method operationMethod, Object endpoint) {
		this.operationMethod = operationMethod;
		this.endpoint = endpoint;
	}
	
	@Override
	public void handle(OperationRequest req, Context context) throws SoapClientException {
		Object[] parameterValues = context.getOperationMethodParameters();
		
		Object response;
		try {
			response = operationMethod.invoke(endpoint, parameterValues);
			context.setOperationResponse(response);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new SoapClientException("Unable to invoke operation due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
	}
	
}
