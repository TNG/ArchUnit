package com.tngtech.archunit.lang.conditions.testobjects;

public class TargetClass {
    public static final String publicStringField = "publicString";
    public static final String appendStringMethod = "appendString";
    public static final Class<?>[] appendStringParams = new Class<?>[]{String.class};
    public String publicString;

    public String appendString(String string) {
        return publicString + string;
    }
}
