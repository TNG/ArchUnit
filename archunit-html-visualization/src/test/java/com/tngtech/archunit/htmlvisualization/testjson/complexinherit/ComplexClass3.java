package com.tngtech.archunit.htmlvisualization.testjson.complexinherit;

@SuppressWarnings("unused")
public class ComplexClass3 extends ComplexClass2 {
    public ComplexClass3(String name) {
        super(name);
    }

    @Override
    public void sayHelloAndBye() {
        super.sayHelloAndBye();
    }
}
