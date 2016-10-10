/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;
import org.json.*;

/**
 *
 * @author camerong
 */
public class Response {
    public static String build(spark.Response response, Object object) {
        response.type("application/json");
        return object.toString();
    }
    
    public static JSONObject objectOf(Object... parameters) {
        JSONObject object = new JSONObject();
        String currentKey = null;
        
        for (Object parameter : parameters) {
            if (currentKey == null) {
                currentKey = parameter.toString();
            } else {
                object.put(currentKey, parameter);
                currentKey = null;
            }
        }
        
        return object;
    }
    
    public static JSONArray arrayOf(Object... parameters) {
        JSONArray array = new JSONArray();
        for (Object parameter : parameters) {
            array.put(parameter);
        }
        return array;
    }
}
