package com.tngtech.archunit.visual;

import java.util.Set;

class VisualizationContext {
    private Set<String> basePkgs;
    private boolean includeEverything = false;
    private boolean ignoreAccessToSuperConstructorFromConstructor = false;

    void setBasePkgs(Set<String> basePkgs) {
        this.basePkgs = basePkgs;
    }

    void setIgnoreAccessToSuperConstructorFromConstructor(boolean ignoreAccessToSuperConstructorFromConstructor) {
        this.ignoreAccessToSuperConstructorFromConstructor = ignoreAccessToSuperConstructorFromConstructor;
    }

    void setIncludeEverything() {
        this.includeEverything = true;
    }

    VisualizationContext() {
    }

    boolean isElementIncluded(String fullname) {
        if (includeEverything) {
            return true;
        }
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
        return !ignoreAccessToSuperConstructorFromConstructor || !(origin instanceof JsonJavaClazz)
                || !((JsonJavaClazz) origin).hasAsSuperClass(targetOwner);
    }
}
