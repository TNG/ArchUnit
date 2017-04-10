package com.tngtech.archunit.core.importer.testexamples.classhierarchyimport;

public class BaseClass implements OtherInterface {
    static {
        System.out.println(BaseClass.class.getSimpleName() + " initializing");
    }

    private String someField;

    public BaseClass() {
        someField = "default";
    }

    public BaseClass(String someField) {
        this.someField = someField;
    }

    protected String getSomeField() {
        return someField;
    }

    public void baseClassSay() {
        System.out.println(someField);
    }
}
