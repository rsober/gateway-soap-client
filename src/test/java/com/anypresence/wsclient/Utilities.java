package com.anypresence.wsclient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.*;
import org.w3c.dom.Document;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Utilities {

	public static String prettyJson(String ugly) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(ugly);
		String pretty = gson.toJson(je);
		return pretty;
	}
	
	public static String prettyXml(String ugly) {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(ugly.getBytes()));
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			//initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
			String pretty = result.getWriter().toString();
			return pretty;
		} catch(Exception e) {
			throw new RuntimeException("Unable to prettify XML", e);
		}
	}

	public static Document stringToDocument(String xml) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setCoalescing(true);
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setIgnoringComments(true);
		DocumentBuilder bldr = dbf.newDocumentBuilder();
		InputSource insrc = new InputSource(new StringReader(xml));

		return bldr.parse(insrc);
	}

	public static String docToString(Document doc) throws TransformerException, ParserConfigurationException {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "no");

		//initialize StreamResult with File object to save to file
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);

		return result.getWriter().toString();
	}


	public static void traverseArray(List<Object> array, JsonArray jo) {
		for (Iterator<JsonElement> it = jo.iterator(); it.hasNext() ;) {
			JsonElement je = it.next();

			if (je.isJsonPrimitive()) {
				array.add(je.toString());
			} else if (je.isJsonObject()) {
				Map<String,Object> map = new HashMap();
				array.add(map);
				traverse(map, je.getAsJsonObject());
			}

		}
	}

	public  static void traverse(Map<String, Object> map, JsonObject jo) {
		for (Map.Entry<String, JsonElement> e :  jo.entrySet()) {

			if (e.getValue().isJsonPrimitive()) {
				map.put(e.getKey().trim(), e.getValue().toString());
			} else if (e.getValue().isJsonArray()) {
				List<Object> arr = new ArrayList<>();
				map.put(e.getKey().trim(), arr);
				traverseArray(arr, e.getValue().getAsJsonArray());
			}
			else {
				if (e.getValue().isJsonObject()) {
					Map<String,Object> innerMap = new HashMap<>();
					map.put(e.getKey().trim(), innerMap);
					traverse(innerMap, e.getValue().getAsJsonObject());
				}
			}
		}
	}

	public static boolean compareJsonAsMaps(String jsonActual, String jsonExpected) {
		JsonParser j = new JsonParser();

		Map<String,Object> mapActual  = new HashMap<>();
		Map<String,Object> mapExpected  = new HashMap<>();

		JsonElement je = j.parse(jsonActual);
		if (je.isJsonObject()) {
			traverse(mapActual, je.getAsJsonObject());
		}

		JsonElement je2 = j.parse(jsonExpected);
		if (je2.isJsonObject()) {
			traverse(mapExpected, je2.getAsJsonObject());
		}

		return compareMaps(mapActual, mapExpected);
	}

	private static boolean compareMaps(Map<String,Object> mapActual, Map<String,Object> mapExpected) {
		for  (String key : mapActual.keySet()) {
			Object a = mapActual.get(key);

			if (!mapExpected.containsKey(key)) {
				System.out.println("no key? : " + key);
				System.out.println(mapExpected.toString());
				return false;
			}
			Object e = mapExpected.get(key);

			if (a instanceof String) {
				if (!a.toString().equals(e.toString())) {
					return false;
				}
			} else if (a instanceof HashMap) {
				return compareMaps((Map<String,Object>)mapActual.get(key), (Map<String,Object>)mapExpected.get(key));
			}
		}

		return true;
	}
	
}
