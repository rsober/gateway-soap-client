package com.anypresence.wsclient;

import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.anypresence.wsclient.dto.OperationRequest;
import com.anypresence.wsclient.gson.*;
import com.anypresence.wsclient.utils.MembraneUtils;
import com.anypresence.wsclient.utils.ParseUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.predic8.wsdl.Definitions;
import com.predic8.wsdl.Port;
import com.predic8.wsdl.Service;
import com.predic8.wsdl.WSDLParser;
import com.predic8.wstool.creator.RequestCreator;
import com.predic8.wstool.creator.SOARequestCreator;
import groovy.xml.MarkupBuilder;
import org.apache.logging.log4j.*;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFault;

public class CxfWorker implements Runnable {
    private Socket sock;

    private static Path certTempDir = null;

    static Logger log = LogManager.getLogger(CxfWorker.class.getName());

    static Gson gson = new GsonBuilder().registerTypeAdapter(SOAPFault.class, new SoapFaultSerializer())
            .registerTypeAdapter(JAXBElement.class, new JaxbTypeAdapter())
            .registerTypeAdapter(XMLGregorianCalendar.class, new XMLGregorianCalendarTypeAdapter())
            .registerTypeHierarchyAdapter(Enum.class, new EnumTypeAdapter())
            .registerTypeHierarchyAdapter(Node.class, new GenericXmlSerializer())
            .setPrettyPrinting()
            .create();

    public CxfWorker(Socket sock) {
        this.sock = sock;
    }

    public static final Path getCertTempDir() throws IOException {
        if (certTempDir != null) {
            return certTempDir;
        }

        certTempDir = Files.createTempDirectory("cert");
        // deletes file when the virtual machine terminate
        certTempDir.toFile().deleteOnExit();

        System.out.println("Temporary directory created: " + certTempDir.toString());

        return certTempDir;
    }

    @Override
    public void run() {
        StringBuilder builder = new StringBuilder("");

        withSocket(sock, () -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                String line = null;

                // Read in the payload
                while((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                    if (builder.toString().endsWith("\n\n")) {
                        break;
                    }
                }

                String payload = builder.toString().trim();

                // Process the request
                OperationRequest operationRequest = gson.fromJson(payload, OperationRequest.class);

                log.debug("Converted json: " + operationRequest.toString());

                String action = operationRequest.getActionName();
                if (action == null || action.isEmpty()) {
                    action = operationRequest.getOperationName();
                }
                String service = operationRequest.getServiceName();

                String wsdlUrl = operationRequest.getWsdl();
                String response = "";
                try {
                    WSDLParser parser = new WSDLParser();

                    Definitions defs = ParseUtils.definitionsFromUrl(wsdlUrl);

                    StringWriter writer = new StringWriter();

                    HashMap<String, String> formParams = new HashMap<String, String>();

                    SOARequestCreator creator = new SOARequestCreator(defs, new RequestCreator(), new MarkupBuilder(writer));

                    JsonElement rawParams = operationRequest.getParams();

                    if (ParseUtils.isValidJson(rawParams.toString())) {
                        ParseUtils.injectParametersIntoXml(formParams, action, rawParams, true);
                    }
                    if (!formParams.isEmpty()) {
                        for (Map.Entry<String,String> f : formParams.entrySet()) {
                            log.debug("form: (" + f.getKey() + ", " + f.getValue() + ")");
                        }
                        creator.setFormParams(formParams);
                    }

                    String binding =  MembraneUtils.findFirstBinding(defs).getName();
                    log.debug("wsdl: " + wsdlUrl + ", service: " + service + ", action: " + action + ", binding: " + binding);
                    // The first parameter is actually not needed...
                    creator.createRequest(service, action, binding);

                    // Finally have the request envelope
                    String requestEnvelope = writer.toString();

                    log.debug("Envelope looks like: " + requestEnvelope);

                    response = executeWithRequest(defs, service, binding, requestEnvelope, gson, payload);
                } catch (SoapClientException e) {
                    Log.error("Unable to execute...", e);
                }catch(Exception e) {
                    Log.error("Runtime error..." + e.getMessage(), e);
                }

                log.debug("Writing response to the socket");
                log.debug("Raw Response: " + response);
                try(BufferedWriter responseWriter = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {
                    responseWriter.write(ParseUtils.xmlToJson(response));
                } catch(IOException e) {
                    Log.info("Unable to fully write response due to IOException: " + e.getMessage());
                    Log.error(e);
                }
            } catch(IOException e) {
                Log.info("Unable to fully write response due to IOException: " + e.getMessage());
                Log.error(e);
            } catch(JsonSyntaxException e) {
                log.error(e.getMessage(), e);

                Log.info(e.getMessage());
            }

        });

    }

    /**
     * Executes the request.
     *
     * @param requestEnvelope
     * @param gson
     * @param payload
     * @return the response as a string
     */
    private String executeWithRequest(Definitions defs, String service, String binding, String requestEnvelope, Gson gson, String payload) {
        OperationRequest req = gson.fromJson(payload,OperationRequest.class);

        log.debug("Executing request: service: " + service + ", defs: " + defs.toString());
        Service s = MembraneUtils.serviceByName(defs, service);
        QName qService = new QName(s.getNamespaceUri(), s.getName());
        Port b = MembraneUtils.portForBinding(s, binding);
        QName qPort = new QName(b.getNamespaceUri(), b.getName());

        try {
            URI u = new URI(req.getWsdl());
            String wsdlUrl = u.toURL().toString();

            DispatchClient.Builder builder = new DispatchClient.Builder();

            if (req.getKey() != null && !req.getKey().isEmpty()) {
                builder.useAuth(true);
                builder.username(req.getWssePasswordCredentials().getUsername());
                builder.username(req.getWssePasswordCredentials().getPassword());
                builder.alias(req.getKeyAlias());
            }

            String response = builder.create().processRequest("file://" + u.toURL().getPath(), qService, qPort, requestEnvelope);
            Log.info("Response: " + response);

            return response;
        } catch (MalformedURLException e) {
            Log.error("Unable to get response: " + e.getMessage(), e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void withSocket(Socket sock, Runnable r) {
        try {
            r.run();
        } finally {
            if (sock != null) {
                try {
                    sock.close();
                } catch(IOException e) {
                    // Ignore
                }
            }
        }
    }



}
