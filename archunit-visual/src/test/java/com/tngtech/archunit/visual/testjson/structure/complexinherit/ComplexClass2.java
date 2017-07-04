package com.tngtech.archunit.visual.testjson.structure.complexinherit;

public class ComplexClass2 {
    protected String name;

    public ComplexClass2(String name) {
        this.name = name;
    }

    public void sayHelloAndBye() {
        ComplexClass1 c = new ComplexClass1("Hi" + " " + name);
        c.sayHello();
        c.sayHi();
        System.out.println("Bye" + " " + name);
        ComplexInterface1 i = new ComplexInterface1() {
            @Override
            public void sayHello() {
                //ensure that this access is ignored by the exporter
                ComplexClass2 complexClass2 = new ComplexClass2("Thorsten");
            }
        };
    }
}
