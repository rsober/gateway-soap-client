package com.anypresence.wsclient.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import com.anypresence.wsclient.CxfWorker;
import com.anypresence.wsclient.Log;
import java.io.IOException;
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


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
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

//import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;

import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;


public class KeyToolUtils {
	static Logger log = LogManager.getLogger(KeyToolUtils.class.getName());

	private static String PEM_FILE_NAME= "certificate.pem";
	private static String DER_FILE_NAME= "certificate.der";

	public static void addPemCertToKeystore(String pathToKeystore, String alias, String pemCert) throws IOException {
		// Work in a temp directory
		Path tmp = Files.createTempDirectory("cert");

		log.info("Temporary directory created: " + tmp.toString());

		// Files.newBufferedWriter() uses UTF-8 encoding by default
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(tmp.toString() + "/" + PEM_FILE_NAME))) {
			writer.write(pemCert);
		} // the file will be automatically closed

		// Convert pem to der and import into the keystore
		String[] cmd = {
				"openssl",
				"x509",
				"-outform",
				"der",
				"-in",
				PEM_FILE_NAME,
				"-out",
				DER_FILE_NAME
		};
		Runtime.getRuntime().exec(cmd);

		String cmdImport[] = {
				"keytool",
				"-import",
				"-alias",
				alias,
				"-keystore",
				pathToKeystore,
				"-file",
				DER_FILE_NAME

		};
		Runtime.getRuntime().exec(cmdImport);

	}
	
}