package com.anypresence.wsclient.soap.step;

import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;

public interface ProcessorStep {
	
	public void handle(OperationRequest req, Context context) throws SoapClientException;

}
