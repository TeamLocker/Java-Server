/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;
import com.google.common.io.Resources;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.DynaBean;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
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
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
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
        
        get("/users/:userId/encrypted_aes_keys/", (request, response) -> {
            int userId;
            if (request.params(":userId").equals("self")) {
                userId = Auth.getCurrentUserId(request);
            } else {
                Auth.enforceAdmin(request, response);
                userId = Integer.parseInt(request.params(":userId"));
            }
            
            List<DynaBean> accountData;
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                accountData = database.getUserAccountData(userId);
            }
            
            ArrayList<JSONObject> aesKeyObjects = new ArrayList<>();
            for (DynaBean accountDataItem : accountData) {
                aesKeyObjects.add(ResponseBuilder.objectOf(
                    "account_id", (int)accountDataItem.get("account_id"),
                    "encrypted_aes_key", (String)accountDataItem.get("encrypted_aes_key")
                ));
            }
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf(
                    "encrypted_aes_keys", ResponseBuilder.fromArrayList(aesKeyObjects)));
        });
        
        get("/users/", (request, response) -> {
            Auth.enforceAdmin(request, response);
            ArrayList<JSONObject> userObjects = new ArrayList<>();
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
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
        
        put("/users/", (request, response) -> {
            JSONObject requestJson = null;
            try {
                requestJson = RequestJson.getValidated(request, "putUsers");
            } catch (JSONValidationException ex) {
                // TODO: Friendly error messages for JSONValidationExceptions rather than raw output from validation library
                ResponseBuilder.errorHalt(response, 400, ex.getMessage());
            }
            
            int userId = -1;
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                userId = database.addUser(
                        requestJson.getString("full_name"),
                        requestJson.getString("username"),
                        requestJson.getString("email"),
                        BCrypt.hashpw(requestJson.getString("auth_key"), BCrypt.gensalt()),
                        requestJson.getString("encrypted_private_key"),
                        requestJson.getString("public_key"),
                        requestJson.getBoolean("admin"),
                        requestJson.getString("pbkdf2_salt"),
                        requestJson.getString("aes_iv")
                );
            }
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("user_id", userId));
        });
        
        get("/folders/", (request, response) -> {
            ArrayList<JSONObject> folderObjects = new ArrayList<>();
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
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
        
        put("/folders/", (request, response) -> {
            JSONObject requestJson = null;
            try {
                requestJson = RequestJson.getValidated(request, "putFolder");
            } catch (JSONValidationException ex) {
                // TODO: Friendly error messages for JSONValidationExceptions rather than raw output from validation library
                ResponseBuilder.errorHalt(response, 400, ex.getMessage());
            }
            Auth.enforceAdmin(request, response);
            
            int folderId = -1;
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                try {
                    database.getFolder(requestJson.getString("name"));
                    ResponseBuilder.errorHalt(response, 409, "A folder with that name already exists");
                } catch(ObjectNotFoundException ex) {
                    // We don't care if it doesn't exist, we actually want this exception to be thrown!
                }
                
                folderId = database.addFolder(requestJson.getString("name"));
            }
                        
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("folder_id", folderId));
        });
        
        post("/folders/:folderId/", (request, response) -> {
            int folderId = -1;
            try {
                folderId = Integer.parseInt(request.params(":folderId"));
            } catch (NumberFormatException ex) {
                ResponseBuilder.errorHalt(response, 400, "Folder ID must be a number");
            }
            Auth.enforceFolderPermission(request, response, folderId, Auth.PERMISSION_WRITE);
            
            JSONObject requestJson = null;
            try {
                requestJson = RequestJson.getValidated(request, "postFolders");
            } catch (JSONValidationException ex) {
                // TODO: Friendly error messages for JSONValidationExceptions rather than raw output from validation library
                ResponseBuilder.errorHalt(response, 400, ex.getMessage());
            }
            
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                try {
                    database.updateFolder(folderId, requestJson.getString("name"));
                } catch (ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "Folder not found");
                }
            }
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("success", true));
        });
        
        // TODO - Replace with generic update user method
        post("/users/self/update_password/", (request, response) -> {
            JSONObject requestJson = null;
            try {
                requestJson = RequestJson.getValidated(request, "postUsersUpdatePassword");
            } catch (JSONValidationException ex) {
                // TODO: Friendly error messages for JSONValidationExceptions rather than raw output from validation library
                ResponseBuilder.errorHalt(response, 400, ex.getMessage());
            }
            
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                try {
                    database.updateUserPassword(
                            Auth.getCurrentUserId(request),
                            requestJson.getString("encrypted_private_key"),
                            requestJson.getString("aes_iv"),
                            requestJson.getString("pbkdf2_salt"),
                            BCrypt.hashpw(requestJson.getString("auth_key"), BCrypt.gensalt())
                    );
                } catch (ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "User not found");
                }
            }
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("success", true));
        });
        
        delete("/folders/:folderId/", (request, response) -> {
            int folderId = -1;
            try {
                folderId = Integer.parseInt(request.params(":folderId"));
            } catch (NumberFormatException ex) {
                ResponseBuilder.errorHalt(response, 400, "Folder ID must be a number");
            }
            Auth.enforceFolderPermission(request, response, folderId, Auth.PERMISSION_WRITE);
            
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                try {
                    database.deleteFolder(folderId);
                } catch (ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "Folder not found");
                }
            }
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("success", true));
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
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
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
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
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
        
        post("/folders/:folderId/permissions/", (request, response) -> {
            Auth.enforceAdmin(request, response);
            int folderId = -1;
            try {
                folderId = Integer.parseInt(request.params(":folderId"));
            } catch(NumberFormatException ex) {
                ResponseBuilder.errorHalt(response, 400, "Folder ID must be a number");
            }
            
            JSONObject requestJson = RequestJson.getValidated(request, "postFoldersPermissions");
            
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                try {
                    database.getFolder(folderId);
                } catch (ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "Folder not found!");
                }
                Transaction transaction = new Transaction(database.getConnection());
                
                try {
                    database.deleteFolderPermissions(folderId);

                    JSONArray permissions = requestJson.getJSONArray("permissions");
                    for (int i = 0; i < permissions.length(); i++) {
                        JSONObject permission = permissions.getJSONObject(i);
                        int userId = permission.getInt("user_id");
                        boolean read = permission.getBoolean("read");
                        boolean write = permission.getBoolean("write");
                        try {
                            DynaBean user = database.getUser(userId);
                            if ((boolean)user.get("admin")) {
                                transaction.rollback();
                                ResponseBuilder.errorHalt(response, 400, "Trying to set permissions "
                                        + "for an administrator, administrators already have full permission.");
                            }
                        } catch (ObjectNotFoundException ex) {
                            transaction.rollback();
                            ResponseBuilder.errorHalt(response, 404, "User not found!");
                        }
                        
                        if (write && !read) {
                            transaction.rollback();
                            ResponseBuilder.errorHalt(response, 400, "Users must be able "
                                    + "to read a folder if they are to write to it");
                        }
                        
                        if (!(write || read)) {
                            database.deleteAccountDataForFolder(folderId, userId);
                        } else {
                            database.addPermission(
                                    folderId,
                                    userId,
                                    read,
                                    write
                            );
                        }
                    }
                } catch (SQLException ex) {
                    transaction.rollback();
                    ResponseBuilder.errorHalt(response, 500, "Error updating permissions - " + ex);
                }
                transaction.commit();
            }
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("success", true));
        });
        
        get("/folders/:folderId/public_keys/", (request, response) -> {
            int folderId = -1;
            try {
                folderId = Integer.parseInt(request.params(":folderId"));
            } catch (NumberFormatException ex) {
                ResponseBuilder.errorHalt(response, 400, "Folder ID must be a number");
            }
            Auth.enforceFolderPermission(request, response, folderId, Auth.PERMISSION_WRITE);

            List<DynaBean> users;
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                users = database.getFolderUsers(folderId);
            }
            
            ArrayList<JSONObject> publicKeyObjects = new ArrayList<>();
            for(DynaBean user : users) {
                publicKeyObjects.add(ResponseBuilder.objectOf(
                        "user_id", (int)user.get("id"),
                        "public_key", (String)user.get("public_key")
                ));
            }
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("public_keys",
                    ResponseBuilder.fromArrayList(publicKeyObjects)));
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
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                try {
                    account = database.getAccountData(accountId, Auth.getCurrentUserId(request));
                } catch (ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "Account not found");
                }
            }
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("account", ResponseBuilder.objectOf(
                        "account_metadata", (String)account.get("account_metadata"),
                        "encrypted_aes_key", (String)account.get("encrypted_aes_key")
            )));            
        });
        
        delete("/accounts/:accountId/", (request, response) -> {
            int accountId = -1;
            try {
                accountId = Integer.parseInt(request.params(":accountId"));
            } catch (NumberFormatException ex) {
                ResponseBuilder.errorHalt(response, 400, "Account ID must be a number");
            }
            Auth.enforceAccountPermission(request, response, accountId, Auth.PERMISSION_WRITE);
            
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                try {
                    database.deleteAccount(accountId);
                } catch (ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "Account not found");
                }
            }
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("success", true));
        });
        
        post("/accounts/:accountId/", (request, response) -> {
            JSONObject requestJson = null;
            try {
                requestJson = RequestJson.getValidated(request, "postAccountsSingle");
            } catch (JSONValidationException ex) {
                // TODO: Friendly error messages for JSONValidationExceptions rather than raw output from validation library
                ResponseBuilder.errorHalt(response, 400, "JSON Validation Error - " + ex.getMessage());
            }
            
            int accountId = -1;
            try {
                accountId = Integer.parseInt(request.params(":accountId"));
            } catch (NumberFormatException ex) {
                ResponseBuilder.errorHalt(response, 400, "Account ID must be a number");
            }
            
            Auth.enforceAccountPermission(request, response, accountId, Auth.PERMISSION_WRITE);
            
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                try {
                    database.getAccount(accountId);
                } catch(ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "Account not found");
                }
                try {
                    database.getFolder(requestJson.getInt("folder_id"));
                } catch(ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "Folder not found");
                }
                
                Transaction transaction = new Transaction(database.getConnection());
                try {
                    database.updateAccount(accountId, requestJson.getInt("folder_id"));

                    JSONArray accountDataItems = requestJson.getJSONArray("encrypted_account_data");
                    for (int i = 0; i < accountDataItems.length(); i++) {
                        JSONObject accountDataItem = accountDataItems.getJSONObject(i);
                        int userId = accountDataItem.getInt("user_id");
                        database.deleteAccountData(accountId, userId);
                        database.addAccountDataItem(
                                accountId,
                                userId,
                                accountDataItem.getString("account_metadata"),
                                accountDataItem.getString("password"),
                                accountDataItem.getString("encrypted_aes_key")
                        );
                    }
                } catch (SQLException ex) {
                    transaction.rollback();
                    ResponseBuilder.errorHalt(response, 500, "Error saving account - " + ex);
                } catch (ObjectNotFoundException ex) {
                    transaction.rollback();
                    ResponseBuilder.errorHalt(response, 404, "Error saving account - Object Not Found");
                }
                
                transaction.commit();
            }
                        
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("success", true));
        });
        
        post("/accounts/", (request, response) -> {
            JSONObject requestJson = RequestJson.getValidated(request, "postAccountsBatch");
            JSONArray accounts = requestJson.getJSONArray("accounts");

            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                Transaction transaction = new Transaction(database.getConnection());
                try {
                    for (int i = 0; i < accounts.length(); i++) {
                        JSONObject account = accounts.getJSONObject(i);
                        int accountId = account.getInt("account_id");
                        try {
                            if (!Auth.getAccountPermission(request, response, accountId, Auth.PERMISSION_WRITE)) {
                                transaction.rollback();
                                ResponseBuilder.errorHalt(response, 403, "You do not have write permission for account " + accountId);
                            }
                        } catch (ObjectNotFoundException ex) {
                            transaction.rollback();
                            ex.printStackTrace();
                            ResponseBuilder.errorHalt(response, 404, "Account not found");
                        }

                        JSONArray accountDataItems = account.getJSONArray("encrypted_account_data");
                        for (int j = 0; j < accountDataItems.length(); j++) {
                            JSONObject accountDataItem = accountDataItems.getJSONObject(j);
                            int userId = accountDataItem.getInt("user_id");
                            database.deleteAccountData(accountId, userId);
                            database.addAccountDataItem(
                                    accountId,
                                    userId,
                                    accountDataItem.getString("account_metadata"),
                                    accountDataItem.getString("password"),
                                    accountDataItem.getString("encrypted_aes_key")
                            );
                        }
                    }
                } catch (SQLException ex) {
                    transaction.rollback();
                    ResponseBuilder.errorHalt(response, 500, "Error saving accounts - " + ex);
                }
                transaction.commit();
            }
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("success", true));
        });
        
        put("/accounts/", (request, response) -> {
            JSONObject requestJson = null;
            try {
                requestJson = RequestJson.getValidated(request, "putAccounts");
            } catch (JSONValidationException ex) {
                // TODO: Friendly error messages for JSONValidationExceptions rather than raw output from validation library
                ResponseBuilder.errorHalt(response, 400, ex.getMessage());
            }
            Auth.enforceFolderPermission(request, response, requestJson.getInt("folder_id"), Auth.PERMISSION_WRITE);
            
            int accountId = -1;
            WrappedConnection connection = ConnectionManager.getConnection(request);
            try (Database database = new Database(connection)) {
                try {
                    database.getFolder(requestJson.getInt("folder_id"));
                } catch(ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "Folder not found");
                }
                Transaction transaction = new Transaction(connection);
                
                try {
                    accountId = database.addAccount(requestJson.getInt("folder_id"));
                    JSONArray accountDataItems = requestJson.getJSONArray("encrypted_account_data");
                    for (int i = 0; i < accountDataItems.length(); i++) {
                        JSONObject accountDataItem = accountDataItems.getJSONObject(i);
                        database.addAccountDataItem(
                                accountId,
                                accountDataItem.getInt("user_id"),
                                accountDataItem.getString("account_metadata"),
                                accountDataItem.getString("password"),
                                accountDataItem.getString("encrypted_aes_key")
                        );
                    }
                } catch (SQLException ex) {
                    transaction.rollback();
                    ResponseBuilder.errorHalt(response, 500, "Error adding account - " + ex);
                }
                
                transaction.commit();
            }
                        
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("account_id", accountId));
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
            try (Database database = new Database(ConnectionManager.getConnection(request))) {
                try {
                    account = database.getAccountData(accountId, Auth.getCurrentUserId(request));
                } catch (ObjectNotFoundException ex) {
                    ResponseBuilder.errorHalt(response, 404, "Account not found");
                }
            }
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("password", ResponseBuilder.objectOf(
                        "encrypted_password", (String)account.get("password"),
                        "encrypted_aes_key", (String)account.get("encrypted_aes_key")
            )));    
        });
        
        put("/transaction/", (request, response) -> {
            Transaction transaction = TransactionStore.getTransaction();
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("transaction_id", transaction.getId()));
        });
        
        post("/transaction/:transactionId/commit/", (request, response) -> {
            String transactionId = request.params(":transactionId");
            
            try {
                Transaction transaction = TransactionStore.getTransaction(transactionId);
                transaction.commit();
                transaction.getWrappedConnection().getConnection().close();
                TransactionStore.forgetTransaction(transactionId);
            } catch (ObjectNotFoundException ex) {
                ResponseBuilder.errorHalt(response, 404, "Transaction not found!");
            }
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("success", true));
        });
        
        post("/transaction/:transactionId/rollback/", (request, response) -> {
            String transactionId = request.params(":transactionId");
            
            try {
                Transaction transaction = TransactionStore.getTransaction(transactionId);
                transaction.rollback();
                transaction.getWrappedConnection().getConnection().close();
                TransactionStore.forgetTransaction(transactionId);
            } catch (ObjectNotFoundException ex) {
                ResponseBuilder.errorHalt(response, 404, "Transaction not found!");
            }
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("success", true));
        });
        
        get("/transaction_test/", (request, response) -> {
            Transaction transaction = TransactionStore.getTransaction();
            
            
            
            return ResponseBuilder.build(response, ResponseBuilder.objectOf("transaction_id", transaction.getId()));
        });
        
        exception(Exception.class, (e, request, response) -> {
            System.out.println("An unhandled exception occurred!");
            System.out.println(e.toString());
            e.printStackTrace();
            response.status(500);
            response.type("application/json");
            response.body(ResponseBuilder.objectOf(
                "error", true,
                "message", "An unhandled server error occurred! - " + e.toString()
            ).toString());
        });
        
        //TODO - Disable this in production!
//        spark.debug.DebugScreen.enableDebugScreen();
    }
}
