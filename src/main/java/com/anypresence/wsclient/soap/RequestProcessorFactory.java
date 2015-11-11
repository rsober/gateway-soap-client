package com.anypresence.wsclient.soap;

import java.lang.reflect.Method;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import com.anypresence.wsclient.soap.step.ProcessorStep;
import com.anypresence.wsclient.soap.step.execute.DefaultExecuteStep;
import com.anypresence.wsclient.soap.step.execute.VoidExecuteStep;
import com.anypresence.wsclient.soap.step.request.BareRequestStep;
import com.anypresence.wsclient.soap.step.request.WrappedRequestStep;
import com.anypresence.wsclient.soap.step.response.BareResponseStep;
import com.anypresence.wsclient.soap.step.response.VoidResponseStep;
import com.anypresence.wsclient.soap.step.response.WrappedResponseStep;
import com.anypresence.wsclient.utils.ReflectionUtils;
import com.google.gson.Gson;

public class RequestProcessorFactory {

	private ClassLoader loader;
	private Gson gson;
	private Method endpointMethod;
	private Object endpoint;
	
	public RequestProcessorFactory(ClassLoader loader, Gson gson, Method endpointMethod, Object endpoint) {
		this.loader = loader;
		this.gson = gson;
		this.endpointMethod = endpointMethod;
		this.endpoint = endpoint;
	}
	
	public RequestProcessor createRequestProcessor() {
		Class<?> c = ReflectionUtils.findInterfaceWithAnnotation(endpoint.getClass(), WebService.class);
		SOAPBinding binding = c.getAnnotation(SOAPBinding.class);
		SOAPBinding.ParameterStyle paramStyle = SOAPBinding.ParameterStyle.WRAPPED;
		if (binding != null) {
			paramStyle = binding.parameterStyle();
		}
		
		ProcessorStep rqp = requestProcessor(paramStyle);
		ProcessorStep exp = executeProcessor(endpointMethod);
		ProcessorStep rsp = responseProcessor(endpointMethod, paramStyle);
		
		RequestProcessor proc = new RequestProcessor(gson, rqp, exp, rsp);
		
		return proc;
	}
	
	private ProcessorStep requestProcessor(SOAPBinding.ParameterStyle paramStyle) {
		// Bare or Wrapped?
		ProcessorStep parameterTypeWorker = null;
		if (paramStyle == SOAPBinding.ParameterStyle.WRAPPED) {
			parameterTypeWorker = new WrappedRequestStep(loader, gson, endpointMethod);
		} else {
			parameterTypeWorker = new BareRequestStep(gson, endpointMethod);
		}
		return parameterTypeWorker;
	}
	
	private ProcessorStep executeProcessor(Method endpointMethod) {
		ProcessorStep methodTypeWorker = null;
		if (endpointMethod.getReturnType() == Void.TYPE) {
			methodTypeWorker = new VoidExecuteStep(endpointMethod, endpoint);
		} else {
			methodTypeWorker = new DefaultExecuteStep(endpointMethod, endpoint);
		}
		return methodTypeWorker;
	}
	
	private ProcessorStep responseProcessor(Method endpointMethod, SOAPBinding.ParameterStyle paramStyle) {
		ProcessorStep responseWorker = null;
		if (endpointMethod.getReturnType() == Void.TYPE) {
			responseWorker = new VoidResponseStep();
		} else {
			if (paramStyle == SOAPBinding.ParameterStyle.WRAPPED) {
				responseWorker = new WrappedResponseStep(endpointMethod);
			} else {
				responseWorker = new BareResponseStep();
			}
		}
		return responseWorker;
	}
	
}
