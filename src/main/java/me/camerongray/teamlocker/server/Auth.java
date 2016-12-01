/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.DynaBean;
import spark.Request;
import spark.Response;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author camerong
 */
public class Auth {
    public static void enforceAdmin(Request request, Response response) throws SQLException, ObjectNotFoundException {
        if (!Auth.currentUserIsAdmin(request)) {
            ResponseBuilder.errorHalt(response, 403, "You must be an administrator to perform this action");
        }
    }
    
    public static boolean currentUserIsAdmin(Request request) throws SQLException, ObjectNotFoundException {
        return (boolean)Auth.getCurrentUser(request).get("admin");
    }
    
    public static DynaBean getCurrentUser(Request request) throws SQLException, ObjectNotFoundException {
        try (Database database = new Database(ConnectionManager.getPooledConnection())) {
            return database.getUser((new RequestCredentials(request)).getUsername());
        }
    }
    
    public static boolean checkCredentials(String username, String password) throws SQLException {
        try (Database database = new Database(ConnectionManager.getPooledConnection())) {
            DynaBean user;
            try {
                user = database.getUser(username);
            } catch (ObjectNotFoundException ex) {
                return false;
            }
            
            return BCrypt.checkpw(password, (String)user.get("auth_hash"));
        }
    }
}
