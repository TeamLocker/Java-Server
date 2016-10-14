/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

import java.util.UUID;

/**
 *
 * @author camerong
 */
public class Transaction {
    private String id;
    private int accesses = 0;

    public Transaction() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }
    
    public int access() {
        accesses++;
        return accesses;
    }
}
