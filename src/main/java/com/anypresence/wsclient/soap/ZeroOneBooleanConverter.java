package com.anypresence.wsclient.soap;

import javax.xml.bind.DatatypeConverter;

public class ZeroOneBooleanConverter 
{

	public static Boolean parseZeroOrOneAsBoolean(String value) {
		if (value == null) {
			return null;
		}
		return DatatypeConverter.parseBoolean(value);
	}
	
	public static String printBooleanAsZeroOrOne(Boolean value) {
		if (value == null) {
			return null;
		}
		return value ? "1" : "0";
	}
	
}