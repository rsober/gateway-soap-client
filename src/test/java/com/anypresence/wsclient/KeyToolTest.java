package com.anypresence.wsclient;


import com.anypresence.wsclient.utils.KeyToolUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class KeyToolTest {

    @Test
    public void checkTestPemFileExists() throws Exception {
        File pemFile = new File(KeyToolTest.class.getResource("/foo.pem").toURI());
        Assert.assertTrue(pemFile.exists());
    }

    @Test
    public void addPem() throws URISyntaxException, IOException, InterruptedException {
        String path = KeyToolTest.class.getResource("/foo.pem").toURI().getPath().toString();

        String jksPath = KeyToolTest.class.getResource("/foo.jks").toURI().getPath().toString();

        Assert.assertTrue(Files.exists(Paths.get(path)));

        Assert.assertTrue(Files.exists(Paths.get(jksPath)));

        byte[] encoded = Files.readAllBytes(Paths.get(path));
        String contents = new String(encoded, StandardCharsets.UTF_8);

        KeyToolUtils.addPemCertToKeystore(jksPath, "password" /* password for fake keystore */, "foo_pem", contents);
        System.out.println(path);
    }

}
