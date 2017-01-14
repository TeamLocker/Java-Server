/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.sql.SQLException;

/**
 *
 * @author camerong
 */
public class NullTransaction implements TransactionInterface {  
    @Override
    public final void updateLastUsed() {}
    
    @Override
    public void commit() {}
    
    @Override
    public void rollback() {}
}
