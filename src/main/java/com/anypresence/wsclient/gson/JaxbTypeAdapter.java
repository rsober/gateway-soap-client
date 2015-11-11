package com.anypresence.wsclient.gson;

import java.lang.reflect.Type;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JaxbTypeAdapter implements JsonSerializer<JAXBElement<?>>, JsonDeserializer<JAXBElement<?>> {

	private static final QName DEFAULT = QName.valueOf("##default");
	
	@Override
	public JsonElement serialize(JAXBElement<?> src, Type typeOfSrc, JsonSerializationContext context) {
		return context.serialize(src.getValue());
	}
	
	@Override
	public JAXBElement<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		String baseTypeName = typeOfT.getTypeName().split("\\<|\\>")[1];
		Class<?> baseType = null;
		try {
			baseType = Class.forName(baseTypeName);
		} catch (ClassNotFoundException e) {
			throw new JsonParseException("Unable to deserialize root type", e);
		}
		
		Object a = context.deserialize(json, baseType);
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		JAXBElement<?> elt = new JAXBElement(DEFAULT, baseType, a);
		
		return elt;
	}


} 
