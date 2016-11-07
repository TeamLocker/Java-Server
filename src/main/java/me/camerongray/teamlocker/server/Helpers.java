/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import spark.Request;
import static spark.Spark.halt;

/**
 *
 * @author camerong
 */
public class Helpers {
    public static void enforceAdmin(Request request) throws SQLException {
        Database database = new Database(ConnectionManager.getPooledConnection());
        ResultSet rs = database.getUser((new RequestCredentials(request)).getUsername());
        rs.next();
        if (!rs.getBoolean("admin")) {
            halt(503, "You must be an administrator to perform this action");
        }
    }
}
