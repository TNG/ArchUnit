package com.tngtech.archunit.visual.testjson.simpleInheritStructure;

public class Class1 implements Interface1 {
    private String s;

    public Class1(String s) {
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

    class InnerClass1 implements com.tngtech.archunit.visual.testjson.complexInheritStructure.Interface1 {

        @Override
        public void sayHello() {
            Class1.this.sayHello();
        }
    }
}
