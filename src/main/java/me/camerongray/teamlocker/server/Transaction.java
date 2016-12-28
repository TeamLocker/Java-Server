/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 *
 * @author camerong
 */
public class Transaction {
    private String id;
    private Connection connection;

    public Transaction(Connection connection) throws SQLException {
        this.id = UUID.randomUUID().toString();
        this.connection = connection;
        this.connection.createStatement().execute("START TRANSACTION");
    }

    public Connection getConnection() {
        return connection;
    }
    
    public void commit() throws SQLException {
        this.connection.createStatement().execute("COMMIT");
    }
    
    public void rollback() throws SQLException {
        this.connection.createStatement().execute("ROLLBACK");
    }

    public String getId() {
        return id;
    }
}
