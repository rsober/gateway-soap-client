package com.anypresence.wsclient.utils;


import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to massage errors into json to be sent back to the caller:
 *
 * Error messages should look like:
 * {
 *   error": {
 *    code": 404,
 *    message": "ID not found"
 *   }
 * }
 */
public class ErrorHandlingUtils {

    private static Gson gson = new Gson();

    /**
     * Converts the exception to json.
     *
     * @param throwable
     * @return
     */
    public static String exToJson(final Throwable throwable) {
        String message = throwable.getMessage();

        Map<String, Map<String,String>> map = new HashMap<String, Map<String,String>>();

        Map<String, String> error = new HashMap<String, String>();

        if (message == null || message.isEmpty()) {
            message = "No message available.";
        }
        error.put("message", message);
        error.put("exception", throwable.getClass().getSimpleName().toString());

        map.put("error", error);

        return gson.toJson(map);
    }

}
