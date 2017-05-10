package com.tngtech.archunit.visual.testjson.structure.complexinherit;

import com.tngtech.archunit.visual.testjson.structure.simpleinherit.SimpleClass1;

public class ComplexClass1 implements ComplexInterface1, ComplexInterface2 {
    private String s;

    public ComplexClass1(String s) {
        this.s = s;
    }

    @Override
    public void sayHello() {
        SimpleClass1 c = new SimpleClass1(this.s);
        ComplexClass2 cc2 = new ComplexClass2("hi");
        c.sayHi();
    }

    public void sayHi() {
        SimpleClass1 c = new SimpleClass1(this.s);
        c.sayHello();
    }
}
