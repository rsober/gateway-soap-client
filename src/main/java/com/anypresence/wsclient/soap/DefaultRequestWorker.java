package com.anypresence.wsclient.soap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;

public class DefaultRequestWorker implements RequestWorker {

	private Method operationMethod;
	private Object endpoint;
	
	public DefaultRequestWorker(Method operationMethod, Object endpoint) {
		this.operationMethod = operationMethod;
		this.endpoint = endpoint;
	}
	
	@Override
	public void handle(OperationRequest req, Context context) throws SoapClientException {
		if (context.getOutputParameterNames() != null && context.getOutputParameterNames().length > 0) {
			throw new SoapClientException("Expected no output parameters");
		}
		
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
