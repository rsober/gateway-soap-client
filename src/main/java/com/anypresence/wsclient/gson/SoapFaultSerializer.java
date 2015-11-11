package com.anypresence.wsclient.gson;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class SoapFaultSerializer implements JsonSerializer<SOAPFault> {

	@Override
	public JsonElement serialize(SOAPFault src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		
		try {
			if (src.getFaultActor() != null) {
				obj.addProperty("faultActor", src.getFaultActor());
			}
		} catch(UnsupportedOperationException e) {
			// Ignore
		}
		
		try {
			if (src.getFaultCode() != null) {
				obj.addProperty("faultCode", src.getFaultCode());
			}
		} catch(UnsupportedOperationException e) {
			// Ignore
		}
		
		try {
			if (src.getFaultNode() != null) {
				obj.addProperty("faultNode", src.getFaultNode());
			}
		} catch(UnsupportedOperationException e) {
			// Ignore
		}
		
		try {
			if (src.getFaultReasonLocales() != null) {
				@SuppressWarnings("unchecked")
				Iterator<Locale> it = (Iterator<Locale>)src.getFaultReasonLocales();
				Map<String, String> reasonTexts = new HashMap<String, String>();
				while (it.hasNext()) {
					Locale loc = it.next();
					reasonTexts.put(loc.toString(), src.getFaultReasonText(loc));
				}
				if (reasonTexts.size() == 1) {
					obj.addProperty("faultReasonText", reasonTexts.values().iterator().next());
				} else if (reasonTexts.size() > 0){
					JsonObject json = new JsonObject();
					for (String key : reasonTexts.keySet()) {
						json.addProperty(key, reasonTexts.get(key));
					}
					obj.add("faultReasonTexts", json);
				}
			}
		} catch (SOAPException e) {
			// Ignore
		} catch(UnsupportedOperationException e) {
			// Ignore
		}
		
		
		try {
			if (src.getFaultRole() != null) {
				obj.addProperty("faultRole", src.getFaultRole());
			}
		} catch(UnsupportedOperationException e) {
			// Ignore
		}
		
		try {
			if (src.getFaultString() != null) {
				obj.addProperty("faultString", src.getFaultString());
			}
		} catch(UnsupportedOperationException e) {
			// Ignore
		}
		
		try {
			if (src.getFaultSubcodes() != null) {
				@SuppressWarnings("unchecked")
				Iterator<QName> it = (Iterator<QName>)src.getFaultSubcodes();
				JsonArray subcodes = new JsonArray();
				while (it.hasNext()) {
					QName qname = it.next();
					subcodes.add(new JsonPrimitive(qname.toString()));
				}
				if (subcodes.size() > 0) {
					obj.add("faultSubcodes", subcodes);
				}
			}
		} catch(UnsupportedOperationException e) {
			// Ignore
		}
		
		try {
			if (src.hasDetail()) {
				addDetailToJson(obj, src.getDetail());
			}
		} catch(UnsupportedOperationException e) {
			// Ignore
		}
		
		return obj;
	}
	
	private void addDetailToJson(JsonObject obj, Detail d) {
		JsonObject detail = new JsonObject();
		buildJsonTree(detail, d);
		obj.add("faultDetail",detail.get("Detail"));
	}
	
	private void buildJsonTree(JsonObject json, Node n) {
		NodeList children = n.getChildNodes();
		JsonObject child = new JsonObject();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.TEXT_NODE) {
				json.addProperty(n.getLocalName(), node.getTextContent());
			} else {
				json.add(n.getLocalName(), child);
				buildJsonTree(child, node);
			}
		}
	}
	
}
