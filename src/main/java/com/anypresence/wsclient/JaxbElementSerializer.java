package com.anypresence.wsclient;

import java.lang.reflect.Type;

import javax.xml.bind.JAXBElement;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JaxbElementSerializer implements JsonSerializer<JAXBElement<?>>{

	@Override
	public JsonElement serialize(JAXBElement<?> src, Type typeOfSrc, JsonSerializationContext context) {
		Log.debug("[JaxbElementSerializer.serialize] Serializing value! " + src.getValue());
		return context.serialize(src.getValue());
	}

} 
