package com.anypresence.wsclient.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.anypresence.wsclient.CxfWorker;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

public class ParseUtils {
    static Logger log = LogManager.getLogger(ParseUtils.class.getName());

    public static int PRETTY_PRINT_INDENT_FACTOR = 4;

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

    public static boolean isValidJson(String json) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

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
