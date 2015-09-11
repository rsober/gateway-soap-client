package com.anypresence.wsclient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

	private static final DateFormat FORMAT = new SimpleDateFormat("YYYY/MM/dd HH:mm:ss.SSS");
	
	public static boolean isDebugEnabled() {
		return Boolean.getBoolean("DEBUG");
	}
	
	public static void debug(String msg) {
		if (isDebugEnabled()) {
			info(msg);
		}
	}
	
	public static void info(String msg) {
		System.out.println(format(msg));
	}
	
	private static String format(String msg) {
		return FORMAT.format(new Date()) + "    [soap] " + msg;
	}
	
}
