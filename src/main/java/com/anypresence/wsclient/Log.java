package com.anypresence.wsclient;

public class Log {

	public static boolean isDebugEnabled() {
		return Boolean.getBoolean("DEBUG");
	}
	
	public static void debug(String msg) {
		if (isDebugEnabled()) {
			System.out.println(msg);
		}
	}
	
}
