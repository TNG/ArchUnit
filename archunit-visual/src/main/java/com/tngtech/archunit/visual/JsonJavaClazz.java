package com.tngtech.archunit.visual;

import com.google.gson.annotations.Expose;

class JsonJavaClazz extends JsonJavaElement {
    @Expose
    private String superclass;

    JsonJavaClazz(String name, String fullname, String type, String superclass) {
        super(name, fullname, type);
        this.superclass = superclass;
    }

    boolean hasAsSuperClass(String fullname) {
        return superclass.equals(fullname);
    }
}
