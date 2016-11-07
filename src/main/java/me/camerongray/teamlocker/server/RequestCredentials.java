/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import spark.Request;

/**
 *
 * @author camerong
 */
public class RequestCredentials {
    String username;
    String password;
    
    public RequestCredentials(Request request) {
        String[] credentials = new String(Base64.getDecoder().decode(StringUtils.substringAfter(request.headers("Authorization"), "Basic").trim())).split(":");
        this.username = credentials[0];
        this.password = (credentials.length > 1) ? credentials[1] : "";
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    
}
