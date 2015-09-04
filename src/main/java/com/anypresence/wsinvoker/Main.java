package com.anypresence.wsinvoker;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.jws.WebMethod;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.handler.Handler;

import com.google.gson.Gson;

public class Main {

	public static void main(String[] args) {
		
		String host = null;
		Integer port = null;
		
		if (args.length == 2) {
			host = args[0];
			port = Integer.parseInt(args[1]);
		} else {
			host = "localhost";
			port = 19083;
		}
		
		ServerSocket server = null;
		try {
			server = new ServerSocket();
			server.bind(new InetSocketAddress(host, port));
			while(true) {
				Socket sock = server.accept();
				//handleConnection(sock);
				new Thread(new Worker(sock)).run();
			}
		} catch(IOException e) {
			// TODO
		}
		
		
	}
	
	private static RequestHandler getRequestResponseHandler(ClassLoader loader, Gson gson, Method endpointMethod, Object endpoint) {
		if (endpointMethod.getReturnType() == Void.TYPE) {
			return new VoidRequestHandler(loader, gson, endpointMethod, endpoint);
		} else {
			return new DefaultRequestHandler(loader, gson, endpointMethod, endpoint);
		}
		
	}
	
	private static class Worker implements Runnable {

		private Socket sock;
		
		public Worker(Socket sock) {
			this.sock = sock;
		}
		
		@Override
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				String line = null;
				StringBuilder builder = new StringBuilder("");
				while((line = reader.readLine()) != null) {
					builder.append(line).append("\n");
					if (builder.toString().endsWith("\n\n")) {
						break;
					}
				}
				String payload = builder.toString().trim();
				
				String response = processRequestPayload(payload);
				
				if (response == null) {
					// TODO ?
					System.out.println("Error - TODO");
				} else {
					System.out.println("Writing");
					//sock.getOutputStream().write(response.getBytes());
					//sock.getOutputStream().flush();
					//sock.close();
					//sock.getOutputStream().close();
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
					writer.write(response);
					writer.close();
					//sock.close();
					System.out.println("Done");
				}
			} catch(Exception e) {
				// TODO
				e.printStackTrace();
			} finally {
				if (sock != null) {
					try {
						sock.close();
					} catch(IOException e) {
						
					}
				}
			}
			
		}
		
		private String processRequestPayload(String payload) throws Exception {
			System.out.println("payload: " + payload);
			
			// Deserialize the JSON
			Gson gson = new Gson();
			
			OperationRequest req = gson.fromJson(payload,OperationRequest.class);
			
			URLClassLoader child = new URLClassLoader(new URL[] { new URI(req.getJarUrl()).toURL() }, this.getClass().getClassLoader());
			
			WssePasswordSecurityCredentials creds = req.getWssePasswordCredentials();
			SecurityHandler handler = null;
			if (creds != null) {
				System.out.println("Creds::: " + creds);
				handler = new SecurityHandler(creds.getUsername(), creds.getPassword());
			}
			System.out.println("Received request: " + req);
			
			// Crack open the jar file and look for the service to instantiate
			JarFile file = new JarFile(new File(new URI(req.getJarUrl())));
			
			Enumeration<JarEntry> enumerator = file.entries();
			
			Class<?> serviceClass = null;
			
			outer:
			while (enumerator.hasMoreElements()) {
				JarEntry entry = enumerator.nextElement();
				String className = entry.getName();
				System.out.println("Entry " + className);
				if (className.endsWith(".class")) {
					className = className.replaceAll("/", ".");
					className = className.substring(0,  className.length() - 6);
					
					Class<?> clazzToLoad = child.loadClass(className);
					for (Annotation anno : clazzToLoad.getDeclaredAnnotationsByType(WebServiceClient.class)) {
						System.out.println(anno.toString());
						if (anno.annotationType() == WebServiceClient.class) {
							WebServiceClient cl = (WebServiceClient)anno;
							if (cl.name().equals(req.getServiceName())) {
								// found the service!
								System.out.println("We've found our service!  " + clazzToLoad);
								serviceClass = clazzToLoad;
								break outer;
							}
						}
					}
				}
			}
			
			if (serviceClass == null) {
				// TODO
				
				System.out.println("No handler found");
				return null;
			}
			
			
			Method[] methods = serviceClass.getMethods();
			Method endpointMethod = null;
			outer:
			for (Method method: methods) {
				if (method.getParameterCount() > 0) {
					continue;
				}
				Annotation[] annos = method.getAnnotationsByType(WebEndpoint.class);
				for (Annotation anno: annos) {
					WebEndpoint we = (WebEndpoint)anno;
					if (we.name().equals(req.getEndpointName())) {
						endpointMethod = method;
						break outer;
					}
				}
			}
			
			if (endpointMethod == null) {
				// TODO
				
				System.out.println("No endpoint found");
				return null;
			}
			
			Object service = serviceClass.newInstance();
			Object endpoint = endpointMethod.invoke(service, new Object[0]);
			
		    Binding binding = ((BindingProvider) endpoint).getBinding();
		    List<Handler> handlerList = binding.getHandlerChain();
		    if (handlerList == null) {
		      handlerList = new ArrayList<Handler>();
		    }

		    if (handler != null) {
		    	handlerList.add(handler);
		    }
		    
		    binding.setHandlerChain(handlerList);
			
			Class<?> returnType = endpointMethod.getReturnType();
			
			Method operationMethod = null;
			Method[] serviceMethods = returnType.getMethods();
			outer:
			for (Method method: serviceMethods) {
				for (Annotation anno : method.getAnnotationsByType(WebMethod.class)) {
					WebMethod wm = (WebMethod)anno;
					if ((req.getActionName() != null && req.getActionName().equals(wm.action())) || (req.getOperationName() != null && req.getOperationName().equals(wm.operationName()))) {
						operationMethod = method;
						break outer;
					}
				}
			}
			
			if (operationMethod == null) {
				// TODO
				
				System.out.println("No operation found");
				return null;
			}
			
			return getRequestResponseHandler(child, gson, operationMethod, endpoint).handle(req);
		}
		
	}

}
