/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

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
    }
    
    public static Transaction getTransaction() throws SQLException {
        Transaction transaction = new Transaction();
        instance.transactionMap.put(transaction.getId(), transaction);
        return transaction;
    }
    
    public static Transaction getTransaction(String transactionId) {
        return instance.transactionMap.get(transactionId);
    }
}
