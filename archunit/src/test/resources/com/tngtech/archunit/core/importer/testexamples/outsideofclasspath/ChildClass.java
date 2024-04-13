package com.tngtech.archunit.core.importer.testexamples.outsideofclasspath;

import java.io.Serializable;

public class ChildClass extends MiddleClass {
    private final String parameter;
    private final Object addon;
    public String someField;
    private final ExistingDependency existingDependency = new ExistingDependency();

    public ChildClass(String parameter, Object addon) {
        this.parameter = parameter;
        this.addon = addon;
    }

    @Override
    public void overrideMe() {
        existingDependency.init(new MySeed(parameter));
    }

    private class MySeed implements Serializable, ExistingDependency.GimmeADescription {
        private final String description;

        public MySeed(String description) {
            this.description = description;
        }

        @Override
        public String gimme() {
            return description;
        }
    }
}
