package com.tngtech.archunit.core.importer.testexamples.integration;

public class ClassXDependingOnClassesABCD extends ClassCDependingOnClassB_SuperClassOfX implements InterfaceOfClassX {
    private ClassA classA;
    private ClassBDependingOnClassA classB;

    public ClassXDependingOnClassesABCD() {
        super(1, 2, 3);
        this.classA = new ClassA("init");
    }

    void callDependentA(String someArg) {
        classA.setState(someArg);
    }

    void changeDependantA() {
        classA.accessibleField = 10;
    }

    public String getStateFromAViaB() {
        return classB.getStateFromA();
    }

    public int getSuperStateModified() {
        return super.getState() + 6;
    }

    void setGlobalState(String state) {
        ClassD.globalState = state;
    }
}
