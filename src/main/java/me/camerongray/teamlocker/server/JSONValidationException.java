/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;

/**
 *
 * @author camerong
 */
public class JSONValidationException extends Exception {

    JSONValidationException(String message) {
        super(message);
    }
    
    JSONValidationException(Exception ex) {
        super(ex);
    }
}
