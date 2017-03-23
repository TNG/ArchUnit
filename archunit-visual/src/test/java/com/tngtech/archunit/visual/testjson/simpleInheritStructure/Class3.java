package com.tngtech.archunit.visual.testjson.simpleInheritStructure;

public class Class3 extends Class2 {
    public Class3(String name) {
        super(name);
    }

    @Override
    public void sayHelloAndBye() {
        //super.sayHelloAndBye();
        System.out.println(name);
    }
}
