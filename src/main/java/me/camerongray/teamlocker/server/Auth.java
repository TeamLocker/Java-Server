/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.DynaBean;
import spark.Request;
import spark.Response;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import org.mindrot.jbcrypt.BCrypt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;
import static spark.Spark.halt;

/**
 *
 * @author camerong
 */
public class Auth {
    public static int PERMISSION_READ = 1;
    public static int PERMISSION_WRITE = 2;
    
    public static void enforceAdmin(Request request, Response response) throws SQLException, ObjectNotFoundException {
        if (!Auth.currentUserIsAdmin(request)) {
            ResponseBuilder.errorHalt(response, 403, "You must be an administrator to perform this action");
        }
    }
    
    public static void enforceFolderPermission(Request request, Response response, int folderId, int permission) throws SQLException, ObjectNotFoundException {
        if (Auth.currentUserIsAdmin(request)) {
            return;
        }
        
        boolean hasPermission = Auth.getFolderPermission(request, response, folderId, permission);
        if (!hasPermission) {
            String permissionName = (permission == Auth.PERMISSION_READ) ? "read" : "write";
            ResponseBuilder.errorHalt(response, 403, "You do not have " + permissionName + " permission for this folder");
        }
    }
    
    public static void enforceAccountPermission(Request request, Response response, int accountId, int permission) throws SQLException, ObjectNotFoundException {
        if (Auth.currentUserIsAdmin(request)) {
            return;
        }
        
        boolean hasPermission = Auth.getAccountPermission(request, response, accountId, permission);
        if (!hasPermission) {
            String permissionName = (permission == Auth.PERMISSION_READ) ? "read" : "write";
            ResponseBuilder.errorHalt(response, 403, "You do not have " + permissionName + " permission for this folder");
        }
    }
    
    public static boolean getAccountPermission(Request request, Response response, int accountId, int permission) throws SQLException, ObjectNotFoundException {
        int folderId = -1;
        try (Database database = new Database(ConnectionManager.getPooledConnection())) {
            folderId = database.getAccount(accountId);
        }
        return Auth.getFolderPermission(request, response, folderId, permission);
    }
    
    public static boolean getFolderPermission(Request request, Response response, int folderId, int permission) throws SQLException, ObjectNotFoundException {
        int userId = Auth.getCurrentUserId(request);
        DynaBean permissions;
        try (Database database = new Database(ConnectionManager.getPooledConnection())) {
            permissions = database.getFolderPermissions(folderId, userId);
        }
        boolean hasPermission = false;
        if (permission == Auth.PERMISSION_READ) {
            hasPermission = (boolean)permissions.get("read");
        } else if (permission == Auth.PERMISSION_WRITE) {
            hasPermission = (boolean)permissions.get("write");
        }
        
        return hasPermission;
    }
    
    public static boolean currentUserIsAdmin(Request request) throws SQLException, ObjectNotFoundException {
        return (boolean)Auth.getCurrentUser(request).get("admin");
    }
    
    public static int getCurrentUserId(Request request) throws SQLException, ObjectNotFoundException {
        return (int)Auth.getCurrentUser(request).get("id");
    }
    
    public static DynaBean getCurrentUser(Request request) throws SQLException, ObjectNotFoundException {
        try (Database database = new Database(ConnectionManager.getPooledConnection())) {
            return database.getUser((new RequestCredentials(request)).getUsername());
        }
    }
    
    public static boolean checkCredentials(String username, String password) throws SQLException {
        try (Database database = new Database(ConnectionManager.getPooledConnection())) {
            DynaBean user;
            try {
                user = database.getUser(username);
            } catch (ObjectNotFoundException ex) {
                return false;
            }
            
            return BCrypt.checkpw(password, (String)user.get("auth_hash"));
        }
    }
}
