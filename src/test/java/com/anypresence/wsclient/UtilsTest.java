package com.anypresence.wsclient;


import com.anypresence.wsclient.utils.MembraneUtils;
import com.predic8.wsdl.Binding;
import com.predic8.wsdl.Definitions;
import com.predic8.wsdl.Service;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;

public class UtilsTest {

    private static Definitions getDef() throws URISyntaxException, IOException {
        File f = new File(UtilsTest.class.getResource("/wsdl/aig.wsdl").toURI());
        String contents = new String(Files.readAllBytes(f.toPath()));

        return MembraneUtils.parseWsdl(contents);
    }

    @Test
    public void badServiceShouldFail() throws Exception {
        Definitions def = getDef();

        Assert.assertNotNull(def);

        Binding binding = MembraneUtils.findFirstBinding(def);

        Assert.assertNotNull(binding);

        Service service = MembraneUtils.serviceByName(def, "fake");

        Assert.assertNull(service);
    }

    @Test
    public void goodServiceShouldSucceed() throws Exception {
        Definitions def = getDef();

        Assert.assertNotNull(def);

        Binding binding = MembraneUtils.findFirstBinding(def);

        Assert.assertNotNull(binding);

        Service service = MembraneUtils.serviceByName(def, "UserProfileService");

        Assert.assertNotNull(service);
    }

}
