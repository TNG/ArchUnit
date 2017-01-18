package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.lang.Priority;

public class SlicesRuleDefinition {
    public static Creator allSlices() {
        return new Creator();
    }

    public static class Creator {
        private Creator() {
        }

        public GivenSlices matching(String packageIdentifier) {
            return new GivenSlices(Priority.MEDIUM, Slices.matching(packageIdentifier));
        }
    }
}
