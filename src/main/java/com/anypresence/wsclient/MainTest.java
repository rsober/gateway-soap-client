package com.anypresence.wsclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MainTest {

	public static void main(String[] args) throws IOException {
		//testChargepoint();
		testWeather();
	}
	
	@SuppressWarnings(value="unused")
	private static void testWeather() throws IOException {
		makeReauest("{ \"serviceName\": \"Weather\", \"endpointName\": \"WeatherSoap\", \"operationName\": \"GetWeatherInformation\", \"params\": null, \"jarUrl\": \"file:///Users/rsnyder/platform/gateway/tmp/weather.jar\" }\n\n");
		//makeReauest("{ \"serviceName\": \"Weather\", \"endpointName\": \"WeatherSoap\", \"operationName\": \"GetCityForecastByZIP\", \"params\": { \"zip\": \"22180\" }, \"jarUrl\": \"file:///Users/rsnyder/platform/gateway/tmp/weather.jar\" }\n\n");
	}
	
	@SuppressWarnings(value="unused")
	private static void testChargepoint() throws IOException{
		makeReauest("{ \"serviceName\": \"chargepointservices\", \"endpointName\": \"chargepointservicesSOAP\", \"actionName\": \"urn:provider/interface/chargepointservices/getStations\", \"params\": { \"searchQuery\": { \"orgID\": \"1:ORG08313\" } }, \"jarUrl\": \"file:///Users/rsnyder/platform/gateway/tmp/myJar.jar\", \"wssePasswordCredentials\": { \"username\": \"e39e34c98def1db64229f0554306133b53fd057661b271409090934\", \"password\": \"217eb8c9c92b91c52f25c9354cc773e2\"} }\n\n");
	}
	
	private static void makeReauest(String requestPayload) throws IOException {
		System.out.println("Sending: \n\n" + requestPayload);
		Socket sock = new Socket("localhost", 19083);
		sock.getOutputStream().write(requestPayload.getBytes());
		sock.getOutputStream().flush();
		
		String responseLine = null;
		StringBuilder responseBuilder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		while ((responseLine = reader.readLine()) != null) {
			responseBuilder.append(responseLine);
		}
		reader.close();
		sock.close();
		
		System.out.println("Received: \n\n" + responseBuilder.toString().trim());
	}
	
}
