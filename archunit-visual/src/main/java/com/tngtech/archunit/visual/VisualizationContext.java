package com.tngtech.archunit.visual;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class VisualizationContext {
    private Set<String> basePaths;

    public VisualizationContext(String... basePaths) {
        this.basePaths = new HashSet<>(Arrays.asList(basePaths));
    }

    public boolean isRelevant(String fullname) {
        for (String s : basePaths) {
            if (fullname.startsWith(s)) {
                return true;
            }
        }
        return false;
    }
}
