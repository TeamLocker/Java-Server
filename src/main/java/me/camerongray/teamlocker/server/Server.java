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
    public static void main(String[] args) throws PropertyVetoException, SQLException {
        ConnectionManager.initialise("localhost", "teamlocker", "teamlocker", "teamlocker");
        TransactionStore.initialise();
        
        
        before((request, response) -> {
           // Check authentication here
        });
        
        put("/folders/", (request, response) -> {
            
            return "";
        });
        
        //TODO - Disable this in production!
        spark.debug.DebugScreen.enableDebugScreen();
    }
}
