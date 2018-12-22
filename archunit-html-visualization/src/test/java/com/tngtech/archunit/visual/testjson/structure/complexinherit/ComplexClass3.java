package com.tngtech.archunit.visual.testjson.structure.complexinherit;

public class ComplexClass3 extends ComplexClass2 {
    public ComplexClass3(String name) {
        super(name);
    }

    @Override
    public void sayHelloAndBye() {
        super.sayHelloAndBye();
    }
}
