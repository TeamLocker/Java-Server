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
        stmt.setString(1, username);
        this.rs = stmt.executeQuery();
        return this.objectFromRS(rs);
    }
    
    public DynaBean getUser(int userId) throws SQLException, ObjectNotFoundException {
        this.stmt = this.connection.prepareStatement("SELECT * FROM users WHERE id=?");
        stmt.setInt(1, userId);
        this.rs = stmt.executeQuery();
        return this.objectFromRS(rs);
    }
    
    public List<DynaBean> getAllUsers() throws SQLException {
        this.stmt = this.connection.prepareStatement("SELECT * FROM users");
        this.rs = stmt.executeQuery();
        return this.listFromRS(rs);
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
