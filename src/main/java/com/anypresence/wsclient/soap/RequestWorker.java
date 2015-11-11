package com.anypresence.wsclient.soap;

import com.anypresence.wsclient.SoapClientException;
import com.anypresence.wsclient.dto.OperationRequest;

interface RequestWorker {
	
	public void handle(OperationRequest req, Context context) throws SoapClientException;

}
