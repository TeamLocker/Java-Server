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
    private String value = "foo";
    public static void main(String[] args) {
        TransactionStore.initialise();
        
        before((request, response) -> {
           // Check authentication here
        });
        
        get("/", (request, response) -> {
            return Response.build(response, Response.objectOf(
                    "success", true,
                    "message", "Thing happened",
                    "numbers", Response.arrayOf(1,2,3)));
        });
        
        get("/new/", (request, response) -> {
            Transaction transaction = TransactionStore.getTransaction();
            return Response.build(response, Response.objectOf("id", transaction.getId()));
        });
        
        get("/existing/", (request, response) -> {
            System.out.println(request.queryParams("id"));
            Transaction transaction = TransactionStore.getTransaction(request.queryParams("id"));
            return Response.build(response, Response.objectOf("accesses", transaction.access()));
        });
        
        //TODO - Disable this in production!
        spark.debug.DebugScreen.enableDebugScreen();
    }
}
