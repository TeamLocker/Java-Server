/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author camerong
 */
public class TransactionStore {
    private static TransactionStore instance;
    private ConcurrentHashMap<String, Transaction> transactionMap = new ConcurrentHashMap<>();
    protected TransactionStore() {
        // Prevent instantiation
    }
    
    public static void initialise() {
        instance = new TransactionStore();
        instance.startStaleTransactionCollector();
    }
    
    public static Transaction getTransaction() throws SQLException {
        Transaction transaction = new Transaction(ConnectionManager.getNewConnection().getConnection());
        instance.transactionMap.put(transaction.getId(), transaction);
        return transaction;
    }
    
    public static Transaction getTransaction(String transactionId) throws ObjectNotFoundException {
        Transaction transaction = instance.transactionMap.get(transactionId);
        transaction.updateLastUsed();
        if (transaction == null) {
            throw new ObjectNotFoundException();
        }
        return transaction;
    }
    
    public static void forgetTransaction(String transactionId) {
        instance.transactionMap.remove(transactionId);
    }
    
    public Thread startStaleTransactionCollector() {
        final int COLLECTION_INTERVAL_MILLIS = 1000;
        
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        TransactionStore.rollbackStaleTransactions();
                        Thread.sleep(COLLECTION_INTERVAL_MILLIS);
                    } catch (SQLException ex) {
                        Logger.getLogger(TransactionStore.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TransactionStore.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } 
        });
        
        t.start();
        
        return t;
    }
    
    public static void rollbackStaleTransactions() throws SQLException {
        final int MAX_AGE_SECONDS = 20;
        
        long currentTime;
        for (Transaction transaction : instance.transactionMap.values()) {
            currentTime = Instant.now().getEpochSecond();
            if (currentTime - transaction.getLastUsed() > MAX_AGE_SECONDS) {
                System.out.println("Rolling back stale transaction - " + transaction.getId());
                transaction.rollback();
                transaction.getWrappedConnection().getConnection().close();
                TransactionStore.forgetTransaction(transaction.getId());
            }
        }
    }
}
