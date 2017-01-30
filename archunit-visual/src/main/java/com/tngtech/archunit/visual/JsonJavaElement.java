package com.tngtech.archunit.visual;

import java.util.HashSet;
import java.util.Set;

class JsonJavaElement {
    private String name;
    private String fullname;
    private String type;
    private Set<JsonJavaElement> children = new HashSet<>();

    JsonJavaElement(String name, String fullname, String type) {
        this.name = name;
        this.fullname = fullname;
        this.type = type;
    }

    void addChild(JsonJavaElement el) {
        this.children.add(el);
    }
}
