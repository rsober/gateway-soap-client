package com.anypresence.wsclient.utils;

import com.anypresence.wsclient.CxfWorker;
import com.anypresence.wsclient.SoapClientException;
import com.predic8.wsdl.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utility class to work with the {@link com.predic8.wsdl.Definitions}.
 *
 *
 */
public class MembraneUtils {
    static Logger log = LogManager.getLogger(MembraneUtils.class.getName());

    /**
     * Parses the wsdl into a Definitions object.
     *
     * @see com.predic8.wsdl.Definitions
     *
     * @param wsdlContent
     * @return the Definitions object
     */
    public static Definitions parseWsdl(String wsdlContent) {
        WSDLParser parser = new WSDLParser();
        InputStream stream = new ByteArrayInputStream(wsdlContent.getBytes(StandardCharsets.UTF_8));
        Definitions defs = parser.parse(stream);

        return defs;
    }

    public static String getOperationProperty(Definitions defs, String operationName, String portName, String property) throws SoapClientException {
        if (defs.getBindings().isEmpty()) {
            throw new SoapClientException("There's no binding");
        }

        try {
            Operation operation = defs.getOperation(operationName, portName);

            if (operation == null) {
                return "";
            }

            Binding b = findFirstBinding(defs);
            Object prop = b.getOperation(operationName).getOperation().getSoapAction();

            return prop.toString();
        } catch (NullPointerException e) {
            // Did not want to do this, but an NPE gets thrown from the underlying groovy code
            // if it can't find the operation.
            return "";
        }

    }

    public static Binding findFirstBinding(Definitions defs) throws SoapClientException {
        if (defs.getBindings().isEmpty()) {
            throw new SoapClientException("There's no binding");
        }
        return defs.getBindings().get(0);
    }

    /**
     * Finds first port for the specified binding and service.
     *
     * @param service
     * @param binding
     * @return the port
     */
    public static Port portForBinding(Service service, String binding) {

        for (Port p: service.getPorts()) {

            log.debug("Searching for port; " + binding + " : " + p.getName());
            if (p.getBinding().getName().equals(binding)) {
                return p;
            }
        }

        return null;
    }

    /**
     * Finds the service by name.
     *
     * @param defs the definitions
     * @param service the service name
     * @return the service or null if not found
     */
    public static Service serviceByName(Definitions defs, String service) {
        for (Service s : defs.getServices()) {
            if (s.getName().equals(service)) {
                return s;
            }
        }

        return null;
    }

}
