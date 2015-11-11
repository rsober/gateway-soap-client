package com.anypresence.wsclient.gson;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class XMLGregorianCalendarTypeAdapter implements JsonSerializer<XMLGregorianCalendar>, JsonDeserializer<XMLGregorianCalendar> {

	@Override
	public XMLGregorianCalendar deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		Calendar c = DatatypeConverter.parseDateTime(json.getAsString());
		if (!(c instanceof GregorianCalendar)) {
			throw new JsonParseException("Expected GregorianCalendar upon call to DatatypeConverter.parseDateTime");
		}
		GregorianCalendar gc = (GregorianCalendar)c;
		
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
		} catch (DatatypeConfigurationException e) {
			throw new JsonParseException("Unable to instantiate DatatypeFactory");
		}
	}

	@Override
	public JsonElement serialize(XMLGregorianCalendar src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(DatatypeConverter.printDateTime(src.toGregorianCalendar()));
	}

}
