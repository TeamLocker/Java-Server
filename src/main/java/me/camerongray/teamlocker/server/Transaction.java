/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

/**
 *
 * @author camerong
 */
public class Transaction {
    private String id;
    private Connection connection;
    private long lastUsed;

    public Transaction(Connection connection) throws SQLException {
        this.id = UUID.randomUUID().toString();
        this.connection = connection;
        this.connection.createStatement().execute("START TRANSACTION");
        this.updateLastUsed();
    }
    
    public Transaction(WrappedConnection connectionWrapper) throws SQLException {
        this(connectionWrapper.getConnection());
    }
    
    public final void updateLastUsed() {
        this.lastUsed = Instant.now().getEpochSecond();
    }

    public WrappedConnection getWrappedConnection() {
        return new WrappedConnection(this.connection, false);
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

    public long getLastUsed() {
        return lastUsed;
    }
}
