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
import java.sql.SQLException;
import java.sql.Statement;
import static spark.Spark.*;

/**
 *
 * @author camerong
 */
public class Server {
    private String value = "foo";
    public static void main(String[] args) throws PropertyVetoException, SQLException {
        ConnectionManager.initialise("localhost", "teamlocker", "teamlocker", "teamlocker");
        TransactionStore.initialise();
        
        
        before((request, response) -> {
           // Check authentication here
        });
        
        get("/", (request, response) -> {
            return Response.build(response, Response.objectOf(
                    "success", true,
                    "message", "Thing happened",
                    "numbers", Response.arrayOf(1,2,3)));
        });
        
        get("/start/", (request, response) -> {
            Transaction t = TransactionStore.getTransaction();
            return t.getId(); 
        });
        
        get("/insert/", (request, response) -> {
            Transaction t = TransactionStore.getTransaction(request.queryParams("id"));
            PreparedStatement stmt = t.getConnection().prepareStatement("INSERT INTO test (value) VALUES (?)");
            stmt.setString(1, request.queryParams("id"));
            stmt.execute();
            return ""; 
        });
        
        get("/commit/", (request, response) -> {
            Transaction t = TransactionStore.getTransaction(request.queryParams("id"));
            t.commit();
            return ""; 
        });
        
        get("/rollback/", (request, response) -> {
            Transaction t = TransactionStore.getTransaction(request.queryParams("id"));
            t.rollback();
            return ""; 
        });
        
        get("/new/", (request, response) -> {
            Connection conn = ConnectionManager.getNewConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("INSERT INTO test (value) VALUES ('new');");
            conn.close();
            return ""; 
        });
        
        //TODO - Disable this in production!
        spark.debug.DebugScreen.enableDebugScreen();
    }
}
