package com.anypresence.wsclient.gson;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EnumTypeAdapter implements JsonSerializer<Enum<?>>, JsonDeserializer<Enum<?>> {

	@Override
	public Enum<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		Class<?> clazz = (Class<?>)typeOfT;
		for (Object o : clazz.getEnumConstants()) {
			try {
				Method m = clazz.getMethod("value");
				String value = (String)m.invoke(o);
				if (value.equals(json.getAsString())) {
					return (Enum<?>)o;
				}
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new JsonParseException("Unable to get method value on class " + o.getClass());
			}
			
		}
		return null;
	}

	@Override
	public JsonElement serialize(Enum<?> src, Type typeOfSrc, JsonSerializationContext context) {
		try {
			Method m = src.getClass().getMethod("value");
			String str = (String)m.invoke(src);
			return new JsonPrimitive(str);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Unable to get value of enum for type " + typeOfSrc + " with value of src"); 
		}
	}

}
