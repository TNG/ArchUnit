package com.tngtech.archunit.visual;

import com.google.gson.annotations.Expose;

class JsonJavaClass extends JsonJavaElement {
    @Expose
    private String superclass;

    // FIXME: Can we use Formatters.ensureSimpleName() to derive name from fullName??
    // FiXME: Can't we just take JavaClass as input??
    // FIXME: Isn't type always 'class' for a JsonJavaClass? Why do we have to supply it from outside?
    JsonJavaClass(String name, String fullName, String type, String superclass) {
        super(name, fullName, type);
        this.superclass = superclass;
    }

    boolean directlyExtends(String fullName) {
        return superclass.equals(fullName);
    }
}
