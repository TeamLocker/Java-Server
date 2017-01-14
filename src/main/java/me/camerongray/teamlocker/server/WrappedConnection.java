/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.sql.Connection;

/**
 *
 * @author camerong
 */
public class WrappedConnection {
    private Connection connection;
    private boolean hasOpenTransaction;

    public WrappedConnection(Connection connection, boolean hasOpenTransaction) {
        this.connection = connection;
        this.hasOpenTransaction = hasOpenTransaction;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean hasOpenTransaction() {
        return hasOpenTransaction;
    }
}
