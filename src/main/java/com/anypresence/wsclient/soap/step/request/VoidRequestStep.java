package com.anypresence.wsclient.soap.step.request;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.ws.Holder;

import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;
import com.anypresence.wsclient.soap.step.Context;
import com.anypresence.wsclient.soap.step.ProcessorStep;
import com.google.gson.Gson;

public class VoidRequestStep implements ProcessorStep {

	private Method operationMethod;
	private WrappedRequestStep wrappedRequestStep;
	
	public VoidRequestStep(ClassLoader classLoader, Gson gson, Method operationMethod) {
		this.operationMethod = operationMethod;
		this.wrappedRequestStep = new WrappedRequestStep(classLoader, gson, operationMethod);
	}
	
	@Override
	public void handle(OperationRequest req, Context context) throws SoapClientException {
		wrappedRequestStep.handle(req,  context);
		
		Object requestInstance = context.getRequestWrapper();
		String[] inputParamNames = context.getParameterNames();
		String[] outputParams = context.getOutputParameterNames();
		
		Parameter[] parameters = operationMethod.getParameters();
		Object[] parameterValues = new Object[parameters.length];
		Map<String, Holder<?>> resultHolders = new HashMap<String, Holder<?>>();
		
		context.setResultHolders(resultHolders);
		
		ProcessingUtils.populateParameterArray(inputParamNames, requestInstance, parameterValues);
		
		Integer idx = null;
		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			WebParam webParam = param.getDeclaredAnnotation(WebParam.class);
			if (webParam.mode() == Mode.OUT) {
				idx = i;
				break;
			}
		}
		
		if (idx != null) {
			for (String parameter: outputParams) {
				Holder<Object> holder = new Holder<Object>();
				parameterValues[idx++] = holder;
				resultHolders.put(parameter, holder);
			}
		}
		
		context.setOperationMethodParameters(parameterValues);
	}

}
