package com.anypresence.wsclient.soap;

import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;
import com.anypresence.wsclient.dto.OperationResponse;
import com.anypresence.wsclient.soap.step.Context;
import com.anypresence.wsclient.soap.step.ProcessorStep;
import com.google.gson.Gson;

public class RequestProcessor {

	private Gson gson;
	private ProcessorStep requestStep;
	private ProcessorStep executeStep;
	private ProcessorStep responseStep;
	
	public RequestProcessor(Gson gson, ProcessorStep requestStep, ProcessorStep executeStep, ProcessorStep responseStep) {
		this.gson = gson;
		this.requestStep = requestStep;
		this.executeStep = executeStep;
		this.responseStep = responseStep;
	}
	
	public String process(OperationRequest req) throws SoapClientException {
		Context ctx = new Context();
		
		requestStep.handle(req, ctx);
		executeStep.handle(req, ctx);
		responseStep.handle(req,  ctx);;
		
		return gson.toJson(OperationResponse.newSuccessfulOperationResponse(ctx.getResponseInstance()));
	}
	
}
