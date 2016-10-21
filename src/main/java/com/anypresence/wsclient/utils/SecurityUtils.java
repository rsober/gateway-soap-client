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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.SOAPConstants;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.common.CustomHandler;
import org.apache.wss4j.dom.common.KeystoreCallbackHandler;
import org.apache.wss4j.dom.common.SOAPUtil;
import org.apache.wss4j.dom.common.SecretKeyCallbackHandler;
import org.apache.wss4j.dom.common.SecurityTestUtil;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.HandlerAction;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.str.STRParser.REFERENCE_TYPE;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import org.w3c.dom.Document;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;

import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;

import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

public class SecurityUtils {
    private static final String WSSE_NS   = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String WSU_NS    = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
	private static final String PROP_FILE = "Client_Encrypt.properties";

	public static Set<QName> getHeaders() {
		QName securityHeader = new QName(
				"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security");
		Set<QName> headers = new HashSet<QName>();
		headers.add(securityHeader);
		return headers;
	}

	public static void getDispatch(String sourceFile, QName qNameService, QName qNamePort, String wsdl, String ns, String operationName) throws MalformedURLException {
        URL wsdlURL = new URL(wsdl);
        Service service = Service.create(wsdlURL, qNameService);
        Dispatch<Source> disp = service.createDispatch(qNamePort, Source.class, Service.Mode.MESSAGE);
        disp.getRequestContext().put(MessageContext.WSDL_OPERATION, new QName(ns, operationName));
        return disp;
    }

    public static void changeEndpoint(Dispatch<Source> disp, String endpoint) {
    	disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
    }

    public static void applyEncryptionProperties(Dispatch<Source> disp, String alias, String username, String password) {
    	disp.getRequestContext().put("ws-security.encryption.properties", PROP_FILE);
    	// The certificate in the keystore defined in Client_Encrypt.properties
	    disp.getRequestContext().put("ws-security.encryption.username", alias);	    
	    disp.getRequestContext().put("ws-security.username", username);	    
	    disp.getRequestContext().put("ws-security.password", password);
    }

    public static void signHeader(String userInfo) {
    	WSSecEncrypt builder = new WSSecEncrypt();
        builder.setUserInfo(userInfo);
        builder.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
        builder.setSymmetricEncAlgorithm(WSConstants.TRIPLE_DES);
        Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
        WSSecHeader secHeader = new WSSecHeader(doc);
        secHeader.insertSecurityHeader();
        Document encryptedDoc = builder.build(doc, crypto, secHeader);

        String outputString = XMLUtils.prettyDocumentToString(encryptedDoc);
        return outputString;
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