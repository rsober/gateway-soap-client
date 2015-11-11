package com.anypresence.wsclient.soap;

import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;
import com.anypresence.wsclient.dto.OperationResponse;
import com.anypresence.wsclient.soap.step.Context;
import com.anypresence.wsclient.soap.step.ProcessorStep;
import com.google.gson.Gson;

public class RequestProcessor {

	private Gson gson;
	private ProcessorStep[] steps;
	
	public RequestProcessor(Gson gson, ProcessorStep... steps) {
		this.gson = gson;
		this.steps = steps;
	}
	
	public String process(OperationRequest req) throws SoapClientException {
		Context ctx = new Context();
		
		for (ProcessorStep step : steps) {
			step.handle(req, ctx);
		}
		
		return gson.toJson(OperationResponse.newSuccessfulOperationResponse(ctx.getResponseInstance()));
	}
	
}
