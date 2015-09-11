package com.anypresence.wsclient;

import java.lang.reflect.Type;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
				// TODO getFaultReasonLocales
			
				// TODO getFaultReasonTexts()
			}
		} catch (SOAPException e) {
			
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
				// TODO getFaultSubcodes()
			}
		} catch(UnsupportedOperationException e) {
			// Ignore
		}
		
		try {
			if (src.hasDetail()) {
				// TODO getDetail()
			}
		} catch(UnsupportedOperationException e) {
			// Ignore
		}
		
		return obj;
	}

}
