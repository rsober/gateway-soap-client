package com.anypresence.wsclient.utils;

public class ReflectionException extends RuntimeException {
	
	private static final long serialVersionUID = 1389121745503603344L;

	public ReflectionException(String message) {
		super(message);
	}
	
	public ReflectionException(String message, Throwable cause) {
		super(message, cause);
	}

}
