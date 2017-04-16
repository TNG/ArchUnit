package com.tngtech.archunit.visual.testjson.structure.simpleinherit;

public class SimpleClass1 implements SimpleInterface1 {
    private String s;

    public SimpleClass1(String s) {
        this.s = s;
    }

    @Override
    public void sayHello() {
        System.out.println(this.s);
        InnerClass1 c = new InnerClass1();
        c.sayHello();
    }

    public void sayHi() {
        System.out.println(this.s);
    }

    class InnerClass1 implements SimpleInterface1 {
        @Override
        public void sayHello() {
            //sayHi();
            SimpleClass1.this.sayHello();
        }
    }
}
