package com.anypresence.wsclient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GenericXmlSerializer implements JsonSerializer<Node> {

	@Override
	public JsonElement serialize(Node src, Type typeOfSrc, JsonSerializationContext context) {
		if (src == null || src.getNodeType() != Node.ELEMENT_NODE) {
			throw new IllegalArgumentException("Expected src to have NodeType of " + Node.ELEMENT_NODE);
		}
		
		return toJsonElement(src.getChildNodes());
	}
	
	private JsonElement toJsonElement(NodeList nodes) {
		if (nodes.getLength() == 0) {
			return JsonNull.INSTANCE;
		} else if (nodes.getLength() == 1 && nodes.item(0).getNodeType() != Node.ELEMENT_NODE) {
			return toJsonElement(nodes.item(0));
		}
		
		Bucketizer buckets = new Bucketizer();
		
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.TEXT_NODE && node.getTextContent().trim().isEmpty()) {
				continue;
			}
			JsonElement elt = toJsonElement(node);
			if (elt != null) {
				buckets.add(node.getLocalName(), elt);
			}
		}
		
		if (buckets.keys().size() == 0) {
			return JsonNull.INSTANCE;
		}
		
		JsonObject obj = new JsonObject();
		
		for (String key : buckets.keys()) {
			if (buckets.get(key).size() == 0) {
				obj.add(key, JsonNull.INSTANCE);
			} else if (buckets.get(key).size() == 1) {
				obj.add(key, buckets.get(key).get(0));
			} else {
				JsonArray jsonArray = new JsonArray();
				for (JsonElement elt : buckets.get(key)) {
					jsonArray.add(elt);
				}
				obj.add(key, jsonArray);
			}
		}
		
		return obj;
	}
	
	private JsonElement toJsonElement(Node node) {
		if (node == null) {
			return JsonNull.INSTANCE;
		}
		
		switch (node.getNodeType()) {
		case Node.TEXT_NODE:
			Text txt = (Text)node;
			return new JsonPrimitive(txt.getTextContent().trim());
		case Node.ELEMENT_NODE:
			String localName = node.getLocalName();
			if (localName == null) {
				return null;
			}
			
			NodeList children = node.getChildNodes();
			if (children.getLength() == 0) {
				return JsonNull.INSTANCE;
			}
			
			return toJsonElement(children);
		default:
			throw new UnsupportedOperationException("Node type " + node.getNodeType() + " not supported yet for node " + node.getLocalName());
		}
	}
	
	private static class Bucketizer {
		private Map<String, List<JsonElement>> eltsMap;
		
		public Bucketizer() {
			this.eltsMap = new HashMap<String, List<JsonElement>>();
		}
		
		public Collection<String> keys() {
			return eltsMap.keySet();
		}
		
		public void add(String key, JsonElement e) {
			List<JsonElement> l = this.eltsMap.get(key);
			if (l == null) {
				l = new ArrayList<JsonElement>();
				this.eltsMap.put(key, l);
			}
			l.add(e);
		}
		
		public List<JsonElement> get(String key) {
			List<JsonElement> l = eltsMap.get(key);
			if (l == null) {
				return new ArrayList<JsonElement>();
			} else {
				return l;
			}
		}
	}
}
