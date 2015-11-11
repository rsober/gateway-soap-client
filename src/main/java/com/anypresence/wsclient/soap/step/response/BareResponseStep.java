package com.anypresence.wsclient.soap.step.response;

import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;
import com.anypresence.wsclient.soap.step.Context;
import com.anypresence.wsclient.soap.step.ProcessorStep;

public class BareResponseStep implements ProcessorStep {
	
	@Override
	public void handle(OperationRequest req, Context context) throws SoapClientException {
		Object response = context.getOperationResponse();
		
		context.setResponseInstance(response);
	}

}
