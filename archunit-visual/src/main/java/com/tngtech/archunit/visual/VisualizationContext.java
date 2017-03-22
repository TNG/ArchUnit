package com.tngtech.archunit.visual;

import java.util.Set;

class VisualizationContext {
    private Set<String> basePkgs;


    public void setBasePkgs(Set<String> basePkgs) {
        this.basePkgs = basePkgs;
    }

    VisualizationContext() {
    }

    boolean isIncluded(String fullname) {
        for (String s : basePkgs) {
            if (fullname.startsWith(s)) {
                return true;
            }
        }
        return false;
    }
}
