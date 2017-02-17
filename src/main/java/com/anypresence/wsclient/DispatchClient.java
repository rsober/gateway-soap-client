package com.anypresence.wsclient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
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
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import com.anypresence.wsclient.utils.ParseUtils;
import com.anypresence.wsclient.utils.SecurityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.staxutils.StaxUtils;

import javax.xml.ws.handler.Handler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import java.util.List;
import java.util.*;

public class DispatchClient {
    static Logger log = LogManager.getLogger(DispatchClient.class.getName());

    private boolean useAuth;
    private String pemFile;
    private String alias;
    private String username;
    private String password;
    private Builder.DispatchType type;

    private DispatchClient(Builder builder) {
        this.useAuth = builder.useAuth;
        this.pemFile = builder.pemFile;
        this.alias = builder.alias;
        this.username = builder.username;
        this.password = builder.password;
        this.type = builder.type;
    }

    private static Dispatch<SOAPMessage> createDispatchMessage(String wsdl, QName qNameService, QName qNamePort) throws MalformedURLException, SOAPException {
        Pattern p = Pattern.compile("(file:[/]+)(.*)");
        Matcher m = p.matcher(wsdl);

        // Try to normalize the url
        if (m.find()) {
            wsdl = "file:///" + m.group(2);
        }

        URL wsdlURL = new URL(wsdl);

        log.debug("Dispatching: " + wsdlURL + ", " + qNameService.toString());

        Service service = Service.create(wsdlURL, qNameService);
        Dispatch<SOAPMessage> dispatch = service.createDispatch(qNamePort, SOAPMessage.class, Service.Mode.MESSAGE);

        BindingProvider bp = (BindingProvider) dispatch;
        MessageFactory factory = ((SOAPBinding) bp.getBinding()).getMessageFactory();
        SOAPMessage request = factory.createMessage();
        SOAPHeader header = request.getSOAPHeader();
        SOAPBody body = request.getSOAPBody();

        SOAPPart part = request.getSOAPPart();
        SOAPEnvelope env = part.getEnvelope();


        return dispatch;
    }

    /**
     * Creates a dispatch object.
     *
     * Details can be found: http://docs.oracle.com/cd/E21764_01/web.1111/e13734/provider.htm#WSADV562
     *
     * @param wsdl
     * @param qNameService
     * @param qNamePort
     * @return
     * @throws MalformedURLException
     */
    private static Dispatch<Source> createDispatch(String wsdl, QName qNameService, QName qNamePort) throws MalformedURLException {
        Pattern p = Pattern.compile("(file:[/]+)(.*)");
        Matcher m = p.matcher(wsdl);

        // Try to normalize the url
        if (m.find()) {
            wsdl = "file:///" + m.group(2);
        }

        URL wsdlURL = new URL(wsdl);

        log.debug("Dispatching: " + wsdlURL + ", " + qNameService.toString());

        Service service = Service.create(wsdlURL, qNameService);

        Dispatch<Source> disp = service.createDispatch(qNamePort, Source.class, Service.Mode.MESSAGE);

        return disp;
    }

    /**
     *
     * @param wsdl
     * @param qNameService
     * @param qNamePort
     * @param payload
     * @return
     * @throws MalformedURLException
     */
    public String processRequest(String wsdl, QName qNameService, QName qNamePort, String payload) throws MalformedURLException {
        return processRequest(wsdl, qNameService, qNamePort, payload, "");
    }

    /**
     * Process the request.
     *
     * @param wsdl
     * @param qNameService
     * @param qNamePort
     * @param payload
     * @return
     * @throws MalformedURLException
     */
    public String processRequest(String wsdl, QName qNameService, QName qNamePort, String payload, String soapAction) throws MalformedURLException {
        payload = "<?xml version=\"1.0\"?>" + payload;
        if (type == Builder.DispatchType.SOURCE) {
            Dispatch<Source> disp = createDispatch(wsdl, qNameService, qNamePort);
            if (useAuth) {
                // Apply auth
                SecurityUtils.applyEncryptionProperties(disp, alias, username, password);
            }

            Source request = new StreamSource(stringToStream(payload));
            if (soapAction != null && !soapAction.isEmpty()) {
                disp.getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, true);
                disp.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, soapAction);
            }
            Source response = disp.invoke(request);

            try {
                return parseDom(response);
            } catch (TransformerFactoryConfigurationError | TransformerException e) {
                e.printStackTrace();
            }

        }

        return "";
    }

    private static InputStream stringToStream(String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses the dom.
     *
     * @param source
     * @return
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     */
    private static String parseDom(Source source) throws TransformerFactoryConfigurationError, TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StreamResult result = new StreamResult(new StringWriter());
        transformer.transform(source, result);

        return result.getWriter().toString();
    }


    public static class Builder {
        private boolean useAuth;
        private String pemFile;
        private String alias;
        private String username;
        private String password;
        private DispatchType type = DispatchType.SOURCE;

        enum DispatchType {
            SOURCE,
            MESSAGE
        }

        Builder() {}

        public Builder useAuth(boolean useAuth) {
            this.useAuth = useAuth;
            return this;
        }

        public Builder type(DispatchType type) {
            this.type = type;
            return this;
        }

        public Builder pemFile(String pemFile) {
            this.pemFile = pemFile;
            return this;
        }

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String username) {
            this.password = password;
            return this;
        }

        public DispatchClient create() {
            return new DispatchClient(this);
        }

    }
}