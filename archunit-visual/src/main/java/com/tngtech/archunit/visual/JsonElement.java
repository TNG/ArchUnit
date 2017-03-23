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
        return fullname.substring(0, fullname.length() - name.length() - 1);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonElement that = (JsonElement) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (fullname != null ? !fullname.equals(that.fullname) : that.fullname != null) return false;
        return type != null ? type.equals(that.type) : that.type == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (fullname != null ? fullname.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
