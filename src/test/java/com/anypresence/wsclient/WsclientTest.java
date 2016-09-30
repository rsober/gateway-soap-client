package com.anypresence.wsclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.*;

import com.anypresence.wsclient.utils.ParseUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.JsonElement;
import org.mockserver.integration.ClientAndProxy;
import org.mockserver.integration.ClientAndServer;
import org.w3c.dom.Document;

import static org.mockserver.integration.ClientAndProxy.startClientAndProxy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@RunWith(Parameterized.class)
public class WsclientTest {
    static Logger log = LogManager.getLogger(WsclientTest.class.getName());

    private static final Map<String, String> ENDPOINTS = new HashMap<>();

    {
        ENDPOINTS.put("brook-brothers", "/CWDirectCPService/services/CWOrderIn");
        ENDPOINTS.put("chargepoint", "/webservices/chargepoint/services/4.1");
        ENDPOINTS.put("crc", "/meaweb/services/OxyExt_CRC_AMOVE_MPOC");
        ENDPOINTS.put("stock", "/stockquote.asmx");
        ENDPOINTS.put("weather", "/WeatherWS/Weather.asmx");
        ENDPOINTS.put("aig_2", "/_vti_bin/UserProfileService.asmx");
        ENDPOINTS.put("aig", "/_vti_bin/UserProfileService.asmx");
        ENDPOINTS.put("aeropost", "/Devel/WS_MyAero/Services.svc");
    }

    private static Thread wsclientThread;
    private static Map<String, File> jars = new HashMap<String, File>();
    private String testCase;

    private ClientAndProxy proxy;
    private ClientAndServer mockServer;

    @Parameters
    public static Iterable<? extends Object> data() {
        URL uri = WsclientTest.class.getResource("/wsdl");
        List<String> testNames = new ArrayList<String>();
        try {
            File wsdlDir = new File(uri.toURI());
            for (File f : wsdlDir.listFiles()) {
                if (f.isFile()) {
                    String filename = f.getName();
                    if (filename.endsWith("wsdl")) {
                        testNames.add(filename.split("\\.")[0]);
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to get list of test cases due to URISyntaxException", e);
        }
        return testNames;
    }

    private static void listAllHelper(File file, List<String> results) {
        if (file.isDirectory()) {
            if (file.listFiles() == null) {
                return;
            }
            for (File f : file.listFiles()) {
                if (f.getName().endsWith(".java")) {
                    results.add(f.getAbsolutePath());
                }
                listAllHelper(f, results);
            }
        }
    }

    private static List<String> listAll(File directory) {
        List<String> files = new ArrayList<String>();
        listAllHelper(directory, files);
        return files;
    }

    private static void debugCmd(String... args) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(args).inheritIO();
        Process p = builder.start();
        p.waitFor();
    }

    @BeforeClass
    public static void setup() {
        wsclientThread = new Thread() {
            public void run() {
                Wsclient.main(new String[]{"localhost", "19085", "10"});
            }
        };

        wsclientThread.start();
        ;
    }

    @AfterClass
    public static void teardown() {
        wsclientThread.interrupt();
    }

    @Before
    public void startProxy() {
        mockServer = startClientAndServer(1080);
        proxy = startClientAndProxy(1090);
    }

    @After
    public void stopProxy() {
        proxy.stop();
        mockServer.stop();
    }

    public WsclientTest(String testCase) {
        this.testCase = testCase;
    }

    private String jsonRequest(String caseName) throws Exception {
        Map<String, Object> ctx = new HashMap<String, Object>();
        URL uri = WsclientTest.class.getResource("/wsdl/" + caseName + ".wsdl");
        
        ctx.put("jarUrl", uri.toString());
        return Utilities.prettyJson(resourceAsString(caseName, "json_requests", "json", ctx));
    }

    private String jsonResponse(String caseName) throws Exception {
        return Utilities.prettyJson(resourceAsString(caseName, "json_responses", "json", null));
    }

    private String soapRequest(String caseName) throws Exception {
        return Utilities.prettyXml(resourceAsString(caseName, "soap_requests", "xml", null));
    }

    private String soapResponse(String caseName) throws Exception {
        return Utilities.prettyXml(resourceAsString(caseName, "soap_responses", "xml", null));
    }

    private String resourceAsString(String caseName, String folderName, String extension, Map<String, Object> ctx) throws Exception {
        File f = new File(getClass().getResource("/" + folderName + "/" + caseName + "." + extension).toURI());
        String contents = new String(Files.readAllBytes(f.toPath()));
        if (ctx != null) {
            for (Map.Entry<String, Object> entry : ctx.entrySet()) {
                contents = contents.replaceAll("\\$\\{" + entry.getKey() + "\\}", entry.getValue().toString());
            }
        }
        return contents;
    }


    @Test
    public void executeCase() {
        Socket sock = null;

        try {
            mockServer.when(request().withMethod("POST")
                    .withPath(ENDPOINTS.get(testCase))).respond(response()
                    .withBody(soapResponse(testCase))
                    .withStatusCode(200));

            sock = new Socket("localhost", 19085);
            String requestPayload = jsonRequest(testCase) + "\n\n";

            sock.getOutputStream().write(requestPayload.getBytes());
            sock.getOutputStream().flush();

            BufferedReader r = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String line = null;
            StringBuilder actualResponseBuilder = new StringBuilder();
            while ((line = r.readLine()) != null) {
                actualResponseBuilder.append(line).append("\n");
            }

            Document expected = Utilities.stringToDocument(soapResponse(testCase));
            expected.normalizeDocument();

            String actualAsString = actualResponseBuilder.toString();
            actualAsString = actualAsString.trim().replaceAll("\n ", "");

            String expectedAsString = ParseUtils.xmlToJson(Utilities.docToString(expected));
            expectedAsString = expectedAsString.trim().replaceAll("\n ", "");
            
            if (!Utilities.compareJsonAsMaps(actualAsString, expectedAsString)) {
                Assert.fail("Expected response and actual response do not match\n\nActual:\n\n'" + actualAsString + "'\n\nExpected:\n\n'" + expectedAsString + "'");
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (IOException e) {
            e.printStackTrace();
            if (e.getCause() != null)
                e.getCause().printStackTrace();
            Assert.fail("Unable to run IO: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Unable to run: " + e.getMessage());
        } finally {
            try {
                if (sock != null) {
                    sock.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

}
