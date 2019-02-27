package com.cisco.adt.util;

public class Utils {

    public static Throwable getRootException(Throwable exception){
        Throwable rootException=exception;
        while(rootException.getCause()!=null){
            rootException = rootException.getCause();
        }
        return rootException;
    }

}
