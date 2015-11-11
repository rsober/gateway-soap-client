package com.anypresence.wsclient.soap;

import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;

public class BareResponseWorker implements RequestWorker {
	
	@Override
	public void handle(OperationRequest req, Context context) throws SoapClientException {
		Object response = context.getOperationResponse();
		
		context.setResponseInstance(response);
	}

}
