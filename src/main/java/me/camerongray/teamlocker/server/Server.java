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
            return Response.build(response, Response.objectOf("success", true));
        });
        
        get("/users/:userId/", (request, response) -> {
            Database database = new Database(ConnectionManager.getPooledConnection());
            
            ResultSet rs = null;
            if (request.params(":userId").equals("self")) {
                rs = database.getUser((new RequestCredentials(request)).getUsername());
            } else {
                Helpers.enforceAdmin(request);
                try {
                    rs = database.getUser(Integer.parseInt(request.params(":userId")));
                } catch (NumberFormatException e) {
                    halt(400, "User ID must be a number");
                }
            }
            
            if (!rs.next()) halt(404, "User not found");
            
            return Response.build(response, Response.objectOf(
                    "id", rs.getInt("id"),
                    "full_name", rs.getString("full_name"),
                    "username", rs.getString("username"),
                    "email", rs.getString("email"),
                    "auth_hash", rs.getString("auth_hash"),
                    "encrypted_private_key", rs.getString("encrypted_private_key"),
                    "public_key", rs.getString("public_key"),
                    "admin", rs.getBoolean("admin"),
                    "pbkdf2_salt", rs.getString("pbkdf2_salt"),
                    "aes_iv", rs.getString("aes_iv")
            ));
        });
        
        put("/folders/", (request, response) -> {
            
            return "";
        });
        
        //TODO - Disable this in production!
        spark.debug.DebugScreen.enableDebugScreen();
    }
}
