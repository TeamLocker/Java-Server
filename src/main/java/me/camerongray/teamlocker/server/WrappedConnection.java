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
    private boolean autoClose;

    public WrappedConnection(Connection connection, boolean autoClose) {
        this.connection = connection;
        this.autoClose = autoClose;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isAutoClose() {
        return autoClose;
    }
}
