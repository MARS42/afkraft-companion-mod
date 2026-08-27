package com.mars4two.ikang.core.util;


public class PrimitiveJsonUtils {

    private PrimitiveJsonUtils() {
        // NO-OP
    }

    public static String key(String key, String value, boolean sep){
        return "\"" + key + "\": \"" + value + "\"" + (sep ? ", " : "");
    }

    public static String key(String key, int value, boolean sep){
        return "\"" + key + "\": " + value + (sep ? ", " : "");
    }

    public static String key(String key, boolean value, boolean sep){
        return "\"" + key + "\": " + value + (sep ? ", " : "");
    }
}
