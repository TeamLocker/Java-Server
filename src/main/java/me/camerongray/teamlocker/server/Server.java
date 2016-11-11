/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.DynaBean;
import static spark.Spark.*;

/**
 *
 * @author camerong
 */
public class Server {
    public static void main(String[] args) throws PropertyVetoException, SQLException {
        ConnectionManager.initialise("localhost", "teamlocker", "teamlocker", "teamlocker");
        TransactionStore.initialise();
        
        
        before((request, response) -> {
            if (request.headers("Authorization") == null) {
                response.header("WWW-Authenticate", "Basic");
                halt(401);
            }
            RequestCredentials credentials = new RequestCredentials(request);
        });
        
        get("/check_auth/", (request, response) -> {
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("success", true));
        });
                
        get("/users/:userId/", (request, response) -> {
            DynaBean user = null;
            try (Database database = new Database(ConnectionManager.getPooledConnection())) {
                if (request.params(":userId").equals("self")) {
                    try {
                        user = database.getUser((new RequestCredentials(request)).getUsername());
                    } catch (ObjectNotFoundException e) {
                        ResponseBuilder.errorHalt(response, 404, "User not found");
                    }
                } else {
                    Helpers.enforceAdmin(request, response);
                    try {
                        user = database.getUser(Integer.parseInt(request.params(":userId")));
                    } catch (NumberFormatException e) {
                        ResponseBuilder.errorHalt(response, 400, "User ID must be a number");
                    } catch (ObjectNotFoundException e) {
                        ResponseBuilder.errorHalt(response, 404, "User not found");
                    }
                }
            }
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf(
                    "id", (int)user.get("id"),
                    "full_name",(String)user.get("full_name"),
                    "username",(String)user.get("username"),
                    "email",(String)user.get("email"),
                    "auth_hash",(String)user.get("auth_hash"),
                    "encrypted_private_key",(String)user.get("encrypted_private_key"),
                    "public_key",(String)user.get("public_key"),
                    "admin", (boolean)user.get("admin"),
                    "pbkdf2_salt",(String)user.get("pbkdf2_salt"),
                    "aes_iv",(String)user.get("aes_iv")
            ));
        });
        
        put("/folders/", (request, response) -> {
            
            return "";
        });
        
        //TODO - Disable this in production!
        spark.debug.DebugScreen.enableDebugScreen();
    }
}
