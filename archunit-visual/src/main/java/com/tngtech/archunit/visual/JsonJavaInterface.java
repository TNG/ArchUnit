package com.tngtech.archunit.visual;

class JsonJavaInterface extends JsonJavaElement {
    private static final String TYPE = "interface";
    JsonJavaInterface(String name, String fullname) {
        super(name, fullname, TYPE);
    }
}
