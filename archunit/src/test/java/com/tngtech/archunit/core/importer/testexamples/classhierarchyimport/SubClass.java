package com.tngtech.archunit.core.importer.testexamples.classhierarchyimport;

public class SubClass extends BaseClass implements SubInterface {
    static {
        System.out.println(SubClass.class.getSimpleName() + " initializing");
    }

    private int intField;

    public SubClass() {
        intField = 1;
    }

    public SubClass(String someField) {
        super(someField + "by" + SubClass.class.getSimpleName());
        intField = 1;
    }

    public SubClass(String someField, int addition) {
        this(someField + addition);
        this.intField = addition;
    }

    @Override
    public String getSomeField() {
        return super.getSomeField();
    }

    public void subClassSay() {
        System.out.println(getSomeField());
    }

    public void setIntField(int intField) {
        this.intField = intField;
    }
}
