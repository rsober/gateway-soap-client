package com.anypresence.wsclient;

public class WssePasswordSecurityCredentials {

	public static final String TYPE = "WSSE_PASSWORD";
	
	private String username;
	private String password;
	
	public WssePasswordSecurityCredentials() {
		// Empty Constructor
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		return "WssePasswordSecurityCredentials [username=" + username + ", password=" + password + "]";
	}
	
}
