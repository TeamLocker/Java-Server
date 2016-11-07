/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import spark.Request;

/**
 *
 * @author camerong
 */
public class Database {
    private Connection connection;

    public Database(Connection connection) {
        this.connection = connection;
    }
    
    public ResultSet getUser(String username) throws SQLException {
        PreparedStatement stmt = this.connection.prepareStatement("SELECT * FROM users WHERE username=?");
        stmt.setString(1, username);
        return stmt.executeQuery();
    }
    
    public ResultSet getUser(int userId) throws SQLException {
        PreparedStatement stmt = this.connection.prepareStatement("SELECT * FROM users WHERE id=?");
        stmt.setInt(1, userId);
        return stmt.executeQuery();
    }
}
