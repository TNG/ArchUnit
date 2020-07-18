package com.tngtech.archunit.core.importer.testexamples.classhierarchyimport;

public class Subclass extends BaseClass implements Subinterface {
    static {
        System.out.println(Subclass.class.getSimpleName() + " initializing");
    }

    private int intField;

    public Subclass() {
        intField = 1;
    }

    public Subclass(String someField) {
        super(someField + "by" + Subclass.class.getSimpleName());
        intField = 1;
    }

    public Subclass(String someField, int addition) {
        this(someField + addition);
        this.intField = addition;
    }

    @Override
    public String getSomeField() {
        return super.getSomeField();
    }

    public void subclassSay() {
        System.out.println(getSomeField());
    }

    public void setIntField(int intField) {
        this.intField = intField;
    }
}
