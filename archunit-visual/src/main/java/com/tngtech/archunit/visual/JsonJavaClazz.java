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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JsonJavaClazz that = (JsonJavaClazz) o;

        return superclass != null ? superclass.equals(that.superclass) : that.superclass == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (superclass != null ? superclass.hashCode() : 0);
        return result;
    }
}
