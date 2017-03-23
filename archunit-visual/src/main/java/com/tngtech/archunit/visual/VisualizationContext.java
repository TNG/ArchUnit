package com.tngtech.archunit.visual;

import java.util.Set;

class VisualizationContext {
    private Set<String> basePkgs;
    private boolean ignoreAccessToSuperConstructor = false;

    void setBasePkgs(Set<String> basePkgs) {
        this.basePkgs = basePkgs;
    }

    public void setIgnoreAccessToSuperConstructor(boolean ignoreAccessToSuperConstructor) {
        this.ignoreAccessToSuperConstructor = ignoreAccessToSuperConstructor;
    }

    VisualizationContext() {
    }

    boolean isElementIncluded(String fullname) {
        for (String s : basePkgs) {
            if (fullname.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    boolean isDependencyIncluded(JsonJavaElement origin, String targetOwner, boolean constructorCall) {
        return !targetOwner.equals(origin.fullname) &&
                isElementIncluded(targetOwner) &&
                (!constructorCall || isIncludedConstructorCall(origin, targetOwner));
    }

    private boolean isIncludedConstructorCall(JsonJavaElement origin, String targetOwner) {
        return !ignoreAccessToSuperConstructor || !(origin instanceof JsonJavaClazz)
                || !((JsonJavaClazz) origin).hasAsSuperClass(targetOwner);
    }
}
