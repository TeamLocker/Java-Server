/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.camerongray.teamlocker.server;
import static spark.Spark.*;

/**
 *
 * @author camerong
 */
public class Server {
    public static void main(String[] args) {
        before((request, response) -> {
           // Check authentication here
        });
        
        get("/", (request, response) -> {
            return Response.build(response, Response.objectOf(
                    "success", true,
                    "message", "Thing happened",
                    "numbers", Response.arrayOf(1,2,3)));
        });
    }
}
