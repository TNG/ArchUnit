package com.tngtech.archunit.visual;

import com.google.gson.annotations.Expose;
import com.tngtech.archunit.core.Optional;

import java.util.Set;

abstract class JsonElement {
    @Expose
    protected String name;
    @Expose
    protected String fullname;
    @Expose
    protected String type;

    JsonElement(String name, String fullname, String type) {
        this.name = name;
        this.fullname = fullname;
        this.type = type;
    }

    String getPath() {
        String res = fullname.substring(0, fullname.length() - name.length() - 1);
        return res;
    }

    abstract Set<? extends JsonElement> getChildren();

    Optional<? extends JsonElement> getChild(String fullnameChild) {
        if (fullname.equals(fullnameChild)) {
            return Optional.of(this);
        }
        for (JsonElement el : getChildren()) {
            if (fullnameChild.startsWith(el.fullname)) {
                return el.getChild(fullnameChild);
            }
        }
        return Optional.absent();
    }

    abstract void insertJavaElement(JsonJavaElement el);
}
