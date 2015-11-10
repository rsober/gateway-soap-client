package com.anypresence.wsclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
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
		URL uri = WsclientTest.class.getResource("/wsdl");
		List<String> testNames = new ArrayList<String>();
		try {
			File wsdlDir = new File(uri.toURI());
			for (File f : wsdlDir.listFiles()) {
				if (f.isFile()) {
					String filename = f.getName();
					testNames.add(filename.split("\\.")[0]);
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
	public static void setup() throws IOException, URISyntaxException, InterruptedException {
		wsclientThread = new Thread(() -> {
			Wsclient.main(new String[] {"localhost", "19083", "10"});
		});
		
		wsclientThread.start();
		
		// set up the mock server
		Map<String, Process> processes = new HashMap<String, Process>();
		
		for (Object casename : data() ) {
			File f = File.createTempFile("wsclienttest", ".jar");
			File d = Files.createTempDirectory("wsimport").toFile();
			String testCase = (String)casename;
			String cmd = "wsimport -classpath bin -p temp -extension -d " + d.getAbsolutePath()  + " -Xnocompile " + WsclientTest.class.getResource("/wsdl/" + testCase + ".wsdl.xml").toURI().getPath();
			Process wsimportProcess = Runtime.getRuntime().exec(cmd);
			int exitCode = wsimportProcess.waitFor();
			if (exitCode == 0) {
				List<String> javaFiles = listAll(d);
				String javacCmdClasspath = System.getProperty("javac_cmd_classpath");
				if (javacCmdClasspath == null) { 
					throw new RuntimeException("System property javac_cmd_classpath ought to be set");
				}
				String javacCmd = "javac -cp " + javacCmdClasspath + " "  + String.join(" ", javaFiles);
				
				String[] javaFilesArray = javaFiles.toArray(new String[javaFiles.size()]);
				List<String> args = new ArrayList<String>();
				args.add("javac");
				args.add("-cp");
				args.add(javacCmdClasspath);
				args.addAll(Arrays.asList(javaFilesArray));
				ProcessBuilder builder = new ProcessBuilder(args).inheritIO();
				Process javacProcess = builder.start();
				int javacExitCode = javacProcess.waitFor();
				if (javacExitCode == 0) {
					String jarCmd = "jar -cvf " + f.getAbsolutePath() + " -C " + d.getAbsolutePath() + " .";
					Process jarProcess = Runtime.getRuntime().exec(jarCmd);
					processes.put(jarCmd, jarProcess);
					jarProcess.waitFor();
					debugCmd("jar", "tvf", f.getAbsolutePath());
					jars.put(testCase, f);
				} else {
					
					throw new RuntimeException("Unexpected exit code from javac: " + javacExitCode);
				}
			} else {
				throw new RuntimeException("Unexpected exit code from wsimport: " + exitCode);
			}
		}
		
		for (Map.Entry<String, Process> processEntry : processes.entrySet()) {
			String cmd = processEntry.getKey();
			Process proc = processEntry.getValue();
			int i = proc.waitFor();
			if (i != 0) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				StringBuilder builder = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					builder.append(line).append("\n");
				}
				reader.close();
				
				reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				builder = new StringBuilder();
				line = null;
				while ((line = reader.readLine()) != null) {
					builder.append(line).append("\n");
				}
				reader.close();
				
				throw new RuntimeException("Received non-zero exit code from command: " + cmd);
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
		
		MockServer ms = new MockServer(soapRequest(testCase), soapResponse(testCase));
		Thread mockServerThread = new Thread(ms);
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
			ms.close();
			mockServerThread.interrupt();
		}
		
	}

}
