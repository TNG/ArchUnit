package com.tngtech.archunit.visual.testjson.complexInheritStructure;

public class Class1 implements Interface1, Interface2 {
    private String s;

    public Class1(String s) {
        this.s = s;
    }

    @Override
    public void sayHello() {
        com.tngtech.archunit.visual.testjson.simpleInheritStructure.Class1 c = new com.tngtech.archunit.visual.testjson.simpleInheritStructure.Class1(this.s);
        c.sayHi();
    }

    public void sayHi() {
        com.tngtech.archunit.visual.testjson.simpleInheritStructure.Class1 c = new com.tngtech.archunit.visual.testjson.simpleInheritStructure.Class1(this.s);
        c.sayHello();
    }
}
