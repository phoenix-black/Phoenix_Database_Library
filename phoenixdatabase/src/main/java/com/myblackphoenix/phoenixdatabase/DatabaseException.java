package com.myblackphoenix.phoenixdatabase;

/**
 * Created by Praba on 9/3/2017.
 */
public class DatabaseException extends Exception {
    public DatabaseException(String message){
        super("DatabaseException: "+message);
    }
}
