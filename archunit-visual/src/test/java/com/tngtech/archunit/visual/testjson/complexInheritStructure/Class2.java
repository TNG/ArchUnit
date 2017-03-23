package com.tngtech.archunit.visual.testjson.complexInheritStructure;

public class Class2 {
    protected String name;

    public Class2(String name) {
        this.name = name;
    }

    public void sayHelloAndBye() {
        Class1 c = new Class1("Hi" + " " + name);
        c.sayHello();
        c.sayHi();
        System.out.println("Bye" + " " + name);
        Interface1 i = new Interface1() {
            @Override
            public void sayHello() {

            }
        };
        //i.sayHello();
    }
}
