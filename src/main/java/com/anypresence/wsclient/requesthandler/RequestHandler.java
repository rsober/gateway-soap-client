package com.anypresence.wsclient.requesthandler;

import java.lang.reflect.Method;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import com.anypresence.wsclient.OperationRequest;
import com.anypresence.wsclient.OperationResponse;
import com.anypresence.wsclient.ReflectionUtils;
import com.anypresence.wsclient.SoapClientException;
import com.google.gson.Gson;

public class RequestHandler {

	private ClassLoader loader;
	private Gson gson;
	private Method endpointMethod;
	private Object endpoint;
	
	public RequestHandler(ClassLoader loader, Gson gson, Method endpointMethod, Object endpoint) {
		this.loader = loader;
		this.gson = gson;
		this.endpointMethod = endpointMethod;
		this.endpoint = endpoint;
	}
	
	public String handle(OperationRequest req) throws SoapClientException {
		Context ctx = new Context();
		
		Class<?> c = ReflectionUtils.findInterfaceWithAnnotation(endpoint.getClass(), WebService.class);
		SOAPBinding binding = c.getAnnotation(SOAPBinding.class);
		SOAPBinding.ParameterStyle paramStyle = SOAPBinding.ParameterStyle.WRAPPED;
		if (binding != null) {
			paramStyle = binding.parameterStyle();
		}
		
		// Bare or Wrapped?
		RequestWorker parameterTypeWorker = null;
		if (paramStyle == SOAPBinding.ParameterStyle.WRAPPED) {
			parameterTypeWorker = new WrappedRequestWorker(loader, gson, endpointMethod, endpoint);
		} else {
			parameterTypeWorker = new BareRequestWorker(loader, gson, endpointMethod, endpoint);
		}
		
		parameterTypeWorker.handle(req, ctx);
		
		// Void or not?
		RequestWorker methodTypeWorker = null;
		if (endpointMethod.getReturnType() == Void.TYPE) {
			methodTypeWorker = new VoidRequestWorker(endpointMethod, endpoint);
		} else {
			methodTypeWorker = new DefaultRequestWorker(endpointMethod, endpoint);
		}
		
		methodTypeWorker.handle(req, ctx);
		
		// Response handler - Bare or Wrapped?
		RequestWorker responseWorker = null;
		if (endpointMethod.getReturnType() == Void.TYPE) {
			responseWorker = new VoidResponseWorker(endpointMethod, endpoint);
		} else {
			if (paramStyle == SOAPBinding.ParameterStyle.WRAPPED) {
				responseWorker = new WrappedResponseWorker(loader, gson, endpointMethod, endpoint);
			} else {
				responseWorker = new BareResponseWorker();
			}
		}
		
		responseWorker.handle(req, ctx);
		return gson.toJson(OperationResponse.newSuccessfulOperationResponse(ctx.getResponseInstance()));
	}

}
