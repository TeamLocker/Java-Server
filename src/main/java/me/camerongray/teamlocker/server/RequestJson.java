/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import spark.Request;

/**
 *
 * @author camerong
 */
public class RequestJson {
    public static void validateSchema(String schemaName, String toValidate) throws JSONValidationException {
        String schemaBody = new Scanner(Server.class.getResourceAsStream("requestSchemas/" + schemaName + ".json"), "UTF-8").useDelimiter("\\A").next();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode schemaNode = mapper.readTree(schemaBody);
            JsonNode inputNode = mapper.readTree(toValidate);
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            JsonSchema schema = factory.getJsonSchema(schemaNode);
            ProcessingReport result = schema.validate(inputNode);
            
            if (!result.isSuccess()) {
                throw new JSONValidationException(result.toString());
            }
        } catch (IOException ex) {
            throw new JSONValidationException(ex);
        } catch (ProcessingException ex) {
            throw new JSONValidationException(ex);
        }
    }
    
    public static JSONObject getValidated(Request request, String schema) throws JSONValidationException {
        String jsonString = request.body();
        try {
            RequestJson.validateSchema(schema, jsonString);
        } catch (JSONValidationException ex) {
            throw new JSONValidationException(ex.getMessage());
        }
        
        JSONObject json = new JSONObject(jsonString);
        return json;
    }
}
