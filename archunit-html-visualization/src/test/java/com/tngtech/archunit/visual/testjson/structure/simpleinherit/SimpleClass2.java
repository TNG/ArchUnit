package com.tngtech.archunit.visual.testjson.structure.simpleinherit;

public class SimpleClass2 {
    protected String name;

    public SimpleClass2(String name) {
        this.name = name;
    }

    public void sayHelloAndBye() {
        SimpleClass1 c = new SimpleClass1("Hi" + " " + name);
        c.sayHello(42);
        c.sayHi();
        System.out.println("Bye" + " " + name);
    }
}
