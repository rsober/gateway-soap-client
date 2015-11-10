package com.anypresence.wsclient.requesthandler;

import com.anypresence.wsclient.OperationRequest;
import com.anypresence.wsclient.SoapClientException;

public class BareResponseWorker implements RequestWorker {
	
	@Override
	public void handle(OperationRequest req, Context context) throws SoapClientException {
		Object response = context.getOperationResponse();
		
		context.setResponseInstance(response);
	}

}
