package com.anypresence.wsclient.requesthandler;

import com.anypresence.wsclient.OperationRequest;
import com.anypresence.wsclient.SoapClientException;

interface RequestWorker {
	
	public void handle(OperationRequest req, Context context) throws SoapClientException;

}
