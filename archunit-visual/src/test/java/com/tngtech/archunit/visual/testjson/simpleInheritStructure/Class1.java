package com.tngtech.archunit.visual.testjson.simpleInheritStructure;

public class Class1 implements Interface1 {
    private String s;

    public Class1(String s) {
        this.s = s;
    }

    @Override
    public void sayHello() {
        System.out.println(this.s);
    }

    public void sayHi() {
        System.out.println(this.s);
    }
}
