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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.DynaBean;
import org.json.JSONObject;
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
            // Log request
            StringBuilder sb = new StringBuilder();
            sb.append(request.requestMethod());
            sb.append(" " + request.url());
            sb.append(" " + request.body());
            System.out.println(sb);
            
            if (request.headers("Authorization") == null) {
                response.header("WWW-Authenticate", "Basic");
                halt(401);
            }
            RequestCredentials credentials = new RequestCredentials(request);
            if (!Auth.checkCredentials(credentials.username, credentials.password)) {
                ResponseBuilder.errorHalt(response, 401, "Incorrect username/password");
            }
        });
        
        get("/check_auth/", (request, response) -> {
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("success", true));
        });
                
        get("/users/:userId/", (request, response) -> {
            DynaBean user = null;
            try (Database database = new Database(ConnectionManager.getPooledConnection())) {
                if (request.params(":userId").equals("self")) {
                    try {
                        user = database.getUser((new RequestCredentials(request)).getUsername());
                    } catch (ObjectNotFoundException e) {
                        ResponseBuilder.errorHalt(response, 404, "User not found");
                    }
                } else {
                    Auth.enforceAdmin(request, response);
                    try {
                        user = database.getUser(Integer.parseInt(request.params(":userId")));
                    } catch (NumberFormatException e) {
                        ResponseBuilder.errorHalt(response, 400, "User ID must be a number");
                    } catch (ObjectNotFoundException e) {
                        ResponseBuilder.errorHalt(response, 404, "User not found");
                    }
                }
            }
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("user", 
                    ResponseBuilder.objectOf(
                        "id", (int)user.get("id"),
                        "full_name",(String)user.get("full_name"),
                        "username",(String)user.get("username"),
                        "email",(String)user.get("email"),
                        "auth_hash",(String)user.get("auth_hash"),
                        "encrypted_private_key",(String)user.get("encrypted_private_key"),
                        "public_key",(String)user.get("public_key"),
                        "admin", (boolean)user.get("admin"),
                        "pbkdf2_salt",(String)user.get("pbkdf2_salt"),
                        "aes_iv",(String)user.get("aes_iv")
            )));
        });
        
        get("/users/", (request, response) -> {
            Auth.enforceAdmin(request, response);
            ArrayList<JSONObject> userObjects = new ArrayList<>();
            try (Database database = new Database(ConnectionManager.getPooledConnection())) {
                List<DynaBean> users = database.getAllUsers();
                
                for (DynaBean user : users) {
                    userObjects.add(ResponseBuilder.objectOf(
                        "id", (int)user.get("id"),
                        "full_name",(String)user.get("full_name"),
                        "username",(String)user.get("username"),
                        "email",(String)user.get("email"),
                        "auth_hash",(String)user.get("auth_hash"),
                        "encrypted_private_key",(String)user.get("encrypted_private_key"),
                        "public_key",(String)user.get("public_key"),
                        "admin", (boolean)user.get("admin"),
                        "pbkdf2_salt",(String)user.get("pbkdf2_salt"),
                        "aes_iv",(String)user.get("aes_iv")
                    ));
                }
            }
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("users", ResponseBuilder.fromArrayList(userObjects)));
        });
        
        get("/folders/", (request, response) -> {
            ArrayList<JSONObject> folderObjects = new ArrayList<>();
            try (Database database = new Database(ConnectionManager.getPooledConnection())) {
                List<DynaBean> folders = database.getFolders((int)Auth.getCurrentUser(request).get("id"));
                for (DynaBean folder : folders) {
                    folderObjects.add(ResponseBuilder.objectOf(
                        "id", (int)folder.get("id"),
                        "name", (String)folder.get("name"),
                        "read", (boolean)folder.get("read"),
                        "write", (boolean)folder.get("write")
                    ));
                }
            }
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("folders",
                    ResponseBuilder.fromArrayList(folderObjects)));
        });
        
        get("/folders/:folderId/accounts/", (request, response) -> {
            int folderId = -1;
            try {
                folderId = Integer.parseInt(request.params(":folderId"));
            } catch (NumberFormatException ex) {
                ResponseBuilder.errorHalt(response, 400, "Folder ID must be a number");
            }
            Auth.enforceFolderPermission(request, response, folderId, Auth.PERMISSION_READ);
            
            ArrayList<JSONObject> accountObjects = new ArrayList<>();
            try (Database database = new Database(ConnectionManager.getPooledConnection())) {
                List<DynaBean> accounts = new ArrayList<>();
                accounts = database.getFolderAccounts(folderId, Auth.getCurrentUserId(request));
                
                for (DynaBean account : accounts) {
                    accountObjects.add(ResponseBuilder.objectOf(
                        "id", (int)account.get("account_id"),
                        "account_metadata", (String)account.get("account_metadata"),
                        "encrypted_aes_key", (String)account.get("encrypted_aes_key")
                    ));
                }
            }
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("accounts",
                    ResponseBuilder.fromArrayList(accountObjects)));
        });
        
        get("/folders/:folderId/permissions/", (request, response) -> {
            Auth.enforceAdmin(request, response);

            List<DynaBean> permissions = new ArrayList<>();
            try (Database database = new Database(ConnectionManager.getPooledConnection())) {
                try {
                    permissions = database.getFolderPermissions(Integer.parseInt(request.params(":folderId")));
                } catch (NumberFormatException ex) {
                    ResponseBuilder.errorHalt(response, 400, "Folder ID must be a number");
                }
            }
            
            ArrayList<JSONObject> responseObjects = new ArrayList<>();
            for(DynaBean permission : permissions) {
                responseObjects.add(ResponseBuilder.objectOf(
                        "user_id", (int)permission.get("user_id"),
                        "read", (boolean)permission.get("read"),
                        "write", (boolean)permission.get("write")
                ));
            }
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("permissions",
                    ResponseBuilder.fromArrayList(responseObjects)));
        });
        
        get("/accounts/:accountId/", (request, response) -> {
            int accountId = -1;
            try {
                accountId = Integer.parseInt(request.params(":accountId"));
            } catch (NumberFormatException ex) {
                ResponseBuilder.errorHalt(response, 400, "Account ID must be a number");
            }
            Auth.enforceAccountPermission(request, response, accountId, Auth.PERMISSION_READ);
            
            DynaBean account = null;
            try (Database database = new Database(ConnectionManager.getPooledConnection())) {
                try {
                    account = database.getAccount(accountId, Auth.getCurrentUserId(request));
                } catch (ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "Account not found");
                }
            }
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("account", ResponseBuilder.objectOf(
                        "account_metadata", (String)account.get("account_metadata"),
                        "encrypted_aes_key", (String)account.get("encrypted_aes_key")
            )));            
        });
        
        get("/accounts/:accountId/password/", (request, response) -> {
            int accountId = -1;
            try {
                accountId = Integer.parseInt(request.params(":accountId"));
            } catch (NumberFormatException ex) {
                ResponseBuilder.errorHalt(response, 400, "Account ID must be a number");
            }
            Auth.enforceAccountPermission(request, response, accountId, Auth.PERMISSION_READ);
            
            DynaBean account = null;
            try (Database database = new Database(ConnectionManager.getPooledConnection())) {
                try {
                    account = database.getAccount(accountId, Auth.getCurrentUserId(request));
                } catch (ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "Account not found");
                }
            }
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("password", ResponseBuilder.objectOf(
                        "encrypted_password", (String)account.get("password"),
                        "encrypted_aes_key", (String)account.get("encrypted_aes_key")
            )));    
        });
        
        get("/validate/", (request, response) -> {
            try {
                RequestJson.validateSchema(
                        "{\"type\": \"object\",\"properties\": {\"firstName\": {\"type\": \"string\", \"minLength\": 1},\"lastName\": {\"type\": \"string\"}},\"required\": [\"firstName\"]}",
                        "{\"firstName\":\"\", \"lastName\":\"bar\"}");
            } catch (JSONValidationException ex) {
                return (ex.getMessage());
            }
            
            return "Great success!";
        });
        
        //TODO - Disable this in production!
        spark.debug.DebugScreen.enableDebugScreen();
    }
}
