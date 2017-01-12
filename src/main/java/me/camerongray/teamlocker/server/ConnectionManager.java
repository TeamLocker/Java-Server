/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.text.MessageFormat;
import spark.Request;

/**
 *
 * @author camerong
 */
public class ConnectionManager {
    private static ConnectionManager instance = null;
    private String dbServer;
    private String dbName;
    private String dbUser;
    private String dbPassword;
    private String jdbcUrl;
    private ComboPooledDataSource cpds = null;

    public ConnectionManager() {
        // Prevent instantiation
    }

    public static void initialise(String dbServer, String dbName, String dbUser, String dbPassword) throws PropertyVetoException {
        instance = new ConnectionManager();
        instance.dbServer = dbServer;
        instance.dbName = dbName;
        instance.dbUser = dbUser;
        instance.dbPassword = dbPassword;
        instance.jdbcUrl = MessageFormat.format("jdbc:postgresql://{0}/{1}", dbServer, dbName);
        
        instance.cpds = new ComboPooledDataSource();
        instance.cpds.setDriverClass("org.postgresql.Driver");
        // TODO - Perform some sort of escaping/encoding on the values going into URL
        instance.cpds.setJdbcUrl(instance.jdbcUrl);
        instance.cpds.setUser(dbUser);
        instance.cpds.setPassword(dbPassword);
    }
    
    public static Connection getPooledConnection() throws SQLException {
        return instance.cpds.getConnection();
    }
    
    public static Connection getNewConnection() throws SQLException {
        return DriverManager.getConnection(instance.jdbcUrl, instance.dbUser, instance.dbPassword);
    }
    
    public static Connection getConnection(Request request) throws SQLException, ObjectNotFoundException {
        String transactionId = request.queryParams("transaction_id");
        if (transactionId == null) {
            return ConnectionManager.getPooledConnection();
        } else {
            return TransactionStore.getTransaction(transactionId).getConnection();
        }
    }
}