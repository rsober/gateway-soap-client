package com.anypresence.wsclient;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class GenericXmlSerializerTest {
	
	private GenericXmlSerializer serializer;
	
	@Before
	public void setup() {
		serializer = new GenericXmlSerializer();
	}
	
	@After
	public void teardown() {
		serializer = null;
	}
	
	@Test
	public void testNullTag() throws Exception {
		Node n = toNode("<user:Value/>");
		JsonElement elt = serializer.serialize(n, null, null);
		Assert.assertEquals(JsonNull.INSTANCE, elt);
	}
	
	@Test
	public void testEmptyTag() throws Exception {
		Node n = toNode("<user:Value></user:Value>");
		JsonElement elt = serializer.serialize(n, null, null);
		Assert.assertEquals(JsonNull.INSTANCE, elt);
	}
	
	@Test
	public void testTextNode() throws Exception {
		Node n = toNode("<user:Value>aa</user:Value>");
		JsonElement elt = serializer.serialize(n, null, null);
		Assert.assertEquals(new JsonPrimitive("aa"), elt); 
	}
	
	@Test
	public void testMultipleChildNodes() throws Exception {
		Node n = toNode("<user:Value><aa>thing</aa><aa>otherThing</aa><bb>myThing</bb></user:Value>");
		JsonElement elt = serializer.serialize(n, null, null);
		Assert.assertEquals(new JsonParser().parse("{\"aa\": [ \"thing\", \"otherThing\" ], \"bb\": \"myThing\"}"), elt);
	}
	
	@Test
	public void testMultiLevelNestedNodes() throws Exception {
		Node n = toNode("<user:Value><a><b>bb</b><c>cc</c><d><e>ee</e></d></a><x>xxx</x></user:Value>");
		JsonElement elt = serializer.serialize(n, null, null);
		Assert.assertEquals(new JsonParser().parse("{ \"a\": { \"b\": \"bb\", \"c\": \"cc\", \"d\": {\"e\": \"ee\"} }, \"x\":\"xxx\" }"), elt);
	}
	
	private static Node toNode(String src) throws Exception {
		StringBuilder dummyDoc = new StringBuilder();
		dummyDoc.append("<dummy xmlns:user=\"aaa\">\n").append(src).append("\n</dummy>");
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		fac.setNamespaceAware(true);
		DocumentBuilder db = fac.newDocumentBuilder();
		Document doc = db.parse(new ByteArrayInputStream(dummyDoc.toString().getBytes()));
		Node child = doc.getChildNodes().item(0);
		return child.getChildNodes().item(1);
	}

}
