package com.anypresence.wsclient.soap;


import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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

public class SecurityHandler implements SOAPHandler<SOAPMessageContext> {

	private String username;
	private String password;

	public SecurityHandler(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public Set<QName> getHeaders() {
		QName securityHeader = new QName(
				"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security");
		Set<QName> headers = new HashSet<QName>();
		headers.add(securityHeader);
		return headers;
	}

	public boolean handleMessage(SOAPMessageContext messageContext) {
		Boolean outboundProperty = (Boolean) messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		if (outboundProperty.booleanValue()) {
			Log.info("Adding security credentials to outbound request via SecurityHandler");

			Log.debug("Sending the following request to SOAP service:");
			try {
				SOAPMessage message = messageContext.getMessage();
				SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
				SOAPHeader header = envelope.getHeader();

				SOAPElement security = header.addChildElement("Security", "wsse",
						"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
				security.addAttribute(
						new QName("http://schemas.xmlsoap.org/soap/envelope/", "mustUnderstand", "SOAP-ENV"), "1");

				SOAPElement usernameToken = security.addChildElement("UsernameToken", "wsse");
				usernameToken.addAttribute(new QName("xmlns:wsu"),
						"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");

				SOAPElement username = usernameToken.addChildElement("Username", "wsse");
				username.addTextNode(this.username);

				SOAPElement password = usernameToken.addChildElement("Password", "wsse");
				password.addAttribute(new QName("Type"),
						"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
				password.addTextNode(this.password);

				if (Log.isDebugEnabled()) {
					messageContext.getMessage().writeTo(System.out);
					System.out.println("\n");
				}
			} catch (SOAPException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Log.debug("Received the following response from SOAP service:");
			try {
				if (Log.isDebugEnabled()) {
					messageContext.getMessage().writeTo(System.out);
					System.out.println("\n");
				}
			} catch (SOAPException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	public boolean handleFault(SOAPMessageContext messageContext) {
		Log.debug("Received SOAPFault");
		return true;
	}

	public void close(MessageContext messageContext) {
		//
	}
}
