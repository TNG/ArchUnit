package com.tngtech.archunit.htmlvisualization.testjson.simpleinherit.sub;

@SuppressWarnings("unused")
public class SimpleClass3 extends SimpleClass2 {
    public SimpleClass3(String name) {
        super(name);
    }

    @Override
    public void sayHelloAndBye() {
        System.out.println(name);
    }
}
