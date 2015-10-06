package com.anypresence.wsclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Node;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Worker implements Runnable {

	private static final String RAWTYPES = "rawtypes";
	private static final String DEFAULT_NODE_NAME = "##default";
	private Socket sock;

	public Worker(Socket sock) {
		this.sock = sock;
	}

	@Override
	public void run() {
		StringBuilder builder = new StringBuilder("");
		withSocket(sock, () ->	{
			Gson gson = new GsonBuilder().registerTypeAdapter(SOAPFault.class, new SoapFaultSerializer())
										 .registerTypeAdapter(JAXBElement.class, new JaxbElementSerializer())
										 .registerTypeHierarchyAdapter(Node.class, new GenericXmlSerializer())
										 .setPrettyPrinting()
										 .setFieldNamingStrategy(new FieldNamer())
										 .create();

			boolean proceed = true;
			String readError = null;

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
				Log.info("Unable to fully read request due to IOException: " + e.getMessage());
				if (Log.isDebugEnabled()) {
					e.printStackTrace(System.out);
				}
				proceed = false;
				readError = e.getMessage();
			}

			String response = null;
			if (proceed) {
				Log.debug("Proceeding with processing of payload");
				String payload = builder.toString().trim();

				try {
					response = processRequestPayload(gson, payload);
				} catch (SoapClientException e) {
					if (e.getCause() != null && e.getCause() instanceof InvocationTargetException && e.getCause().getCause() != null && e.getCause().getCause() instanceof SOAPFaultException) {
						Log.debug("Detected SOAPFault scenario");
						SOAPFaultException soapFaultExc = (SOAPFaultException)e.getCause().getCause();
						response = gson.toJson(OperationResponse.newFaultOperationResponse(soapFaultExc.getFault()));
					} else {
						Log.info("Encountered SoapClientException while trying to process request: " + e.getMessage());
						if (Log.isDebugEnabled()) {
							e.printStackTrace(System.out);
						}
						response = gson.toJson(OperationResponse.newFailedOperationResponse(e.getMessage()));
					}
				} catch(Exception e) {
					Log.info("Encountered " + e.getClass().getSimpleName() + " while trying to process request: " + e.getMessage());
					if (Log.isDebugEnabled()) {
						e.printStackTrace(System.out);
					}
					response = gson.toJson(OperationResponse.newFailedOperationResponse(e.getMessage()));
				}

			} else {
				Log.debug("Returning failed response");
				response = gson.toJson(OperationResponse.newFailedOperationResponse(readError));
			}

			Log.debug("Writing response to socket");
			try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {
				writer.write(response);
			} catch(IOException e) {
				Log.info("Unable to fully write response due to IOException: " + e.getMessage());
				if (Log.isDebugEnabled()) {
					e.printStackTrace(System.out);
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
	private String processRequestPayload(Gson gson, String payload) throws SoapClientException {
		Log.debug("Attempting to process request payload: " + payload);

		URLClassLoader child = null;
		JarFile file = null;

		try {

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
				Log.debug("Using security handler due to presence of wssePasswordCredentials");
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
				Log.debug("Jar entry: " + className);
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
						if (anno.annotationType() == WebServiceClient.class) {
							WebServiceClient cl = (WebServiceClient)anno;
							if (cl.name().equals(req.getServiceName())) {

								// found the service!
								Log.debug("Successfully located service in class:  " + clazzToLoad);
								serviceClass = clazzToLoad;
								break outer;
							}
						}
					}
				}
			}

			if (serviceClass == null) {
				throw new SoapClientException("Unable to locate service class ");
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

			BindingProvider bp = ((BindingProvider)endpoint);
			if (req.getUrl() != null) {
				bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, req.getUrl());
			}

		    Binding binding = bp.getBinding();
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
	
	private static class FieldNamer implements FieldNamingStrategy{
		@Override
		public String translateName(Field f) {
			System.out.println("f.getName() : " + f.getName());
			XmlElement elt = f.getDeclaredAnnotation(XmlElement.class);
			if (elt == null || elt.name() == null || elt.name().equals(DEFAULT_NODE_NAME)) {
				System.out.println("Returning f.getName(): " + f.getName());
				return f.getName();
			} else {
				System.out.println("Returning elt name " + elt.name());
				return elt.name();
			}
		}
		
	}

}
