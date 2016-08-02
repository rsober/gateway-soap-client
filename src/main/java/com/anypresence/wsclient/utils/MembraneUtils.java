package com.anypresence.wsclient.utils;

import com.anypresence.wsclient.CxfWorker;
import com.anypresence.wsclient.SoapClientException;
import com.predic8.wsdl.Binding;
import com.predic8.wsdl.Definitions;
import com.predic8.wsdl.Port;
import com.predic8.wsdl.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class MembraneUtils {
    static Logger log = LogManager.getLogger(MembraneUtils.class.getName());

    public static Binding findFirstBinding(Definitions defs) throws SoapClientException {
        if (defs.getBindings().isEmpty()) {
            throw new SoapClientException("There's no binding");
        }
        return defs.getBindings().get(0);
    }

    public static Port portForBinding(Service service, String binding) {
        for (Port p: service.getPorts()) {

            log.debug("Searching for port; " + binding + " : " + p.getName());
            if (p.getBinding().getName().equals(binding)) {
                return p;
            }
        }

        return null;
    }


    public static Service serviceByName(Definitions defs, String service) {
        for (Service s : defs.getServices()) {
            if (s.getName().equals(service)) {
                return s;
            }
        }

        return null;
    }

}
