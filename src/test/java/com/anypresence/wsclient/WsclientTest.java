package com.anypresence.wsclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

@RunWith(Parameterized.class)
public class WsclientTest {

	private static Thread wsclientThread;
	private static Map<String, File> jars = new HashMap<String, File>();
	private String testCase;
	
	@Parameters
	public static Iterable<? extends Object> data() {
		return Arrays.asList("chargepoint", "weather", "aeropost");
	}
	
	@BeforeClass
	public static void setup() throws IOException, URISyntaxException, InterruptedException {
		wsclientThread = new Thread(() -> {
			Wsclient.main(new String[] {"localhost", "19083", "10"});
		});
		
		wsclientThread.start();
		
		// set up the mock server
		List<Process> processes = new ArrayList<Process>();
		
		for (Object casename : data() ) {
			File f = File.createTempFile("wsclienttest", ".jar");
			String testCase = (String)casename;
			Process p = Runtime.getRuntime().exec("wsimport -extension -clientjar " + f.getAbsolutePath() + " " + WsclientTest.class.getResource("/wsdl/" + testCase + ".wsdl.xml").toURI().getPath());
			processes.add(p);
			jars.put(testCase, f);
		}
		
		for (Process proc : processes) {
			int i = proc.waitFor();
			if (i != 0) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				StringBuilder builder = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					builder.append(line).append("\n");
				}
				reader.close();
				System.out.println(builder.toString());
				
				reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				builder = new StringBuilder();
				line = null;
				while ((line = reader.readLine()) != null) {
					builder.append(line).append("\n");
				}
				reader.close();
				
				System.out.println(builder.toString());
				throw new RuntimeException("Received non-zero exit code from process");
			}
		}
	}
	
	@AfterClass
	public static void teardown() {
		wsclientThread.interrupt();
		
		for (File f : jars.values()) {
			f.delete();
		}
	}
	
	public WsclientTest(String testCase) {
		this.testCase = testCase;
	}
	
	
	/*@Test
	public void test() throws Exception {
		//fail("Not yet implemented");
		URL jsonRequests = getClass().getResource("/json_requests");
		URL jsonResponses = getClass().getResource("/json_responses");
		URL soapRequests = getClass().getResource("/soap_requests");
		URL soapResponses = getClass().getResource("/soap_responses");
		
		File jsonRequestsDir = new File(jsonRequests.toURI());
		File jsonResponsesDir = new File(jsonResponses.toURI());
		File soapRequestsDir = new File(soapRequests.toURI());
		File soapResponsesDir = new File(soapResponses.toURI());
		
		List<String> jsonRequestFiles = Arrays.asList(jsonRequestsDir.list());
		List<String> jsonResponseFiles = Arrays.asList(jsonResponsesDir.list());
		List<String> soapRequestFiles = Arrays.asList(soapRequestsDir.list());
		List<String> soapResponseFiles = Arrays.asList(soapResponsesDir.list());
		
		if (jsonRequestFiles.size() != jsonResponseFiles.size() || jsonRequestFiles.size() != soapRequestFiles.size() || jsonRequestFiles.size() != soapResponseFiles.size()) {
			Assert.fail("Each test case must contain a json request, a json response, a soap request, and a soap response");
		}
		
		List<String> cases = new ArrayList<String>();
		
		for (String file : jsonRequestFiles) {
			String currentCase = file.split("\\.")[0];
			cases.add(currentCase);
			Assert.assertTrue(jsonResponseFiles.contains(currentCase + ".json"));
			Assert.assertTrue(soapRequestFiles.contains(currentCase + ".xml"));
			Assert.assertTrue(soapResponseFiles.contains(currentCase + ".xml"));
			
			executeCase(currentCase);
		}
	}*/
	
	private String jsonRequest(String caseName) throws Exception {
		Map<String, Object> ctx = new HashMap<String, Object>();
		ctx.put("jarUrl", jars.get(this.testCase).toURI());
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
			for (Map.Entry<String, Object> entry:  ctx.entrySet()) {
				contents = contents.replaceAll("\\$\\{" + entry.getKey() + "\\}", entry.getValue().toString());
			}
		}
		return contents;
	}
	
	@Test
	public void executeCase() throws Exception{
		
		Thread mockServerThread = new Thread(new MockServer(soapRequest(testCase), soapResponse(testCase)));
		Socket sock = null;
		try {
			mockServerThread.start();
			
			sock = new Socket("localhost", 19083);
			String requestPayload = jsonRequest(testCase) + "\n\n";
			String expectedResponse = jsonResponse(testCase);
			
			sock.getOutputStream().write(requestPayload.getBytes());
			sock.getOutputStream().flush();
			
			BufferedReader r = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			String line = null;
			StringBuilder actualResponseBuilder = new StringBuilder();
			while ((line = r.readLine()) != null) {
				actualResponseBuilder.append(line).append("\n");
			}
			
			Gson gson = new Gson();
			JsonElement actual = gson.fromJson(actualResponseBuilder.toString(), JsonElement.class);
			JsonElement expected = gson.fromJson(expectedResponse, JsonElement.class);
			
			if (!expected.equals(actual)) {
				Assert.fail("Expected response and actual response do not match\n\nActual:\n\n'" + Utilities.prettyJson(actualResponseBuilder.toString()) + "'\n\nExpected:\n\n'" + Utilities.prettyJson(expectedResponse) + "'");
			}
		} finally {
			sock.close();
			mockServerThread.interrupt();
		}
		
	}

}
