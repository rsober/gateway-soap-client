package com.anypresence.wsclient.utils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import com.anypresence.wsclient.CxfWorker;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.predic8.wsdl.Definitions;
import com.predic8.wsdl.WSDLParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.apache.commons.collections.map.LRUMap;

public class ParseUtils {
    static Logger log = LogManager.getLogger(ParseUtils.class.getName());

    public static int PRETTY_PRINT_INDENT_FACTOR = 4;
    private static int MAX_CAPACITY = 3;

    private static Map<String, Definitions> lruMap = (Map<String, Definitions>) Collections.synchronizedMap(new org.apache.commons.collections.LRUMap(MAX_CAPACITY));

    private static String keyForWsdl(String message) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(message.getBytes());

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            if ((0xff & hash[i]) < 0x10) {
                hexString.append("0"
                        + Integer.toHexString((0xFF & hash[i])));
            } else {
                hexString.append(Integer.toHexString(0xFF & hash[i]));
            }
        }

        return hexString.toString();
    }

    public static Definitions definitionsFromUrl(String wsdlUrl) {
        WSDLParser parser = new WSDLParser();

        try {
            String key = keyForWsdl(wsdlUrl);
            if (lruMap.containsKey(key)) {
                return lruMap.get(key);
            } else {
                Definitions defs = parser.parse(wsdlUrl);
                lruMap.put(key, defs);

                return defs;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Parse json into a map.
     *
     * @param json
     * @return
     */
    public static Map<String,Object> parseJsonToMap(String json) {
        HashMap<String, Object> map = new HashMap<String, Object>();

        ObjectMapper mapper = new ObjectMapper();
        try
        {
            //Convert Map to JSON
            map = mapper.readValue(json, new TypeReference<Map<String, Object>>(){});
            //Print JSON output
            System.out.println(map);
        }
        catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    /**
     * Convert xml to json.
     *
     * @param xml
     * @return
     */
    public static String xmlToJson(String xml) {
        String jsonPrettyPrintString = "";
        try {
            JSONObject xmlJSONObj = XML.toJSONObject(xml);
            jsonPrettyPrintString = xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR);
        } catch (JSONException je) {
            je.printStackTrace();
        }

        return jsonPrettyPrintString;
    }

    /**
     * Checks if the json is valid.
     *
     * @param json
     * @return
     */
    public static boolean isValidJson(String json) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Inject parameters into the xml.
     *
     * @param formParams
     * @param action
     * @param params
     * @param shouldPrependAction
     */
    public static void injectParametersIntoXml(HashMap<String, String> formParams, String action, JsonElement params, boolean shouldPrependAction) {
        List<Holder> holders = traverseMap(jsonToMap(params.toString()));
        for (Holder h : holders) {
            if (!h.xValue.isEmpty()) {
                if (!shouldPrependAction) {
                    formParams.put("xpath:/" + h.xPath.toString(), h.xValue);
                } else {
                    formParams.put("xpath:/" + action + "/" + h.xPath.toString(), h.xValue);
                }

            }
        }
    }

    private static HashMap<String, Object> jsonToMap(String json) {

        HashMap<String, Object> map = new HashMap<String, Object>();

        ObjectMapper mapper = new ObjectMapper();
        try
        {
            //Convert Map to JSON
            map = mapper.readValue(json, new TypeReference<Map<String, Object>>(){});
        }
        catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    private static List<Holder> traverseMap(HashMap<String,Object> map) {
        List<Holder> data = new ArrayList<Holder>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Holder h = new Holder();
            Holder newHolder = findValue(entry.getKey(), entry.getValue(), h, data);

            data.add(newHolder);
        }

        return data;
    }

    private static List<Holder> traverseMapLevel(String xPath, HashMap<String,Object> map) {
        List<Holder> data = new ArrayList<Holder>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Holder h = new Holder();
            Holder newHolder = findValue(xPath + "/" + entry.getKey(), entry.getValue(), h, data);

            data.add(newHolder);
        }

        return data;
    }

    @SuppressWarnings("unchecked")
    private static Holder findValue(String s, Object o, Holder val, List<Holder> values) {
        val.xPath.append(s);

        if (!(o instanceof HashMap)) {
            val.xValue = o.toString();

            return val;
        } else if (o instanceof HashMap) {
            HashMap<String,Object> map = (HashMap<String, Object>)o;

            values.addAll(traverseMapLevel(val.xPath.toString(), map));
        }

        return val;

    }

    static class Holder {
        public StringBuilder xPath;
        public String xValue;

        Holder() {
            xPath = new StringBuilder();
            xValue = "";
        }
    }

}
