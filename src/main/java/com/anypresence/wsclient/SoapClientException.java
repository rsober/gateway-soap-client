package com.anypresence.wsclient;

public class SoapClientException extends Exception {

	private static final long serialVersionUID = 1389121745503603344L;

	public SoapClientException(String message) {
		super(message);
	}
	
	public SoapClientException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
