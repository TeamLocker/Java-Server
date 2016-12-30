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
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.RowSetDynaClass;

/**
 *
 * @author camerong
 */
public class Database implements AutoCloseable {
    private Connection connection;
    private PreparedStatement stmt;
    private ResultSet rs;

    public Database(Connection connection) {
        this.connection = connection;
    }
    
    public DynaBean getUser(String username) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement("SELECT * FROM users WHERE username=?");
        this.stmt.setString(1, username);
        this.rs = stmt.executeQuery();
        return this.objectFromRS(rs);
    }
    
    public DynaBean getUser(int userId) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement("SELECT * FROM users WHERE id=?");
        this.stmt.setInt(1, userId);
        this.rs = stmt.executeQuery();
        return this.objectFromRS(rs);
    }
    
    public List<DynaBean> getAllUsers() throws SQLException {
        this.stmt = this.connection.prepareStatement("SELECT * FROM users");
        this.rs = stmt.executeQuery();
        return this.listFromRS(rs);
    }
    
    public DynaBean getFolder(int folderId) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement("SELECT * FROM folders WHERE id=?");
        this.stmt.setInt(1, folderId);
        this.rs = stmt.executeQuery();
        return this.objectFromRS(this.rs);
    }
    
    public DynaBean getFolder(String folderName) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement("SELECT * FROM folders WHERE name=?");
        this.stmt.setString(1, folderName);
        this.rs = stmt.executeQuery();
        return this.objectFromRS(this.rs);
    }

    public List<DynaBean> getFolders(int userId) throws SQLException, ObjectNotFoundException {
        boolean isAdmin = (boolean)this.getUser(userId).get("admin");
        
        if (isAdmin) {
            this.stmt = this.connection.prepareStatement(""
                    + "SELECT id, name, true as read, true as write FROM folders");
        } else {
            this.stmt = this.connection.prepareStatement(""
                    + "SELECT f.id, f.name, p.read, p.write "
                    + "FROM folders as f, users as u, permissions as p "
                    + "WHERE p.user_id=u.id "
                        + "AND p.folder_id=f.id "
                        + "AND u.id=?");
            this.stmt.setInt(1, userId);
        }
        
        this.rs = stmt.executeQuery();
        return this.listFromRS(rs);
    }
    
    public List<DynaBean> getFolderAccounts(int folderId, int userId) throws SQLException {
        this.stmt = this.connection.prepareStatement(""
                + "SELECT * "
                + "FROM account_data "
                + "WHERE user_id=? "
                + " AND account_id IN ( "
                + "  SELECT id "
                + "  FROM accounts "
                + "  WHERE folder_id=? "
                + " )");
        this.stmt.setInt(1, userId);
        this.stmt.setInt(2, folderId);
        this.rs = stmt.executeQuery();
        return this.listFromRS(this.rs);
    }
    
    public DynaBean getFolderPermissions(int folderId, int userId) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement(""
                + "SELECT * FROM permissions WHERE folder_id=? AND user_id=?");
        this.stmt.setInt(1, folderId);
        this.stmt.setInt(2, userId);
        this.rs = stmt.executeQuery();
        return this.objectFromRS(this.rs);
    }
    
    public void deleteFolderPermissions(int folderId) throws SQLException {
        this.stmt = this.connection.prepareStatement("DELETE FROM permissions WHERE folder_id=?");
        this.stmt.setInt(1, folderId);
        this.stmt.executeUpdate();
    }
    
    public List<DynaBean> getFolderPermissions(int folderId) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement(""
                + "SELECT * FROM permissions WHERE folder_id=?");
        this.stmt.setInt(1, folderId);
        this.rs = stmt.executeQuery();
        return this.listFromRS(this.rs);
    }
    
    public int addPermission(int folderId, int userId, boolean read, boolean write) throws SQLException {
        this.stmt = this.connection.prepareStatement(""
                + "INSERT INTO permissions (folder_id, user_id, read, write) VALUES (?, ?, ?, ?) RETURNING id");
        this.stmt.setInt(1, folderId);
        this.stmt.setInt(2, userId);
        this.stmt.setBoolean(3, read);
        this.stmt.setBoolean(4, write);
        this.rs = this.stmt.executeQuery();
        return this.idFromRS(this.rs);
    }
    
    public DynaBean getAccountData(int accountId, int userId) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement(""
                + "SELECT * FROM account_data WHERE account_id=? AND user_id=?;");
        this.stmt.setInt(1, accountId);
        this.stmt.setInt(2, userId);
        this.rs = stmt.executeQuery();
        return this.objectFromRS(this.rs);
    }
    
    public int getAccount(int accountId) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement(""
                + "SELECT folder_id FROM accounts WHERE id=?;");
        this.stmt.setInt(1, accountId);
        this.rs = stmt.executeQuery();
        return (int)this.objectFromRS(this.rs).get("folder_id");
    }
    
    public List<DynaBean> getFolderUsers(int folderId) throws SQLException {
        this.stmt = this.connection.prepareStatement(""
                + "SELECT * "
                + "FROM users "
                + "WHERE id IN( "
                + " ((SELECT user_id FROM permissions WHERE folder_id=? AND read=true) "
                + "     UNION (SELECT id FROM users WHERE admin=true))) AND"
                + " ? IN (SELECT id FROM folders)");
        this.stmt.setInt(1, folderId);
        this.stmt.setInt(2, folderId);
        this.rs = stmt.executeQuery();
        return this.listFromRS(this.rs);
    }
    
    public int addFolder(String folderName) throws SQLException {
        this.stmt = this.connection.prepareStatement("INSERT INTO folders (name) VALUES (?) RETURNING id;");
        this.stmt.setString(1, folderName);
        this.rs = this.stmt.executeQuery();
        return this.idFromRS(this.rs);
    }
    
    public int addAccount(int folderId) throws SQLException {
        this.stmt = this.connection.prepareStatement("INSERT INTO accounts (folder_id) VALUES (?) RETURNING id;");
        this.stmt.setInt(1, folderId);
        this.rs = this.stmt.executeQuery();
        return this.idFromRS(this.rs);
    }
    
    public void updateAccount(int accountId, int folderId) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement("UPDATE accounts SET (folder_id) = (?) WHERE id=?;");
        this.stmt.setInt(1, folderId);
        this.stmt.setInt(2, accountId);
        int affectedRows = this.stmt.executeUpdate();
        if (affectedRows == 0) {
            throw new ObjectNotFoundException();
        }
    }
    
    public int addAccountDataItem(int accountId, int userId, String accountMetadata, String password, String encryptedAesKey) throws SQLException {
        this.stmt = this.connection.prepareStatement(""
                + "INSERT INTO account_data "
                + " (account_id, user_id, account_metadata, password, encrypted_aes_key)"
                + "VALUES (?, ?, ?, ?, ?)"
                + "RETURNING id;");
        this.stmt.setInt(1, accountId);
        this.stmt.setInt(2, userId);
        this.stmt.setString(3, accountMetadata);
        this.stmt.setString(4, password);
        this.stmt.setString(5, encryptedAesKey);
        this.rs = this.stmt.executeQuery();
        return this.idFromRS(this.rs);
    }
    
    public void deleteAccount(int accountId) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement("DELETE FROM accounts WHERE id=?");
        this.stmt.setInt(1, accountId);
        int affectedRows = this.stmt.executeUpdate();
        if (affectedRows == 0) {
            throw new ObjectNotFoundException();
        }
    }
    
    public void deleteAccountData(int accountId) throws SQLException {
        this.stmt = this.connection.prepareStatement("DELETE FROM account_data WHERE account_id=?");
        this.stmt.setInt(1, accountId);
        this.stmt.executeUpdate();
    }
    
    public void deleteFolder(int folderId) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement("DELETE FROM folders WHERE id=?");
        this.stmt.setInt(1, folderId);
        int affectedRows = this.stmt.executeUpdate();
        if (affectedRows == 0) {
            throw new ObjectNotFoundException();
        }
    }
    
    public List<DynaBean> getUserAccountData(int userId) throws SQLException {
        this.stmt = this.connection.prepareStatement("SELECT * FROM account_data WHERE user_id=?");
        this.stmt.setInt(1, userId);
        this.rs = stmt.executeQuery();
        return this.listFromRS(this.rs);
    }
    
    private List<DynaBean> listFromRS(ResultSet rs) throws SQLException {
        return new RowSetDynaClass(rs).getRows();
    }
    
    private DynaBean objectFromRS(ResultSet rs) throws SQLException, ObjectNotFoundException {
        List<DynaBean> rows = this.listFromRS(rs);
        if (rows.isEmpty()) {
            throw new ObjectNotFoundException();
        }
        return rows.get(0);
    }
    
    private int idFromRS(ResultSet rs) throws SQLException {
        List<DynaBean> rows = this.listFromRS(rs);
        return (int)rows.get(0).get("id");
    }
    
    public Connection getConnection() {
        return this.connection;
    }
    
    public void close() {
        try {
            this.rs.close();
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.FINE, "Unable to close ResultSet", ex);
        }
        
        try {
            this.stmt.close();
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.FINE, "Unable to close PreparedStatement", ex);
        }
        
        try {
            this.connection.close();
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.FINE, "Unable to close Connection", ex);
        }
    }
}
