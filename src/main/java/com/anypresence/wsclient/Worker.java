package com.anypresence.wsclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
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

public class Worker implements Runnable {

	private static final String RAWTYPES = "rawtypes";
	private Socket sock;
	
	public Worker(Socket sock) {
		this.sock = sock;
	}
	
	@Override
	public void run() {
		StringBuilder builder = new StringBuilder("");
		withSocket(sock, () ->	{
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			String line = null;
			while((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
				if (builder.toString().endsWith("\n\n")) {
					break;
				}
			}
		} catch(IOException e) {
			// TODO - return error response
			return;
		} 
		
		String payload = builder.toString().trim();
			
		String response = null;
		try {
			response = processRequestPayload(payload);
		} catch (SoapClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();	
		}
		
		Log.debug("Writing");
		
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			writer.write(response);
			
		} catch(IOException e) {
			// TODO - return error response
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch(IOException e) {
					// Ignore
				}
			}
		}
		Log.debug("Done");
		});
	}
	
	private void withSocket(Socket sock, Runnable r) {
		try {
			r.run();
		} finally {
			if (sock != null) {
				try {
					sock.close();
				} catch(IOException e) {
					// Ignore
				}
			}
		}
	}
	
	@SuppressWarnings(RAWTYPES)
	private String processRequestPayload(String payload) throws SoapClientException {
		Log.debug("payload: " + payload);
		
		URLClassLoader child = null;
		JarFile file = null;
		
		try {
			// Deserialize the JSON
			Gson gson = new Gson();
			
			OperationRequest req = gson.fromJson(payload,OperationRequest.class);
			
			URI jarURI;
			try {
				jarURI = new URI(req.getJarUrl());
			} catch (URISyntaxException e) {
				throw new SoapClientException("Invalid URI syntax for jar URL: " + req.getJarUrl(), e);
			}
			
			try {
				child = new URLClassLoader(new URL[] { jarURI.toURL() }, this.getClass().getClassLoader());
			} catch (MalformedURLException e) {
				throw new SoapClientException("Malformed jar URL for jar URI: " + jarURI.toString());
			}
			
			WssePasswordSecurityCredentials creds = req.getWssePasswordCredentials();
			SecurityHandler handler = null;
			if (creds != null) {
				Log.debug("Creds::: " + creds);
				handler = new SecurityHandler(creds.getUsername(), creds.getPassword());
			}
			Log.debug("Received request: " + req);
			
			// Crack open the jar file and look for the service to instantiate
			
			try {
				file = new JarFile(new File(jarURI));
			} catch (IOException e) {
				throw new SoapClientException("Unable to open file at " + req.getJarUrl() + " due to IOException: " + e.getMessage(), e);
			}
			
			Enumeration<JarEntry> enumerator = file.entries();
			
			Class<?> serviceClass = null;
			
			outer:
			while (enumerator.hasMoreElements()) {
				JarEntry entry = enumerator.nextElement();
				String className = entry.getName();
				Log.debug("Entry " + className);
				if (className.endsWith(".class")) {
					className = className.replaceAll("/", ".");
					className = className.substring(0,  className.length() - 6);
					
					Class<?> clazzToLoad;
					try {
						clazzToLoad = child.loadClass(className);
					} catch (ClassNotFoundException e) {
						throw new SoapClientException("Unable to load class for class name " + className + " due to ClassNotFoundException", e);
					}
					
					for (Annotation anno : clazzToLoad.getDeclaredAnnotationsByType(WebServiceClient.class)) {
						Log.debug(anno.toString());
						if (anno.annotationType() == WebServiceClient.class) {
							WebServiceClient cl = (WebServiceClient)anno;
							if (cl.name().equals(req.getServiceName())) {
								// found the service!
								Log.debug("We've found our service!  " + clazzToLoad);
								serviceClass = clazzToLoad;
								break outer;
							}
						}
					}
				}
			}
			
			if (serviceClass == null) {
				throw new SoapClientException("Unable to locate service class");
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
				throw new SoapClientException("Unable to find endpoint");
			}
			
			Object service;
			try {
				service = serviceClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new SoapClientException("Unable to instantiate class " + serviceClass.getName() + " due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
			}
			
			Object endpoint;
			try {
				endpoint = endpointMethod.invoke(service, new Object[0]);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SoapClientException("Unable to invoke method " + endpointMethod.getName() + " due to " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
			}
			
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
				throw new SoapClientException("Unable to find operation to invoke");
			}
			
			return getRequestResponseHandler(child, gson, operationMethod, endpoint).handle(req);
		} finally {
			if (file != null) {
				try {
					file.close();
				} catch(IOException e) {
					// Ignore
				}
			}
			if (child != null) {
				try {
					child.close();
				} catch(IOException e) {
					// Ignore
				}
			}
		}
	}
	
	private static RequestHandler getRequestResponseHandler(ClassLoader loader, Gson gson, Method endpointMethod, Object endpoint) {
		if (endpointMethod.getReturnType() == Void.TYPE) {
			return new VoidRequestHandler(loader, gson, endpointMethod, endpoint);
		} else {
			return new DefaultRequestHandler(loader, gson, endpointMethod, endpoint);
		}
		
	}
	
}
