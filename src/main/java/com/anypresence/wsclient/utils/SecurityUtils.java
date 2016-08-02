package com.anypresence.wsclient.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.anypresence.wsclient.Log;

public class SecurityUtils {

	public static Set<QName> getHeaders() {
		QName securityHeader = new QName(
				"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security");
		Set<QName> headers = new HashSet<QName>();
		headers.add(securityHeader);
		return headers;
	}

	private static String generateNonce()
			throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
		String dateTimeString = Long.toString(new Date().getTime());
		byte[] nonceByte = dateTimeString.getBytes();
		return Base64.getEncoder().encodeToString(nonceByte);
	}

	private static String timestamp() {
		// This is used to get time in SOAP request in yyyy-MM-dd'T'HH:mm:ss.SSS'Z' format
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

		Date timestamp = new java.util.Date();

		return formatter.format(timestamp);
	}

}