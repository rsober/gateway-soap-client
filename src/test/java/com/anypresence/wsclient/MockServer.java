package com.anypresence.wsclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class MockServer implements Runnable {

	private String expectedRequest;
	private String cannedResponse;
	
	public MockServer(String expectedRequest, String cannedResponse) {
		this.expectedRequest = expectedRequest;
		this.cannedResponse = cannedResponse;
	}
	
	public void run() {
		try {
			runImpl();
		} catch (Throwable t) {
			if (t instanceof RuntimeException) {
				throw (RuntimeException)t;
			}
			throw new RuntimeException("Unexpected exception encountered", t);
		}
	}
	
	private void runImpl() throws Throwable {
		ServerSocket server = null;
		Socket sock = null;
		try {
			server = new ServerSocket();
			server.bind(new InetSocketAddress("localhost", 51311));
			sock = server.accept();
			
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			String line = null;
			int contentLength = 0;
			char[] content = new char[0];
			while((line = reader.readLine()) != null) {
				if (line.startsWith("Content-Length:")) {
					String token = line.split(":")[1].trim();
					contentLength = Integer.parseInt(token);
					System.out.println("Content length of : " + contentLength);
				}
				
				builder.append(line).append("\n");
				
				if (builder.toString().endsWith("\n\n")) {
					System.out.println("Attempting to read ...");
					content = new char[contentLength];
					reader.read(content);
					builder.append(new String(content));
					break;
				}
			}
			
			String actualRequest = new String(content);
			
			String exp = Utilities.prettyXml(expectedRequest);
			String act = Utilities.prettyXml(actualRequest);
			
			System.out.println("Expected:\n\n" + exp + "\n\nActual:\n\n" + act);
			
			if (!exp.trim().equals(act.trim())) {
				throw new RuntimeException("Actual request did not equal expected request");
			}
			
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			writer.write("HTTP/1.1 200 OK\n");
			writer.write("Content-Type: text/xml; charset=utf-8\n");
			writer.write("Content-Length: " + cannedResponse.length() + "\n\n");
			writer.write(Utilities.prettyXml(cannedResponse));
			writer.flush();
		} finally {
			try {
				if (sock != null) {
					sock.close();
				}
			} catch(IOException e) {
				
			}
			try {
				if (server != null) {
					server.close();
				}
			} catch (IOException e) {
				
			}
		}
		
	}
	
}
